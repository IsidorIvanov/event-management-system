import { useState } from "react";

const EMPTY = {
  iznos: "",
  razlog: "",
};

const RESERVED_REFUND_STATUSES = new Set(["TRAZENA", "ODOBRENA", "IZVRSENA"]);

const formatMoney = (value) =>
  new Intl.NumberFormat("sr-Latn-RS", {
    style: "currency",
    currency: "RSD",
    maximumFractionDigits: 2,
  }).format(Number(value || 0));

const refundabilno = (placanje) => {
  const rezervisano = (placanje?.refundacije || [])
    .filter((refundacija) => RESERVED_REFUND_STATUSES.has(refundacija.status))
    .reduce((sum, refundacija) => sum + Number(refundacija.iznos || 0), 0);
  return Math.max(0, Number(placanje?.iznos || 0) - rezervisano);
};

export default function RefundacijaModal({ placanje, onClose, onSubmit }) {
  const [form, setForm] = useState(EMPTY);
  const [error, setError] = useState(null);
  const preostalo = refundabilno(placanje);

  const set = (field) => (e) => {
    setForm((prev) => ({ ...prev, [field]: e.target.value }));
    if (error) setError(null);
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    const iznos = Number(form.iznos);
    if (iznos <= 0) return setError("Iznos mora biti veći od 0.");
    if (iznos > preostalo) {
      return setError("Iznos refundacije ne sme preći preostali refundabilni iznos.");
    }
    if (!form.razlog.trim()) return setError("Razlog je obavezan.");

    onSubmit(placanje.placanjeId, {
      iznos: String(form.iznos),
      razlog: form.razlog.trim(),
    });
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div
        className="modal-card modal-card-wide"
        style={{ maxWidth: 560 }}
        onClick={(e) => e.stopPropagation()}
      >
        <h3>Zahtev za refundaciju</h3>
        <p className="page-subtitle" style={{ marginTop: "0.35rem" }}>
          Preostalo za refundaciju: {formatMoney(preostalo)}
        </p>

        {error && (
          <div className="error-msg" style={{ marginBottom: "1rem" }}>
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label>Iznos *</label>
            <input
              type="number"
              min="0"
              step="0.01"
              max={preostalo}
              className="form-control"
              value={form.iznos}
              onChange={set("iznos")}
            />
          </div>

          <div className="form-group">
            <label>Razlog *</label>
            <textarea
              className="form-control"
              rows={4}
              value={form.razlog}
              onChange={set("razlog")}
              placeholder="npr. dvostruka uplata"
            />
          </div>

          <div className="modal-actions">
            <button type="button" className="btn btn-outline" onClick={onClose}>
              Otkaži
            </button>
            <button
              type="submit"
              className="btn btn-primary"
              style={{ width: "auto" }}
            >
              Pošalji zahtev
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
