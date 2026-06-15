import { useState, useEffect, useRef } from 'react';
import { Client } from '@stomp/stompjs';
import * as notifikacijeService from '@/features/notifikacije/services/notifikacijeService';

/** Događaj kojim se traži trenutno osvežavanje broja nepročitanih notifikacija. */
export const NOTIF_READ_EVENT = 'notifikacije:read';
/** Događaj koji nosi novopristiglu notifikaciju (za žive liste, npr. stranicu obaveštenja). */
export const NOTIF_NEW_EVENT = 'notifikacije:new';

/** Emituj kad se notifikacije pročitaju da se badge odmah osveži. */
export function notifyNotifikacijeRead() {
  window.dispatchEvent(new Event(NOTIF_READ_EVENT));
}

/**
 * Vraća broj nepročitanih notifikacija za trenutnog korisnika.
 * - WebSocket: badge se osvežava ODMAH kada notifikacija stigne (push, AFTER_COMMIT).
 * - `onNew`: opcioni callback (npr. za toast) pozvan sa pristiglom notifikacijom.
 * - `NOTIF_READ_EVENT`: odmah reaguje kada se notifikacije označe pročitanim.
 * - Poll: fallback ako WebSocket nije dostupan.
 */
export function useUnreadNotifikacije(enabled = true, onNew = null, intervalMs = 20000) {
  const [count, setCount] = useState(0);
  const onNewRef = useRef(onNew);
  useEffect(() => { onNewRef.current = onNew; }, [onNew]);

  useEffect(() => {
    if (!enabled) {
      setCount(0);
      return;
    }
    let active = true;
    const fetchCount = () => {
      notifikacijeService.getNeprocitaneBroj()
        .then((r) => {
          if (active) setCount(typeof r.data === 'number' ? r.data : (r.data?.count ?? 0));
        })
        .catch(() => { /* tiho — indikator nije kritičan */ });
    };

    fetchCount();
    const id = setInterval(fetchCount, intervalMs);
    window.addEventListener(NOTIF_READ_EVENT, fetchCount);

    // WebSocket — trenutna reakcija na pristiglu notifikaciju
    let client = null;
    const token = localStorage.getItem('token');
    if (token) {
      const proto = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
      client = new Client({
        brokerURL: `${proto}//${window.location.host}/ws-native`,
        connectHeaders: { Authorization: `Bearer ${token}` },
        reconnectDelay: 5000,
        onConnect: () => {
          client.subscribe('/user/queue/notifikacije', (msg) => {
            try {
              const nova = JSON.parse(msg.body);
              if (!nova || nova.notifikacijaId == null) return;
              if (onNewRef.current) onNewRef.current(nova);
              window.dispatchEvent(new CustomEvent(NOTIF_NEW_EVENT, { detail: nova }));
              fetchCount();
            } catch { /* ignore */ }
          });
        },
      });
      client.activate();
    }

    return () => {
      active = false;
      clearInterval(id);
      window.removeEventListener(NOTIF_READ_EVENT, fetchCount);
      client?.deactivate();
    };
  }, [enabled, intervalMs]);

  return count;
}
