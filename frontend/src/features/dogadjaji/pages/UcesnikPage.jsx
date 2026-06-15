import { useState, useEffect } from 'react';
import { useAuth } from '@/features/auth/context/AuthContext';
import { useNavigate } from 'react-router-dom';
import * as registracijaApi from '@/features/dogadjaji/services/registracijaService';
import { NOTIF_NEW_EVENT } from '@/features/notifikacije/hooks/useNotifikacije';
import { formatDate } from '@/shared/utils/format';

const STATUS_KARTE_BADGE = {
  VALIDNA:     { label: 'Validna',     color: 'var(--success)' },
  NEVAZECA:    { label: 'Nevažeća',    color: 'var(--text-muted)' },
  ISKORISCENA: { label: 'Iskorišćena', color: 'var(--text-muted)' },
  NA_CEKANJU:  { label: 'Na čekanju',  color: 'var(--warning)' },
};

const STATUS_DOGADJAJA_BADGE = {
  OBJAVLJEN: { label: 'Objavljen', cls: 'status-badge status-published' },
  AKTIVAN:   { label: 'Aktivan',   cls: 'status-badge status-ongoing' },
  DRAFT:     { label: 'Nacrt',     cls: 'status-badge status-draft' },
  ZAVRSEN:   { label: 'Završen',   cls: 'status-badge status-finished' },
};

export default function UcesnikPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [registracije, setRegistracije] = useState([]);
  const [loading, setLoading] = useState(true);
  const [cancellingId, setCancellingId] = useState(null);
  const [confirmCancel, setConfirmCancel] = useState(null);

  const loadRegistrations = () => {
    setLoading(true);
    registracijaApi
      .getMyRegistrations()
      .then((res) => setRegistracije(res.data))
      .catch(() => setRegistracije([]))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    loadRegistrations();
  }, []);

  // Osveži registracije kada stigne notifikacija (npr. promocija sa liste čekanja — D3).
  useEffect(() => {
    const onNotif = () => loadRegistrations();
    window.addEventListener(NOTIF_NEW_EVENT, onNotif);
    return () => window.removeEventListener(NOTIF_NEW_EVENT, onNotif);
  }, []);

  const handleCancel = (e, r) => {
    e.stopPropagation();
    setConfirmCancel(r);
  };

  const confirmCancelRegistration = () => {
    if (!confirmCancel) return;
    setCancellingId(confirmCancel.registracijaId);
    setConfirmCancel(null);
    registracijaApi
      .cancelRegistration(confirmCancel.registracijaId)
      .then(() => {
        setRegistracije((prev) =>
          prev.map((r) =>
            r.registracijaId === confirmCancel.registracijaId
              ? { ...r, status: 'OTKAZANA', statusKarte: 'NEVAZECA' }
              : r
          )
        );
      })
      .catch(() => {})
      .finally(() => setCancellingId(null));
  };

  const active = registracije.filter((r) => r.status !== 'OTKAZANA');

  return (
    <div className="ucesnik-page">
      {/* Confirm cancel modal */}
      {confirmCancel && (
        <div className="modal-overlay" onClick={() => setConfirmCancel(null)}>
          <div className="modal-box" onClick={(e) => e.stopPropagation()}>
            <div className="modal-icon">⚠️</div>
            <h3 className="modal-title">Otkaži registraciju</h3>
            <p className="modal-body">
              Da li ste sigurni da želite da otkažete registraciju za{' '}
              <strong>„{confirmCancel.dogadjajNaziv}"</strong>?
              <br />
              <span style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>
                Ova akcija se ne može poništiti.
              </span>
            </p>
            <div className="modal-actions">
              <button className="btn btn-outline" onClick={() => setConfirmCancel(null)}>
                Nazad
              </button>
              <button className="btn btn-danger" onClick={confirmCancelRegistration}>
                Otkaži registraciju
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Header */}
      <div className="ucesnik-header">
        <h1 className="ucesnik-welcome">Dobrodošli, {user?.ime}!</h1>
        <p className="ucesnik-subtitle">vaši nadolazeći događaji i registracije</p>
      </div>

      {/* Heads-up */}
      {active.length > 0 && (
        <div className="ucesnik-headsup">
          <span className="ucesnik-headsup-dot" />
          <span className="ucesnik-headsup-text">
            <strong>Registrovani ste</strong> na {active.length}{' '}
            {active.length === 1 ? 'događaj' : 'događaja'}.{' '}
          </span>
          <button
            className="ucesnik-headsup-view btn btn-outline btn-sm"
            onClick={() => navigate('/dashboard/otkrijte')}
          >
            Otkrijte više
          </button>
        </div>
      )}

      {/* My registrations */}
      <section className="ucesnik-section">
        <h2 className="ucesnik-section-title">Moji događaji</h2>
        {loading ? (
          <p className="ucesnik-empty">Učitavanje...</p>
        ) : active.length === 0 ? (
          <div className="ucesnik-empty-box">
            <p>Niste registrovani ni na jedan događaj.</p>
            <p style={{ fontSize: '0.85rem', marginTop: '0.5rem', color: 'var(--text-muted)' }}>
              Pogledajte sekciju <strong>Otkrijte događaje</strong> da se prijavite.
            </p>
            <button
              className="discover-btn-register"
              style={{ marginTop: '1rem', padding: '0.5rem 1.25rem', fontSize: '0.9rem' }}
              onClick={() => navigate('/dashboard/otkrijte')}
            >
              Otkrijte događaje
            </button>
          </div>
        ) : (
          <div className="ucesnik-events-grid">
            {active.map((r) => {
              const kartaBadge = STATUS_KARTE_BADGE[r.statusKarte] || {};
              const dogadjajBadge = STATUS_DOGADJAJA_BADGE[r.dogadjajStatus] || null;
              const isCancelling = cancellingId === r.registracijaId;
              return (
                <div
                  key={r.registracijaId}
                  className="ucesnik-event-card"
                  onClick={() => navigate(`/dashboard/dogadjaj/${r.dogadjajId}`)}
                >
                  <div className="ucesnik-event-info">
                    <div className="ucesnik-event-name">{r.dogadjajNaziv}</div>
                    <div className="ucesnik-event-meta">
                      {formatDate(r.dogadjajDatumPocetka)}
                      {r.dogadjajDatumZavrsetka && r.dogadjajDatumZavrsetka !== r.dogadjajDatumPocetka
                        ? ` – ${formatDate(r.dogadjajDatumZavrsetka)}`
                        : ''}
                      {r.lokacijaGrad ? ` · ${r.lokacijaGrad}` : ''}
                      {r.lokacijaDrzava ? `, ${r.lokacijaDrzava}` : ''}
                    </div>
                    <div className="ucesnik-event-ticket">🎫 {r.nazivTipa}</div>
                    {r.brojKarte && (
                      <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)', marginTop: '0.3rem' }}>
                        #{r.brojKarte}
                        {kartaBadge.label && (
                          <span style={{ marginLeft: '0.5rem', color: kartaBadge.color, fontWeight: 600 }}>
                            · {kartaBadge.label}
                          </span>
                        )}
                      </div>
                    )}
                    <div
                      style={{ marginTop: '0.75rem', display: 'flex', gap: '0.5rem' }}
                      onClick={(e) => e.stopPropagation()}
                    >
                      <button
                        className="btn btn-outline btn-xs"
                        onClick={() => navigate(`/dashboard/dogadjaj/${r.dogadjajId}`)}
                      >
                        Detalji
                      </button>
                      <button
                        className="btn btn-xs btn-danger-outline"
                        disabled={isCancelling}
                        onClick={(e) => handleCancel(e, r)}
                      >
                        {isCancelling ? 'Otkazivanje...' : 'Otkaži'}
                      </button>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </section>
    </div>
  );
}
