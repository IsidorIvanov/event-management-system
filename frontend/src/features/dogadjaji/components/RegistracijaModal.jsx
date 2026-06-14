import { useState, useEffect } from 'react';
import * as tipKarteApi from '@/features/dogadjaji/services/tipKarteService';
import * as registracijaApi from '@/features/dogadjaji/services/registracijaService';

const VRSTA_LABEL = {
  VISEDNEVNA: 'Full Pass',
  JEDNODNEVNA: 'Day Pass',
  POJEDINACNA_SESIJA: 'Session Pass',
  BESPLATNA: 'Free',
};

export default function RegistracijaModal({ event, onClose, onSuccess }) {
  const [karte, setKarte] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selected, setSelected] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    tipKarteApi
      .getTipKarteByDogadjaj(event.dogadjajId)
      .then((res) => {
        setKarte(res.data);
        if (res.data.length > 0) setSelected(res.data[0].nazivTipa);
      })
      .catch(() => setError('Greška pri učitavanju tipova karata.'))
      .finally(() => setLoading(false));
  }, [event.dogadjajId]);

  const handleSubmit = async () => {
    if (!selected) return;
    setSubmitting(true);
    setError(null);
    try {
      const res = await registracijaApi.register({
        dogadjajId: event.dogadjajId,
        nazivTipa: selected,
      });
      onSuccess(res.data);
    } catch (e) {
      setError(e?.response?.data?.message || e?.response?.data || 'Greška pri registraciji.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-card" style={{ maxWidth: 480 }} onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h3 className="modal-title">Register for {event.naziv}</h3>
          <button className="modal-close" onClick={onClose}>✕</button>
        </div>

        {loading ? (
          <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>Učitavanje karata...</p>
        ) : karte.length === 0 ? (
          <div className="ucesnik-empty-box">
            <p>Nema dostupnih tipova karata za ovaj događaj.</p>
          </div>
        ) : (
          <>
            <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', marginBottom: '1rem' }}>
              Izaberite tip karte:
            </p>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.6rem', marginBottom: '1.5rem' }}>
              {karte.map((k) => (
                <label
                  key={k.nazivTipa}
                  className={`reg-ticket-option${selected === k.nazivTipa ? ' selected' : ''}`}
                  onClick={() => setSelected(k.nazivTipa)}
                >
                  <div className="reg-ticket-radio">
                    <div className={`reg-ticket-dot${selected === k.nazivTipa ? ' active' : ''}`} />
                  </div>
                  <div className="reg-ticket-info">
                    <div className="reg-ticket-name">{k.nazivTipa}</div>
                    <div className="reg-ticket-meta">
                      {VRSTA_LABEL[k.vrsta] || k.vrsta}
                      {k.opis && ` · ${k.opis}`}
                    </div>
                  </div>
                  <div className="reg-ticket-price">
                    {k.vrsta === 'BESPLATNA' ? (
                      <span style={{ color: 'var(--success)' }}>Free</span>
                    ) : (
                      <span>{Number(k.cena).toLocaleString('sr-Latn')} RSD</span>
                    )}
                  </div>
                </label>
              ))}
            </div>

            {error && (
              <div className="form-error" style={{ marginBottom: '1rem' }}>{error}</div>
            )}

            <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'flex-end' }}>
              <button className="btn btn-outline" onClick={onClose} disabled={submitting}>
                Otkaži
              </button>
              <button
                className="discover-btn-register"
                style={{ padding: '0.55rem 1.25rem', fontSize: '0.9rem', fontWeight: 600 }}
                onClick={handleSubmit}
                disabled={submitting || !selected}
              >
                {submitting ? 'Registracija...' : 'Confirm Registration'}
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  );
}

