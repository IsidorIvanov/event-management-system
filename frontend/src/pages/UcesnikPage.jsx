import { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import api from '../services/api';

const formatDate = (s) =>
  s
    ? new Date(s).toLocaleDateString('sr-Latn', {
        day: '2-digit',
        month: 'short',
        year: 'numeric',
      })
    : '—';

const TIP_DISPLAY = {
  IN_PERSON: 'In-person',
  ONLINE: 'Online',
  HYBRID: 'Hibridni',
};

function EventCard({ event }) {
  return (
    <div className="ucesnik-event-card">
      <div className="ucesnik-event-banner">
        <span className="ucesnik-event-banner-label">[ event banner ]</span>
      </div>
      <div className="ucesnik-event-info">
        <div className="ucesnik-event-name">{event.naziv}</div>
        <div className="ucesnik-event-meta">
          {formatDate(event.datumPocetka)}
          {event.lokacijaGrad ? ` · ${event.lokacijaGrad}` : ''}
          {event.tip ? ` · ${TIP_DISPLAY[event.tip] || event.tip}` : ' · In-person'}
        </div>
        {event.tipKarte && (
          <div className="ucesnik-event-ticket">
            ticket: {event.tipKarte}
          </div>
        )}
      </div>
    </div>
  );
}

export default function UcesnikPage() {
  const { user } = useAuth();
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api
      .get('/dogadjaj')
      .then((res) => {
        const now = new Date();
        const upcoming = res.data
          .filter(
            (e) =>
              (e.status === 'OBJAVLJEN' || e.status === 'AKTIVAN') &&
              new Date(e.datumPocetka) >= now,
          )
          .sort((a, b) => new Date(a.datumPocetka) - new Date(b.datumPocetka));
        setEvents(upcoming);
      })
      .catch(() => setEvents([]))
      .finally(() => setLoading(false));
  }, []);

  return (
    <div className="ucesnik-page">
      {/* Header */}
      <div className="ucesnik-header">
        <h1 className="ucesnik-welcome">Dobrodošli, {user?.ime}!</h1>
        <p className="ucesnik-subtitle">vaši nadolazeći događaji i preporuke</p>
      </div>

      {/* Heads-up notification */}
      {events.length > 0 && (
        <div className="ucesnik-headsup">
          <span className="ucesnik-headsup-dot" />
          <span className="ucesnik-headsup-text">
            <strong>Napomena:</strong> Pronašli smo {events.length}{' '}
            {events.length === 1
              ? 'nadolazeći događaj'
              : events.length < 5
              ? 'nadolazeća događaja'
              : 'nadolazećih događaja'}{' '}
            za vas.
          </span>
          <button className="ucesnik-headsup-view btn btn-outline btn-sm">
            Pregled
          </button>
        </div>
      )}

      {/* My Upcoming Events */}
      <section className="ucesnik-section">
        <h2 className="ucesnik-section-title">Moji nadolazeći događaji</h2>
        {loading ? (
          <p className="ucesnik-empty">Učitavanje...</p>
        ) : events.length === 0 ? (
          <div className="ucesnik-empty-box">
            <p>Nema nadolazećih događaja.</p>
            <p style={{ fontSize: '0.85rem', marginTop: '0.5rem', color: 'var(--text-muted)' }}>
              Pogledajte sekciju <strong>Otkrijte događaje</strong> da se prijavite na neki događaj.
            </p>
          </div>
        ) : (
          <div className="ucesnik-events-grid">
            {events.map((event) => (
              <EventCard key={event.dogadjajId} event={event} />
            ))}
          </div>
        )}
      </section>
    </div>
  );
}

