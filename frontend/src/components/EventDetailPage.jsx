import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import api from '../services/api';
import * as sesijaApi from '../services/sesijaService';
import UpsertEventModal from './UpsertEventModal.jsx';
import SesijaModal from './SesijaModal.jsx';
import { useToast } from './ToastNotification';

const STATUS_DISPLAY = { OBJAVLJEN: 'Objavljen', AKTIVAN: 'Aktivan', DRAFT: 'Nacrt', ZAVRSEN: 'Završen' };
const STATUS_CLASS   = {
  OBJAVLJEN: 'status-badge status-published',
  AKTIVAN:   'status-badge status-ongoing',
  DRAFT:     'status-badge status-draft',
  ZAVRSEN:   'status-badge status-finished',
};

const formatDate = (s) => s
  ? new Date(s).toLocaleDateString('sr-Latn', { day: '2-digit', month: 'long', year: 'numeric' })
  : '—';

const TABS = ['Pregled', 'Sesije & Agenda', 'Govornici', 'Karte', 'Prisustvo', 'Izveštaj'];

export default function EventDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const toast = useToast();

  const [event, setEvent]       = useState(null);
  const [loading, setLoading]   = useState(true);
  const [error, setError]       = useState(null);
  const [activeTab, setActiveTab] = useState(0);
  const [showEdit, setShowEdit] = useState(false);

  useEffect(() => {
    api.get(`/dogadjaj/${id}`)
      .then(res => setEvent(res.data))
      .catch(() => setError('Greška pri učitavanju događaja.'))
      .finally(() => setLoading(false));
  }, [id]);

  if (loading) return <div className="event-detail-loading">Učitavanje...</div>;
  if (error)   return <div className="event-detail-loading" style={{ color: 'var(--danger)' }}>{error}</div>;
  if (!event)  return null;

  return (
    <div className="event-detail">
      {showEdit && (
        <UpsertEventModal
          event={event}
          onClose={() => setShowEdit(false)}
          onCreated={(updated) => {
            setEvent(updated);
            toast(`Događaj „${updated.naziv}" je uspešno izmenjen.`, 'success');
          }}
        />
      )}

      {/* Top bar */}
      <div className="event-detail-topbar">
        <button className="btn btn-outline btn-sm" onClick={() => navigate(-1)}>← Svi događaji</button>
        <div className="event-detail-actions">
          <button className="btn btn-outline" onClick={() => setShowEdit(true)}>Izmeni detalje</button>
        </div>
      </div>

      {/* Header */}
      <div className="event-detail-header">
        <h1 className="event-detail-title">{event.naziv}</h1>
        <div className="event-detail-meta">
          <span>{formatDate(event.datumPocetka)}</span>
          {event.datumZavrsetka !== event.datumPocetka && (
            <span> — {formatDate(event.datumZavrsetka)}</span>
          )}
          <span className="meta-sep">·</span>
          <span>{event.lokacijaGrad}, {event.lokacijaDrzava}</span>
          <span className="meta-sep">·</span>
          <span className={STATUS_CLASS[event.status] || 'status-badge status-draft'}>
            {STATUS_DISPLAY[event.status] || event.status}
          </span>
        </div>
      </div>

      {/* Tabs */}
      <div className="event-detail-tabs">
        {TABS.map((tab, i) => (
          <button
            key={tab}
            className={`tab-btn ${activeTab === i ? 'active' : ''}`}
            onClick={() => setActiveTab(i)}
          >
            {tab}
          </button>
        ))}
      </div>

      {/* Tab content */}
      <div className="event-detail-content">
        {activeTab === 0 && <OverviewTab event={event} />}
        {activeTab === 1 && <SesijeTab event={event} />}
        {activeTab !== 0 && activeTab !== 1 && (
          <div className="tab-placeholder">
            <p>🚧 Sekcija „{TABS[activeTab]}" je u razvoju.</p>
          </div>
        )}
      </div>
    </div>
  );
}

function OverviewTab({ event }) {
  return (
    <div className="overview-layout">
      {/* Left — About */}
      <div className="overview-about">
        <div className="events-table-card" style={{ padding: '1.5rem' }}>
          <h3 style={{ marginBottom: '1.25rem' }}>O događaju</h3>

          <div className="event-info-grid">
            <div className="event-info-row">
              <span className="event-info-label">Naziv</span>
              <span className="event-info-value">{event.naziv}</span>
            </div>
            <div className="event-info-row">
              <span className="event-info-label">Lokacija</span>
              <span className="event-info-value">{event.lokacijaNaziv}</span>
            </div>
            <div className="event-info-row">
              <span className="event-info-label">Grad</span>
              <span className="event-info-value">{event.lokacijaGrad}, {event.lokacijaDrzava}</span>
            </div>
            <div className="event-info-row">
              <span className="event-info-label">Datum početka</span>
              <span className="event-info-value">{formatDate(event.datumPocetka)}</span>
            </div>
            <div className="event-info-row">
              <span className="event-info-label">Datum završetka</span>
              <span className="event-info-value">{formatDate(event.datumZavrsetka)}</span>
            </div>
            <div className="event-info-row">
              <span className="event-info-label">Status</span>
              <span className={STATUS_CLASS[event.status] || 'status-badge status-draft'}>
                {STATUS_DISPLAY[event.status] || event.status}
              </span>
            </div>
            {event.opis && (
              <div className="event-info-row event-info-row--full">
                <span className="event-info-label">Opis</span>
                <span className="event-info-value" style={{ lineHeight: 1.7 }}>{event.opis}</span>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Right — Stats + Quick actions */}
      <div className="overview-sidebar">
        <div className="overview-stats">
          <div className="info-card">
            <div className="label">Kapacitet</div>
            <div className="value accent">{event.maksKapacitet}</div>
            <div className="card-hint">maksimalno mesta</div>
          </div>
          <div className="info-card">
            <div className="label">Sesije</div>
            <div className="value success">—</div>
            <div className="card-hint">planirane sesije</div>
          </div>
          <div className="info-card">
            <div className="label">Govornici</div>
            <div className="value warning">—</div>
            <div className="card-hint">potvrđeni</div>
          </div>
          <div className="info-card">
            <div className="label">Prodate karte</div>
            <div className="value">—</div>
            <div className="card-hint">od ukupnog kapaciteta</div>
          </div>
        </div>

        <div className="events-table-card" style={{ padding: '1.5rem' }}>
          <h3 style={{ marginBottom: '1rem' }}>Brze akcije</h3>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
            <button className="btn btn-outline" style={{ justifyContent: 'flex-start' }}>
              📧 Pošalji email svim učesnicima
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

function ConfirmModal({ naziv, onConfirm, onCancel }) {
  if (!naziv) return null;
  return (
    <div className="modal-overlay" onClick={onCancel}>
      <div className="modal-box" onClick={e => e.stopPropagation()}>
        <div className="modal-icon">🗑️</div>
        <h3 className="modal-title">Obriši sesiju</h3>
        <p className="modal-body">
          Da li ste sigurni da želite da obrišete sesiju <strong>„{naziv}"</strong>?<br />
          <span style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>Ova akcija se ne može poništiti.</span>
        </p>
        <div className="modal-actions">
          <button className="btn btn-outline" onClick={onCancel}>Otkaži</button>
          <button className="btn btn-danger" onClick={onConfirm}>Obriši</button>
        </div>
      </div>
    </div>
  );
}

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

function SesijeTab({ event }) {
  const toast = useToast();
  const [sesije, setSesije] = useState([]);
  const [loading, setLoading] = useState(true);
  const [modal, setModal] = useState(null); // null | 'add' | { sesija }
  const [search, setSearch] = useState('');
  const [filterDay, setFilterDay] = useState('all');
  const [filterTrack, setFilterTrack] = useState('all');
  const [filterRoom, setFilterRoom] = useState('all');
  const [confirmDelete, setConfirmDelete] = useState(null);

  const load = async () => {
    setLoading(true);
    try {
      const res = await sesijaApi.getSesijeByDogadjaj(event.dogadjajId);
      setSesije(res.data);
    } catch {
      toast('Greška pri učitavanju sesija.', 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, [event.dogadjajId]);

  const handleSave = async (data) => {
    if (modal?.sesija) {
      await sesijaApi.updateSesija(modal.sesija.sesijaId, data);
      toast('Sesija je izmenjena.', 'success');
    } else {
      await sesijaApi.createSesija(data);
      toast('Sesija je dodana.', 'success');
    }
    setModal(null);
    await load();
  };

  const handleDelete = async (s) => {
    setConfirmDelete(s);
  };

  const confirmDeleteHandler = async () => {
    if (!confirmDelete) return;
    try {
      await sesijaApi.deleteSesija(confirmDelete.sesijaId);
      toast('Sesija je obrisana.', 'success');
      await load();
    } catch {
      toast('Greška pri brisanju sesije.', 'error');
    } finally {
      setConfirmDelete(null);
    }
  };

  // unique days/tracks/rooms for filters
  const days = [...new Set(sesije.map((s) => s.datum))].sort();
  const tracks = [...new Set(sesije.map((s) => s.tip))];
  const rooms = [...new Set(sesije.map((s) => s.nazivSale))];

  const filtered = sesije.filter((s) => {
    if (search && !s.naziv.toLowerCase().includes(search.toLowerCase())) return false;
    if (filterDay !== 'all' && s.datum !== filterDay) return false;
    if (filterTrack !== 'all' && s.tip !== filterTrack) return false;
    if (filterRoom !== 'all' && s.nazivSale !== filterRoom) return false;
    return true;
  });

  // group by day
  const grouped = filtered.reduce((acc, s) => {
    (acc[s.datum] = acc[s.datum] || []).push(s);
    return acc;
  }, {});
  const sortedDays = Object.keys(grouped).sort();

  // stats
  const total = sesije.length;

  // Day label helper: "Day 1 · May 22" style
  const dayLabel = (datum) => {
    const d = new Date(datum + 'T00:00:00');
    const start = new Date(event.datumPocetka + 'T00:00:00');
    const dayNum = Math.round((d - start) / 86400000) + 1;
    const monthDay = d.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
    return `Day ${dayNum} · ${monthDay}`;
  };

  return (
    <div className="sesije-tab">
      {/* Stats cards */}
      <div className="sesije-stats">
        <div className="sesija-stat-card">
          <div className="sesija-stat-label">Sessions</div>
          <div className="sesija-stat-value">{total}</div>
        </div>
        <div className="sesija-stat-card">
          <div className="sesija-stat-label">Keynotes</div>
          <div className="sesija-stat-value">{sesije.filter(s => s.tip === 'KEYNOTE').length}</div>
        </div>
        <div className="sesija-stat-card">
          <div className="sesija-stat-label">Workshops</div>
          <div className="sesija-stat-value">{sesije.filter(s => s.tip === 'WORKSHOP').length}</div>
        </div>
        <div className="sesija-stat-card">
          <div className="sesija-stat-label">Avg fill rate</div>
          <div className="sesija-stat-value">—</div>
        </div>
      </div>

      {/* Toolbar */}
      <div className="sesije-toolbar">
        <div className="sesije-filters">
          <div className="sesija-filter-input">
            <span className="filter-icon">🔍</span>
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
        <button className="btn btn-primary sesija-add-btn" onClick={() => setModal('add')} style={{ width: 'auto' }}>
          + Add Session
        </button>
      </div>

      {/* Session list */}
      {loading ? (
        <p className="empty-hint">Učitavanje sesija...</p>
      ) : filtered.length === 0 ? (
        <div className="sesije-empty">
          <p>Nema sesija za ovaj događaj.</p>
          <button className="btn btn-primary" onClick={() => setModal('add')} style={{ width: 'auto', marginTop: '0.5rem' }}>
            + Add Session
          </button>
        </div>
      ) : (
        sortedDays.map((datum) => (
          <div key={datum} className="sesije-day-group">
            <div className="sesije-day-header">
              <span className="sesije-day-title">{dayLabel(datum)}</span>
              <span className="sesije-day-count">{grouped[datum].length} session{grouped[datum].length !== 1 ? 's' : ''}</span>
            </div>
            {grouped[datum].map((s) => (
              <SesijaRow
                key={s.sesijaId}
                sesija={s}
                dayLabel={dayLabel(s.datum)}
                onEdit={() => setModal({ sesija: s })}
                onDelete={() => handleDelete(s)}
              />
            ))}
          </div>
        ))
      )}

      {(modal === 'add' || modal?.sesija) && (
        <SesijaModal
          dogadjajId={event.dogadjajId}
          event={event}
          sesija={modal?.sesija || null}
          onClose={() => setModal(null)}
          onSaved={handleSave}
        />
      )}

      <ConfirmModal
        naziv={confirmDelete?.naziv}
        onConfirm={confirmDeleteHandler}
        onCancel={() => setConfirmDelete(null)}
      />
    </div>
  );
}

function SesijaRow({ sesija, dayLabel, onEdit, onDelete }) {
  const { pct, color } = fillBar(0, sesija.kapacitet);
  const speakers = sesija.govornici;
  const speakerText = speakers && speakers.length > 0
    ? speakers.map((g) => `${g.ime} ${g.prezime}`).join(', ')
    : `${sesija.kapacitet} attendees`;

  return (
    <div className="sesija-row">
      <div className="sesija-row-left">
        <div className="sesija-day-info">{dayLabel}</div>
        <div className="sesija-time">{sesija.vremePocetka?.slice(0,5)} - {sesija.vremeZavrsetka?.slice(0,5)}</div>
        <div className="sesija-room">
          <span className="sesija-room-dot">📍</span> {sesija.nazivSale}
        </div>
        <span className="sesija-track-badge">{TIP_LABEL[sesija.tip] || sesija.tip}</span>
      </div>
      <div className="sesija-row-main">
        <div className="sesija-title-row">
          <span className="sesija-title">{sesija.naziv}</span>
        </div>
        {sesija.opis && <div className="sesija-opis">{sesija.opis}</div>}
        <div className="sesija-meta-row">
          <span className="sesija-speaker">Speaker: {speakerText}</span>
          <span className="sesija-capacity">capacity: {sesija.kapacitet}</span>
          <span className="sesija-registered">registered: — / {sesija.kapacitet}</span>
          <div className="sesija-fill-bar">
            <div
              className="sesija-fill-bar-inner"
              style={{ width: `${pct}%`, background: color }}
            />
          </div>
          <span className="sesija-pct">{pct}%</span>
        </div>
      </div>
      <div className="sesija-row-actions">
        <button className="btn btn-outline btn-sm" onClick={onEdit}>Edit</button>
        <button className="btn btn-outline btn-sm btn-danger-outline" onClick={onDelete}>Delete</button>
      </div>
    </div>
  );
}
