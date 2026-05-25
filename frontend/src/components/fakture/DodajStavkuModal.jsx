import { useEffect, useMemo, useState } from "react";
import api from "../../services/api";

const EMPTY = {
  naziv: "",
  kolicina: "",
  jedinicnaCena: "",
  napomena: "",
  budzetId: "",
  kategorijaId: "",
};

const toNumber = (value) => Number(value || 0);

export default function DodajStavkuModal({ onClose, onSubmit, dogadjajId }) {
  const [form, setForm] = useState(EMPTY);
  const [error, setError] = useState(null);
  const [budzeti, setBudzeti] = useState([]);
  const [budzetiLoading, setBudzetiLoading] = useState(false);
  const [budzetiError, setBudzetiError] = useState(null);
  const [kategorije, setKategorije] = useState([]);

  useEffect(() => {
    if (!dogadjajId) {
      setBudzeti([]);
      setKategorije([]);
      return;
    }
    let active = true;
    setBudzetiLoading(true);
    api
      .get(`/budzet/dogadjaj/${dogadjajId}`)
      .then((res) => {
        if (!active) return;
        setBudzeti(Array.isArray(res.data) ? res.data : []);
        setBudzetiError(null);
      })
      .catch((err) => {
        if (!active) return;
        setBudzeti([]);
        setBudzetiError("Nije moguće učitati budžete za događaj.");
      })
      .finally(() => {
        if (!active) return;
        setBudzetiLoading(false);
      });

    return () => {
      active = false;
    };
  }, [dogadjajId]);

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
    if (!Number.isInteger(toNumber(form.kolicina)))
      return setError("Količina mora biti ceo broj.");
    if (toNumber(form.jedinicnaCena) < 0)
      return setError("Jedinična cena ne može biti negativna.");
    if (!form.budzetId) return setError("Budžet je obavezan.");
    if (!form.kategorijaId) return setError("Kategorija je obavezna.");

    onSubmit({
      naziv: form.naziv.trim(),
      kolicina: toNumber(form.kolicina),
      jedinicnaCena: String(form.jedinicnaCena || "0"),
      ukupnaCena: ukupnaCena.toFixed(2),
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
                step="1"
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
              <label>Budžet *</label>
              <select
                className="form-control"
                value={form.budzetId}
                onChange={(e) => {
                  const val = e.target.value;
                  setForm((prev) => ({
                    ...prev,
                    budzetId: val,
                    kategorijaId: "",
                  }));
                  if (error) setError(null);
                  // set categories based on selected budget
                  const b = budzeti.find(
                    (x) => String(x.budzetId) === String(val),
                  );
                  setKategorije(b ? b.stavke || [] : []);
                }}
              >
                <option value="">Izaberi budžet</option>
                {budzeti.map((b) => (
                  <option key={b.budzetId} value={b.budzetId}>
                    {b.nazivBudzeta}
                  </option>
                ))}
              </select>
              {budzetiLoading && (
                <small style={{ color: "var(--text-muted)" }}>
                  Učitavanje budžeta...
                </small>
              )}
            </div>
            <div className="form-group">
              <label>Kategorija *</label>
              <select
                className="form-control"
                value={form.kategorijaId}
                onChange={set("kategorijaId")}
                disabled={!form.budzetId}
              >
                <option value="">Izaberi kategoriju</option>
                {kategorije.map((s) => (
                  <option key={s.kategorijaId} value={s.kategorijaId}>
                    {s.kategorijaNaziv}
                  </option>
                ))}
              </select>
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
