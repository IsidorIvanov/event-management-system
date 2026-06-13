import { useState, useEffect, useMemo } from 'react';
import api from '../services/api';

const formatDate = (s) =>
  s
    ? new Date(s).toLocaleDateString('sr-Latn', {
        day: '2-digit',
        month: 'short',
        year: 'numeric',
      })
    : '—';

export default function OtkrijteDogadjajePageUcesnik() {
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [cityFilter, setCityFilter] = useState('any');

  useEffect(() => {
    api
      .get('/dogadjaj')
      .then((res) => {
        const published = res.data
          .filter((e) => e.status === 'OBJAVLJEN' || e.status === 'AKTIVAN')
          .sort((a, b) => new Date(a.datumPocetka) - new Date(b.datumPocetka));
        setEvents(published);
      })
      .catch(() => setEvents([]))
      .finally(() => setLoading(false));
  }, []);

  const cities = useMemo(() => {
    const set = new Set(events.map((e) => e.lokacijaGrad).filter(Boolean));
    return Array.from(set).sort();
  }, [events]);

  const filtered = events.filter((e) => {
    const matchSearch = [e.naziv, e.lokacijaGrad, e.lokacijaDrzava, e.opis]
      .filter(Boolean)
      .some((s) => s.toLowerCase().includes(search.toLowerCase()));
    const matchCity = cityFilter === 'any' || e.lokacijaGrad === cityFilter;
    return matchSearch && matchCity;
  });

  return (
    <div className="discover-page">
      {/* Header row */}
      <div className="discover-header">
        <div>
          <h1 className="ucesnik-welcome">Otkrijte događaje</h1>
          <p className="ucesnik-subtitle">pregledajte šta dolazi</p>
        </div>
        <div className="discover-filters">
          <div className="discover-search-wrap">
            <span className="discover-search-icon">🔍</span>
            <input
              className="discover-search-input"
              placeholder="search"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
          </div>
          <select
            className="discover-filter-select"
            value={cityFilter}
            onChange={(e) => setCityFilter(e.target.value)}
          >
            <option value="any">city: any</option>
            {cities.map((c) => (
              <option key={c} value={c}>
                {c}
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* Cards */}
      {loading ? (
        <p className="ucesnik-empty">Učitavanje...</p>
      ) : filtered.length === 0 ? (
        <div className="ucesnik-empty-box">
          <p>Nema dostupnih događaja.</p>
        </div>
      ) : (
        <div className="discover-grid">
          {filtered.map((event) => (
            <div key={event.dogadjajId} className="discover-card">
              <div className="discover-card-body">
                <div className="discover-card-name">{event.naziv}</div>
                <div className="discover-card-meta">
                  {formatDate(event.datumPocetka)}
                  {event.lokacijaGrad ? ` · ${event.lokacijaGrad}` : ''}
                  {event.lokacijaDrzava ? `, ${event.lokacijaDrzava}` : ''}
                  {' · In-person'}
                </div>
                {event.opis && (
                  <p className="discover-card-desc">{event.opis}</p>
                )}
                <div className="discover-card-actions">
                  <button className="discover-btn-details">Details</button>
                  <button className="discover-btn-register">Register</button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
