import { useState, useEffect } from 'react';
import * as sesijaApi from '@/features/dogadjaji/services/sesijaService';
import { NOTIF_NEW_EVENT } from '@/features/notifikacije/hooks/useNotifikacije';
import { useToast } from '@/shared/components/ToastNotification';

const TIP_LABEL = {
  KEYNOTE: 'Keynote',
  WORKSHOP: 'Radionica',
  PANEL: 'Panel',
  NETWORKING: 'Networking',
};

function fillBar(registered, capacity) {
  const pct = capacity > 0 ? Math.round((registered / capacity) * 100) : 0;
  const color = pct >= 100 ? 'var(--danger)' : pct >= 80 ? 'var(--warning)' : 'var(--accent)';
  return { pct, color };
}

export default function AgendaTab({ event, isRegistered, userRegistration }) {
  const [sesije, setSesije] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [filterDay, setFilterDay] = useState('all');
  const [filterTrack, setFilterTrack] = useState('all');
  const [filterRoom, setFilterRoom] = useState('all');
  const [rasporedIds, setRasporedIds] = useState(new Set());
  const [togglingId, setTogglingId] = useState(null);
  const toast = useToast();

  /**
   * Determines whether the participant's ticket allows adding the given session
   * to their schedule:
   *  - VISEDNEVNA / BESPLATNA  → all sessions
   *  - JEDNODNEVNA             → only sessions on the ticket's day (nazivTipa == sesija.datum)
   *  - POJEDINACNA_SESIJA      → only the session whose name matches (nazivTipa == sesija.naziv)
   */
  const canAddToRaspored = (sesija) => {
    if (!isRegistered || !userRegistration) return false;
    const { vrstaKarte, nazivTipa } = userRegistration;
    if (vrstaKarte === 'VISEDNEVNA' || vrstaKarte === 'BESPLATNA') return true;
    if (vrstaKarte === 'JEDNODNEVNA') return sesija.datum === nazivTipa;
    if (vrstaKarte === 'POJEDINACNA_SESIJA') return sesija.naziv === nazivTipa;
    return false;
  };

  const loadAgenda = () => {
    const loads = [sesijaApi.getSesijeByDogadjaj(event.dogadjajId)];
    if (isRegistered) loads.push(sesijaApi.getMojRasporedIds());

    Promise.all(loads)
      .then(([sesRes, idsRes]) => {
        setSesije(sesRes.data);
        if (idsRes) setRasporedIds(new Set(idsRes.data));
      })
      .catch(() => setSesije([]))
      .finally(() => setLoading(false));
  };

  useEffect(() => { loadAgenda(); }, [event.dogadjajId, isRegistered]);

  // Osveži agendu kad stigne notifikacija (nova/izmenjena/otkazana sesija, govornik — S1–S4).
  useEffect(() => {
    const onNotif = () => loadAgenda();
    window.addEventListener(NOTIF_NEW_EVENT, onNotif);
    return () => window.removeEventListener(NOTIF_NEW_EVENT, onNotif);
  }, [event.dogadjajId, isRegistered]);

  const handleToggleRaspored = async (e, sesija) => {
    e.stopPropagation();
    if (togglingId) return;
    setTogglingId(sesija.sesijaId);
    try {
      if (rasporedIds.has(sesija.sesijaId)) {
        await sesijaApi.removeFromRaspored(sesija.sesijaId);
        setRasporedIds((prev) => { const n = new Set(prev); n.delete(sesija.sesijaId); return n; });
        toast(`Sesija "${sesija.naziv}" uklonjena iz rasporeda.`, 'info');
      } else {
        await sesijaApi.addToRaspored(sesija.sesijaId);
        setRasporedIds((prev) => new Set([...prev, sesija.sesijaId]));
        toast(`Sesija "${sesija.naziv}" dodata u raspored.`, 'success');
      }
    } catch (err) {
      toast('Došlo je do greške. Pokušajte ponovo.', 'error');
    } finally {
      setTogglingId(null);
    }
  };

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
    return `Dan ${dayNum} · ${d.toLocaleDateString('sr-Latn', { month: 'short', day: 'numeric' })}`;
  };

  return (
    <div className="sesije-tab">
      {/* Filteri */}
      <div className="sesije-toolbar">
        <div className="sesije-filters">
          <div className="sesija-filter-input">
            <span>🔍</span>
            <input
              placeholder="pretraži..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="sesija-search"
            />
          </div>
          <select className="sesija-filter-select" value={filterDay} onChange={(e) => setFilterDay(e.target.value)}>
            <option value="all">dan: svi</option>
            {days.map((d) => <option key={d} value={d}>{dayLabel(d)}</option>)}
          </select>
          <select className="sesija-filter-select" value={filterTrack} onChange={(e) => setFilterTrack(e.target.value)}>
            <option value="all">tip: svi</option>
            {tracks.map((t) => <option key={t} value={t}>{TIP_LABEL[t] || t}</option>)}
          </select>
          <select className="sesija-filter-select" value={filterRoom} onChange={(e) => setFilterRoom(e.target.value)}>
            <option value="all">sala: sve</option>
            {rooms.map((r) => <option key={r} value={r}>{r}</option>)}
          </select>
        </div>
      </div>

      {/* Lista sesija */}
      {loading ? (
        <p className="empty-hint">Učitavanje sesija...</p>
      ) : filtered.length === 0 ? (
        <div className="sesije-empty">
          <p>Nema pronađenih sesija za ovaj događaj.</p>
        </div>
      ) : (
        sortedDays.map((datum) => (
          <div key={datum} className="sesije-day-group">
            <div className="sesije-day-header">
              <span className="sesije-day-title">{dayLabel(datum)}</span>
              <span className="sesije-day-count">
                {grouped[datum].length} {grouped[datum].length === 1 ? 'sesija' : 'sesija'}
              </span>
            </div>
            {grouped[datum].map((s) => {
              const { pct, color } = fillBar(s.popunjenost ?? 0, s.kapacitet);
              const speakerText = s.govornici?.length > 0
                ? s.govornici.map((g) => `${g.ime} ${g.prezime}`).join(', ')
                : null;
              const inRaspored = rasporedIds.has(s.sesijaId);
              const isToggling = togglingId === s.sesijaId;

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
                      {canAddToRaspored(s) && (
                        <button
                          className={`sesija-raspored-btn${inRaspored ? ' in-raspored' : ''}`}
                          disabled={isToggling}
                          onClick={(e) => handleToggleRaspored(e, s)}
                          title={inRaspored ? 'Ukloni iz rasporeda' : 'Dodaj u raspored'}
                        >
                          {isToggling ? '...' : inRaspored ? '✓ U rasporedu' : '+ Raspored'}
                        </button>
                      )}
                    </div>
                    {s.opis && <div className="sesija-opis">{s.opis}</div>}
                    <div className="sesija-meta-row">
                      <span>kapacitet: {s.kapacitet}</span>
                      <div className="sesija-fill-bar">
                        <div className="sesija-fill-bar-inner" style={{ width: `${pct}%`, background: color }} />
                      </div>
                      <span>{pct}%</span>
                    </div>
                    <div className="sesija-meta-row">
                      {speakerText && <span>Govornik: {speakerText}</span>}
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
