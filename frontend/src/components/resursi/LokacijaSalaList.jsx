const TIP_LABELS = {
  GLAVNA: 'Glavna',
  WORKSHOP: 'Workshop',
  PANEL: 'Panel',
  FOAJE: 'Foaje',
};

export default function LokacijaSalaList({
  lokacije,
  selectedLokacijaId,
  onSelectLokacija,
  onEditLokacija,
  onDeleteLokacija,
  onAddSala,
  onEditSala,
  onDeleteSala,
  canManageLokacija,
  canManageSala,
}) {
  const selected = lokacije.find((l) => l.lokacijaId === selectedLokacijaId);

  return (
    <div className="lokacija-list">
      <div className="lokacija-list-sidebar">
        <h3>Lokacije</h3>
        {lokacije.length === 0 && (
          <p className="empty-hint">Nema unetih lokacija.</p>
        )}
        <ul>
          {lokacije.map((lok) => (
            <li key={lok.lokacijaId}>
              <button
                type="button"
                className={`lokacija-item ${selectedLokacijaId === lok.lokacijaId ? 'active' : ''}`}
                onClick={() => onSelectLokacija(lok.lokacijaId)}
              >
                <strong>{lok.naziv}</strong>
                <span>{lok.grad}, {lok.drzava}</span>
              </button>
              {canManageLokacija && selectedLokacijaId === lok.lokacijaId && (
                <div className="item-actions">
                  <button type="button" className="btn-link" onClick={() => onEditLokacija(lok)}>
                    Izmeni
                  </button>
                  <button type="button" className="btn-link danger" onClick={() => onDeleteLokacija(lok)}>
                    Obriši
                  </button>
                </div>
              )}
            </li>
          ))}
        </ul>
      </div>

      <div className="lokacija-list-detail">
        {!selected ? (
          <p className="empty-hint">Izaberite lokaciju za pregled sala.</p>
        ) : (
          <>
            <div className="detail-header">
              <div>
                <h3>{selected.naziv}</h3>
                <p className="detail-meta">
                  {selected.adresa}, {selected.grad}, {selected.drzava}
                </p>
              </div>
              {canManageSala && (
                <button type="button" className="btn btn-primary" style={{ width: 'auto' }} onClick={onAddSala}>
                  + Nova sala
                </button>
              )}
            </div>

            {(selected.sale || []).length === 0 ? (
              <p className="empty-hint">Ova lokacija nema sale.</p>
            ) : (
              <div className="sala-grid">
                {selected.sale.map((sala) => (
                  <div key={`${sala.lokacijaId}-${sala.nazivSale}`} className="sala-card">
                    <h4>{sala.nazivSale}</h4>
                    <p>
                      <span className="badge">{TIP_LABELS[sala.tipSale] || sala.tipSale}</span>
                    </p>
                    <p>Kapacitet: <strong>{sala.kapacitet}</strong></p>
                    <p>Cena/dan: <strong>{Number(sala.baznaCenaPoDanu).toLocaleString('sr-RS')} RSD</strong></p>
                    {canManageSala && (
                      <div className="item-actions">
                        <button type="button" className="btn-link" onClick={() => onEditSala(sala)}>
                          Izmeni
                        </button>
                        <button type="button" className="btn-link danger" onClick={() => onDeleteSala(sala)}>
                          Obriši
                        </button>
                      </div>
                    )}
                  </div>
                ))}
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}
