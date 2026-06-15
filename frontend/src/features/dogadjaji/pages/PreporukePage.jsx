import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import * as preporukaApi from '@/features/dogadjaji/services/preporukaService';
import * as sesijaApi from '@/features/dogadjaji/services/sesijaService';
import { formatDate } from '@/shared/utils/format';

const TIP_LABEL = {
  KEYNOTE: 'Keynote',
  WORKSHOP: 'Radionica',
  PANEL: 'Panel',
  NETWORKING: 'Networking',
};

const formatRange = (od, doDatum) => {
  if (!od) return '—';
  const start = formatDate(od);
  if (!doDatum || doDatum === od) return start;
  return `${start} – ${formatDate(doDatum)}`;
};

const formatSesijaDate = (datum) => {
  if (!datum) return '—';
  const d = new Date(datum + 'T00:00:00');
  return d.toLocaleDateString('sr-Latn', { day: '2-digit', month: 'short', year: 'numeric' });
};

const dayOfWeek = (datum) => {
  if (!datum) return '';
  const d = new Date(datum + 'T00:00:00');
  return d.toLocaleDateString('sr-Latn', { weekday: 'long' });
};

const skorClass = (skor) => {
  const n = Number(skor);
  if (n >= 75) return 'preporuka-skor-high';
  if (n >= 50) return 'preporuka-skor-mid';
  return 'preporuka-skor-low';
};

export default function PreporukePage() {
  const navigate = useNavigate();
  const [preporuke, setPreporuke] = useState([]);
  const [loading, setLoading] = useState(true);
  // Set of dogadjajId-s that are currently collapsed
  const [collapsed, setCollapsed] = useState(new Set());
  // Sessions per event, lazily loaded: { [dogadjajId]: { loading, sesije } }
  const [sessions, setSessions] = useState({});
  // Contacts per event, lazily loaded: { [dogadjajId]: { loading, organizatori, ucesnici } }
  const [kontakti, setKontakti] = useState({});
  // Set of sesijaId-s already in the participant's schedule
  const [rasporedIds, setRasporedIds] = useState(new Set());
  const [togglingId, setTogglingId] = useState(null);

  useEffect(() => {
    setLoading(true);
    Promise.all([
      preporukaApi.getMojePreporuke(),
      sesijaApi.getMojRasporedIds(),
    ])
      .then(([prepRes, idsRes]) => {
        setPreporuke(prepRes.data);
        setRasporedIds(new Set(idsRes.data));
        // Events render expanded by default → preload their sessions + contacts.
        prepRes.data.forEach((p) => {
          loadSessions(p.dogadjajId);
          loadKontakti(p.dogadjajId);
        });
      })
      .catch(() => setPreporuke([]))
      .finally(() => setLoading(false));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const loadSessions = (dogadjajId) => {
    setSessions((prev) => {
      if (prev[dogadjajId]) return prev; // already loaded / loading
      return { ...prev, [dogadjajId]: { loading: true, sesije: [] } };
    });
    sesijaApi.getSesijeByDogadjaj(dogadjajId)
      .then((res) => setSessions((prev) => ({ ...prev, [dogadjajId]: { loading: false, sesije: res.data } })))
      .catch(() => setSessions((prev) => ({ ...prev, [dogadjajId]: { loading: false, sesije: [] } })));
  };

  const loadKontakti = (dogadjajId) => {
    setKontakti((prev) => {
      if (prev[dogadjajId]) return prev; // already loaded / loading
      return { ...prev, [dogadjajId]: { loading: true, organizatori: [], ucesnici: [] } };
    });
    preporukaApi.getKontaktiZaDogadjaj(dogadjajId)
      .then((res) => setKontakti((prev) => ({ ...prev, [dogadjajId]: { loading: false, ...res.data } })))
      .catch(() => setKontakti((prev) => ({ ...prev, [dogadjajId]: { loading: false, organizatori: [], ucesnici: [] } })));
  };

  const toggleCollapse = (dogadjajId) => {
    setCollapsed((prev) => {
      const next = new Set(prev);
      if (next.has(dogadjajId)) {
        next.delete(dogadjajId);
      } else {
        next.add(dogadjajId);
      }
      return next;
    });
    // When expanding (it was collapsed), make sure sessions + contacts are loaded
    if (collapsed.has(dogadjajId)) {
      if (!sessions[dogadjajId]) loadSessions(dogadjajId);
      if (!kontakti[dogadjajId]) loadKontakti(dogadjajId);
    }
  };

  const handleToggleRaspored = async (e, sesija) => {
    e.stopPropagation();
    if (togglingId) return;
    setTogglingId(sesija.sesijaId);
    try {
      if (rasporedIds.has(sesija.sesijaId)) {
        await sesijaApi.removeFromRaspored(sesija.sesijaId);
        setRasporedIds((prev) => { const n = new Set(prev); n.delete(sesija.sesijaId); return n; });
      } else {
        await sesijaApi.addToRaspored(sesija.sesijaId);
        setRasporedIds((prev) => new Set([...prev, sesija.sesijaId]));
      }
    } catch {
      // silently fail
    } finally {
      setTogglingId(null);
    }
  };

  const renderSesijaCard = (s) => {
    const inRaspored = rasporedIds.has(s.sesijaId);
    const isToggling = togglingId === s.sesijaId;
    return (
      <div key={s.sesijaId} className="raspored-sesija-card">
        <div className="raspored-sesija-time">
          <span>{s.vremePocetka?.slice(0, 5)}</span>
          <span className="raspored-sesija-time-sep">–</span>
          <span>{s.vremeZavrsetka?.slice(0, 5)}</span>
        </div>

        <div className="raspored-sesija-body">
          <div className="raspored-sesija-name">{s.naziv}</div>
          <div className="raspored-sesija-meta">
            <span className="sesija-track-badge" style={{ fontSize: '0.7rem' }}>
              {TIP_LABEL[s.tip] || s.tip}
            </span>
            {s.nazivSale && (
              <span className="raspored-sesija-room">📍 {s.nazivSale}</span>
            )}
          </div>
          {s.opis && <div className="raspored-sesija-opis">{s.opis}</div>}
          {s.govornici?.length > 0 && (
            <div className="raspored-sesija-speakers">
              🎤 {s.govornici.map((g) => `${g.ime} ${g.prezime}`).join(', ')}
            </div>
          )}
        </div>

        <div className="raspored-sesija-actions">
          <button
            className={`sesija-raspored-btn${inRaspored ? ' in-raspored' : ''}`}
            disabled={isToggling}
            onClick={(e) => handleToggleRaspored(e, s)}
            title={inRaspored ? 'Ukloni iz rasporeda' : 'Dodaj u raspored'}
          >
            {isToggling ? '...' : inRaspored ? '✓ U rasporedu' : '+ Raspored'}
          </button>
        </div>
      </div>
    );
  };

  const renderSesije = (dogadjajId) => {
    const entry = sessions[dogadjajId];
    if (!entry || entry.loading) {
      return <p className="empty-hint">Učitavanje sesija...</p>;
    }
    if (entry.sesije.length === 0) {
      return <p className="empty-hint">Ovaj događaj još nema objavljenih sesija.</p>;
    }

    const uniqueDates = [...new Set(entry.sesije.map((s) => s.datum))].sort();
    return uniqueDates.map((datum) => {
      const daySesije = entry.sesije
        .filter((s) => s.datum === datum)
        .sort((a, b) => (a.vremePocetka > b.vremePocetka ? 1 : -1));
      return (
        <div key={datum} className="raspored-event-day-block">
          <div className="raspored-date-label">
            <span className="raspored-date-label-text">{formatSesijaDate(datum)}</span>
            <span className="raspored-date-label-dow">{dayOfWeek(datum)}</span>
            <span className="raspored-date-label-count">{daySesije.length} ses.</span>
          </div>
          <div className="raspored-sesije-list">
            {daySesije.map(renderSesijaCard)}
          </div>
        </div>
      );
    });
  };

  const openPoruke = (k) => {
    navigate('/dashboard/poruke', { state: { kontakt: k } });
  };

  const renderKontaktCard = (k) => {
    const initials = `${k.ime?.[0] || ''}${k.prezime?.[0] || ''}`.toUpperCase();
    const sub = k.uloga
      ? k.uloga.replace(/_/g, ' ').toLowerCase()
      : [k.pozicija, k.kompanija].filter(Boolean).join(' · ');
    return (
      <div key={k.korisnikId} className="preporuka-kontakt-card">
        <div className="preporuka-kontakt-avatar">{initials || '?'}</div>
        <div className="preporuka-kontakt-body">
          <div className="preporuka-kontakt-name">{k.ime} {k.prezime}</div>
          {sub && <div className="preporuka-kontakt-sub">{sub}</div>}
          <div className="preporuka-kontakt-links">
            {k.email && <a href={`mailto:${k.email}`} className="preporuka-kontakt-link">✉ {k.email}</a>}
            {k.telefon && <a href={`tel:${k.telefon}`} className="preporuka-kontakt-link">☎ {k.telefon}</a>}
          </div>
          <button
            className="preporuka-kontakt-msg-btn"
            onClick={() => openPoruke(k)}
          >
            💬 Pošalji poruku
          </button>
        </div>
      </div>
    );
  };

  const renderKontakti = (dogadjajId) => {
    const entry = kontakti[dogadjajId];
    if (!entry || entry.loading) {
      return <p className="empty-hint">Učitavanje kontakata...</p>;
    }
    const orgs = entry.organizatori || [];
    const ucs = entry.ucesnici || [];
    if (orgs.length === 0 && ucs.length === 0) {
      return <p className="empty-hint">Nema dostupnih kontakata za ovaj događaj.</p>;
    }
    return (
      <>
        <div className="preporuka-kontakt-grupa">
          <div className="preporuka-kontakt-grupa-label">Organizatori ({orgs.length})</div>
          {orgs.length > 0 ? (
            <div className="preporuka-kontakt-list">{orgs.map(renderKontaktCard)}</div>
          ) : (
            <p className="empty-hint">Nema organizatora.</p>
          )}
        </div>
        <div className="preporuka-kontakt-grupa">
          <div className="preporuka-kontakt-grupa-label">Učesnici ({ucs.length})</div>
          {ucs.length > 0 ? (
            <div className="preporuka-kontakt-list">{ucs.map(renderKontaktCard)}</div>
          ) : (
            <p className="empty-hint">Još nema registrovanih učesnika.</p>
          )}
        </div>
      </>
    );
  };

  return (
    <div className="ucesnik-page">
      <div className="ucesnik-header">
        <h1 className="ucesnik-welcome">⭐ Preporuke</h1>
        <p className="ucesnik-subtitle">događaji prilagođeni vašim interesovanjima</p>
      </div>

      {loading ? (
        <p className="ucesnik-empty">Učitavanje...</p>
      ) : preporuke.length === 0 ? (
        <div className="ucesnik-empty-box">
          <p>Trenutno nemamo preporuka za vas.</p>
          <p style={{ fontSize: '0.85rem', marginTop: '0.5rem', color: 'var(--text-muted)' }}>
            Dodajte ili izmenite svoja <strong>interesovanja</strong> u profilu kako bismo
            pronašli događaje koji vam odgovaraju. Preporučuju se samo objavljeni i aktivni događaji.
          </p>
          <button
            className="discover-btn-register"
            style={{ marginTop: '1rem', padding: '0.5rem 1.25rem', fontSize: '0.9rem' }}
            onClick={() => navigate('/dashboard/otkrijte')}
          >
            Otkrijte događaje
          </button>
        </div>
      ) : (
        <div className="raspored-container">
          <div className="raspored-topbar">
            <div className="ucesnik-headsup" style={{ margin: 0, width: '100%' }}>
              <span className="ucesnik-headsup-dot" />
              <span className="ucesnik-headsup-text">
                Pronašli smo <strong>{preporuke.length}</strong>{' '}
                {preporuke.length === 1 ? 'događaj' : 'događaja'}{' '}
                koji se poklapaju sa vašim interesovanjima.
              </span>
            </div>
            {preporuke.length > 1 && (
              <div className="raspored-expand-btns">
                <button
                  className="raspored-expand-btn"
                  onClick={() => {
                    setCollapsed(new Set());
                    preporuke.forEach((p) => {
                      if (!sessions[p.dogadjajId]) loadSessions(p.dogadjajId);
                      if (!kontakti[p.dogadjajId]) loadKontakti(p.dogadjajId);
                    });
                  }}
                >
                  ↕ Expand all
                </button>
                <button
                  className="raspored-expand-btn"
                  onClick={() => setCollapsed(new Set(preporuke.map((p) => p.dogadjajId)))}
                >
                  ↕ Collapse all
                </button>
              </div>
            )}
          </div>

          {preporuke.map((p) => {
            const isCollapsed = collapsed.has(p.dogadjajId);
            return (
              <div key={p.dogadjajId} className="raspored-event-group">
                {/* Collapsible event header */}
                <div
                  className={`raspored-event-header${isCollapsed ? ' collapsed' : ''}`}
                  onClick={() => toggleCollapse(p.dogadjajId)}
                  role="button"
                  tabIndex={0}
                  onKeyDown={(e) => e.key === 'Enter' && toggleCollapse(p.dogadjajId)}
                >
                  <span className="raspored-event-chevron">{isCollapsed ? '▶' : '▼'}</span>
                  <span className="raspored-event-name">{p.naziv || `Događaj #${p.dogadjajId}`}</span>
                  <span className={`preporuka-skor-badge ${skorClass(p.skorPoklapanja)}`}>
                    {Math.round(Number(p.skorPoklapanja))}% poklapanje
                  </span>
                  {p.lokacijaGrad && (
                    <span className="raspored-event-city">📍 {p.lokacijaGrad}</span>
                  )}
                  <button
                    className="raspored-event-goto"
                    onClick={(e) => { e.stopPropagation(); navigate(`/dashboard/dogadjaj/${p.dogadjajId}`); }}
                    title="Idi na događaj"
                  >
                    →
                  </button>
                </div>

                {/* Collapsible body */}
                {!isCollapsed && (
                  <div className="raspored-event-body">
                    <div className="preporuka-detalji">
                      <div className="preporuka-meta">
                        <span className="preporuka-meta-item">
                          📅 {formatRange(p.datumPocetka, p.datumZavrsetka)}
                        </span>
                        {(p.lokacijaNaziv || p.lokacijaGrad) && (
                          <span className="preporuka-meta-item">
                            📍 {[p.lokacijaNaziv, p.lokacijaGrad, p.lokacijaDrzava].filter(Boolean).join(', ')}
                          </span>
                        )}
                      </div>

                      {p.razlog && (
                        <div className="preporuka-razlog">
                          <span className="preporuka-razlog-icon">💡</span>
                          <span>{p.razlog}</span>
                        </div>
                      )}

                      {p.zajednickiTagovi?.length > 0 && (
                        <div className="preporuka-tagovi">
                          <span className="preporuka-tagovi-label">Zajednička interesovanja:</span>
                          {p.zajednickiTagovi.map((t) => (
                            <span key={t} className="preporuka-tag preporuka-tag-match">{t}</span>
                          ))}
                        </div>
                      )}

                      {p.tagovi?.length > 0 && (
                        <div className="preporuka-tagovi">
                          <span className="preporuka-tagovi-label">Sve teme događaja:</span>
                          {[...p.tagovi].map((t) => {
                            const isMatch = p.zajednickiTagovi?.some(
                              (z) => z.toLowerCase() === t.toLowerCase()
                            );
                            return (
                              <span
                                key={t}
                                className={`preporuka-tag${isMatch ? ' preporuka-tag-match' : ''}`}
                              >
                                {t}
                              </span>
                            );
                          })}
                        </div>
                      )}

                      {p.opis && <p className="preporuka-opis">{p.opis}</p>}

                      <div className="preporuka-actions">
                        <button
                          className="discover-btn-details"
                          onClick={() => navigate(`/dashboard/dogadjaj/${p.dogadjajId}`)}
                        >
                          Detalji
                        </button>
                        <button
                          className="discover-btn-register"
                          onClick={() => navigate(`/dashboard/dogadjaj/${p.dogadjajId}`)}
                        >
                          Registruj se
                        </button>
                      </div>
                    </div>

                    {/* Sessions — same look as Moj raspored, with add-to-schedule */}
                    <div className="preporuka-sesije">
                      <div className="preporuka-sesije-title">Sesije događaja</div>
                      {renderSesije(p.dogadjajId)}
                    </div>

                    {/* Contacts — organizers & participants of the event */}
                    <div className="preporuka-kontakti">
                      <div className="preporuka-sesije-title">Kontakti</div>
                      {renderKontakti(p.dogadjajId)}
                    </div>
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
