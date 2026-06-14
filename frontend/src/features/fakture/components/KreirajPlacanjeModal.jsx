import { useState } from "react";
import { formatMoney } from "@/shared/utils/format";
import Modal from "@/shared/components/Modal";

const EMPTY = {
  iznos: "",
  referentniBroj: "",
  datumPlacanja: new Date().toISOString().split("T")[0],
  metod: "BANKOVNI_TRANSFER",
  dokumentUrl: "",
  napomena: "",
};

export default function KreirajPlacanjeModal({ onClose, onSubmit, faktura }) {
  const [form, setForm] = useState(EMPTY);
  const [error, setError] = useState(null);
  const preostalo = Math.max(
    0,
    Number(faktura?.ukupnaIznos || 0) - Number(faktura?.placeniIznos || 0),
  );

  const set = (field) => (e) => {
    setForm((prev) => ({ ...prev, [field]: e.target.value }));
    if (error) setError(null);
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    const iznos = Number(form.iznos);
    if (iznos <= 0) return setError("Iznos mora biti veći od 0.");
    if (preostalo > 0 && iznos > preostalo) {
      return setError("Iznos plaćanja ne sme preći preostali dug fakture.");
    }
    if (!form.referentniBroj.trim()) {
      return setError("Referentni broj je obavezan.");
    }
    if (!form.datumPlacanja) {
      return setError("Datum plaćanja je obavezan.");
    }

    onSubmit({
      iznos: String(form.iznos),
      referentniBroj: form.referentniBroj.trim(),
      datumPlacanja: form.datumPlacanja,
      metod: form.metod,
      dokumentUrl: form.dokumentUrl.trim() || null,
      napomena: form.napomena || null,
    });
  };

  return (
    <Modal
      boxClassName="modal-card modal-card-wide"
      style={{ maxWidth: 560 }}
      onClose={onClose}
    >
        <h3>Novo plaćanje</h3>
        {faktura && (
          <p className="page-subtitle" style={{ marginTop: "0.35rem" }}>
            Preostali dug: {formatMoney(preostalo)}
          </p>
        )}

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
              max={preostalo || undefined}
              className="form-control"
              value={form.iznos}
              onChange={set("iznos")}
            />
          </div>

          <div className="form-group">
            <label>Referentni broj *</label>
            <input
              className="form-control"
              value={form.referentniBroj}
              onChange={set("referentniBroj")}
              placeholder="npr. TR-2026-001"
            />
          </div>

          <div className="form-group">
            <label>Datum plaćanja *</label>
            <input
              type="date"
              className="form-control"
              value={form.datumPlacanja}
              onChange={set("datumPlacanja")}
            />
          </div>

          <div className="form-group">
            <label>Metod plaćanja *</label>
            <select
              className="form-control"
              value={form.metod}
              onChange={set("metod")}
            >
              <option value="BANKOVNI_TRANSFER">BANKOVNI_TRANSFER</option>
              <option value="KARTICA">KARTICA</option>
              <option value="GOTOVINA">GOTOVINA</option>
              <option value="CEK">CEK</option>
            </select>
          </div>

          <div className="form-group">
            <label>Dokument URL</label>
            <input
              className="form-control"
              value={form.dokumentUrl}
              onChange={set("dokumentUrl")}
              placeholder="https://..."
            />
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
    </Modal>
  );
}
