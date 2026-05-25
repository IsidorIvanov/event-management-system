import { useState } from "react";

const EMPTY = {
  iznos: "",
  metod: "GOTOVINA",
  napomena: "",
};

export default function KreirajPlacanjeModal({ onClose, onSubmit }) {
  const [form, setForm] = useState(EMPTY);
  const [error, setError] = useState(null);

  const set = (field) => (e) => {
    setForm((prev) => ({ ...prev, [field]: e.target.value }));
    if (error) setError(null);
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    if (Number(form.iznos) <= 0) return setError("Iznos mora biti veći od 0.");

    onSubmit({
      iznos: Number(form.iznos),
      metod: form.metod,
      napomena: form.napomena || null,
    });
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div
        className="modal-card modal-card-wide"
        style={{ maxWidth: 560 }}
        onClick={(e) => e.stopPropagation()}
      >
        <h3>Novo plaćanje</h3>

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
            <label>Metod plaćanja *</label>
            <select
              className="form-control"
              value={form.metod}
              onChange={set("metod")}
            >
              <option value="GOTOVINA">GOTOVINA</option>
              <option value="TRANSFER">TRANSFER</option>
              <option value="KARTICA">KARTICA</option>
              <option value="KOMPENZACIJA">KOMPENZACIJA</option>
            </select>
          </div>

          <div className="form-group">
            <label>Napomena</label>
            <textarea
              className="form-control"
              rows={3}
              value={form.napomena}
              onChange={set("napomena")}
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
              Kreiraj plaćanje
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
