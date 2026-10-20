import { salonProviders } from './data.js';

const KEY = 'lifeadmin-demo-state-v1';
const initial = { tasks: [], bookings: [], reserved: [], proposals: [], approvals: [] };
const categories = [
  { pattern: /knotless|braid|salon|hair/i, service: 'Knotless braids', category: 'Hair salon' },
  { pattern: /clean|cleaner|housekeep/i, service: 'Home cleaning', category: 'Home cleaning' },
  { pattern: /tailor|alter|trouser|clothing repair/i, service: 'Trouser alterations', category: 'Tailoring' },
];

function normalizeState(value = {}) {
  const state = { ...initial, ...value, tasks: value.tasks ?? [], bookings: value.bookings ?? [], reserved: value.reserved ?? [], proposals: value.proposals ?? [], approvals: value.approvals ?? [] };
  // Migrate proposals created by the earlier demo version, which stored only a task status.
  for (const task of state.tasks.filter(item => item.status === 'awaiting_approval' && !item.proposalId)) {
    const provider = salonProviders.find(item => item.name === task.provider);
    if (!provider) continue;
    const proposalId = 'legacy-' + task.id;
    state.proposals.push({ id: proposalId, taskId: task.id, providerId: provider.id, provider: provider.name, service: task.service || provider.service, slot: task.slot?.split('· ')[1] || '', dateLabel: task.slot?.split(' · ')[0] || 'Tomorrow', price: task.price ?? provider.price, status: 'pending' });
    state.approvals.push({ id: 'approval-' + proposalId, proposalId, taskId: task.id, status: 'pending', decision: null });
    task.proposalId = proposalId;
  }
  return state;
}

export function loadState() {
  try { return normalizeState(JSON.parse(localStorage.getItem(KEY) ?? '{}')); }
  catch { return normalizeState(); }
}

export function saveState(state) {
  try { localStorage.setItem(KEY, JSON.stringify(normalizeState(state))); }
  catch { throw new Error('Demo data could not be saved in this browser. Check storage settings and try again.'); }
}
export function resetState() { localStorage.removeItem(KEY); }

export function parseRequest(text = '') {
  const match = categories.find(item => item.pattern.test(text));
  const budgetMatch = text.match(/(?:under|below|less than|max(?:imum)?(?: budget)?(?: of)?|budget(?: of)?|₦\s*)\s*₦?\s*([\d,]+)/i);
  const locationMatch = text.match(/lekki|victoria island|\bvi\b|ikoyi|ajah/i)?.[0] ?? '';
  const location = locationMatch.toLowerCase().replace(/\b\w/g, letter => letter.toUpperCase());
  const deadline = /before\s*3|by\s*3|3\s*pm/i.test(text) ? 'Before 3:00 PM' : 'Flexible';
  return {
    service: match?.service ?? 'Personal admin request',
    category: match?.category ?? '',
    location,
    budget: budgetMatch ? Number(budgetMatch[1].replaceAll(',', '')) : null,
    date: /tomorrow/i.test(text) ? 'Tomorrow' : 'Next available day',
    deadline,
    calendar: /calendar|clash|conflict/i.test(text) ? 'Calendar connection unavailable' : 'Calendar connection unavailable',
  };
}

export function interpretRequest(text) {
  const intent = parseRequest(text);
  return {
    ...intent,
    budget: intent.budget ? '₦' + intent.budget.toLocaleString() : 'Flexible',
    location: intent.location ? intent.location + ', Lagos' : 'Your preferred area',
  };
}

export async function searchProviders({ query = '', location = '', category = '', maxPrice = '', request = '', reserved = [] } = {}) {
  await new Promise(resolve => setTimeout(resolve, 180));
  if (/\[demo search error\]/i.test(request + ' ' + query)) throw new Error('Requested demo search failure');
  const intent = parseRequest(request);
  const searchTerm = (query || (request ? intent.service : '') || '').trim().toLowerCase();
  const terms = searchTerm.split(/\s+/).filter(word => word.length > 2 && !['find','the','for','tomorrow','under','before','with','where','around','please','service'].includes(word));
  const area = (location || intent.location).toLowerCase();
  const categoryFilter = (category || (!request ? intent.category : '')).toLowerCase();
  const budget = Number(maxPrice) || intent.budget || null;
  return salonProviders.filter(provider => {
    const haystack = [provider.name, provider.service, provider.category, provider.area, provider.description].join(' ').toLowerCase();
    const serviceFits = !intent.category || provider.category.toLowerCase() === intent.category.toLowerCase() || intent.category === 'Hair salon' && provider.category === 'Beauty studio';
    const textFits = !terms.length || terms.some(term => haystack.includes(term));
    const areaFits = !area || provider.area.toLowerCase().includes(area) || (area === 'victoria island' && /oniru/i.test(provider.area)) || Boolean(request && area === 'lekki' && /oniru|victoria island/i.test(provider.area));
    const categoryFits = !categoryFilter || provider.category.toLowerCase() === categoryFilter;
    const budgetFits = !maxPrice || provider.price <= Number(maxPrice);
    return serviceFits && textFits && areaFits && categoryFits && budgetFits;
  }).map(provider => ({
    ...provider,
    match: (!budget || provider.price <= budget) && (intent.deadline === 'Flexible' || !/finish after your 3 PM deadline/i.test(provider.reason)),
    reason: budget && provider.price > budget ? 'Over your ₦' + budget.toLocaleString() + ' budget' : provider.reason,
    availability: provider.slots.some(time => !reserved.includes(provider.id + ':' + time)) ? 'Sample slots available' : 'No sample slots available',
  }));
}

export async function getProvider({ providerId } = {}) {
  const provider = salonProviders.find(item => item.id === providerId);
  if (!provider) throw new Error('Provider was not found in the sample data.');
  return { ...provider, services: [{ id: provider.id + '-service', name: provider.service, price: provider.price, currency: 'NGN', duration: provider.duration }] };
}

export async function getAvailability({ providerId, serviceId, dateRange } = {}, state = loadState()) {
  const provider = await getProvider({ providerId });
  if (serviceId && !provider.services.some(item => item.id === serviceId)) throw new Error('Service was not found for this sample provider.');
  const rangeIncludesDemoDay = !dateRange || !dateRange.start || dateRange.start === 'tomorrow';
  if (!rangeIncludesDemoDay) return [];
  return provider.slots.map((slot, index) => ({ id: `${providerId}-slot-${index}`, slot, dateLabel: 'Tomorrow', available: !state.reserved.includes(provider.id + ':' + slot), source: 'demo' }));
}

export async function checkCalendarConflicts() {
  return { status: 'unavailable', conflicts: [], message: 'Calendar is disconnected. Conflicts were not checked.' };
}

export async function submitTask({ text } = {}, state = loadState()) {
  if (!text?.trim()) throw new Error('Task text is required.');
  const task = { id: 't-' + Date.now(), title: text.length > 66 ? text.slice(0, 63) + '…' : text, created: 'Just now', status: 'processing' };
  const next = normalizeState({ ...state, tasks: [task, ...state.tasks] });
  saveState(next);
  return { state: next, task };
}

export async function getTask({ taskId } = {}, state = loadState()) {
  const task = state.tasks.find(item => item.id === taskId);
  if (!task) throw new Error('Task was not found.');
  return { ...task, progress: task.progress ?? [] };
}

export async function createProposal({ taskId, providerId, slot } = {}, state = loadState()) {
  if (!taskId || !providerId || !slot) throw new Error('A task, provider, service and available time are required.');
  const provider = await getProvider({ providerId });
  const key = provider.id + ':' + slot;
  if (!provider.slots.includes(slot) || state.reserved.includes(key)) throw new Error('That sample time is unavailable. Choose another time.');
  const proposal = { id: 'prop-' + Date.now(), taskId, providerId, provider: provider.name, service: provider.service, slot, dateLabel: 'Tomorrow', price: provider.price, status: 'pending' };
  const approval = { id: 'approval-' + Date.now(), proposalId: proposal.id, taskId, status: 'pending', decision: null };
  const tasks = state.tasks.map(task => task.id === taskId ? { ...task, status: 'awaiting_approval', provider: provider.name, service: provider.service, slot: proposal.dateLabel + ' · ' + slot, price: provider.price, proposalId: proposal.id } : task);
  const next = normalizeState({ ...state, tasks, proposals: [proposal, ...state.proposals], approvals: [approval, ...state.approvals] });
  saveState(next);
  return { state: next, proposal, approval };
}

export async function getProposal({ proposalId } = {}, state = loadState()) {
  const proposal = state.proposals.find(item => item.id === proposalId);
  if (!proposal) throw new Error('Proposal was not found.');
  return proposal;
}

export async function listApprovals(state = loadState()) {
  return state.approvals.filter(item => item.status === 'pending');
}

export async function respondToApproval({ approvalId, decision } = {}, state = loadState()) {
  if (!['approve', 'reject'].includes(decision)) throw new Error('Choose approve or reject.');
  const approval = state.approvals.find(item => item.id === approvalId);
  if (!approval || approval.status !== 'pending') throw new Error('This approval is no longer pending. Refresh the task history and try again.');
  const proposal = await getProposal({ proposalId: approval.proposalId }, state);
  if (decision === 'reject') {
    const next = normalizeState({ ...state,
      tasks: state.tasks.map(task => task.id === approval.taskId ? { ...task, status: 'rejected' } : task),
      proposals: state.proposals.map(item => item.id === proposal.id ? { ...item, status: 'rejected' } : item),
      approvals: state.approvals.map(item => item.id === approvalId ? { ...item, status: 'rejected', decision } : item),
    });
    saveState(next);
    return { state: next, status: 'rejected' };
  }
  const next = normalizeState({ ...state,
    tasks: state.tasks.map(task => task.id === approval.taskId ? { ...task, status: 'approved' } : task),
    proposals: state.proposals.map(item => item.id === proposal.id ? { ...item, status: 'approved' } : item),
    approvals: state.approvals.map(item => item.id === approvalId ? { ...item, status: 'approved', decision } : item),
  });
  saveState(next);
  return { state: next, status: 'approved' };
}

export async function executeApprovedProposal({ approvalId } = {}, state = loadState()) {
  const approval = state.approvals.find(item => item.id === approvalId);
  if (!approval || approval.status !== 'approved') throw new Error('Only an approved proposal can be executed. Review the task status before continuing.');
  const proposal = await getProposal({ proposalId: approval.proposalId }, state);
  if (proposal.status !== 'approved') throw new Error('The proposal is not approved and cannot be executed.');
  const provider = await getProvider({ providerId: proposal.providerId });
  const bookingResult = buildBooking(state, approval.taskId, provider, proposal.slot);
  const next = normalizeState({ ...bookingResult.state,
    proposals: bookingResult.state.proposals.map(item => item.id === proposal.id ? { ...item, status: 'executed' } : item),
    approvals: bookingResult.state.approvals.map(item => item.id === approvalId ? { ...item, status: 'executed' } : item),
  });
  saveState(next);
  return { state: next, status: 'executed', booking: next.bookings[0] };
}

function buildBooking(state, taskId, provider, slot) {
  const slotKey = provider.id + ':' + slot;
  if (state.reserved.includes(slotKey)) throw new Error('That slot was just taken in this demo. Choose another time.');
  const ref = 'LA-' + Math.floor(100000 + Math.random() * 899999);
  const booking = { id: 'b-' + Date.now(), taskId, providerId: provider.id, provider: provider.name, service: provider.service, slot: 'Tomorrow · ' + slot, price: provider.price, status: 'Confirmed', ref, source: 'demo' };
  const tasks = state.tasks.map(task => task.id === taskId ? { ...task, status: 'completed', provider: provider.name, service: provider.service, slot: booking.slot, price: provider.price, ref, bookingId: booking.id } : task);
  const next = normalizeState({ ...state, tasks, bookings: [booking, ...state.bookings], reserved: [...state.reserved, slotKey] });
  return { state: next, booking };
}

export async function getBooking({ bookingId } = {}, state = loadState()) {
  const booking = state.bookings.find(item => item.id === bookingId);
  if (!booking) throw new Error('Booking was not found.');
  return booking;
}

export async function getTaskHistory(state = loadState()) {
  return [...state.tasks].sort((a, b) => (b.createdAt ?? b.id).localeCompare(a.createdAt ?? a.id));
}

export const service = Object.freeze({ submitTask, getTask, searchProviders, getProvider, getAvailability, checkCalendarConflicts, createProposal, getProposal, listApprovals, respondToApproval, executeApprovedProposal, getBooking, getTaskHistory });
export const apiConfig = Object.freeze({ mode: 'demo', baseUrl: import.meta.env?.VITE_API_BASE_URL ?? '' });
