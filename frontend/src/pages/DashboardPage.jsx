import { useAuth } from '../context/AuthContext';
import { useNavigate, Link, useLocation } from 'react-router-dom';

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

  return (
    <div className="dashboard-layout">
      {/* Sidebar */}
      <aside className="sidebar">
        <div className="sidebar-header">
          <h2>📋 EventSys</h2>
          <div className="role-badge">
            {user.uloga ? ULOGA_DISPLAY[user.uloga] : TIP_DISPLAY[user.tipKorisnika]}
          </div>
        </div>

        <nav className="sidebar-nav">
          <Link to="/dashboard" className={location.pathname === '/dashboard' ? 'active' : ''}>
            <span>🏠</span> <span>Početna</span>
          </Link>

          {/* Finansijski podsistem — samo za FINANSIJSKI_KONTROLOR i MENADZER_DOGADJAJA */}
          {isFinansije && (
            <>
              <Link to="/dashboard/budzet" className={location.pathname.startsWith('/dashboard/budzet') ? 'active' : ''}>
                <span>💰</span> <span>Budžeti</span>
              </Link>
              <Link to="/dashboard/fakture" className={location.pathname.startsWith('/dashboard/fakture') ? 'active' : ''}>
                <span>📄</span> <span>Fakture</span>
              </Link>
              <Link to="/dashboard/troskovi" className={location.pathname.startsWith('/dashboard/troskovi') ? 'active' : ''}>
                <span>📊</span> <span>Troškovi</span>
              </Link>
              <Link to="/dashboard/placanja" className={location.pathname.startsWith('/dashboard/placanja') ? 'active' : ''}>
                <span>💳</span> <span>Plaćanja</span>
              </Link>
            </>
          )}

          {/* Resursi — za KOORDINATOR_RESURSA i MENADZER */}
          {(hasRole('KOORDINATOR_RESURSA') || hasRole('MENADZER_DOGADJAJA')) && (
            <Link to="/dashboard/resursi" className={location.pathname.startsWith('/dashboard/resursi') ? 'active' : ''}>
              <span>🏢</span> <span>Resursi</span>
            </Link>
          )}

          {/* Program — za KOORDINATOR_PROGRAMA i MENADZER */}
          {(hasRole('KOORDINATOR_PROGRAMA') || hasRole('MENADZER_DOGADJAJA')) && (
            <Link to="/dashboard/program" className={location.pathname.startsWith('/dashboard/program') ? 'active' : ''}>
              <span>📅</span> <span>Program</span>
            </Link>
          )}

          {/* Klijent vidi svoje događaje */}
          {hasRole('KLIJENT') && (
            <Link to="/dashboard/moji-dogadjaji" className={location.pathname.startsWith('/dashboard/moji-dogadjaji') ? 'active' : ''}>
              <span>📋</span> <span>Moji događaji</span>
            </Link>
          )}

          {/* Učesnik vidi registracije */}
          {hasRole('UCESNIK') && (
            <Link to="/dashboard/registracije" className={location.pathname.startsWith('/dashboard/registracije') ? 'active' : ''}>
              <span>🎫</span> <span>Moje registracije</span>
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

      {/* Main Content */}
      <main className="main-content">
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
            <h3 style={{ marginBottom: '1rem' }}>🏦 Finansijski podsistem</h3>
            <p style={{ color: 'var(--text-secondary)', lineHeight: 1.6 }}>
              Imate pristup upravljanju budžetima, fakturama, troškovima i plaćanjima.
              Koristite navigaciju sa leve strane za pristup modulima. Funkcionalnost
              upravljanja budžetom i planiranja troškova po kategorijama je u razvoju.
            </p>
          </div>
        )}
      </main>
    </div>
  );
}
