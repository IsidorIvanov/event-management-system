import { useState } from "react";

const EMPTY = {
  iznos: "",
  razlog: "",
};

export default function RefundacijaModal({ placanjeId, onClose, onSubmit }) {
  const [form, setForm] = useState(EMPTY);
  const [error, setError] = useState(null);

  const set = (field) => (e) => {
    setForm((prev) => ({ ...prev, [field]: e.target.value }));
    if (error) setError(null);
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    if (Number(form.iznos) <= 0) return setError("Iznos mora biti veći od 0.");
    if (!form.razlog.trim()) return setError("Razlog je obavezan.");

    onSubmit(placanjeId, {
      iznos: Number(form.iznos),
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
