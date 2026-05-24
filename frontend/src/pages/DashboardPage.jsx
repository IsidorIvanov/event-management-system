import { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { useNavigate, Link, useLocation } from 'react-router-dom';
import api from '../services/api';
import UpsertEventModal from '../components/UpsertEventModal.jsx';
import { useToast } from '../components/ToastNotification';

const ULOGA_DISPLAY = {
  MENADZER_DOGADJAJA: 'Menadžer događaja',
  KOORDINATOR_RESURSA: 'Koordinator resursa',
  KOORDINATOR_PROGRAMA: 'Koordinator programa',
  FINANSIJSKI_KONTROLOR: 'Finansijski kontrolor',
};

const TIP_DISPLAY = { ZAPOSLENI: 'Zaposleni', KLIJENT: 'Klijent', UCESNIK: 'Učesnik' };

const STATUS_DISPLAY = { OBJAVLJEN: 'Objavljen', AKTIVAN: 'Aktivan', DRAFT: 'Nacrt', ZAVRSEN: 'Završen' };

const STATUS_CLASS = {
  OBJAVLJEN: 'status-badge status-published',
  AKTIVAN:   'status-badge status-ongoing',
  DRAFT:     'status-badge status-draft',
  ZAVRSEN:   'status-badge status-finished',
};

const STATUS_OPTIONS = ['SVE', 'OBJAVLJEN', 'AKTIVAN', 'DRAFT', 'ZAVRSEN'];

const formatDate = (s) => s
  ? new Date(s).toLocaleDateString('sr-Latn', { day: '2-digit', month: 'short', year: 'numeric' })
  : '—';

function ConfirmModal({ event, onConfirm, onCancel }) {
  if (!event) return null;
  return (
    <div className="modal-overlay" onClick={onCancel}>
      <div className="modal-box" onClick={e => e.stopPropagation()}>
        <div className="modal-icon">🗑️</div>
        <h3 className="modal-title">Obriši događaj</h3>
        <p className="modal-body">
          Da li ste sigurni da želite da obrišete događaj <strong>„{event.naziv}"</strong>?<br />
          <span style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>Ova akcija se ne može poništiti.</span>
        </p>
        <div className="modal-actions">
          <button className="btn btn-outline" onClick={onCancel}>Otkaži</button>
          <button className="btn btn-danger" onClick={onConfirm}>Obriši</button>
        </div>
      </div>
    </div>
  );
}

function ProgramSection({ user }) {
  const toast = useToast();
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('SVE');
  const [sortDir, setSortDir] = useState('asc');
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [showCreate, setShowCreate]     = useState(false);
  const [editTarget, setEditTarget]     = useState(null);

  useEffect(() => {
    api.get('/dogadjaj')
      .then(res => setEvents(res.data))
      .catch(() => setError('Greška pri učitavanju događaja.'))
      .finally(() => setLoading(false));
  }, []);

  const confirmDelete = () => {
    api.delete(`/dogadjaj/${deleteTarget.dogadjajId}`)
      .then(() => {
        setEvents(prev => prev.filter(e => e.dogadjajId !== deleteTarget.dogadjajId));
        toast(`Događaj „${deleteTarget.naziv}" je uspešno obrisan.`, 'success');
      })
      .catch(() => toast('Greška pri brisanju događaja.', 'error'))
      .finally(() => setDeleteTarget(null));
  };

  const filtered = events
    .filter(e => statusFilter === 'SVE' || e.status === statusFilter)
    .filter(e => [e.naziv, `${e.lokacijaGrad}, ${e.lokacijaDrzava}`]
      .some(s => s.toLowerCase().includes(search.toLowerCase())))
    .sort((a, b) => {
      const cmp = new Date(a.datumPocetka) - new Date(b.datumPocetka);
      return sortDir === 'asc' ? cmp : -cmp;
    });

  const statCards = [
    { label: 'Ukupno događaja', value: events.length,                                    cls: 'accent',  hint: 'u bazi' },
    { label: 'Objavljeni',      value: events.filter(e => e.status === 'OBJAVLJEN').length, cls: 'success', hint: 'vidljivi učesnicima' },
    { label: 'Aktivni',         value: events.filter(e => e.status === 'AKTIVAN').length,   cls: 'warning', hint: 'u toku' },
    { label: 'Završeni',        value: events.filter(e => e.status === 'ZAVRSEN').length,   cls: 'muted',   hint: 'arhiva' },
  ];

  return (
    <div className="program-page">
      <ConfirmModal event={deleteTarget} onConfirm={confirmDelete} onCancel={() => setDeleteTarget(null)} />
      {showCreate && (
        <UpsertEventModal
          onClose={() => setShowCreate(false)}
          onCreated={(newEvent) => {
            setEvents(prev => [...prev, newEvent]);
            toast(`Događaj „${newEvent.naziv}" je uspešno kreiran.`, 'success');
          }}
        />
      )}
      {editTarget && (
        <UpsertEventModal
          event={editTarget}
          onClose={() => setEditTarget(null)}
          onCreated={(updated) => {
            setEvents(prev => prev.map(e => e.dogadjajId === updated.dogadjajId ? updated : e));
            setEditTarget(null);
            toast(`Događaj „${updated.naziv}" je uspešno izmenjen.`, 'success');
          }}
        />
      )}

      <div className="program-header">
        <div>
          <h1>Dobrodošli, {user.ime}!</h1>
          <p className="page-subtitle">pregled — vaši aktivni i nadolazeći događaji</p>
        </div>
        <div className="program-header-actions">
          <button className="btn btn-outline btn-sm" onClick={() => setStatusFilter('SVE')}>
            filter: {statusFilter === 'SVE' ? 'svi' : STATUS_DISPLAY[statusFilter]}
          </button>
          <button className="btn btn-outline btn-sm" onClick={() => setSortDir(d => d === 'asc' ? 'desc' : 'asc')}>
            sortiraj: datum {sortDir === 'asc' ? '↑' : '↓'}
          </button>
        </div>
      </div>

      <div className="info-cards" style={{ marginBottom: '2rem' }}>
        {statCards.map(({ label, value, cls, hint }) => (
          <div key={label} className="info-card">
            <div className="label">{label}</div>
            <div className={`value ${cls === 'muted' ? '' : cls}`} style={cls === 'muted' ? { color: 'var(--text-muted)' } : {}}>
              {value}
            </div>
            <div className="card-hint">{hint}</div>
          </div>
        ))}
      </div>

      <div className="events-table-card">
        <div className="events-table-header">
          <h2>Događaji</h2>
          <div className="events-table-controls">
            <input className="search-input" placeholder="pretraži..." value={search} onChange={e => setSearch(e.target.value)} />
            <select className="status-select" value={statusFilter} onChange={e => setStatusFilter(e.target.value)}>
              {STATUS_OPTIONS.map(s => (
                <option key={s} value={s}>{s === 'SVE' ? 'status: svi' : `status: ${STATUS_DISPLAY[s]}`}</option>
              ))}
            </select>
          </div>
        </div>

        <table className="events-table">
          <thead>
            <tr><th>NAZIV</th><th>DATUM</th><th>LOKACIJA</th><th>STATUS</th><th>AKCIJE</th></tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan={5} style={{ textAlign: 'center', color: 'var(--text-muted)', padding: '2rem' }}>Učitavanje...</td></tr>
            ) : error ? (
              <tr><td colSpan={5} style={{ textAlign: 'center', color: 'var(--danger)', padding: '2rem' }}>{error}</td></tr>
            ) : filtered.length === 0 ? (
              <tr><td colSpan={5} style={{ textAlign: 'center', color: 'var(--text-muted)', padding: '2rem' }}>Nema rezultata.</td></tr>
            ) : filtered.map(event => (
              <tr key={event.dogadjajId}>
                <td><span className="event-name-badge">{event.naziv}</span></td>
                <td>{formatDate(event.datumPocetka)}</td>
                <td>{event.lokacijaGrad}{event.lokacijaDrzava ? `, ${event.lokacijaDrzava}` : ''}</td>
                <td>
                  <span className={STATUS_CLASS[event.status] || 'status-badge status-draft'}>
                    {STATUS_DISPLAY[event.status] || event.status}
                  </span>
                </td>
                <td>
                  <div className="action-buttons">
                    <button className="btn btn-outline btn-xs" onClick={() => setEditTarget(event)}>Uredi</button>
                    <button className="btn btn-outline btn-xs">Izveštaj</button>
                    <button className="btn btn-xs btn-danger-outline" onClick={() => setDeleteTarget(event)}>Obriši</button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <button className="fab-btn" onClick={() => setShowCreate(true)}>+ Novi događaj</button>
    </div>
  );
}

function NavLink({ to, icon, label }) {
  const { pathname } = useLocation();
  const active = to === '/dashboard' ? pathname === to : pathname.startsWith(to);
  return (
    <Link to={to} className={active ? 'active' : ''}>
      <span>{icon}</span> <span>{label}</span>
    </Link>
  );
}

export default function DashboardPage() {
  const { user, logout, hasRole } = useAuth();
  const navigate = useNavigate();

  if (!user) return null;

  const initials = `${user.ime?.[0] || ''}${user.prezime?.[0] || ''}`.toUpperCase();
  const isProgram   = hasRole('KOORDINATOR_PROGRAMA') || hasRole('MENADZER_DOGADJAJA');
  const isFinansije = hasRole('FINANSIJSKI_KONTROLOR') || hasRole('MENADZER_DOGADJAJA');
  const isResursi   = hasRole('KOORDINATOR_RESURSA')   || hasRole('MENADZER_DOGADJAJA');

  return (
    <div className="dashboard-layout">
      <aside className="sidebar">
        <div className="sidebar-header">
          <h2>📋 EventSys</h2>
          <div className="role-badge">{user.uloga ? ULOGA_DISPLAY[user.uloga] : TIP_DISPLAY[user.tipKorisnika]}</div>
        </div>

        <nav className="sidebar-nav">
          <NavLink to="/dashboard" icon="🏠" label="Početna" />
          {isFinansije && <>
            <NavLink to="/dashboard/budzet"   icon="💰" label="Budžeti" />
            <NavLink to="/dashboard/fakture"  icon="📄" label="Fakture" />
            <NavLink to="/dashboard/troskovi" icon="📊" label="Troškovi" />
            <NavLink to="/dashboard/placanja" icon="💳" label="Plaćanja" />
          </>}
          {isResursi   && <NavLink to="/dashboard/resursi"        icon="🏢" label="Resursi" />}
          {hasRole('KLIJENT')  && <NavLink to="/dashboard/moji-dogadjaji" icon="📋" label="Moji događaji" />}
          {hasRole('UCESNIK') && <NavLink to="/dashboard/registracije"   icon="🎫" label="Moje registracije" />}
        </nav>

        <div className="sidebar-footer">
          <div className="user-info">
            <div className="user-avatar">{initials}</div>
            <div className="user-meta">
              <div className="user-name">{user.ime} {user.prezime}</div>
              <div className="user-email">{user.email}</div>
            </div>
          </div>
          <button onClick={() => { logout(); navigate('/login'); }} className="btn btn-outline"
            style={{ width: '100%', marginTop: '1rem', fontSize: '0.85rem' }}>
            Odjavi se
          </button>
        </div>
      </aside>

      <main className="main-content">
        {isProgram ? <ProgramSection user={user} /> : (
          <>
            <h1>Dobrodošli, {user.ime}!</h1>
            <p className="page-subtitle">
              Prijavljeni ste kao {TIP_DISPLAY[user.tipKorisnika]}
              {user.uloga && ` — ${ULOGA_DISPLAY[user.uloga]}`}
            </p>
            <div className="info-cards">
              <div className="info-card">
                <div className="label">Tip naloga</div>
                <div className="value accent">{TIP_DISPLAY[user.tipKorisnika]}</div>
              </div>
              {user.uloga && <div className="info-card">
                <div className="label">Uloga</div>
                <div className="value success">{ULOGA_DISPLAY[user.uloga]}</div>
              </div>}
              <div className="info-card">
                <div className="label">Email</div>
                <div className="value" style={{ fontSize: '1rem', wordBreak: 'break-all' }}>{user.email}</div>
              </div>
              <div className="info-card">
                <div className="label">ID Korisnika</div>
                <div className="value warning">#{user.korisnikId}</div>
              </div>
            </div>
            {isFinansije && (
              <div className="info-card" style={{ padding: '2rem' }}>
                <h3 style={{ marginBottom: '1rem' }}>🏦 Finansijski podsistem</h3>
                <p style={{ color: 'var(--text-secondary)', lineHeight: 1.6 }}>
                  Imate pristup upravljanju budžetima, fakturama, troškovima i plaćanjima.
                  Koristite navigaciju sa leve strane za pristup modulima.
                </p>
              </div>
            )}
          </>
        )}
      </main>
    </div>
  );
}
