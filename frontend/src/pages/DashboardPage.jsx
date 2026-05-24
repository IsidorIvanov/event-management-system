import { useAuth } from '../context/AuthContext';
import { useNavigate, Link, useLocation, Routes, Route } from 'react-router-dom';
import ResursiPage from './ResursiPage';

const ULOGA_DISPLAY = {
  MENADZER_DOGADJAJA: 'Menadžer događaja',
  KOORDINATOR_RESURSA: 'Koordinator resursa',
  KOORDINATOR_PROGRAMA: 'Koordinator programa',
  FINANSIJSKI_KONTROLOR: 'Finansijski kontrolor',
};

const TIP_DISPLAY = {
  ZAPOSLENI: 'Zaposleni',
  KLIJENT: 'Klijent',
  UCESNIK: 'Učesnik',
};

function DashboardHome() {
  const { user, hasRole } = useAuth();
  const isFinansije = hasRole('FINANSIJSKI_KONTROLOR') || hasRole('MENADZER_DOGADJAJA');

  return (
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

        {user.uloga && (
          <div className="info-card">
            <div className="label">Uloga</div>
            <div className="value success">{ULOGA_DISPLAY[user.uloga]}</div>
          </div>
        )}

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
          <h3 style={{ marginBottom: '1rem' }}>Finansijski podsistem</h3>
          <p style={{ color: 'var(--text-secondary)', lineHeight: 1.6 }}>
            Imate pristup upravljanju budžetima, fakturama, troškovima i plaćanjima.
            Koristite navigaciju sa leve strane za pristup modulima.
          </p>
        </div>
      )}
    </>
  );
}

export default function DashboardPage() {
  const { user, logout, hasRole } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  if (!user) return null;

  const initials = `${user.ime?.[0] || ''}${user.prezime?.[0] || ''}`.toUpperCase();
  const isFinansije = hasRole('FINANSIJSKI_KONTROLOR') || hasRole('MENADZER_DOGADJAJA');
  const canViewResursi =
    hasRole('KOORDINATOR_RESURSA') ||
    hasRole('MENADZER_DOGADJAJA') ||
    hasRole('KOORDINATOR_PROGRAMA');

  return (
    <div className="dashboard-layout">
      <aside className="sidebar">
        <div className="sidebar-header">
          <h2>EventSys</h2>
          <div className="role-badge">
            {user.uloga ? ULOGA_DISPLAY[user.uloga] : TIP_DISPLAY[user.tipKorisnika]}
          </div>
        </div>

        <nav className="sidebar-nav">
          <Link
            to="/dashboard"
            end
            className={location.pathname === '/dashboard' ? 'active' : ''}
          >
            <span>Početna</span>
          </Link>

          {isFinansije && (
            <>
              <Link to="/dashboard/budzet" className={location.pathname.startsWith('/dashboard/budzet') ? 'active' : ''}>
                <span>Budžeti</span>
              </Link>
              <Link to="/dashboard/fakture" className={location.pathname.startsWith('/dashboard/fakture') ? 'active' : ''}>
                <span>Fakture</span>
              </Link>
              <Link to="/dashboard/troskovi" className={location.pathname.startsWith('/dashboard/troskovi') ? 'active' : ''}>
                <span>Troškovi</span>
              </Link>
              <Link to="/dashboard/placanja" className={location.pathname.startsWith('/dashboard/placanja') ? 'active' : ''}>
                <span>Plaćanja</span>
              </Link>
            </>
          )}

          {canViewResursi && (
            <Link to="/dashboard/resursi" className={location.pathname.startsWith('/dashboard/resursi') ? 'active' : ''}>
              <span>Resursi</span>
            </Link>
          )}

          {(hasRole('KOORDINATOR_PROGRAMA') || hasRole('MENADZER_DOGADJAJA')) && (
            <Link to="/dashboard/program" className={location.pathname.startsWith('/dashboard/program') ? 'active' : ''}>
              <span>Program</span>
            </Link>
          )}

          {hasRole('KLIJENT') && (
            <Link to="/dashboard/moji-dogadjaji" className={location.pathname.startsWith('/dashboard/moji-dogadjaji') ? 'active' : ''}>
              <span>Moji događaji</span>
            </Link>
          )}

          {hasRole('UCESNIK') && (
            <Link to="/dashboard/registracije" className={location.pathname.startsWith('/dashboard/registracije') ? 'active' : ''}>
              <span>Moje registracije</span>
            </Link>
          )}
        </nav>

        <div className="sidebar-footer">
          <div className="user-info">
            <div className="user-avatar">{initials}</div>
            <div className="user-meta">
              <div className="user-name">{user.ime} {user.prezime}</div>
              <div className="user-email">{user.email}</div>
            </div>
          </div>
          <button onClick={handleLogout} className="btn btn-outline" style={{ width: '100%', marginTop: '1rem', fontSize: '0.85rem' }}>
            Odjavi se
          </button>
        </div>
      </aside>

      <main className="main-content">
        <Routes>
          <Route index element={<DashboardHome />} />
          <Route path="resursi" element={<ResursiPage />} />
        </Routes>
      </main>
    </div>
  );
}
