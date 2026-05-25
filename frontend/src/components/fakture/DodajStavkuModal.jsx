import { useEffect, useMemo, useState } from "react";

const EMPTY = {
  naziv: "",
  kolicina: "",
  jedinicnaCena: "",
  napomena: "",
  budzetId: "",
  kategorijaId: "",
};

const toNumber = (value) => Number(value || 0);

export default function DodajStavkuModal({ onClose, onSubmit }) {
  const [form, setForm] = useState(EMPTY);
  const [error, setError] = useState(null);

  const ukupnaCena = useMemo(() => {
    const kolicina = toNumber(form.kolicina);
    const cena = toNumber(form.jedinicnaCena);
    return kolicina && cena ? kolicina * cena : 0;
  }, [form.kolicina, form.jedinicnaCena]);

  const set = (field) => (e) => {
    setForm((prev) => ({ ...prev, [field]: e.target.value }));
    if (error) setError(null);
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!form.naziv.trim()) return setError("Naziv stavke je obavezan.");
    if (toNumber(form.kolicina) <= 0)
      return setError("Količina mora biti veća od 0.");
    if (toNumber(form.jedinicnaCena) < 0)
      return setError("Jedinična cena ne može biti negativna.");
    if (!form.budzetId) return setError("Budžet je obavezan.");
    if (!form.kategorijaId) return setError("Kategorija je obavezna.");

    onSubmit({
      naziv: form.naziv.trim(),
      kolicina: toNumber(form.kolicina),
      jedinicnaCena: toNumber(form.jedinicnaCena),
      ukupnaCena,
      napomena: form.napomena || null,
      budzetId: Number(form.budzetId),
      kategorijaId: Number(form.kategorijaId),
    });
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div
        className="modal-card modal-card-wide"
        style={{ maxWidth: 620 }}
        onClick={(e) => e.stopPropagation()}
      >
        <h3>Dodaj stavku</h3>

        {error && (
          <div className="error-msg" style={{ marginBottom: "1rem" }}>
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label>Naziv *</label>
            <input
              className="form-control"
              value={form.naziv}
              onChange={set("naziv")}
              placeholder="npr. Iznajmljivanje opreme"
            />
          </div>

          <div className="form-row">
            <div className="form-group">
              <label>Količina *</label>
              <input
                type="number"
                min="1"
                className="form-control"
                value={form.kolicina}
                onChange={set("kolicina")}
              />
            </div>
            <div className="form-group">
              <label>Jedinična cena *</label>
              <input
                type="number"
                min="0"
                step="0.01"
                className="form-control"
                value={form.jedinicnaCena}
                onChange={set("jedinicnaCena")}
              />
            </div>
          </div>

          <div className="form-row">
            <div className="form-group">
              <label>Budžet ID *</label>
              <input
                type="number"
                min="1"
                className="form-control"
                value={form.budzetId}
                onChange={set("budzetId")}
              />
            </div>
            <div className="form-group">
              <label>Kategorija ID *</label>
              <input
                type="number"
                min="1"
                className="form-control"
                value={form.kategorijaId}
                onChange={set("kategorijaId")}
              />
            </div>
          </div>

          <div className="form-group">
            <label>Ukupna cena</label>
            <input
              className="form-control"
              value={ukupnaCena.toFixed(2)}
              readOnly
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
              Dodaj stavku
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
