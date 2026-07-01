function formatDate(d) {
  return d.toISOString().slice(0, 10);
}

function addDays(dateStr, days) {
  const d = new Date(dateStr);
  d.setDate(d.getDate() + days);
  return formatDate(d);
}

export default function SalaKalendar({ dostupnost, loading, onSlotClick }) {
  if (loading) {
    return <p className="empty-hint">Učitavanje kalendara...</p>;
  }

  if (!dostupnost || !dostupnost.sale?.length) {
    return <p className="empty-hint">Nema podataka za kalendar ove sale.</p>;
  }

  const sala = dostupnost.sale[0];

  const handleSlotClick = (slot) => {
    if (!slot.dostupno && slot.sesijaId && onSlotClick) {
      onSlotClick(slot.sesijaId);
    }
  };

  return (
    <div className="sala-kalendar">
      <p className="kalendar-period">
        Period: {dostupnost.datumOd} — {dostupnost.datumDo}
      </p>
      <p className="empty-hint" style={{ marginBottom: '0.75rem' }}>
        Kliknite na zauzeti termin za detalje sesije i događaja.
      </p>

      <div className="kalendar-sala-block">
        <div className="kalendar-legend">
          <span className="legend-free">Slobodno</span>
          <span className="legend-busy">Zauzeto (klik za detalje)</span>
        </div>

        {sala.dani.map((dan) => (
          <div key={dan.datum} className="kalendar-day">
            <div className="kalendar-day-label">
              {new Date(dan.datum + 'T12:00:00').toLocaleDateString('sr-RS', {
                weekday: 'short',
                day: 'numeric',
                month: 'short',
              })}
            </div>
            <div className="kalendar-grid">
              {dan.slotovi.map((slot) => (
                <div
                  key={`${dan.datum}-${slot.vremeOd}`}
                  role={!slot.dostupno && slot.sesijaId ? 'button' : undefined}
                  tabIndex={!slot.dostupno && slot.sesijaId ? 0 : undefined}
                  className={`kalendar-slot ${slot.dostupno ? 'free' : 'busy'} ${!slot.dostupno && slot.sesijaId ? 'clickable' : ''}`}
                  title={
                    slot.dostupno
                      ? `Slobodno ${slot.vremeOd}–${slot.vremeDo}`
                      : `Kliknite za detalje: ${slot.nazivSesije || 'Sesija'}`
                  }
                  onClick={() => handleSlotClick(slot)}
                  onKeyDown={(e) => {
                    if ((e.key === 'Enter' || e.key === ' ') && !slot.dostupno) {
                      e.preventDefault();
                      handleSlotClick(slot);
                    }
                  }}
                >
                  <span className="slot-time">{slot.vremeOd?.slice(0, 5)}</span>
                  {!slot.dostupno && (
                    <span className="slot-label">{slot.nazivSesije || 'Zauzeto'}</span>
                  )}
                </div>
              ))}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

export { formatDate, addDays };
