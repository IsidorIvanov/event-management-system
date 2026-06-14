import { useState, useEffect, useRef, useCallback } from 'react';
import { Client } from '@stomp/stompjs';
import { useAuth } from '@/features/auth/context/AuthContext';
import * as porukeService from '@/features/poruke/services/porukeService';
import { ULOGA_DISPLAY } from '@/shared/constants/korisnik';

function formatTime(dt) {
  if (!dt) return '';
  const d = new Date(dt);
  const now = new Date();
  if (d.toDateString() === now.toDateString()) {
    return d.toLocaleTimeString('sr-Latn', { hour: '2-digit', minute: '2-digit' });
  }
  return d.toLocaleDateString('sr-Latn', { day: '2-digit', month: 'short' });
}

function initials(ime, prezime) {
  return `${ime?.[0] || ''}${prezime?.[0] || ''}`.toUpperCase();
}

export default function PorukeStrana() {
  const { user } = useAuth();
  const [kontakti, setKontakti] = useState([]);
  const [aktivniKontakt, setAktivniKontakt] = useState(null);
  const [poruke, setPoruke] = useState([]);
  const [tekst, setTekst] = useState('');
  const [loadingKontakti, setLoadingKontakti] = useState(true);
  const [loadingPoruke, setLoadingPoruke] = useState(false);
  const [sending, setSending] = useState(false);
  const [wsConnected, setWsConnected] = useState(false);

  const bottomRef = useRef(null);
  const inputRef = useRef(null);
  const stompRef = useRef(null);
  const aktivniRef = useRef(aktivniKontakt);
  // Keep fresh references for use inside callbacks/intervals
  const userRef = useRef(user);
  const porukeRef = useRef(poruke);

  useEffect(() => { aktivniRef.current = aktivniKontakt; }, [aktivniKontakt]);
  useEffect(() => { userRef.current = user; }, [user]);
  useEffect(() => { porukeRef.current = poruke; }, [poruke]);

  // load contacts
  useEffect(() => {
    porukeService.getKontakti()
      .then(r => setKontakti(r.data))
      .catch(console.error)
      .finally(() => setLoadingKontakti(false));
  }, []);

  // websocket — instant delivery when WS works
  useEffect(() => {
    const token = localStorage.getItem('token');
    if (!token) return;
    const proto = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const client = new Client({
      brokerURL: `${proto}//${window.location.host}/ws-native`,
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 3000,
      onConnect: () => {
        setWsConnected(true);
        client.subscribe('/user/queue/messages', (msg) => {
          try {
            const nova = JSON.parse(msg.body);
            // Ignore malformed / bodyless frames — only real messages have an id + content
            if (!nova || nova.porukaId == null || !nova.sadrzaj) return;
            const currentUser = userRef.current;
            // Add to messages if this conversation is open
            if (aktivniRef.current) {
              const otherId = aktivniRef.current.korisnikId;
              if (nova.posiljalacId === otherId || nova.primalacId === otherId) {
                setPoruke(prev => {
                  // Avoid duplicates (polling might have already added it)
                  if (prev.some(p => p.porukaId === nova.porukaId)) return prev;
                  return [...prev, nova];
                });
              }
            }
            // Update contact list preview
            if (currentUser) {
              const drugaStrana = nova.posiljalacId === currentUser.korisnikId
                ? nova.primalacId : nova.posiljalacId;
              setKontakti(prev => prev.map(k =>
                k.korisnikId === drugaStrana
                  ? {
                      ...k,
                      poslednjaPorukaPreview: nova.sadrzaj.length > 55
                        ? nova.sadrzaj.slice(0, 55) + '...' : nova.sadrzaj,
                      vremePoslednjePoruke: nova.vremeSlamja,
                      neprocitanihPoruka: aktivniRef.current?.korisnikId === drugaStrana
                        ? 0 : (k.neprocitanihPoruka || 0) + 1,
                    }
                  : k
              ));
            }
          } catch { /* ignore */ }
        });
      },
      onDisconnect: () => setWsConnected(false),
    });
    client.activate();
    stompRef.current = client;
    return () => client.deactivate();
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  // load conversation when contact changes
  useEffect(() => {
    if (!aktivniKontakt) return;
    setLoadingPoruke(true);
    setPoruke([]);
    porukeService.getKonverzacija(aktivniKontakt.korisnikId)
      .then(({ data }) => {
        setPoruke(data);
        setKontakti(prev => prev.map(k =>
          k.korisnikId === aktivniKontakt.korisnikId ? { ...k, neprocitanihPoruka: 0 } : k
        ));
      })
      .catch(console.error)
      .finally(() => setLoadingPoruke(false));
  }, [aktivniKontakt]);

  // Polling fallback — catches any messages WebSocket missed
  useEffect(() => {
    if (!aktivniKontakt) return;
    const interval = setInterval(() => {
      porukeService.getKonverzacija(aktivniKontakt.korisnikId)
        .then(({ data }) => {
          setPoruke(prev => {
            const existingIds = new Set(prev.map(p => p.porukaId));
            const novePoruke = data.filter(p => !existingIds.has(p.porukaId));
            if (novePoruke.length === 0) return prev;
            // Update contact preview for newly arrived messages
            novePoruke.forEach(nova => {
              const currentUser = userRef.current;
              if (currentUser) {
                const drugaStrana = nova.posiljalacId === currentUser.korisnikId
                  ? nova.primalacId : nova.posiljalacId;
                setKontakti(prev2 => prev2.map(k =>
                  k.korisnikId === drugaStrana
                    ? {
                        ...k,
                        poslednjaPorukaPreview: nova.sadrzaj.length > 55
                          ? nova.sadrzaj.slice(0, 55) + '...' : nova.sadrzaj,
                        vremePoslednjePoruke: nova.vremeSlamja,
                      }
                    : k
                ));
              }
            });
            return [...prev, ...novePoruke];
          });
        })
        .catch(() => {}); // silent — polling failure is non-critical
    }, 3000);
    return () => clearInterval(interval);
  }, [aktivniKontakt]);

  // scroll to bottom on new message
  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [poruke]);

  // send message
  const handleSend = useCallback(async () => {
    if (!tekst.trim() || !aktivniKontakt || sending) return;
    const txt = tekst.trim();
    setTekst('');
    setSending(true);
    try {
      const { data: sent } = await porukeService.posaljiPoruku(aktivniKontakt.korisnikId, txt);
      setPoruke(prev => [...prev, sent]);
      setKontakti(prev => prev.map(k =>
        k.korisnikId === aktivniKontakt.korisnikId
          ? { ...k, poslednjaPorukaPreview: txt.length > 55 ? txt.slice(0, 55) + '...' : txt, vremePoslednjePoruke: sent.vremeSlamja }
          : k
      ));
    } catch (e) {
      console.error(e);
      setTekst(txt);
    } finally {
      setSending(false);
      inputRef.current?.focus();
    }
  }, [tekst, aktivniKontakt, sending]);

  const onKey = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  return (
    <div className="poruke-layout">

      {/* ── Left: contacts ── */}
      <div className="poruke-sidebar">
        <div className="poruke-sidebar-header">
          <h2>Messages</h2>
          <p className="poruke-sidebar-sub">conversations with other participants</p>
        </div>

        <div className="poruke-contact-list">
          {loadingKontakti ? (
            <div className="poruke-empty">Učitavanje...</div>
          ) : kontakti.length === 0 ? (
            <div className="poruke-empty">Nema dostupnih kontakata.</div>
          ) : (
            kontakti.map(k => (
              <button
                key={k.korisnikId}
                className={`poruke-contact-item${aktivniKontakt?.korisnikId === k.korisnikId ? ' active' : ''}`}
                onClick={() => { setAktivniKontakt(k); setTimeout(() => inputRef.current?.focus(), 80); }}
              >
                <div className="poruke-contact-avatar">
                  {initials(k.ime, k.prezime)}
                </div>
                <div className="poruke-contact-info">
                  <div className="poruke-contact-name">
                    {k.ime} {k.prezime}
                  </div>
                  <div className="poruke-contact-preview">
                    {k.poslednjaPorukaPreview || (k.uloga ? ULOGA_DISPLAY[k.uloga] || k.uloga : 'Učesnik')}
                  </div>
                </div>
                <div className="poruke-contact-meta">
                  {k.vremePoslednjePoruke && (
                    <span className="poruke-contact-time">{formatTime(k.vremePoslednjePoruke)}</span>
                  )}
                  {k.neprocitanihPoruka > 0 && (
                    <span className="poruke-unread-badge">{k.neprocitanihPoruka}</span>
                  )}
                </div>
              </button>
            ))
          )}
        </div>
      </div>

      {/* ── Right: chat ── */}
      <div className="poruke-chat">
        {!aktivniKontakt ? (
          <div className="poruke-empty-chat">
            <div className="poruke-empty-icon">💬</div>
            <h3>Odaberite razgovor</h3>
            <p>Kliknite na kontakt sa leve strane.</p>
            {!wsConnected && (
              <p className="poruke-ws-status disconnected">⚠ nema veze sa serverom</p>
            )}
          </div>
        ) : (
          <>
            <div className="poruke-chat-header">
              <div className="poruke-chat-header-avatar">
                {initials(aktivniKontakt.ime, aktivniKontakt.prezime)}
              </div>
              <div className="poruke-chat-header-info">
                <div className="poruke-chat-header-name">
                  {aktivniKontakt.ime} {aktivniKontakt.prezime}
                </div>
                <div className="poruke-chat-header-sub">
                  {aktivniKontakt.uloga
                    ? ULOGA_DISPLAY[aktivniKontakt.uloga] || aktivniKontakt.uloga
                    : 'Učesnik'}
                  {wsConnected && <span className="poruke-ws-dot" />}
                </div>
              </div>
            </div>

            <div className="poruke-messages">
              {loadingPoruke ? (
                <div className="poruke-empty">Učitavanje poruka...</div>
              ) : poruke.length === 0 ? (
                <div className="poruke-empty">još nema poruka — pošaljite prvu!</div>
              ) : (
                poruke.filter(p => p && p.porukaId != null && p.sadrzaj?.trim()).map(p => {
                  const mine = p.posiljalacId === user.korisnikId;
                  return (
                    <div key={p.porukaId} className={`poruke-msg-row ${mine ? 'mine' : 'theirs'}`}>
                      {!mine && (
                        <div className="poruke-msg-avatar">
                          {initials(p.posiljalacIme, p.posiljalacPrezime)}
                        </div>
                      )}
                      <div className="poruke-msg-bubble">
                        <div className="poruke-msg-text">{p.sadrzaj}</div>
                        <div className="poruke-msg-time">{formatTime(p.vremeSlamja)}</div>
                      </div>
                    </div>
                  );
                })
              )}
              <div ref={bottomRef} />
            </div>

            <div className="poruke-input-row">
              <input
                ref={inputRef}
                className="poruke-input"
                placeholder="Unesite poruku..."
                value={tekst}
                onChange={e => setTekst(e.target.value)}
                onKeyDown={onKey}
                disabled={sending}
              />
              <button
                className="poruke-send-btn"
                onClick={handleSend}
                disabled={!tekst.trim() || sending}
              >
                Pošalji
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  );
}







