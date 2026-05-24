import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import api from '../services/api';
import UpsertEventModal from './UpsertEventModal.jsx';
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
        {activeTab !== 0 && (
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




