function formatDate(d) {
  return d.toISOString().slice(0, 10);
}

function addDays(dateStr, days) {
  const d = new Date(dateStr);
  d.setDate(d.getDate() + days);
  return formatDate(d);
}

export default function SalaKalendar({ dostupnost, loading, onRefresh }) {
  if (loading) {
    return <p className="empty-hint">Učitavanje kalendara...</p>;
  }

  if (!dostupnost || !dostupnost.sale?.length) {
    return <p className="empty-hint">Nema podataka za kalendar ove sale.</p>;
  }

  const sala = dostupnost.sale[0];

  return (
    <div className="sala-kalendar">
      <div className="kalendar-toolbar">
        <span>
          Period: {dostupnost.datumOd} — {dostupnost.datumDo}
        </span>
        <button type="button" className="btn btn-outline" style={{ width: 'auto' }} onClick={onRefresh}>
          Osveži
        </button>
      </div>

      <div className="kalendar-sala-block">
        <div className="kalendar-legend">
          <span className="legend-free">Slobodno</span>
          <span className="legend-busy">Zauzeto</span>
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
                  className={`kalendar-slot ${slot.dostupno ? 'free' : 'busy'}`}
                  title={
                    slot.dostupno
                      ? `Slobodno ${slot.vremeOd}–${slot.vremeDo}`
                      : `Zauzeto: ${slot.nazivSesije || 'Sesija'} (${slot.vremeOd}–${slot.vremeDo})`
                  }
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
