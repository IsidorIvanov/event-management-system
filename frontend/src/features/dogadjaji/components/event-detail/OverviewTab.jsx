import { useState, useEffect } from 'react';
import * as sesijaApi from '@/features/dogadjaji/services/sesijaService';
import * as govornikApi from '@/features/dogadjaji/services/govornikService';
import * as registracijaApi from '@/features/dogadjaji/services/registracijaService';
import { formatDate as fmtDate } from '@/shared/utils/format';
import { STATUS_DISPLAY, STATUS_CLASS } from '@/features/dogadjaji/constants';

const formatDate = (s) => fmtDate(s, { day: '2-digit', month: 'long', year: 'numeric' });

export default function OverviewTab({ event }) {
  const [stats, setStats] = useState({ sesije: null, govornici: null, prodateKarte: null });

  useEffect(() => {
    Promise.all([
      sesijaApi.getSesijeByDogadjaj(event.dogadjajId),
      govornikApi.getGovornikByDogadjaj(event.dogadjajId),
      registracijaApi.getRegistracijeByDogadjaj(event.dogadjajId),
    ])
      .then(([sRes, gRes, rRes]) => {
        setStats({
          sesije: sRes.data.length,
          govornici: gRes.data.length,
          prodateKarte: rRes.data.filter((r) => r.status === 'POTVRDJENA').length,
        });
      })
      .catch(() => { /* zadrži "—" ako učitavanje ne uspe */ });
  }, [event.dogadjajId]);

  return (
    <div className="overview-layout">
      {/* Left — About */}
      <div className="overview-about">
        <div className="events-table-card" style={{ padding: '1.5rem' }}>
          <h3 style={{ marginBottom: '1.25rem' }}>O događaju</h3>

          <div className="event-info-grid">
            <div className="event-info-row">
              <span className="event-info-label">Naziv</span>
              <span className="event-info-value">{event.naziv}</span>
            </div>
            <div className="event-info-row">
              <span className="event-info-label">Lokacija</span>
              <span className="event-info-value">{event.lokacijaNaziv}</span>
            </div>
            <div className="event-info-row">
              <span className="event-info-label">Grad</span>
              <span className="event-info-value">{event.lokacijaGrad}, {event.lokacijaDrzava}</span>
            </div>
            <div className="event-info-row">
              <span className="event-info-label">Datum početka</span>
              <span className="event-info-value">{formatDate(event.datumPocetka)}</span>
            </div>
            <div className="event-info-row">
              <span className="event-info-label">Datum završetka</span>
              <span className="event-info-value">{formatDate(event.datumZavrsetka)}</span>
            </div>
            <div className="event-info-row">
              <span className="event-info-label">Status</span>
              <span className={STATUS_CLASS[event.status] || 'status-badge status-draft'}>
                {STATUS_DISPLAY[event.status] || event.status}
              </span>
            </div>
            {event.opis && (
              <div className="event-info-row event-info-row--full">
                <span className="event-info-label">Opis</span>
                <span className="event-info-value" style={{ lineHeight: 1.7 }}>{event.opis}</span>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Right — Stats + Quick actions */}
      <div className="overview-sidebar">
        <div className="overview-stats">
          <div className="info-card">
            <div className="label">Kapacitet</div>
            <div className="value accent">{event.maksKapacitet}</div>
            <div className="card-hint">maksimalno mesta</div>
          </div>
          <div className="info-card">
            <div className="label">Sesije</div>
            <div className="value success">{stats.sesije ?? '—'}</div>
            <div className="card-hint">planirane sesije</div>
          </div>
          <div className="info-card">
            <div className="label">Govornici</div>
            <div className="value warning">{stats.govornici ?? '—'}</div>
            <div className="card-hint">potvrđeni</div>
          </div>
          <div className="info-card">
            <div className="label">Prodate karte</div>
            <div className="value">{stats.prodateKarte ?? '—'}</div>
            <div className="card-hint">od ukupnog kapaciteta</div>
          </div>
        </div>
      </div>
    </div>
  );
}
