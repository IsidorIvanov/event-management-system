import { useState, useEffect, useCallback, useRef } from 'react';
import * as notifikacijeService from '@/features/notifikacije/services/notifikacijeService';
import { notifyNotifikacijeRead, NOTIF_NEW_EVENT } from '@/features/notifikacije/hooks/useNotifikacije';

const PAGE_SIZE = 10;

const TIP_META = {
  DOGADJAJ:  { icon: '📅', label: 'Događaj',   color: '#6366f1' },
  SESIJA:    { icon: '🎤', label: 'Sesija',    color: '#a855f7' },
  PODSETNIK: { icon: '⏰', label: 'Podsetnik', color: '#f59e0b' },
  OPSTE:     { icon: '📢', label: 'Opšte',     color: '#14b8a6' },
};

// Redosled tabova za filter po tipu obaveštenja.
const TIP_FILTERI = ['DOGADJAJ', 'SESIJA', 'PODSETNIK', 'OPSTE'];

function formatVreme(dt) {
  if (!dt) return '';
  const d = new Date(dt);
  const now = new Date();
  const min = Math.floor((now - d) / 60000);
  if (min < 1) return 'upravo sada';
  if (min < 60) return `pre ${min} min`;
  if (d.toDateString() === now.toDateString()) return `pre ${Math.floor(min / 60)} h`;
  return d.toLocaleDateString('sr-Latn', { day: '2-digit', month: 'short', year: 'numeric' })
    + ' · ' + d.toLocaleTimeString('sr-Latn', { hour: '2-digit', minute: '2-digit' });
}

export default function ObavestenjaPage() {
  const [items, setItems] = useState([]);
  const [tip, setTip] = useState(null);      // null → svi tipovi
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [markingAll, setMarkingAll] = useState(false);

  const load = useCallback(() => {
    setLoading(true);
    notifikacijeService.getMojeNotifikacije({ tip, page, size: PAGE_SIZE })
      .then((r) => {
        setItems(r.data.content ?? []);
        setTotalPages(r.data.totalPages ?? 0);
      })
      .catch(() => { setItems([]); setTotalPages(0); })
      .finally(() => setLoading(false));
  }, [tip, page]);

  useEffect(() => { load(); }, [load]);

  // Žива lista — kada nova notifikacija stigne preko WebSocket-a, osveži tekuću
  // stranu da paginacija i redosled ostanu ispravni (ref izbegava stale closure).
  const loadRef = useRef(load);
  useEffect(() => { loadRef.current = load; }, [load]);
  useEffect(() => {
    const onNew = () => loadRef.current();
    window.addEventListener(NOTIF_NEW_EVENT, onNew);
    return () => window.removeEventListener(NOTIF_NEW_EVENT, onNew);
  }, []);

  const izaberiTip = (noviTip) => {
    if (noviTip === tip) return;
    setTip(noviTip);
    setPage(0);
  };

  const markRead = (n) => {
    if (n.status === 'PROCITANO') return;
    setItems((prev) => prev.map((x) =>
      x.notifikacijaId === n.notifikacijaId ? { ...x, status: 'PROCITANO' } : x));
    notifikacijeService.oznaciKaoProcitano(n.notifikacijaId)
      .then(() => notifyNotifikacijeRead())
      .catch(() => load());
  };

  const markAll = () => {
    const neprocitane = items.filter((n) => n.status !== 'PROCITANO');
    if (neprocitane.length === 0) return;
    setMarkingAll(true);
    setItems((prev) => prev.map((x) => ({ ...x, status: 'PROCITANO' })));
    Promise.all(neprocitane.map((n) => notifikacijeService.oznaciKaoProcitano(n.notifikacijaId)))
      .then(() => notifyNotifikacijeRead())
      .catch(() => load())
      .finally(() => setMarkingAll(false));
  };

  const neprocitanihBroj = items.filter((n) => n.status !== 'PROCITANO').length;

  return (
    <div className="ucesnik-page">
      <div className="ucesnik-header notif-header">
        <div>
          <h1 className="ucesnik-welcome">🔔 Obaveštenja</h1>
          <p className="ucesnik-subtitle">
            najnovije vesti i promene
            {neprocitanihBroj > 0 && ` · ${neprocitanihBroj} nepročitano na strani`}
          </p>
        </div>
        {neprocitanihBroj > 0 && (
          <button className="btn btn-outline btn-sm" onClick={markAll} disabled={markingAll}>
            {markingAll ? 'Označavanje...' : 'Označi stranu kao pročitano'}
          </button>
        )}
      </div>

      <div className="notif-filteri">
        <button
          className={`notif-filter${tip === null ? ' notif-filter-active' : ''}`}
          onClick={() => izaberiTip(null)}
        >
          Sve
        </button>
        {TIP_FILTERI.map((t) => {
          const meta = TIP_META[t];
          return (
            <button
              key={t}
              className={`notif-filter${tip === t ? ' notif-filter-active' : ''}`}
              style={{ '--notif-accent': meta.color }}
              onClick={() => izaberiTip(t)}
            >
              <span aria-hidden="true">{meta.icon}</span> {meta.label}
            </button>
          );
        })}
      </div>

      <section className="ucesnik-section">
        {loading ? (
          <p className="ucesnik-empty">Učitavanje...</p>
        ) : items.length === 0 ? (
          <div className="ucesnik-empty-box">
            <p>
              {tip
                ? `Nemate obaveštenja tipa „${TIP_META[tip].label}“.`
                : 'Nemate nijedno obaveštenje.'}
            </p>
            <p style={{ fontSize: '0.85rem', marginTop: '0.5rem', color: 'var(--text-muted)' }}>
              Ovde će se pojaviti obaveštenja o vašim događajima, sesijama i podsetnicima.
            </p>
          </div>
        ) : (
          <>
            <div className="notif-list">
              {items.map((n) => {
                const meta = TIP_META[n.tip] || TIP_META.OPSTE;
                const unread = n.status !== 'PROCITANO';
                return (
                  <div
                    key={n.notifikacijaId}
                    className={`notif-item${unread ? ' notif-item-unread' : ''}`}
                    style={{ '--notif-accent': meta.color }}
                    onClick={() => markRead(n)}
                    title={unread ? 'Klikni da označiš kao pročitano' : undefined}
                  >
                    <div className="notif-icon">{meta.icon}</div>
                    <div className="notif-body">
                      <div className="notif-top">
                        <span className="notif-cat">{meta.label}</span>
                        <span className="notif-time">{formatVreme(n.vremeSlanja)}</span>
                      </div>
                      <div className="notif-text">{n.sadrzaj}</div>
                    </div>
                    {unread && <span className="notif-dot" title="Nepročitano" />}
                  </div>
                );
              })}
            </div>

            {totalPages > 1 && (
              <div className="notif-paginacija">
                <button
                  className="btn btn-outline btn-sm"
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                  disabled={page === 0}
                >
                  ← Prethodna
                </button>
                <span className="notif-paginacija-info">
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
          </>
        )}
      </section>
    </div>
  );
}
