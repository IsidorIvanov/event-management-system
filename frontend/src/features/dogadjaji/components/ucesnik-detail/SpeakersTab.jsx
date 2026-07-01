import { useState, useEffect } from 'react';
import * as sesijaApi from '@/features/dogadjaji/services/sesijaService';
import * as govornikApi from '@/features/dogadjaji/services/govornikService';

export default function SpeakersTab({ event }) {
  const [govornici, setGovornici] = useState([]);
  const [sesije, setSesije] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');

  useEffect(() => {
    Promise.all([
      govornikApi.getGovornikByDogadjaj(event.dogadjajId),
      sesijaApi.getSesijeByDogadjaj(event.dogadjajId),
    ])
      .then(([gRes, sRes]) => {
        setGovornici(gRes.data);
        setSesije(sRes.data);
      })
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [event.dogadjajId]);

  const getSesijeForGovornik = (g) =>
    sesije.filter(
      (s) =>
        s.govornici?.some((sg) => sg.govornikId === g.govornikId) ||
        (g.sesijaIds || []).includes(s.sesijaId),
    );

  const filtered = govornici.filter((g) => {
    const q = search.toLowerCase();
    return (
      !q ||
      `${g.ime} ${g.prezime}`.toLowerCase().includes(q) ||
      (g.kompanija || '').toLowerCase().includes(q) ||
      (g.pozicija || '').toLowerCase().includes(q)
    );
  });

  if (loading) return <p className="empty-hint">Učitavanje govornika...</p>;

  return (
    <div className="govornici-tab">
      {/* Pretraga */}
      <div style={{ marginBottom: '1rem' }}>
        <input
          className="search-input"
          style={{ width: '100%', maxWidth: '360px' }}
          placeholder="pretraži govornike..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
      </div>

      {filtered.length === 0 ? (
        <div className="sesije-empty">
          <p>Nema pronađenih govornika za ovaj događaj.</p>
        </div>
      ) : (
        <div className="govornici-grid">
          {filtered.map((g) => {
            const assignedSesije = getSesijeForGovornik(g);
            const sessionNames = assignedSesije.map((s) => s.naziv).join(', ');
            return (
              <div key={g.govornikId} className="govornik-card">
                <div className="govornik-card-avatar">
                  <div className="govornik-avatar-placeholder" />
                </div>
                <div className="govornik-card-body">
                  <div className="govornik-card-name">
                    {g.ime} {g.prezime}
                  </div>
                  <div className="govornik-card-meta">
                    {g.pozicija && <span>{g.pozicija}</span>}
                    {g.pozicija && g.kompanija && <span> · </span>}
                    {g.kompanija && <span>{g.kompanija}</span>}
                  </div>
                  {assignedSesije.length > 0 && (
                    <div className="govornik-card-sessions">
                      sesije:{' '}
                      {sessionNames.length > 50
                        ? sessionNames.slice(0, 50) + '...'
                        : sessionNames}
                    </div>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
