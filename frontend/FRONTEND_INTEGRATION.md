# LifeAdmin frontend integration notes

The app currently runs with fictional provider data and browser-local demo state. It makes no backend requests. UI code should use the operations exported by `src/service.js`; it should not construct network requests directly.

`VITE_API_BASE_URL` is read into `apiConfig` but is not used to make requests. Set it in a local `.env` only after the backend team confirms the base URL and API contract. `.env.example` contains the empty setting. Do not put credentials or secrets in Vite client environment variables.

## Frontend service operations

These are frontend function contracts and required backend capabilities. They are **not** endpoint names or assumed HTTP methods.

| Frontend operation | Input | Result |
| --- | --- | --- |
| `submitTask({ text }, state?)` | Non-empty natural-language task | `{ state, task }`; task has stable `id`, title, created time and initial status |
| `getTask({ taskId }, state?)` | Task ID | Task fields and `progress[]` |
| `searchProviders({ query, location, category, maxPrice, request, reserved })` | Optional search constraints | Provider summaries with IDs, services, price, slots, match/exclusion explanation |
| `getProvider({ providerId })` | Provider ID | Provider details and `services[]` with service ID, name, price, currency and duration |
| `getAvailability({ providerId, serviceId?, dateRange? })` | Provider and optional service/date range | Dated/time-labelled slots with IDs and source |
| `createProposal({ taskId, providerId, slot }, state?)` | Task, provider and selected slot | `{ state, proposal, approval }`; proposal stays pending until a decision |
| `getProposal({ proposalId }, state?)` | Proposal ID | Proposed details and status |
| `listApprovals(state?)` | Optional state | Pending approval requests |
| `respondToApproval({ approvalId, decision }, state?)` | Approval ID and `approve` or `reject` | Updated state and decision status; **approval does not execute the booking** |
| `executeApprovedProposal({ approvalId }, state?)` | Previously approved request | Updated state and booking result; rejects unapproved, repeated, or unavailable execution |
| `getBooking({ bookingId }, state?)` | Booking ID | Booking details and status |
| `getTaskHistory(state?)` | Optional state | Tasks sorted newest first |
| `checkCalendarConflicts({ providerId, slot, dateLabel, duration })` | Proposed schedule | `{ status, conflicts, message }`; status may be `conflict`, `clear`, `unavailable`, or `failed` |

`state?` parameters exist to keep demo operations deterministic and testable. The app persists demo tasks, proposals, approvals, bookings and reserved sample slots in `localStorage`.

## Backend details to confirm

For every capability, provide the confirmed HTTP method and path, auth requirements, request and response schemas, error schema, status enums, pagination rules, and idempotency/retry behavior. Also confirm:

- Stable identifiers and how tasks, proposals, approvals and bookings relate.
- Provider service and price semantics, including currency, estimates, fees and price changes.
- Availability slot IDs, duration, time zone, freshness/expiry and how a slot is revalidated at execution.
- Approval authorization, expiration and whether approval and execution are separate operations. The backend must independently authorize and validate execution after approval.
- Calendar connected/unavailable state and conflict result shape. A failed check must not be represented as conflict-free.
- CORS and authentication setup before enabling a real adapter.

## Demo behavior and limits

Demo task submission, provider search, availability, proposals, approval/rejection, booking execution and history use local sample data. The demo requires two distinct actions: record approval, then choose **Create demo confirmation**. The latter is simulated; it does not reserve a real service or create a calendar event. The calendar adapter always reports disconnected/unavailable in demo mode. Sample events on the overview are examples and are not used for conflict checks.

No real API adapter is enabled yet. Do not replace the demo adapter with network requests until the backend contracts above are supplied. A real adapter must validate unexpected responses, surface failures and never turn a failed request into a successful simulated confirmation.
