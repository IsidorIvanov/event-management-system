import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import api from '../services/api';
import * as sesijaApi from '../services/sesijaService';

const formatDate = (s) =>
  s
    ? new Date(s).toLocaleDateString('en-US', {
        month: 'short',
        day: '2-digit',
        year: 'numeric',
      })
    : '—';

const TABS = ['Info', 'Agenda', 'Speakers', 'Participants'];

const TIP_LABEL = {
  KEYNOTE: 'Keynote',
  WORKSHOP: 'Workshop',
  PANEL: 'Panel',
  NETWORKING: 'Networking',
};

function fillBar(registered, capacity) {
  const pct = capacity > 0 ? Math.round((registered / capacity) * 100) : 0;
  const color = pct >= 100 ? 'var(--danger)' : pct >= 80 ? 'var(--warning)' : 'var(--accent)';
  return { pct, color };
}

function AgendaTab({ event }) {
  const [sesije, setSesije] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [filterDay, setFilterDay] = useState('all');
  const [filterTrack, setFilterTrack] = useState('all');
  const [filterRoom, setFilterRoom] = useState('all');

  useEffect(() => {
    sesijaApi.getSesijeByDogadjaj(event.dogadjajId)
      .then((res) => setSesije(res.data))
      .catch(() => setSesije([]))
      .finally(() => setLoading(false));
  }, [event.dogadjajId]);

  const days   = [...new Set(sesije.map((s) => s.datum))].sort();
  const tracks = [...new Set(sesije.map((s) => s.tip))];
  const rooms  = [...new Set(sesije.map((s) => s.nazivSale))];

  const filtered = sesije.filter((s) => {
    if (search && !s.naziv.toLowerCase().includes(search.toLowerCase())) return false;
    if (filterDay   !== 'all' && s.datum     !== filterDay)   return false;
    if (filterTrack !== 'all' && s.tip       !== filterTrack) return false;
    if (filterRoom  !== 'all' && s.nazivSale !== filterRoom)  return false;
    return true;
  });

  const grouped   = filtered.reduce((acc, s) => { (acc[s.datum] = acc[s.datum] || []).push(s); return acc; }, {});
  const sortedDays = Object.keys(grouped).sort();

  const dayLabel = (datum) => {
    const d     = new Date(datum + 'T00:00:00');
    const start = new Date(event.datumPocetka + 'T00:00:00');
    const dayNum = Math.round((d - start) / 86400000) + 1;
    return `Day ${dayNum} · ${d.toLocaleDateString('en-US', { month: 'short', day: 'numeric' })}`;
  };

  return (
    <div className="sesije-tab">
      {/* Filters */}
      <div className="sesije-toolbar">
        <div className="sesije-filters">
          <div className="sesija-filter-input">
            <span>🔍</span>
            <input
              placeholder="search..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="sesija-search"
            />
          </div>
          <select className="sesija-filter-select" value={filterDay} onChange={(e) => setFilterDay(e.target.value)}>
            <option value="all">day: all</option>
            {days.map((d) => <option key={d} value={d}>{dayLabel(d)}</option>)}
          </select>
          <select className="sesija-filter-select" value={filterTrack} onChange={(e) => setFilterTrack(e.target.value)}>
            <option value="all">track: all</option>
            {tracks.map((t) => <option key={t} value={t}>{TIP_LABEL[t] || t}</option>)}
          </select>
          <select className="sesija-filter-select" value={filterRoom} onChange={(e) => setFilterRoom(e.target.value)}>
            <option value="all">room: all</option>
            {rooms.map((r) => <option key={r} value={r}>{r}</option>)}
          </select>
        </div>
      </div>

      {/* Session list */}
      {loading ? (
        <p className="empty-hint">Loading sessions...</p>
      ) : filtered.length === 0 ? (
        <div className="sesije-empty">
          <p>No sessions found for this event.</p>
        </div>
      ) : (
        sortedDays.map((datum) => (
          <div key={datum} className="sesije-day-group">
            <div className="sesije-day-header">
              <span className="sesije-day-title">{dayLabel(datum)}</span>
              <span className="sesije-day-count">
                {grouped[datum].length} session{grouped[datum].length !== 1 ? 's' : ''}
              </span>
            </div>
            {grouped[datum].map((s) => {
              const { pct, color } = fillBar(0, s.kapacitet);
              const speakerText = s.govornici?.length > 0
                ? s.govornici.map((g) => `${g.ime} ${g.prezime}`).join(', ')
                : null;

              return (
                <div key={s.sesijaId} className="sesija-row">
                  <div className="sesija-row-left">
                    <div className="sesija-day-info">{dayLabel(s.datum)}</div>
                    <div className="sesija-time">{s.vremePocetka?.slice(0, 5)} - {s.vremeZavrsetka?.slice(0, 5)}</div>
                    <div className="sesija-room">
                      <span>📍</span> {s.nazivSale}
                    </div>
                    <span className="sesija-track-badge">{TIP_LABEL[s.tip] || s.tip}</span>
                  </div>

                  <div className="sesija-row-main">
                    <div className="sesija-title-row">
                      <span className="sesija-title">{s.naziv}</span>
                    </div>
                    {s.opis && <div className="sesija-opis">{s.opis}</div>}
                    <div className="sesija-meta-row">
                      <span>capacity: {s.kapacitet}</span>
                      <div className="sesija-fill-bar">
                        <div className="sesija-fill-bar-inner" style={{ width: `${pct}%`, background: color }} />
                      </div>
                      <span>{pct}%</span>
                    </div>
                    <div className="sesija-meta-row">
                      {speakerText && <span>Speaker: {speakerText}</span>}
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        ))
      )}
    </div>
  );
}

export default function UcesnikDogadjajDetaljPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [event, setEvent] = useState(null);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState('Info');

  useEffect(() => {
    api
      .get(`/dogadjaj/${id}`)
      .then((res) => setEvent(res.data))
      .catch(() => setEvent(null))
      .finally(() => setLoading(false));
  }, [id]);

  if (loading) {
    return <div className="ucesnik-empty" style={{ padding: '3rem' }}>Učitavanje...</div>;
  }

  if (!event) {
    return (
      <div className="ucesnik-empty-box" style={{ margin: '2rem 0' }}>
        <p>Događaj nije pronađen.</p>
        <button className="discover-btn-details" style={{ marginTop: '1rem' }} onClick={() => navigate(-1)}>
          ← Nazad
        </button>
      </div>
    );
  }

  return (
    <div className="ev-detail-page">
      {/* Top bar */}
      <div className="ev-detail-topbar">
        <button className="ev-back-btn" onClick={() => navigate(-1)}>
          ← Back
        </button>
        <button className="discover-btn-register ev-register-btn">
          Register for this event
        </button>
      </div>

      {/* Title + meta */}
      <div className="ev-detail-hero">
        <h1 className="ev-detail-title">{event.naziv}</h1>
        <p className="ev-detail-meta">
          {formatDate(event.datumPocetka)}
          {event.lokacijaGrad ? ` · ${event.lokacijaGrad}` : ''}
          {event.lokacijaDrzava ? `, ${event.lokacijaDrzava}` : ''}
          {' · In-person'}
        </p>
      </div>

      {/* Tabs */}
      <div className="ev-tabs">
        {TABS.map((tab) => (
          <button
            key={tab}
            className={`ev-tab-btn${activeTab === tab ? ' active' : ''}`}
            onClick={() => setActiveTab(tab)}
          >
            {tab}
          </button>
        ))}
      </div>

      {/* Tab content */}
      {activeTab === 'Info' && (
        <div className="ev-info-tab">
          <div className="ev-not-registered-banner">
            <div className="ev-not-registered-text">
              <strong>You're not registered yet</strong>
              <p>
                Pick a ticket to join {event.naziv}. Tickets unlock the agenda,
                attendee list, and your QR pass.
              </p>
            </div>
          </div>

          <div className="ev-info-layout">
            <div className="ev-info-left">
              <div className="ev-cover-card">
                <div className="ev-cover-placeholder">[ cover image ]</div>
                <div className="ev-about">
                  <h3 className="ev-about-title">About this event</h3>
                  <p className="ev-about-desc">
                    {event.opis || 'Nema opisa za ovaj događaj.'}
                  </p>
                </div>
              </div>

              <div className="ev-organizer-card">
                <div className="ev-organizer-label">ORGANIZED BY</div>
                <div className="ev-organizer-body">
                  <div className="ev-organizer-logo" />
                  <div className="ev-organizer-info">
                    <div className="ev-organizer-name">
                      {event.lokacijaNaziv || 'Organizator'}
                      <span className="ev-verified-badge">Verified</span>
                    </div>
                    <div className="ev-organizer-meta">
                      Based in {event.lokacijaGrad || '—'}
                    </div>
                    <div className="ev-organizer-desc">
                      Official event organizer for this venue.
                    </div>
                    <div className="ev-organizer-actions">
                      <button className="ev-org-btn">+ Follow</button>
                      <button className="ev-org-btn">View other events</button>
                      <button className="ev-org-btn">Contact organizer</button>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div className="ev-info-right">
              <div className="ev-details-card">
                <h3 className="ev-details-title">Details</h3>
                <div className="ev-details-rows">
                  <div className="ev-details-row">
                    <span className="ev-details-label">date</span>
                    <span className="ev-details-value">{formatDate(event.datumPocetka)}</span>
                  </div>
                  <div className="ev-details-row">
                    <span className="ev-details-label">format</span>
                    <span className="ev-details-value">In-person</span>
                  </div>
                  <div className="ev-details-row">
                    <span className="ev-details-label">country</span>
                    <span className="ev-details-value">{event.lokacijaDrzava || '—'}</span>
                  </div>
                  <div className="ev-details-row">
                    <span className="ev-details-label">city</span>
                    <span className="ev-details-value">{event.lokacijaGrad || '—'}</span>
                  </div>
                  <div className="ev-details-row">
                    <span className="ev-details-label">address</span>
                    <span className="ev-details-value">
                      {event.lokacijaNaziv && event.lokacijaAdresa
                        ? `${event.lokacijaNaziv} · ${event.lokacijaAdresa}`
                        : event.lokacijaNaziv || event.lokacijaAdresa || '—'}
                    </span>
                  </div>
                  {event.maksKapacitet && (
                    <div className="ev-details-row">
                      <span className="ev-details-label">capacity</span>
                      <span className="ev-details-value">{event.maksKapacitet}</span>
                    </div>
                  )}
                </div>
              </div>
            </div>
          </div>
        </div>
      )}

      {activeTab === 'Agenda' && <AgendaTab event={event} />}

      {activeTab === 'Speakers' && (
        <div className="ev-tab-placeholder">
          <p>Speakers list is not yet available.</p>
        </div>
      )}

      {activeTab === 'Participants' && (
        <div className="ev-tab-placeholder">
          <p>Participants list is not yet available.</p>
        </div>
      )}
    </div>
  );
}

