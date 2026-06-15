import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import api from '@/shared/services/api';
import * as sesijaApi from '@/features/dogadjaji/services/sesijaService';
import * as govornikApi from '@/features/dogadjaji/services/govornikService';
import * as registracijaApi from '@/features/dogadjaji/services/registracijaService';
import { NOTIF_NEW_EVENT } from '@/features/notifikacije/hooks/useNotifikacije';
import RegistracijaModal from '@/features/dogadjaji/components/RegistracijaModal';

const formatDate = (s) =>
  s
    ? new Date(s).toLocaleDateString('sr-Latn', {
        day: '2-digit',
        month: 'short',
        year: 'numeric',
      })
    : '—';

const TABS = ['Informacije', 'Agenda', 'Govornici', 'Učesnici'];

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

function AgendaTab({ event, isRegistered, userRegistration }) {
  const [sesije, setSesije] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [filterDay, setFilterDay] = useState('all');
  const [filterTrack, setFilterTrack] = useState('all');
  const [filterRoom, setFilterRoom] = useState('all');
  const [rasporedIds, setRasporedIds] = useState(new Set());
  const [togglingId, setTogglingId] = useState(null);

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
      } else {
        await sesijaApi.addToRaspored(sesija.sesijaId);
        setRasporedIds((prev) => new Set([...prev, sesija.sesijaId]));
      }
    } catch (err) {
      // silently fail or could show a toast
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
              const { pct, color } = fillBar(0, s.kapacitet);
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

function UcesniciTab({ event }) {
  const [registracije, setRegistracije] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');

  useEffect(() => {
    registracijaApi
      .getRegistracijeByDogadjaj(event.dogadjajId)
      .then((res) => setRegistracije(res.data.filter((r) => r.status === 'POTVRDJENA')))
      .catch(() => setRegistracije([]))
      .finally(() => setLoading(false));
  }, [event.dogadjajId]);

  const filtered = registracije.filter((r) => {
    const q = search.toLowerCase();
    return (
      !q ||
      `${r.ucesnikIme || ''} ${r.ucesnikPrezime || ''}`.toLowerCase().includes(q) ||
      (r.ucesnikKompanija || '').toLowerCase().includes(q) ||
      (r.ucesnikPozicija || '').toLowerCase().includes(q)
    );
  });

  if (loading) return <p className="empty-hint">Učitavanje učesnika...</p>;

  return (
    <div className="govornici-tab">
      {/* Pretraga */}
      <div style={{ marginBottom: '1rem' }}>
        <input
          className="search-input"
          style={{ width: '100%', maxWidth: '360px' }}
          placeholder="pretraži učesnike..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
      </div>

      {filtered.length === 0 ? (
        <div className="sesije-empty">
          <p>Nema registrovanih učesnika za ovaj događaj.</p>
        </div>
      ) : (
        <div className="govornici-grid">
          {filtered.map((r) => (
            <div key={r.registracijaId} className="govornik-card">
              <div className="govornik-card-avatar">
                <div className="govornik-avatar-placeholder" />
              </div>
              <div className="govornik-card-body">
                <div className="govornik-card-name">
                  {r.ucesnikIme} {r.ucesnikPrezime}
                </div>
                <div className="govornik-card-meta">
                  {r.ucesnikPozicija && <span>{r.ucesnikPozicija}</span>}
                  {r.ucesnikPozicija && r.ucesnikKompanija && <span> · </span>}
                  {r.ucesnikKompanija && <span>{r.ucesnikKompanija}</span>}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

function SpeakersTab({ event }) {
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

export default function UcesnikDogadjajDetaljPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [event, setEvent] = useState(null);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState('Informacije');
  const [showRegModal, setShowRegModal] = useState(false);
  const [registered, setRegistered] = useState(false);
  const [userRegistration, setUserRegistration] = useState(null);

  useEffect(() => {
    Promise.all([
      api.get(`/dogadjaj/${id}`),
      registracijaApi.getMyRegistrations(),
    ])
      .then(([eventRes, regRes]) => {
        setEvent(eventRes.data);
        const activeReg = regRes.data.find(
          (r) => r.dogadjajId === Number(id) && r.status !== 'OTKAZANA'
        );
        setRegistered(!!activeReg);
        setUserRegistration(activeReg || null);
      })
      .catch(() => setEvent(null))
      .finally(() => setLoading(false));
  }, [id]);

  // Osveži događaj i registraciju kada stigne notifikacija, da se izmene odmah
  // odraze: promocija sa liste čekanja (D3) ili izmenjen datum/lokacija (D5).
  useEffect(() => {
    const onNotif = () => {
      api.get(`/dogadjaj/${id}`)
        .then((res) => setEvent(res.data))
        .catch(() => {});
      registracijaApi.getMyRegistrations()
        .then((res) => {
          const activeReg = res.data.find(
            (r) => r.dogadjajId === Number(id) && r.status !== 'OTKAZANA'
          );
          setRegistered(!!activeReg);
          setUserRegistration(activeReg || null);
        })
        .catch(() => {});
    };
    window.addEventListener(NOTIF_NEW_EVENT, onNotif);
    return () => window.removeEventListener(NOTIF_NEW_EVENT, onNotif);
  }, [id]);

  if (loading) {
    return <div className="ucesnik-empty" style={{ padding: '3rem' }}>Učitavanje...</div>;
  }

  if (!event) {
    return (
      <div className="ucesnik-empty-box" style={{ margin: '2rem 0' }}>
        <p>Događaj nije pronađen.</p>
        <button className="discover-btn-details" style={{ marginTop: '1rem' }} onClick={() => navigate(-1)}>
          ← Nazad
        </button>
      </div>
    );
  }

  const onWaitlist =
    registered &&
    (userRegistration?.statusKarte === 'NA_CEKANJU' ||
      userRegistration?.status === 'NA_CEKANJU');

  return (
    <div className="ev-detail-page">
      {showRegModal && (
        <RegistracijaModal
          event={event}
          onClose={() => setShowRegModal(false)}
          onSuccess={(newReg) => {
            setShowRegModal(false);
            setRegistered(true);
            if (newReg) setUserRegistration(newReg);
            else {
              // Reload registrations to get the full object
              registracijaApi.getMyRegistrations().then((res) => {
                const activeReg = res.data.find(
                  (r) => r.dogadjajId === Number(id) && r.status !== 'OTKAZANA'
                );
                if (activeReg) setUserRegistration(activeReg);
              }).catch(() => {});
            }
          }}
        />
      )}

      {/* Gornja traka */}
      <div className="ev-detail-topbar">
        <button className="ev-back-btn" onClick={() => navigate(-1)}>
          ← Nazad
        </button>
        {registered ? (
          onWaitlist ? (
            <span style={{ fontSize: '0.9rem', color: 'var(--warning)', fontWeight: 600 }}>
              ⏳ Na listi čekanja
            </span>
          ) : (
            <span style={{ fontSize: '0.9rem', color: 'var(--success)', fontWeight: 600 }}>
              ✓ Uspešno registrovani!
            </span>
          )
        ) : (
          <button className="discover-btn-register ev-register-btn" onClick={() => setShowRegModal(true)}>
            Registruj se za ovaj događaj
          </button>
        )}
      </div>

      {/* Naslov + meta */}
      <div className="ev-detail-hero">
        <h1 className="ev-detail-title">{event.naziv}</h1>
        <p className="ev-detail-meta">
          {formatDate(event.datumPocetka)}
          {event.lokacijaGrad ? ` · ${event.lokacijaGrad}` : ''}
          {event.lokacijaDrzava ? `, ${event.lokacijaDrzava}` : ''}
          {' · Uživo'}
        </p>
      </div>

      {/* Tabovi */}
      <div className="ev-tabs">
        {TABS.map((tab) => (
          <button
            key={tab}
            className={`ev-tab-btn${activeTab === tab ? ' active' : ''}`}
            onClick={() => setActiveTab(tab)}
          >
            {tab}
          </button>
        ))}
      </div>

      {/* Sadržaj taba */}
      {activeTab === 'Informacije' && (
        <div className="ev-info-tab">
          {!registered ? (
            <div className="ev-not-registered-banner">
              <div className="ev-not-registered-text">
                <strong>Još niste registrovani</strong>
                <p>
                  Izaberite kartu da se pridružite događaju {event.naziv}. Karte otključavaju
                  agendu, listu učesnika i vašu QR propusnicu.
                </p>
              </div>
              <button className="discover-btn-register" onClick={() => setShowRegModal(true)}>
                Registruj se
              </button>
            </div>
          ) : onWaitlist ? (
            <div className="ev-not-registered-banner" style={{ background: 'rgba(245, 158, 11, 0.12)', borderColor: 'var(--warning)' }}>
              <div className="ev-not-registered-text">
                <strong style={{ color: 'var(--warning)' }}>⏳ Na listi čekanja</strong>
                <p>
                  Događaj je trenutno popunjen, pa je vaša prijava na listi čekanja. Obavestićemo
                  vas čim se oslobodi mesto i tada će vaša karta biti potvrđena.
                </p>
              </div>
            </div>
          ) : (
            <div className="ev-not-registered-banner" style={{ background: 'var(--success-subtle)', borderColor: 'var(--success)' }}>
              <div className="ev-not-registered-text">
                <strong style={{ color: 'var(--success)' }}>✓ Registrovani ste!</strong>
                <p>Vaša karta je potvrđena. Pogledajte svoje registracije za detalje.</p>
              </div>
            </div>
          )}

          <div className="ev-info-layout">
            <div className="ev-info-left">
              <div className="ev-cover-card">
                <div className="ev-about">
                  <h3 className="ev-about-title">O ovom događaju</h3>
                  <p className="ev-about-desc">
                    {event.opis || 'Nema opisa za ovaj događaj.'}
                  </p>
                </div>
              </div>

              <div className="ev-organizer-card">
                <div className="ev-organizer-label">ORGANIZATOR</div>
                <div className="ev-organizer-body">
                  <div className="ev-organizer-logo" />
                  <div className="ev-organizer-info">
                    <div className="ev-organizer-name">
                      {event.lokacijaNaziv || 'Organizator'}
                      <span className="ev-verified-badge">Verifikovan</span>
                    </div>
                    <div className="ev-organizer-meta">
                      Sedište: {event.lokacijaGrad || '—'}
                    </div>
                    <div className="ev-organizer-desc">
                      Zvanični organizator događaja za ovaj prostor.
                    </div>
                    <div className="ev-organizer-actions">
                      <button className="ev-org-btn">+ Prati</button>
                      <button className="ev-org-btn">Pogledaj druge događaje</button>
                      <button className="ev-org-btn">Kontaktiraj organizatora</button>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div className="ev-info-right">
              <div className="ev-details-card">
                <h3 className="ev-details-title">Detalji</h3>
                <div className="ev-details-rows">
                  <div className="ev-details-row">
                    <span className="ev-details-label">datum</span>
                    <span className="ev-details-value">{formatDate(event.datumPocetka)}</span>
                  </div>
                  <div className="ev-details-row">
                    <span className="ev-details-label">format</span>
                    <span className="ev-details-value">Uživo</span>
                  </div>
                  <div className="ev-details-row">
                    <span className="ev-details-label">država</span>
                    <span className="ev-details-value">{event.lokacijaDrzava || '—'}</span>
                  </div>
                  <div className="ev-details-row">
                    <span className="ev-details-label">grad</span>
                    <span className="ev-details-value">{event.lokacijaGrad || '—'}</span>
                  </div>
                  <div className="ev-details-row">
                    <span className="ev-details-label">adresa</span>
                    <span className="ev-details-value">
                      {event.lokacijaNaziv && event.lokacijaAdresa
                        ? `${event.lokacijaNaziv} · ${event.lokacijaAdresa}`
                        : event.lokacijaNaziv || event.lokacijaAdresa || '—'}
                    </span>
                  </div>
                  {event.maksKapacitet && (
                    <div className="ev-details-row">
                      <span className="ev-details-label">kapacitet</span>
                      <span className="ev-details-value">{event.maksKapacitet}</span>
                    </div>
                  )}
                </div>
              </div>
            </div>
          </div>
        </div>
      )}

      {activeTab === 'Agenda' && <AgendaTab event={event} isRegistered={registered} userRegistration={userRegistration} />}

      {activeTab === 'Govornici' && <SpeakersTab event={event} />}

      {activeTab === 'Učesnici' && <UcesniciTab event={event} />}
    </div>
  );
}
