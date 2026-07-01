import { useState, useEffect } from 'react';
import * as sesijaApi from '@/features/dogadjaji/services/sesijaService';
import SesijaModal from '@/features/dogadjaji/components/SesijaModal';
import { useToast } from '@/shared/components/ToastNotification';
import ConfirmDialog from '@/shared/components/ConfirmDialog';

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

function ConfirmModal({ naziv, onConfirm, onCancel }) {
  if (!naziv) return null;
  return (
    <ConfirmDialog title="Obriši sesiju" onConfirm={onConfirm} onCancel={onCancel}>
      Da li ste sigurni da želite da obrišete sesiju <strong>„{naziv}"</strong>?<br />
      <span style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>Ova akcija se ne može poništiti.</span>
    </ConfirmDialog>
  );
}

function SesijaRow({ sesija, dayLabel, onEdit, onDelete }) {
  const registered = sesija.popunjenost ?? 0;
  const { pct, color } = fillBar(registered, sesija.kapacitet);
  const speakers = sesija.govornici;
  const speakerText = speakers && speakers.length > 0
    ? speakers.map((g) => `${g.ime} ${g.prezime}`).join(', ')
    : `${sesija.kapacitet} učesnika`;

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
          <span className="sesija-speaker">Govornik: {speakerText}</span>
          <span className="sesija-capacity">kapacitet: {sesija.kapacitet}</span>
          <span className="sesija-registered">prijavljeno: {registered} / {sesija.kapacitet}</span>
          <div className="sesija-fill-bar">
            <div className="sesija-fill-bar-inner" style={{ width: `${pct}%`, background: color }} />
          </div>
          <span className="sesija-pct">{pct}%</span>
        </div>
      </div>
      <div className="sesija-row-actions">
        <button className="btn btn-outline btn-sm" onClick={onEdit}>Uredi</button>
        <button className="btn btn-outline btn-sm btn-danger-outline" onClick={onDelete}>Obriši</button>
      </div>
    </div>
  );
}

export default function SesijeTab({ event }) {
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
  const avgFillRate = (() => {
    if (sesije.length === 0) return null;
    const totalPct = sesije.reduce((sum, s) => {
      return sum + (s.kapacitet > 0 ? (s.popunjenost ?? 0) / s.kapacitet : 0);
    }, 0);
    return Math.round((totalPct / sesije.length) * 100);
  })();

  // Day label helper: "Day 1 · May 22" style
  const dayLabel = (datum) => {
    const d = new Date(datum + 'T00:00:00');
    const start = new Date(event.datumPocetka + 'T00:00:00');
    const dayNum = Math.round((d - start) / 86400000) + 1;
    const monthDay = d.toLocaleDateString('sr-Latn', { month: 'short', day: 'numeric' });
    return `Dan ${dayNum} · ${monthDay}`;
  };

  return (
    <div className="sesije-tab">
      {/* Stats cards */}
      <div className="sesije-stats">
        <div className="sesija-stat-card">
          <div className="sesija-stat-label">Sesije</div>
          <div className="sesija-stat-value">{total}</div>
        </div>
        <div className="sesija-stat-card">
          <div className="sesija-stat-label">Keynote</div>
          <div className="sesija-stat-value">{sesije.filter(s => s.tip === 'KEYNOTE').length}</div>
        </div>
        <div className="sesija-stat-card">
          <div className="sesija-stat-label">Radionice</div>
          <div className="sesija-stat-value">{sesije.filter(s => s.tip === 'WORKSHOP').length}</div>
        </div>
        <div className="sesija-stat-card">
          <div className="sesija-stat-label">Pros. popunjenost</div>
          <div className="sesija-stat-value">{avgFillRate !== null ? `${avgFillRate}%` : '—'}</div>
        </div>
      </div>

      {/* Toolbar */}
      <div className="sesije-toolbar">
        <div className="sesije-filters">
          <div className="sesija-filter-input">
            <span className="filter-icon">🔍</span>
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
        <button className="btn btn-primary sesija-add-btn" onClick={() => setModal('add')} style={{ width: 'auto' }}>
          + Nova sesija
        </button>
      </div>

      {/* Session list */}
      {loading ? (
        <p className="empty-hint">Učitavanje sesija...</p>
      ) : filtered.length === 0 ? (
        <div className="sesije-empty">
          <p>Nema sesija za ovaj događaj.</p>
          <button className="btn btn-primary" onClick={() => setModal('add')} style={{ width: 'auto', marginTop: '0.5rem' }}>
            + Nova sesija
          </button>
        </div>
      ) : (
        sortedDays.map((datum) => (
          <div key={datum} className="sesije-day-group">
            <div className="sesije-day-header">
              <span className="sesije-day-title">{dayLabel(datum)}</span>
              <span className="sesije-day-count">{grouped[datum].length} sesija</span>
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
          preostaloKapacitet={
            event.maksKapacitet
            - sesije.reduce((sum, s) => sum + (s.kapacitet || 0), 0)
            + (modal?.sesija?.kapacitet || 0)
          }
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
