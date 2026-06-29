import { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate, useLocation } from 'react-router-dom';
import api from '@/shared/services/api';
import * as sesijaApi from '@/features/dogadjaji/services/sesijaService';
import * as govornikApi from '@/features/dogadjaji/services/govornikService';
import * as tipKarteApi from '@/features/dogadjaji/services/tipKarteService';
import * as registracijaApi from '@/features/dogadjaji/services/registracijaService';
import * as analitikaApi from '@/features/dogadjaji/services/analitikaService';
import UpsertEventModal from '@/features/dogadjaji/components/UpsertEventModal';
import SesijaModal from '@/features/dogadjaji/components/SesijaModal';
import UpsertGovornikModal from '@/features/dogadjaji/components/UpsertGovornikModal';
import UpsertKarteModal from '@/features/dogadjaji/components/UpsertKarteModal';
import { useToast } from '@/shared/components/ToastNotification';
import { formatDate as fmtDate } from '@/shared/utils/format';
import { STATUS_DISPLAY, STATUS_CLASS } from '@/features/dogadjaji/constants';
import ConfirmDialog from '@/shared/components/ConfirmDialog';

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

function OverviewTab({ event }) {
  const [stats, setStats] = useState({ sesije: null, govornici: null, prodateKarte: null });

  useEffect(() => {
    Promise.all([
      sesijaApi.getSesijeByDogadjaj(event.dogadjajId),
      govornikApi.getGovornikByDogadjaj(event.dogadjajId),
      registracijaApi.getRegistracijeByDogadjaj(event.dogadjajId),
    ])
      .then(([sRes, gRes, rRes]) => {
        setStats({
          sesije: sRes.data.length,
          govornici: gRes.data.length,
          prodateKarte: rRes.data.filter((r) => r.status === 'POTVRDJENA').length,
        });
      })
      .catch(() => { /* zadrži "—" ako učitavanje ne uspe */ });
  }, [event.dogadjajId]);

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
            <div className="value success">{stats.sesije ?? '—'}</div>
            <div className="card-hint">planirane sesije</div>
          </div>
          <div className="info-card">
            <div className="label">Govornici</div>
            <div className="value warning">{stats.govornici ?? '—'}</div>
            <div className="card-hint">potvrđeni</div>
          </div>
          <div className="info-card">
            <div className="label">Prodate karte</div>
            <div className="value">{stats.prodateKarte ?? '—'}</div>
            <div className="card-hint">od ukupnog kapaciteta</div>
          </div>
        </div>
      </div>
    </div>
  );
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

function SesijeTab({ event }) {
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

function GovorniciTab({ event }) {
  const toast = useToast();
  const [govornici, setGovornici] = useState([]);
  const [sesije, setSesije] = useState([]);
  const [loading, setLoading] = useState(true);
  const [modal, setModal] = useState(null); // null=closed | 'add' | govornik obj
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [search, setSearch] = useState('');

  const load = async () => {
    setLoading(true);
    try {
      const [gRes, sRes] = await Promise.all([
        govornikApi.getGovornikByDogadjaj(event.dogadjajId),
        sesijaApi.getSesijeByDogadjaj(event.dogadjajId),
      ]);
      setGovornici(gRes.data);
      setSesije(sRes.data);
    } catch {
      toast('Greška pri učitavanju govornika.', 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, [event.dogadjajId]);

  const handleSave = async (formData, sesijaIds) => {
    let saved;
    if (modal?.govornikId) {
      // edit
      const res = await govornikApi.updateGovornik(modal.govornikId, formData);
      saved = res.data;

      // sync sessions: add new, remove removed
      const oldIds = new Set(modal.sesijaIds || []);
      const newIds = new Set(sesijaIds);
      for (const sid of newIds) {
        if (!oldIds.has(sid)) await govornikApi.addGovornikToSesija(sid, saved.govornikId);
      }
      for (const sid of oldIds) {
        if (!newIds.has(sid)) await govornikApi.removeGovornikFromSesija(sid, saved.govornikId);
      }
      toast('Govornik je izmenjen.', 'success');
    } else {
      // create
      const res = await govornikApi.createGovornik(formData);
      saved = res.data;
      for (const sid of sesijaIds) {
        await govornikApi.addGovornikToSesija(sid, saved.govornikId);
      }
      toast('Govornik je dodan.', 'success');
    }
    setModal(null);
    await load();
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    try {
      await govornikApi.deleteGovornik(deleteTarget.govornikId);
      toast(`Govornik „${deleteTarget.ime} ${deleteTarget.prezime}" je obrisan.`, 'success');
      await load();
    } catch {
      toast('Greška pri brisanju govornika.', 'error');
    } finally {
      setDeleteTarget(null);
    }
  };

  const filtered = govornici.filter((g) => {
    const q = search.toLowerCase();
    return (
      !q ||
      `${g.ime} ${g.prezime}`.toLowerCase().includes(q) ||
      (g.kompanija || '').toLowerCase().includes(q) ||
      (g.pozicija || '').toLowerCase().includes(q)
    );
  });

  const getSesijeForGovornik = (g) =>
    sesije.filter((s) => s.govornici?.some((sg) => sg.govornikId === g.govornikId) ||
      (g.sesijaIds || []).includes(s.sesijaId));

  return (
    <div className="govornici-tab">
      {/* Delete confirm */}
      {deleteTarget && (
        <ConfirmDialog
          title="Obriši govornika"
          onConfirm={handleDelete}
          onCancel={() => setDeleteTarget(null)}
        >
          Da li ste sigurni da želite da obrišete govornika <strong>„{deleteTarget.ime} {deleteTarget.prezime}"</strong>?
          <br />
          <span style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>
            Ova akcija se ne može poništiti.
          </span>
        </ConfirmDialog>
      )}

      {/* Modal */}
      {modal !== null && (
        <UpsertGovornikModal
          govornik={modal === 'add' ? null : modal}
          dogadjajId={event.dogadjajId}
          onClose={() => setModal(null)}
          onSaved={handleSave}
        />
      )}

      {/* Toolbar */}
      <div className="govornici-toolbar">
        <input
          className="search-input"
          placeholder="pretraži govornike..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
        <button className="btn btn-primary" style={{ width: 'auto' }} onClick={() => setModal('add')}>
          + Novi govornik
        </button>
      </div>

      {/* Grid */}
      {loading ? (
        <p className="empty-hint">Učitavanje...</p>
      ) : filtered.length === 0 ? (
        <div className="sesije-empty">
          <p>Nema govornika za ovaj događaj.</p>
          <button className="btn btn-primary" style={{ width: 'auto', marginTop: '0.5rem' }} onClick={() => setModal('add')}>
            + Novi govornik
          </button>
        </div>
      ) : (
        <div className="govornici-grid">
          {filtered.map((g) => {
            const assignedSesije = getSesijeForGovornik(g);
            return (
              <div key={g.govornikId} className="govornik-card">
                <div className="govornik-card-avatar">
                  <div className="govornik-avatar-placeholder" />
                </div>
                <div className="govornik-card-body">
                  <div className="govornik-card-name">{g.ime} {g.prezime}</div>
                  <div className="govornik-card-meta">
                    {g.pozicija && <span>{g.pozicija}</span>}
                    {g.pozicija && g.kompanija && <span> · </span>}
                    {g.kompanija && <span>{g.kompanija}</span>}
                  </div>
                  {assignedSesije.length > 0 && (
                    <div className="govornik-card-sessions">
                      sesije: {assignedSesije.map((s) => s.naziv).join(', ').slice(0, 40)}
                      {assignedSesije.map((s) => s.naziv).join(', ').length > 40 ? '...' : ''}
                    </div>
                  )}
                  <div className="govornik-card-actions">
                    <button className="btn btn-outline btn-xs" onClick={() => setModal(g)}>Uredi</button>
                    <button className="btn btn-xs btn-danger-outline" onClick={() => setDeleteTarget(g)}>Ukloni</button>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

// ─── Karte Tab ────────────────────────────────────────────────────────────────

const VRSTA_KARTE_LABEL = {
  VISEDNEVNA: 'Višednevna',
  JEDNODNEVNA: 'Jednodnevna',
  POJEDINACNA_SESIJA: 'Pojedinačna sesija',
  BESPLATNA: 'Besplatna',
};

const VRSTA_KARTE_COLOR = {
  VISEDNEVNA: '#3b82f6',
  JEDNODNEVNA: '#f59e0b',
  POJEDINACNA_SESIJA: '#10b981',
  BESPLATNA: '#6b7280',
};

function karteTitle(k) {
  if (k.vrsta === 'VISEDNEVNA') return 'Važi za ceo događaj';
  if (k.vrsta === 'BESPLATNA') return 'Besplatna ulaznica';
  if (k.vrsta === 'JEDNODNEVNA') {
    try {
      return new Date(k.nazivTipa + 'T00:00:00').toLocaleDateString('sr-Latn', {
        weekday: 'long', day: '2-digit', month: 'long', year: 'numeric',
      });
    } catch { return k.nazivTipa; }
  }
  // POJEDINACNA_SESIJA – nazivTipa je naziv sesije
  return k.nazivTipa;
}

function KarteTab({ event }) {
  const toast = useToast();
  const [karte, setKarte] = useState([]);
  const [loading, setLoading] = useState(true);
  const [modal, setModal] = useState(null); // null | 'add' | tipKarte obj
  const [deleteTarget, setDeleteTarget] = useState(null);

  const load = async () => {
    setLoading(true);
    try {
      const res = await tipKarteApi.getTipKarteByDogadjaj(event.dogadjajId);
      setKarte(res.data);
    } catch {
      toast('Greška pri učitavanju tipova karata.', 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, [event.dogadjajId]);

  const handleSave = async (data) => {
    try {
      if (modal?.nazivTipa) {
        await tipKarteApi.updateTipKarte(event.dogadjajId, modal.nazivTipa, data);
        toast('Tip karte je izmenjen.', 'success');
      } else {
        await tipKarteApi.createTipKarte(data);
        toast('Tip karte je dodat.', 'success');
      }
      setModal(null);
      await load();
    } catch (err) {
      toast(err?.response?.data?.message || 'Greška pri čuvanju tipa karte.', 'error');
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    try {
      await tipKarteApi.deleteTipKarte(event.dogadjajId, deleteTarget.nazivTipa);
      toast('Tip karte je obrisan.', 'success');
      await load();
    } catch {
      toast('Greška pri brisanju tipa karte.', 'error');
    } finally {
      setDeleteTarget(null);
    }
  };

  const totalKvota = karte.reduce((s, k) => s + k.kvota, 0);

  return (
    <div className="govornici-tab">
      {/* Delete confirm */}
      {deleteTarget && (
        <ConfirmDialog
          title="Obriši tip karte"
          onConfirm={handleDelete}
          onCancel={() => setDeleteTarget(null)}
        >
          Da li ste sigurni da želite da obrišete kartu{' '}
          <strong>„{VRSTA_KARTE_LABEL[deleteTarget.vrsta] || deleteTarget.vrsta}"</strong>?<br />
          <span style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>
            Ova akcija se ne može poništiti.
          </span>
        </ConfirmDialog>
      )}

      {modal !== null && (
        <UpsertKarteModal
          dogadjajId={event.dogadjajId}
          event={event}
          karte={karte}
          tipKarte={modal === 'add' ? null : modal}
          onClose={() => setModal(null)}
          onSaved={handleSave}
        />
      )}

      {/* Stats */}
      <div className="sesije-stats">
        <div className="sesija-stat-card">
          <div className="sesija-stat-label">Tipovi karata</div>
          <div className="sesija-stat-value">{karte.length}</div>
        </div>
        <div className="sesija-stat-card">
          <div className="sesija-stat-label">Ukupna kvota</div>
          <div className="sesija-stat-value">{totalKvota}</div>
        </div>
        <div className="sesija-stat-card">
          <div className="sesija-stat-label">Sesijske karte</div>
          <div className="sesija-stat-value">{karte.filter((k) => k.vrsta === 'POJEDINACNA_SESIJA').reduce((s, k) => s + (k.kvota || 0), 0)}</div>
        </div>
        <div className="sesija-stat-card">
          <div className="sesija-stat-label">Besplatne karte</div>
          <div className="sesija-stat-value">{karte.filter((k) => k.vrsta === 'BESPLATNA').reduce((s, k) => s + (k.kvota || 0), 0)}</div>
        </div>
      </div>

      {/* Toolbar */}
      <div className="govornici-toolbar">
        <div />
        <button className="btn btn-primary" style={{ width: 'auto' }} onClick={() => setModal('add')}>
          + Dodaj tip karte
        </button>
      </div>

      {/* List */}
      {loading ? (
        <p className="empty-hint">Učitavanje...</p>
      ) : karte.length === 0 ? (
        <div className="sesije-empty">
          <p>Nema tipova karata za ovaj događaj.</p>
          <button className="btn btn-primary" style={{ width: 'auto', marginTop: '0.5rem' }} onClick={() => setModal('add')}>
            + Dodaj tip karte
          </button>
        </div>
      ) : (
        <div className="karte-list">
          {/* Header row */}
          <div className="karte-list-header">
            <span>VRSTA</span>
            <span>DETALJI</span>
            <span>CENA</span>
            <span>KVOTA</span>
            <span></span>
          </div>

          {karte.map((k) => {
            const badge = k.vrsta === 'VISEDNEVNA'
              ? { label: 'ceo događaj', color: '#3b82f6' }
              : k.vrsta === 'JEDNODNEVNA'
              ? { label: (() => { try { return new Date(k.nazivTipa + 'T00:00:00').toLocaleDateString('sr-Latn', { day: 'numeric', month: 'short' }); } catch { return k.nazivTipa; } })(), color: '#f59e0b' }
              : k.vrsta === 'POJEDINACNA_SESIJA'
              ? { label: 'sesija', color: '#10b981' }
              : { label: 'besplatno', color: '#6b7280' };

            return (
              <div key={k.nazivTipa} className="karte-list-row">
                {/* Vrsta */}
                <div className="karte-vrsta-col">
                  <span className="karte-vrsta-label">{VRSTA_KARTE_LABEL[k.vrsta] || k.vrsta}</span>
                </div>

                {/* Naziv + opis */}
                <div className="karte-detalji-col">
                  <div className="karte-naziv-row">
                    <span className="karte-naziv">{karteTitle(k)}</span>
                    <span className="karte-badge" style={{ borderColor: badge.color, color: badge.color }}>
                      {badge.label}
                    </span>
                  </div>
                  {k.opis && <div className="karte-opis">{k.opis}</div>}
                </div>

                {/* Cena */}
                <div className="karte-cena-col">
                  {k.vrsta !== 'BESPLATNA' ? (
                    <span className="karte-cena">{Number(k.cena).toLocaleString('sr-Latn')} RSD</span>
                  ) : (
                    <span className="karte-cena" style={{ color: 'var(--success)' }}>Besplatno</span>
                  )}
                </div>

                {/* Kvota */}
                <div className="karte-kvota-col">
                  <span className="karte-kvota">{k.kvota}</span>
                  <span className="karte-kvota-hint">mesta</span>
                </div>

                {/* Akcije */}
                <div className="sesija-row-actions">
                  <button className="btn btn-outline btn-sm" onClick={() => setModal(k)}>Izmeni</button>
                  <button className="btn btn-outline btn-sm btn-danger-outline" onClick={() => setDeleteTarget(k)}>Obriši</button>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

// ─── Prisustvo Tab ────────────────────────────────────────────────────────────

const STATUS_REG_LABEL = {
  POTVRDJENA: 'Potvrđena',
  OTKAZANA: 'Otkazana',
  NA_CEKANJU: 'Na čekanju',
};

const STATUS_REG_CLASS = {
  POTVRDJENA: 'status-badge status-published',
  OTKAZANA: 'status-badge status-draft',
  NA_CEKANJU: 'status-badge status-ongoing',
};

const STATUS_KARTE_LABEL = {
  VALIDNA: 'Validna',
  NEVAZECA: 'Nevažeća',
  ISKORISCENA: 'Iskorišćena',
};

const STATUS_KARTE_CLASS = {
  VALIDNA: 'status-badge status-published',
  NEVAZECA: 'status-badge status-draft',
  ISKORISCENA: 'status-badge status-finished',
};

const PRISUSTVO_PAGE_SIZE = 10;

function PrisustvoTab({ event }) {
  const toast = useToast();
  const [registracije, setRegistracije] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [filterStatus, setFilterStatus] = useState('all');
  const [filterTipKarte, setFilterTipKarte] = useState('all');
  const [page, setPage] = useState(0);

  const load = async () => {
    setLoading(true);
    try {
      const res = await registracijaApi.getRegistracijeByDogadjaj(event.dogadjajId);
      setRegistracije(res.data);
    } catch {
      toast('Greška pri učitavanju učesnika.', 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, [event.dogadjajId]);

  const tipovi = [...new Set(registracije.map((r) => r.nazivTipa))];

  const filtered = registracije.filter((r) => {
    const q = search.toLowerCase();
    const matchSearch = !q ||
      `${r.ucesnikIme || ''} ${r.ucesnikPrezime || ''}`.toLowerCase().includes(q) ||
      (r.ucesnikEmail || '').toLowerCase().includes(q) ||
      (r.ucesnikKompanija || '').toLowerCase().includes(q) ||
      (r.brojKarte || '').toLowerCase().includes(q);
    const matchStatus = filterStatus === 'all' || r.status === filterStatus;
    const matchTip = filterTipKarte === 'all' || r.nazivTipa === filterTipKarte;
    return matchSearch && matchStatus && matchTip;
  });

  const totalPages = Math.ceil(filtered.length / PRISUSTVO_PAGE_SIZE);
  const pageItems = filtered.slice(
    page * PRISUSTVO_PAGE_SIZE,
    page * PRISUSTVO_PAGE_SIZE + PRISUSTVO_PAGE_SIZE,
  );

  // Reset to first page whenever filters/search change or data reloads.
  useEffect(() => { setPage(0); }, [search, filterStatus, filterTipKarte, registracije]);

  const statsPotvrdjena = registracije.filter((r) => r.status === 'POTVRDJENA').length;
  const statsOtkazana = registracije.filter((r) => r.status === 'OTKAZANA').length;

  return (
    <div className="govornici-tab">
      {/* Stats */}
      <div className="sesije-stats">
        <div className="sesija-stat-card">
          <div className="sesija-stat-label">Ukupno registracija</div>
          <div className="sesija-stat-value">{registracije.length}</div>
        </div>
        <div className="sesija-stat-card">
          <div className="sesija-stat-label">Potvrđene</div>
          <div className="sesija-stat-value">{statsPotvrdjena}</div>
        </div>
        <div className="sesija-stat-card">
          <div className="sesija-stat-label">Otkazane</div>
          <div className="sesija-stat-value">{statsOtkazana}</div>
        </div>
        <div className="sesija-stat-card">
          <div className="sesija-stat-label">Popunjenost</div>
          <div className="sesija-stat-value">
            {event.maksKapacitet > 0
              ? `${Math.round((statsPotvrdjena / event.maksKapacitet) * 100)}%`
              : '—'}
          </div>
        </div>
      </div>

      {/* Filters */}
      <div className="sesije-toolbar">
        <div className="sesije-filters">
          <div className="sesija-filter-input">
            <span className="filter-icon">🔍</span>
            <input
              placeholder="pretraži učesnike..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="sesija-search"
            />
          </div>
          <select
            className="sesija-filter-select"
            value={filterStatus}
            onChange={(e) => setFilterStatus(e.target.value)}
          >
            <option value="all">status: svi</option>
            <option value="POTVRDJENA">Potvrđena</option>
            <option value="OTKAZANA">Otkazana</option>
            <option value="NA_CEKANJU">Na čekanju</option>
          </select>
          <select
            className="sesija-filter-select"
            value={filterTipKarte}
            onChange={(e) => setFilterTipKarte(e.target.value)}
          >
            <option value="all">tip karte: svi</option>
            {tipovi.map((t) => (
              <option key={t} value={t}>{t}</option>
            ))}
          </select>
        </div>
        <button
          className="btn btn-outline btn-sm"
          onClick={load}
          style={{ width: 'auto' }}
        >
          ↻ Osveži
        </button>
      </div>

      {/* Table */}
      {loading ? (
        <p className="empty-hint">Učitavanje učesnika...</p>
      ) : filtered.length === 0 ? (
        <div className="sesije-empty">
          <p>Nema registrovanih učesnika za ovaj događaj.</p>
        </div>
      ) : (
        <div className="events-table-card">
          <div className="events-table-header">
            <h2>Registrovani učesnici</h2>
            <div className="card-hint">{filtered.length} od {registracije.length} ukupno</div>
          </div>
          <table className="events-table">
            <thead>
              <tr>
                <th>IME I PREZIME</th>
                <th>EMAIL</th>
                <th>KOMPANIJA / POZICIJA</th>
                <th>TIP KARTE</th>
                <th>BROJ KARTE</th>
                <th>DATUM REG.</th>
                <th>STATUS REG.</th>
                <th>STATUS KARTE</th>
              </tr>
            </thead>
            <tbody>
              {pageItems.map((r) => (
                <tr key={r.registracijaId}>
                  <td>
                    <span className="event-name-badge">
                      {r.ucesnikIme} {r.ucesnikPrezime}
                    </span>
                  </td>
                  <td>{r.ucesnikEmail || '—'}</td>
                  <td>
                    {[r.ucesnikKompanija, r.ucesnikPozicija].filter(Boolean).join(' · ') || '—'}
                  </td>
                  <td>{r.nazivTipa || '—'}</td>
                  <td>
                    <span style={{ fontFamily: 'monospace', fontSize: '0.85rem' }}>
                      {r.brojKarte || '—'}
                    </span>
                  </td>
                  <td>
                    {r.datumRegistracije
                      ? new Date(r.datumRegistracije).toLocaleDateString('sr-Latn', {
                          day: '2-digit', month: '2-digit', year: 'numeric',
                        })
                      : '—'}
                  </td>
                  <td>
                    <span className={STATUS_REG_CLASS[r.status] || 'status-badge status-draft'}>
                      {STATUS_REG_LABEL[r.status] || r.status || '—'}
                    </span>
                  </td>
                  <td>
                    <span className={STATUS_KARTE_CLASS[r.statusKarte] || 'status-badge status-draft'}>
                      {STATUS_KARTE_LABEL[r.statusKarte] || r.statusKarte || '—'}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {totalPages > 1 && (
            <div className="prisustvo-paginacija">
              <button
                className="btn btn-outline btn-sm"
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0}
              >
                ← Prethodna
              </button>
              <span className="prisustvo-paginacija-info">
                Strana {page + 1} od {totalPages}
              </span>
              <button
                className="btn btn-outline btn-sm"
                onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                disabled={page >= totalPages - 1}
              >
                Sledeća →
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

// ─── Izveštaj (Reports & Analytics) Tab ─────────────────────────────────────────

function IzvestajTab({ event }) {
  const toast = useToast();
  const [analitika, setAnalitika] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [downloading, setDownloading] = useState(false);

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError(false);
    analitikaApi.getAnalitika(event.dogadjajId)
      .then((res) => { if (active) setAnalitika(res.data); })
      .catch(() => { if (active) setError(true); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [event.dogadjajId]);

  const handleDownload = async () => {
    setDownloading(true);
    try {
      const safeName = (event.naziv || 'dogadjaj').replace(/[^\p{L}\p{N}_-]+/gu, '-').slice(0, 60);
      await analitikaApi.downloadPdf(event.dogadjajId, `izvestaj-${safeName}.pdf`);
    } catch {
      toast('Greška pri generisanju PDF izveštaja.', 'error');
    } finally {
      setDownloading(false);
    }
  };

  if (loading) return <p className="empty-hint">Učitavanje izveštaja...</p>;
  if (error) return <p className="empty-hint" style={{ color: 'var(--danger)' }}>Greška pri učitavanju izveštaja.</p>;
  if (!analitika) return null;

  return (
    <div className="izvestaj-tab">
      <div className="izvestaj-header">
        <div>
          <h2 className="izvestaj-title">Reports &amp; Analytics</h2>
        </div>
        <button className="btn btn-primary" style={{ width: 'auto' }} onClick={handleDownload} disabled={downloading}>
          {downloading ? 'Generisanje...' : 'Generiši PDF izveštaj'}
        </button>
      </div>

      {/* Summary */}
      <div className="sesije-stats" style={{ marginBottom: '1.25rem' }}>
        <div className="sesija-stat-card">
          <div className="sesija-stat-label">Stopa prisustva</div>
          <div className="sesija-stat-value">{analitika.stopaPrisustva}%</div>
        </div>
        <div className="sesija-stat-card">
          <div className="sesija-stat-label">Potvrđene registracije</div>
          <div className="sesija-stat-value">{analitika.ukupnoRegistracija} / {analitika.maksKapacitet}</div>
        </div>
        <div className="sesija-stat-card">
          <div className="sesija-stat-label">Prosečan engagement</div>
          <div className="sesija-stat-value">{analitika.prosecniEngagement}%</div>
        </div>
        <div className="sesija-stat-card">
          <div className="sesija-stat-label">Broj sesija</div>
          <div className="sesija-stat-value">{analitika.engagementPoSesiji?.length ?? 0}</div>
        </div>
      </div>

      <div className="izvestaj-charts">
        <div className="events-table-card izvestaj-chart-card">
          <h3 className="izvestaj-chart-title">Attendance Rate</h3>
          <LineChart points={analitika.stopaPrisustvaSerija} unit="%" />
        </div>
        <div className="events-table-card izvestaj-chart-card">
          <h3 className="izvestaj-chart-title">Engagement Score / Session</h3>
          <BarChart points={analitika.engagementPoSesiji} unit="%" />
        </div>
      </div>
    </div>
  );
}

function ChartTooltip({ tip }) {
  if (!tip) return null;
  return (
    <div className="izvestaj-tooltip" style={{ left: tip.x, top: tip.y }}>
      <div className="izvestaj-tooltip-label">{tip.label}</div>
      <div className="izvestaj-tooltip-value">{tip.value}</div>
    </div>
  );
}

function LineChart({ points, unit = '' }) {
  const wrapRef = useRef(null);
  const [tip, setTip] = useState(null);

  if (!points || points.length === 0) {
    return <div className="izvestaj-chart-empty">Nema podataka o registracijama.</div>;
  }

  const W = 560, H = 240, padX = 36, padTop = 16, padBottom = 28;
  const plotW = W - padX - 12;
  const plotH = H - padTop - padBottom;
  const max = Math.max(1, ...points.map((p) => p.vrednost));
  const n = points.length;
  const xAt = (i) => padX + (n === 1 ? plotW / 2 : (plotW * i) / (n - 1));
  const yAt = (v) => padTop + plotH - (plotH * v) / max;

  const path = points.map((p, i) => `${i === 0 ? 'M' : 'L'} ${xAt(i).toFixed(1)} ${yAt(p.vrednost).toFixed(1)}`).join(' ');
  const area = `${path} L ${xAt(n - 1).toFixed(1)} ${(padTop + plotH).toFixed(1)} L ${xAt(0).toFixed(1)} ${(padTop + plotH).toFixed(1)} Z`;
  const step = Math.max(1, Math.ceil(n / 8));

  const showTip = (e, p) => {
    const rect = wrapRef.current?.getBoundingClientRect();
    if (!rect) return;
    setTip({
      x: e.clientX - rect.left,
      y: e.clientY - rect.top,
      label: p.oznaka,
      value: `Prisustvo: ${p.vrednost}${unit}`,
    });
  };

  return (
    <div className="izvestaj-chart-wrap" ref={wrapRef}>
      <svg className="izvestaj-svg" viewBox={`0 0 ${W} ${H}`} preserveAspectRatio="xMidYMid meet">
        {[0, 0.25, 0.5, 0.75, 1].map((f) => {
          const y = padTop + plotH * f;
          return (
            <g key={f}>
              <line x1={padX} y1={y} x2={W - 12} y2={y} className="izvestaj-grid" />
              <text x={padX - 6} y={y + 3} textAnchor="end" className="izvestaj-axis">{Math.round(max * (1 - f))}</text>
            </g>
          );
        })}
        <path d={area} className="izvestaj-area" />
        <path d={path} className="izvestaj-line" />
        {points.map((p, i) => (
          <g key={i}>
            <circle
              cx={xAt(i)}
              cy={yAt(p.vrednost)}
              r="2.5"
              className={`izvestaj-dot${tip && tip.label === p.oznaka ? ' is-active' : ''}`}
            />
            {(i % step === 0 || i === n - 1) && (
              <text x={xAt(i)} y={H - 8} textAnchor="middle" className="izvestaj-axis">{p.oznaka}</text>
            )}
            <circle
              cx={xAt(i)}
              cy={yAt(p.vrednost)}
              r="10"
              className="izvestaj-hit"
              onMouseEnter={(e) => showTip(e, p)}
              onMouseMove={(e) => showTip(e, p)}
              onMouseLeave={() => setTip(null)}
            />
          </g>
        ))}
      </svg>
      <ChartTooltip tip={tip} />
    </div>
  );
}

function BarChart({ points, unit = '' }) {
  const wrapRef = useRef(null);
  const [tip, setTip] = useState(null);

  if (!points || points.length === 0) {
    return <div className="izvestaj-chart-empty">Nema sesija za ovaj događaj.</div>;
  }

  const W = 560, H = 240, padX = 36, padTop = 18, padBottom = 34;
  const plotW = W - padX - 12;
  const plotH = H - padTop - padBottom;
  const max = Math.max(1, ...points.map((p) => p.vrednost));
  const n = points.length;
  const slot = plotW / n;
  const barW = Math.min(46, slot * 0.6);
  const baseY = padTop + plotH;
  const trim = (s) => (s && s.length > 12 ? `${s.slice(0, 11)}…` : s || '');

  const showTip = (e, p) => {
    const rect = wrapRef.current?.getBoundingClientRect();
    if (!rect) return;
    setTip({
      x: e.clientX - rect.left,
      y: e.clientY - rect.top,
      label: p.oznaka,
      value: `Engagement: ${p.vrednost}${unit}`,
    });
  };

  return (
    <div className="izvestaj-chart-wrap" ref={wrapRef}>
      <svg className="izvestaj-svg" viewBox={`0 0 ${W} ${H}`} preserveAspectRatio="xMidYMid meet">
        {[0, 0.25, 0.5, 0.75, 1].map((f) => {
          const y = padTop + plotH * f;
          return (
            <g key={f}>
              <line x1={padX} y1={y} x2={W - 12} y2={y} className="izvestaj-grid" />
              <text x={padX - 6} y={y + 3} textAnchor="end" className="izvestaj-axis">{Math.round(max * (1 - f))}</text>
            </g>
          );
        })}
        {points.map((p, i) => {
          const cx = padX + slot * i + slot / 2;
          const h = (plotH * p.vrednost) / max;
          const active = tip && tip.label === p.oznaka;
          return (
            <g
              key={i}
              onMouseEnter={(e) => showTip(e, p)}
              onMouseMove={(e) => showTip(e, p)}
              onMouseLeave={() => setTip(null)}
            >
              <rect x={padX + slot * i} y={padTop} width={slot} height={plotH} className="izvestaj-hit" />
              <rect x={cx - barW / 2} y={baseY - h} width={barW} height={h} className={`izvestaj-bar${active ? ' is-active' : ''}`} rx="2" />
              <text x={cx} y={baseY - h - 4} textAnchor="middle" className="izvestaj-axis">{p.vrednost}{unit}</text>
              <text x={cx} y={H - 8} textAnchor="middle" className="izvestaj-axis">{trim(p.oznaka)}</text>
            </g>
          );
        })}
      </svg>
      <ChartTooltip tip={tip} />
    </div>
  );
}
