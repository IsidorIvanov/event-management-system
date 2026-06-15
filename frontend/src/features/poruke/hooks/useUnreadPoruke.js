import { useState, useEffect } from 'react';
import { Client } from '@stomp/stompjs';
import * as porukeService from '@/features/poruke/services/porukeService';

/** Događaj kojim se traži trenutno osvežavanje broja nepročitanih poruka. */
export const PORUKE_READ_EVENT = 'poruke:read';

/** Emituj kad se poruke pročitaju (npr. ulazak u chat) da se badge odmah skloni. */
export function notifyPorukeRead() {
  window.dispatchEvent(new Event(PORUKE_READ_EVENT));
}

/**
 * Vraća broj nepročitanih poruka za trenutnog korisnika.
 * - WebSocket: badge se osvežava ODMAH kada poruka stigne (na bilo kojoj strani).
 * - `PORUKE_READ_EVENT`: odmah reaguje kada se poruke pročitaju (ulazak u chat).
 * - Poll: fallback ako WebSocket nije dostupan.
 * Radi samo kada je `enabled` (npr. za uloge koje imaju pristup porukama).
 */
export function useUnreadPoruke(enabled = true, intervalMs = 15000) {
  const [count, setCount] = useState(0);

  useEffect(() => {
    if (!enabled) {
      setCount(0);
      return;
    }
    let active = true;
    const fetchCount = () => {
      porukeService.getNeprocitane()
        .then((r) => { if (active) setCount(r.data?.count ?? 0); })
        .catch(() => { /* tiho — indikator nije kritičan */ });
    };

    fetchCount();
    const id = setInterval(fetchCount, intervalMs);
    window.addEventListener(PORUKE_READ_EVENT, fetchCount);

    // WebSocket — trenutna reakcija na pristiglu poruku
    let client = null;
    const token = localStorage.getItem('token');
    if (token) {
      const proto = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
      client = new Client({
        brokerURL: `${proto}//${window.location.host}/ws-native`,
        connectHeaders: { Authorization: `Bearer ${token}` },
        reconnectDelay: 5000,
        onConnect: () => {
          client.subscribe('/user/queue/messages', (msg) => {
            try {
              const nova = JSON.parse(msg.body);
              if (!nova || nova.porukaId == null || !nova.sadrzaj) return;
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
      window.removeEventListener(PORUKE_READ_EVENT, fetchCount);
      client?.deactivate();
    };
  }, [enabled, intervalMs]);

  return count;
}
