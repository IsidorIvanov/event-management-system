import { formatDate as fmtDate } from '@/shared/utils/format';

const formatDate = (s) => fmtDate(s, { day: '2-digit', month: 'short', year: 'numeric' });

export default function InformacijeTab({ event, registered, onWaitlist, onRegister }) {
  return (
    <div className="ev-info-tab">
      {!registered ? (
        <div className="ev-not-registered-banner">
          <div className="ev-not-registered-text">
            <strong>Još niste registrovani</strong>
            <p>
              Izaberite kartu da se pridružite događaju {event.naziv}. Karte otključavaju
              agendu, listu učesnika i vašu QR propusnicu.
            </p>
          </div>
          <button className="discover-btn-register" onClick={onRegister}>
            Registruj se
          </button>
        </div>
      ) : onWaitlist ? (
        <div className="ev-not-registered-banner" style={{ background: 'rgba(245, 158, 11, 0.12)', borderColor: 'var(--warning)' }}>
          <div className="ev-not-registered-text">
            <strong style={{ color: 'var(--warning)' }}>⏳ Na listi čekanja</strong>
            <p>
              Događaj je trenutno popunjen, pa je vaša prijava na listi čekanja. Obavestićemo
              vas čim se oslobodi mesto i tada će vaša karta biti potvrđena.
            </p>
          </div>
        </div>
      ) : (
        <div className="ev-not-registered-banner" style={{ background: 'var(--success-subtle)', borderColor: 'var(--success)' }}>
          <div className="ev-not-registered-text">
            <strong style={{ color: 'var(--success)' }}>✓ Registrovani ste!</strong>
            <p>Vaša karta je potvrđena. Pogledajte svoje registracije za detalje.</p>
          </div>
        </div>
      )}

      <div className="ev-info-layout">
        <div className="ev-info-left">
          <div className="ev-cover-card">
            <div className="ev-about">
              <h3 className="ev-about-title">O ovom događaju</h3>
              <p className="ev-about-desc">
                {event.opis || 'Nema opisa za ovaj događaj.'}
              </p>
            </div>
          </div>

          <div className="ev-organizer-card">
            <div className="ev-organizer-label">ORGANIZATOR</div>
            <div className="ev-organizer-body">
              <div className="ev-organizer-logo" />
              <div className="ev-organizer-info">
                <div className="ev-organizer-name">
                  {event.lokacijaNaziv || 'Organizator'}
                  <span className="ev-verified-badge">Verifikovan</span>
                </div>
                <div className="ev-organizer-meta">
                  Sedište: {event.lokacijaGrad || '—'}
                </div>
                <div className="ev-organizer-desc">
                  Zvanični organizator događaja za ovaj prostor.
                </div>
              </div>
            </div>
          </div>
        </div>

        <div className="ev-info-right">
          <div className="ev-details-card">
            <h3 className="ev-details-title">Detalji</h3>
            <div className="ev-details-rows">
              <div className="ev-details-row">
                <span className="ev-details-label">datum</span>
                <span className="ev-details-value">{formatDate(event.datumPocetka)}</span>
              </div>
              <div className="ev-details-row">
                <span className="ev-details-label">format</span>
                <span className="ev-details-value">Uživo</span>
              </div>
              <div className="ev-details-row">
                <span className="ev-details-label">država</span>
                <span className="ev-details-value">{event.lokacijaDrzava || '—'}</span>
              </div>
              <div className="ev-details-row">
                <span className="ev-details-label">grad</span>
                <span className="ev-details-value">{event.lokacijaGrad || '—'}</span>
              </div>
              <div className="ev-details-row">
                <span className="ev-details-label">adresa</span>
                <span className="ev-details-value">
                  {event.lokacijaNaziv && event.lokacijaAdresa
                    ? `${event.lokacijaNaziv} · ${event.lokacijaAdresa}`
                    : event.lokacijaNaziv || event.lokacijaAdresa || '—'}
                </span>
              </div>
              {event.maksKapacitet && (
                <div className="ev-details-row">
                  <span className="ev-details-label">kapacitet</span>
                  <span className="ev-details-value">{event.maksKapacitet}</span>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
