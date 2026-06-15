import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import * as sesijaApi from '@/features/dogadjaji/services/sesijaService';
import { NOTIF_NEW_EVENT } from '@/features/notifikacije/hooks/useNotifikacije';
import { useToast } from '@/shared/components/ToastNotification';

const TIP_LABEL = {
  KEYNOTE: 'Keynote',
  WORKSHOP: 'Radionica',
  PANEL: 'Panel',
  NETWORKING: 'Networking',
};

const formatDate = (datum) => {
  if (!datum) return '—';
  const d = new Date(datum + 'T00:00:00');
  return d.toLocaleDateString('sr-Latn', { day: '2-digit', month: 'short', year: 'numeric' });
};

const dayOfWeek = (datum) => {
  if (!datum) return '';
  const d = new Date(datum + 'T00:00:00');
  return d.toLocaleDateString('sr-Latn', { weekday: 'long' });
};

export default function MojRasporedPage() {
  const navigate = useNavigate();
  const toast = useToast();
  const [sesije, setSesije] = useState([]);
  const [loading, setLoading] = useState(true);
  const [removingId, setRemovingId] = useState(null);
  // Set of dogadjajId-s that are currently collapsed
  const [collapsed, setCollapsed] = useState(new Set());

  const load = () => {
    setLoading(true);
    sesijaApi.getMojRaspored()
      .then((res) => setSesije(res.data))
      .catch(() => setSesije([]))
      .finally(() => setLoading(false));
  };

  useEffect(() => { load(); }, []);

  // Osveži raspored kad stigne notifikacija (izmena/otkazivanje sesije — S2/S3).
  useEffect(() => {
    const onNotif = () => load();
    window.addEventListener(NOTIF_NEW_EVENT, onNotif);
    return () => window.removeEventListener(NOTIF_NEW_EVENT, onNotif);
  }, []);

  const handleRemove = async (e, sesijaId, naziv) => {
    e.stopPropagation();
    setRemovingId(sesijaId);
    try {
      await sesijaApi.removeFromRaspored(sesijaId);
      setSesije((prev) => prev.filter((s) => s.sesijaId !== sesijaId));
      toast(`Sesija "${naziv}" uklonjena iz rasporeda.`, 'info');
    } catch {
      toast('Došlo je do greške. Pokušajte ponovo.', 'error');
    } finally {
      setRemovingId(null);
    }
  };

  const toggleCollapse = (dogadjajId) => {
    setCollapsed((prev) => {
      const next = new Set(prev);
      if (next.has(dogadjajId)) next.delete(dogadjajId);
      else next.add(dogadjajId);
      return next;
    });
  };

  // Group by event, preserve insertion order → sort by event name
  const byEvent = sesije.reduce((acc, s) => {
    const key = s.dogadjajId;
    if (!acc[key]) acc[key] = { dogadjajId: s.dogadjajId, dogadjajNaziv: s.dogadjajNaziv, lokacijaGrad: s.lokacijaGrad, sesije: [] };
    acc[key].sesije.push(s);
    return acc;
  }, {});

  const eventGroups = Object.values(byEvent).sort((a, b) =>
    (a.dogadjajNaziv || '').localeCompare(b.dogadjajNaziv || '')
  );

  const renderSesijaCard = (s) => (
    <div
      key={s.sesijaId}
      className="raspored-sesija-card"
      onClick={() => navigate(`/dashboard/dogadjaj/${s.dogadjajId}`)}
      style={{ cursor: 'pointer' }}
    >
      <div className="raspored-sesija-time">
        <span>{s.vremePocetka?.slice(0, 5)}</span>
        <span className="raspored-sesija-time-sep">–</span>
        <span>{s.vremeZavrsetka?.slice(0, 5)}</span>
      </div>

      <div className="raspored-sesija-body">
        <div className="raspored-sesija-name">{s.naziv}</div>
        <div className="raspored-sesija-meta">
          <span className="sesija-track-badge" style={{ fontSize: '0.7rem' }}>
            {TIP_LABEL[s.tip] || s.tip}
          </span>
          {s.nazivSale && (
            <span className="raspored-sesija-room">📍 {s.nazivSale}</span>
          )}
        </div>
        {s.opis && <div className="raspored-sesija-opis">{s.opis}</div>}
        {s.govornici?.length > 0 && (
          <div className="raspored-sesija-speakers">
            🎤 {s.govornici.map((g) => `${g.ime} ${g.prezime}`).join(', ')}
          </div>
        )}
      </div>

      <div className="raspored-sesija-actions" onClick={(e) => e.stopPropagation()}>
        <button
          className="btn btn-xs btn-danger-outline"
          disabled={removingId === s.sesijaId}
          onClick={(e) => handleRemove(e, s.sesijaId, s.naziv)}
        >
          {removingId === s.sesijaId ? '...' : 'Ukloni'}
        </button>
      </div>
    </div>
  );

  const renderEventSesije = (eventSesije) => {
    const uniqueDates = [...new Set(eventSesije.map((s) => s.datum))].sort();
    const multiDay = uniqueDates.length > 1;

    if (!multiDay) {
      // Single day — just show the list (optionally with one date label)
      const sorted = [...eventSesije].sort((a, b) => (a.vremePocetka > b.vremePocetka ? 1 : -1));
      return (
        <div className="raspored-event-day-block">
          <div className="raspored-date-label">
            <span className="raspored-date-label-text">{formatDate(uniqueDates[0])}</span>
            <span className="raspored-date-label-dow">{dayOfWeek(uniqueDates[0])}</span>
          </div>
          <div className="raspored-sesije-list">
            {sorted.map(renderSesijaCard)}
          </div>
        </div>
      );
    }

    // Multiple days — group by date
    return uniqueDates.map((datum) => {
      const daySesije = [...eventSesije.filter((s) => s.datum === datum)]
        .sort((a, b) => (a.vremePocetka > b.vremePocetka ? 1 : -1));
      return (
        <div key={datum} className="raspored-event-day-block">
          <div className="raspored-date-label">
            <span className="raspored-date-label-text">{formatDate(datum)}</span>
            <span className="raspored-date-label-dow">{dayOfWeek(datum)}</span>
            <span className="raspored-date-label-count">{daySesije.length} ses.</span>
          </div>
          <div className="raspored-sesije-list">
            {daySesije.map(renderSesijaCard)}
          </div>
        </div>
      );
    });
  };

  return (
    <div className="ucesnik-page">
      <div className="ucesnik-header">
        <h1 className="ucesnik-welcome">📅 Moj raspored</h1>
        <p className="ucesnik-subtitle">vaše sesije i termini</p>
      </div>

      {loading ? (
        <p className="ucesnik-empty">Učitavanje...</p>
      ) : sesije.length === 0 ? (
        <div className="ucesnik-empty-box">
          <p>Nemate sesija u rasporedu.</p>
          <p style={{ fontSize: '0.85rem', marginTop: '0.5rem', color: 'var(--text-muted)' }}>
            Idite na stranicu događaja → tab <strong>Agenda</strong> i kliknite <strong>+ Raspored</strong> pored željene sesije.
          </p>
          <button
            className="discover-btn-register"
            style={{ marginTop: '1rem', padding: '0.5rem 1.25rem', fontSize: '0.9rem' }}
            onClick={() => navigate('/dashboard')}
          >
            Moji događaji
          </button>
        </div>
      ) : (
        <div className="raspored-container">
          <div className="raspored-topbar">
            <div className="ucesnik-headsup" style={{ margin: 0, width: '100%' }}>
              <span className="ucesnik-headsup-dot" />
              <span className="ucesnik-headsup-text">
                Imate <strong>{sesije.length}</strong>{' '}
                {sesije.length === 1 ? 'sesiju' : sesije.length < 5 ? 'sesije' : 'sesija'} u rasporedu
                {eventGroups.length > 1 ? ` · ${eventGroups.length} događaja` : ''}.
              </span>
            </div>
            {eventGroups.length > 1 && (
              <div className="raspored-expand-btns">
                <button
                  className="raspored-expand-btn"
                  onClick={() => setCollapsed(new Set())}
                >
                  ↕ Expand all
                </button>
                <button
                  className="raspored-expand-btn"
                  onClick={() => setCollapsed(new Set(eventGroups.map((eg) => eg.dogadjajId)))}
                >
                  ↕ Collapse all
                </button>
              </div>
            )}
          </div>

          {eventGroups.map((eg) => {
            const isCollapsed = collapsed.has(eg.dogadjajId);
            return (
              <div key={eg.dogadjajId} className="raspored-event-group">
                {/* Collapsible event header */}
                <div
                  className={`raspored-event-header${isCollapsed ? ' collapsed' : ''}`}
                  onClick={() => toggleCollapse(eg.dogadjajId)}
                  role="button"
                  tabIndex={0}
                  onKeyDown={(e) => e.key === 'Enter' && toggleCollapse(eg.dogadjajId)}
                >
                  <span className="raspored-event-chevron">{isCollapsed ? '▶' : '▼'}</span>
                  <span className="raspored-event-name">{eg.dogadjajNaziv || `Događaj #${eg.dogadjajId}`}</span>
                  {eg.lokacijaGrad && (
                    <span className="raspored-event-city">📍 {eg.lokacijaGrad}</span>
                  )}
                  <span className="raspored-event-count">
                    {eg.sesije.length} {eg.sesije.length === 1 ? 'sesija' : 'sesije/sesija'}
                  </span>
                  <button
                    className="raspored-event-goto"
                    onClick={(e) => { e.stopPropagation(); navigate(`/dashboard/dogadjaj/${eg.dogadjajId}`); }}
                    title="Idi na događaj"
                  >
                    →
                  </button>
                </div>

                {/* Collapsible body */}
                {!isCollapsed && (
                  <div className="raspored-event-body">
                    {renderEventSesije(eg.sesije)}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
