import { useState, useEffect } from 'react';
import * as registracijaApi from '@/features/dogadjaji/services/registracijaService';

export default function UcesniciTab({ event }) {
  const [registracije, setRegistracije] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');

  useEffect(() => {
    registracijaApi
      .getRegistracijeByDogadjaj(event.dogadjajId)
      .then((res) => setRegistracije(res.data.filter((r) => r.status === 'POTVRDJENA')))
      .catch(() => setRegistracije([]))
      .finally(() => setLoading(false));
  }, [event.dogadjajId]);

  const filtered = registracije.filter((r) => {
    const q = search.toLowerCase();
    return (
      !q ||
      `${r.ucesnikIme || ''} ${r.ucesnikPrezime || ''}`.toLowerCase().includes(q) ||
      (r.ucesnikKompanija || '').toLowerCase().includes(q) ||
      (r.ucesnikPozicija || '').toLowerCase().includes(q)
    );
  });

  if (loading) return <p className="empty-hint">Učitavanje učesnika...</p>;

  return (
    <div className="govornici-tab">
      {/* Pretraga */}
      <div style={{ marginBottom: '1rem' }}>
        <input
          className="search-input"
          style={{ width: '100%', maxWidth: '360px' }}
          placeholder="pretraži učesnike..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
      </div>

      {filtered.length === 0 ? (
        <div className="sesije-empty">
          <p>Nema registrovanih učesnika za ovaj događaj.</p>
        </div>
      ) : (
        <div className="govornici-grid">
          {filtered.map((r) => (
            <div key={r.registracijaId} className="govornik-card">
              <div className="govornik-card-avatar">
                <div className="govornik-avatar-placeholder" />
              </div>
              <div className="govornik-card-body">
                <div className="govornik-card-name">
                  {r.ucesnikIme} {r.ucesnikPrezime}
                </div>
                <div className="govornik-card-meta">
                  {r.ucesnikPozicija && <span>{r.ucesnikPozicija}</span>}
                  {r.ucesnikPozicija && r.ucesnikKompanija && <span> · </span>}
                  {r.ucesnikKompanija && <span>{r.ucesnikKompanija}</span>}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
