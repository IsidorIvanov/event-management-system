import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { NOTIF_NEW_EVENT } from '@/features/notifikacije/hooks/useNotifikacije';

const TIP_META = {
  DOGADJAJ:  { icon: '📅', label: 'Događaj',   color: '#6366f1' },
  SESIJA:    { icon: '🎤', label: 'Sesija',    color: '#a855f7' },
  PODSETNIK: { icon: '⏰', label: 'Podsetnik', color: '#f59e0b' },
  OPSTE:     { icon: '📢', label: 'Opšte',     color: '#14b8a6' },
};

const TRAJANJE = 6000;

/**
 * Prikazuje stilizovane „push" toast-ove kada stigne nova notifikacija
 * (NOTIF_NEW_EVENT). Vizuelno prati listu obaveštenja — ikonica u boji tipa,
 * kategorija i poruka skraćena na 2 reda. Klik vodi na stranicu obaveštenja.
 */
export default function NotifikacijaToaster() {
  const [toasts, setToasts] = useState([]);
  const navigate = useNavigate();

  const remove = useCallback((id) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  useEffect(() => {
    const onNew = (e) => {
      const n = e.detail;
      if (!n || n.notifikacijaId == null) return;
      const id = `${n.notifikacijaId}-${Date.now()}`;
      setToasts((prev) => [...prev, { id, n }]);
      setTimeout(() => remove(id), TRAJANJE);
    };
    window.addEventListener(NOTIF_NEW_EVENT, onNew);
    return () => window.removeEventListener(NOTIF_NEW_EVENT, onNew);
  }, [remove]);

  if (toasts.length === 0) return null;

  return (
    <div className="notif-toaster">
      {toasts.map(({ id, n }) => {
        const meta = TIP_META[n.tip] || TIP_META.OPSTE;
        return (
          <div
            key={id}
            className="notif-toast"
            style={{ '--notif-accent': meta.color }}
            onClick={() => { navigate('/dashboard/obavesta'); remove(id); }}
            role="button"
            title="Otvori obaveštenja"
          >
            <div className="notif-toast-icon">{meta.icon}</div>
            <div className="notif-toast-body">
              <div className="notif-toast-title">{meta.label}</div>
              <div className="notif-toast-text">{n.sadrzaj}</div>
            </div>
            <button
              className="notif-toast-close"
              onClick={(ev) => { ev.stopPropagation(); remove(id); }}
              aria-label="Zatvori"
            >
              ✕
            </button>
          </div>
        );
      })}
    </div>
  );
}
