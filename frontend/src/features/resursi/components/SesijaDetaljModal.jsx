const TIP_LABELS = {
  KEYNOTE: 'Keynote',
  WORKSHOP: 'Radionica',
  PANEL: 'Panel',
  NETWORKING: 'Networking',
};

const STATUS_LABELS = {
  DRAFT: 'Nacrt',
  OBJAVLJEN: 'Objavljen',
  AKTIVAN: 'Aktivan',
  ZAVRSEN: 'Završen',
};

function formatTime(t) {
  if (!t) return '—';
  return String(t).slice(0, 5);
}

function formatDate(d) {
  if (!d) return '—';
  return new Date(d + 'T12:00:00').toLocaleDateString('sr-RS', {
    day: 'numeric',
    month: 'long',
    year: 'numeric',
  });
}

function DetailRow({ label, value }) {
  if (value == null || value === '') return null;
  return (
    <div className="detalj-row">
      <span className="detalj-label">{label}</span>
      <span className="detalj-value">{value}</span>
    </div>
  );
}

export default function SesijaDetaljModal({ detalj, loading, error, onClose }) {
  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-card modal-card-wide" onClick={(e) => e.stopPropagation()}>
        <div className="detalj-header">
          <h3>Detalji sesije</h3>
          <button type="button" className="btn-close" onClick={onClose} aria-label="Zatvori">
            ×
          </button>
        </div>

        {loading && <p className="empty-hint">Učitavanje...</p>}
        {error && <div className="error-msg">{error}</div>}

        {!loading && detalj && (
          <div className="detalj-body">
            <section className="detalj-section">
              <h4>{detalj.naziv}</h4>
              <DetailRow
                label="Termin"
                value={`${formatDate(detalj.datum)}, ${formatTime(detalj.vremePocetka)} – ${formatTime(detalj.vremeZavrsetka)}`}
              />
              <DetailRow label="Tip sesije" value={TIP_LABELS[detalj.tip] || detalj.tip} />
              <DetailRow label="Kapacitet sesije" value={detalj.kapacitet} />
              <DetailRow label="Sala" value={detalj.nazivSale} />
              <DetailRow label="Lokacija" value={detalj.lokacijaNaziv} />
              {detalj.opis && <DetailRow label="Opis sesije" value={detalj.opis} />}
            </section>

            <section className="detalj-section">
              <h4>Događaj</h4>
              <DetailRow label="Naziv" value={detalj.dogadjajNaziv} />
              <DetailRow
                label="Status"
                value={STATUS_LABELS[detalj.dogadjajStatus] || detalj.dogadjajStatus}
              />
              <DetailRow
                label="Period događaja"
                value={`${formatDate(detalj.dogadjajDatumOd)} – ${formatDate(detalj.dogadjajDatumDo)}`}
              />
              {detalj.dogadjajOpis && <DetailRow label="Opis događaja" value={detalj.dogadjajOpis} />}
            </section>

            {detalj.govornici?.length > 0 && (
              <section className="detalj-section">
                <h4>Govornici</h4>
                <ul className="detalj-govornici">
                  {detalj.govornici.map((g, i) => (
                    <li key={`${g.ime}-${g.prezime}-${i}`}>
                      <strong>
                        {g.ime} {g.prezime}
                      </strong>
                      {(g.pozicija || g.kompanija) && (
                        <span>
                          {' '}
                          — {[g.pozicija, g.kompanija].filter(Boolean).join(', ')}
                        </span>
                      )}
                    </li>
                  ))}
                </ul>
              </section>
            )}
          </div>
        )}

        <div className="form-actions" style={{ marginTop: '1rem' }}>
          <button type="button" className="btn btn-outline" onClick={onClose}>
            Zatvori
          </button>
        </div>
      </div>
    </div>
  );
}
