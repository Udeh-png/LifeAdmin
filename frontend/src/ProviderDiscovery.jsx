import { useEffect, useRef, useState } from 'react';
import { MapPin, Search, Star, ArrowRight, LoaderCircle, RotateCcw } from 'lucide-react';
import { salonProviders } from './data.js';
import { searchProviders } from './service.js';

const money = amount => '₦' + amount.toLocaleString();
const categories = [...new Set(salonProviders.map(provider => provider.category))];

export default function ProviderDiscovery({ onUseService }) {
  const [details, setDetails] = useState(null);
  const [query, setQuery] = useState('');
  const [location, setLocation] = useState('');
  const [category, setCategory] = useState('');
  const [budget, setBudget] = useState('');
  const [results, setResults] = useState(salonProviders);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [retryCount, setRetryCount] = useState(0);
  const latest = useRef(0);
  const hasFilters = query || location || category || budget;

  useEffect(() => {
    const requestId = ++latest.current;
    const timer = setTimeout(async () => {
      setLoading(true); setError('');
      try {
        const matches = await searchProviders({ query, location, category, maxPrice: budget });
        if (!Array.isArray(matches) || matches.some(provider => !provider?.id || !provider?.name)) throw new Error('Unexpected provider search response.');
        if (requestId === latest.current) setResults(matches);
      } catch {
        if (requestId === latest.current) setError('The sample provider search could not be completed.');
      } finally {
        if (requestId === latest.current) setLoading(false);
      }
    }, 140);
    return () => clearTimeout(timer);
  }, [query, location, category, budget, retryCount]);

  const clear = () => { setQuery(''); setLocation(''); setCategory(''); setBudget(''); setError(''); };

  return <section className="fade-in">
    <div className="page-title"><div className="eyebrow">DISCOVER</div><h1>Find the right help.</h1><p>Search fictional local providers by name, service, neighborhood, category, and budget.</p></div>
    <div className="discover-toolbar">
      <label className="search-field"><Search size={16}/><input aria-label="Search providers by name or service" value={query} placeholder="Search providers or services" onChange={event => setQuery(event.target.value)}/></label>
      <select aria-label="Filter providers by location" className="filter-select" value={location} onChange={event => setLocation(event.target.value)}><option value="">All locations</option><option value="Lekki">Lekki</option><option value="Victoria Island">Victoria Island</option><option value="Ikoyi">Ikoyi</option><option value="Ajah">Ajah</option></select>
      <select aria-label="Filter providers by service category" className="filter-select" value={category} onChange={event => setCategory(event.target.value)}><option value="">All categories</option>{categories.map(value => <option key={value} value={value}>{value}</option>)}</select>
      <label className="budget-filter">Max budget <span>₦</span><input aria-label="Maximum price in naira" type="number" min="0" value={budget} placeholder="Any" onChange={event => setBudget(event.target.value)}/></label>
      <button className="outline-button clear-filters" onClick={clear} disabled={!hasFilters}><RotateCcw size={12}/> Clear</button>
      <span className="demo-tag">SAMPLE DATA</span>
    </div>
    <div className="search-feedback" aria-live="polite">{loading ? <><LoaderCircle size={12} className="spin"/> Searching sample providers…</> : error ? 'Search needs attention' : results.length + ' sample providers found'}</div>
    {error ? <div className="panel empty-state"><h3>Couldn’t load providers</h3><p>{error}</p><button className="send-button" onClick={() => setRetryCount(value => value + 1)}>Try again</button></div> : loading ? <div className="provider-grid" aria-busy="true" aria-label="Loading providers">{[1,2,3,4].map(id => <div className="panel provider-skeleton" key={id}><div className="skeleton-line short"/><div className="skeleton-line"/><div className="skeleton-line medium"/></div>)}</div> : results.length === 0 ? <div className="panel empty-state"><span><Search size={21}/></span><h3>No providers match those filters</h3><p>Try a broader search, a different neighborhood, or a higher budget.</p><button className="outline-button" onClick={clear}>Clear all filters</button></div> : <div className="provider-grid">{results.map(provider => <article className="panel discover-card" key={provider.id}>
      <div className={'provider-art ' + provider.color}><span className="provider-avatar">{provider.initials}</span><span className="sample-chip">SAMPLE</span></div>
      <div className="discover-body"><div className="provider-title"><div><h3>{provider.name}</h3><span><MapPin size={13}/>{provider.area}</span></div><b><Star size={13} fill="currentColor"/>{provider.rating} <small>({provider.reviews} sample reviews)</small></b></div>
      <p>{provider.description}</p><div className="discover-meta"><span>{provider.category}</span><span>{provider.service}</span><span>{provider.duration}</span><span>{provider.availability ?? 'Sample availability'}</span></div>
      <div className="discover-bottom"><b>{money(provider.price)} <small>/ service</small></b><div className="discover-actions"><button className="outline-button" onClick={() => setDetails(provider)}>Details</button><button className="outline-button" onClick={() => onUseService(provider)}>Use in a task <ArrowRight size={14}/></button></div></div></div>
    </article>)}</div>}
    {details && <div className="detail-backdrop" role="presentation" onMouseDown={event => { if (event.target === event.currentTarget) setDetails(null); }}><section className="panel provider-detail" role="dialog" aria-modal="true" aria-labelledby="provider-detail-title"><button className="icon-btn detail-close" aria-label="Close provider details" onClick={() => setDetails(null)}>×</button><span className="sample-chip">SAMPLE LISTING</span><h2 id="provider-detail-title">{details.name}</h2><p>{details.description}</p><dl><div><dt>Location</dt><dd>{details.area}, Lagos</dd></div><div><dt>Category</dt><dd>{details.category}</dd></div><div><dt>Service</dt><dd>{details.service}</dd></div><div><dt>Sample price</dt><dd>{money(details.price)} / service</dd></div><div><dt>Duration</dt><dd>{details.duration}</dd></div><div><dt>Sample availability</dt><dd>{details.slots.join(' · ')}</dd></div></dl><small>Provider and availability information is fictional demo data. No reservation is made here.</small><button className="send-button" onClick={() => { setDetails(null); onUseService(details); }}>Use in a task <ArrowRight size={14}/></button></section></div>}
  </section>;
}
