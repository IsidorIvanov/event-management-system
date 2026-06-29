import { useState, useEffect } from 'react';
import { useParams, useNavigate, useLocation } from 'react-router-dom';
import api from '@/shared/services/api';
import UpsertEventModal from '@/features/dogadjaji/components/UpsertEventModal';
import OverviewTab from '@/features/dogadjaji/components/event-detail/OverviewTab';
import SesijeTab from '@/features/dogadjaji/components/event-detail/SesijeTab';
import GovorniciTab from '@/features/dogadjaji/components/event-detail/GovorniciTab';
import KarteTab from '@/features/dogadjaji/components/event-detail/KarteTab';
import PrisustvoTab from '@/features/dogadjaji/components/event-detail/PrisustvoTab';
import IzvestajTab from '@/features/dogadjaji/components/event-detail/IzvestajTab';
import { useToast } from '@/shared/components/ToastNotification';
import { formatDate as fmtDate } from '@/shared/utils/format';
import { STATUS_DISPLAY, STATUS_CLASS } from '@/features/dogadjaji/constants';

const formatDate = (s) => fmtDate(s, { day: '2-digit', month: 'long', year: 'numeric' });

const TABS = ['Pregled', 'Sesije & Agenda', 'Govornici', 'Karte', 'Prisustvo', 'Izveštaj'];

export default function EventDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const toast = useToast();

  const [event, setEvent]       = useState(null);
  const [loading, setLoading]   = useState(true);
  const [error, setError]       = useState(null);
  const [activeTab, setActiveTab] = useState(() => {
    const i = TABS.indexOf(location.state?.tab);
    return i >= 0 ? i : 0;
  });
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
        {activeTab === 2 && <GovorniciTab event={event} />}
        {activeTab === 3 && <KarteTab event={event} />}
        {activeTab === 4 && <PrisustvoTab event={event} />}
        {activeTab === 5 && <IzvestajTab event={event} />}
      </div>
    </div>
  );
}
