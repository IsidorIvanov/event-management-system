import { useEffect, useMemo, useState } from "react";
import api from "../../services/api";

const EMPTY = {
  brojFakture: "",
  tip: "ULAZNA",
  datumIzdavanja: "",
  rokPlacanja: "",
  dogadjajId: "",
  dobavljacId: "",
  napomena: "",
};

const today = new Date().toISOString().split("T")[0];

const formatEventLabel = (event) => {
  const dateLabel = event?.datumPocetka
    ? new Date(event.datumPocetka).toLocaleDateString("sr-Latn", {
        day: "2-digit",
        month: "2-digit",
        year: "numeric",
      })
    : "bez datuma";

  return `${event?.naziv || "Nepoznat događaj"} — ${dateLabel}`;
};

export default function KreirajFakturuModal({
  onClose,
  onSubmit,
  events = [],
}) {
  const [form, setForm] = useState(EMPTY);
  const [error, setError] = useState(null);
  const [loadedEvents, setLoadedEvents] = useState([]);
  const [eventsLoading, setEventsLoading] = useState(false);
  const [eventsError, setEventsError] = useState(null);

  useEffect(() => {
    if (!form.datumIzdavanja) return;
    if (form.rokPlacanja && form.rokPlacanja < form.datumIzdavanja) {
      setError("Rok plaćanja ne može biti pre datuma izdavanja.");
    } else if (error) {
      setError(null);
    }
  }, [form.datumIzdavanja, form.rokPlacanja]);

  useEffect(() => {
    let active = true;
    setEventsLoading(true);
    api
      .get("/dogadjaj")
      .then((res) => {
        if (!active) return;
        setLoadedEvents(Array.isArray(res.data) ? res.data : []);
        setEventsError(null);
      })
      .catch(() => {
        if (!active) return;
        setLoadedEvents([]);
        setEventsError("Nije moguće učitati događaje.");
      })
      .finally(() => {
        if (!active) return;
        setEventsLoading(false);
      });

    return () => {
      active = false;
    };
  }, []);

  const availableEvents = useMemo(
    () => (events.length ? events : loadedEvents),
    [events, loadedEvents],
  );

  const isUlazna = form.tip === "ULAZNA";

  const set = (field) => (e) => {
    const value = e.target.value;
    setForm((prev) => ({ ...prev, [field]: value }));
    if (error) setError(null);
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!form.brojFakture.trim()) return setError("Broj fakture je obavezan.");
    if (!form.datumIzdavanja) return setError("Datum izdavanja je obavezan.");
    if (!form.rokPlacanja) return setError("Rok plaćanja je obavezan.");
    if (form.rokPlacanja < form.datumIzdavanja)
      return setError("Rok plaćanja ne može biti pre datuma izdavanja.");
    if (isUlazna && !form.dobavljacId)
      return setError("Dobavljač je obavezan za ulaznu fakturu.");
    if (!form.dogadjajId) return setError("Događaj je obavezan.");

    onSubmit({
      brojFakture: form.brojFakture.trim(),
      tip: form.tip,
      datumIzdavanja: form.datumIzdavanja,
      rokPlacanja: form.rokPlacanja,
      dogadjajId: Number(form.dogadjajId),
      dobavljacId: isUlazna ? Number(form.dobavljacId) : null,
      napomena: form.napomena || null,
    });
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div
        className="modal-card modal-card-wide"
        style={{ maxWidth: 680 }}
        onClick={(e) => e.stopPropagation()}
      >
        <h3>Nova faktura</h3>

        {error && (
          <div className="error-msg" style={{ marginBottom: "1rem" }}>
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label>Broj fakture *</label>
            <input
              className="form-control"
              value={form.brojFakture}
              onChange={set("brojFakture")}
              placeholder="npr. FAK-2026-001"
            />
          </div>

          <div className="form-row">
            <div className="form-group">
              <label>Tip *</label>
              <select
                className="form-control"
                value={form.tip}
                onChange={set("tip")}
              >
                <option value="ULAZNA">ULAZNA</option>
                <option value="IZLAZNA">IZLAZNA</option>
              </select>
            </div>
            <div className="form-group">
              <label>Događaj *</label>
              <select
                className="form-control"
                value={form.dogadjajId}
                onChange={set("dogadjajId")}
              >
                <option value="">Izaberi događaj</option>
                {availableEvents.map((event) => (
                  <option key={event.dogadjajId} value={event.dogadjajId}>
                    {formatEventLabel(event)}
                  </option>
                ))}
              </select>
              {eventsLoading && (
                <small style={{ color: "var(--text-muted)" }}>
                  Učitavanje događaja...
                </small>
              )}
              {!eventsLoading && !availableEvents.length && (
                <small style={{ color: "var(--text-muted)" }}>
                  {eventsError || "Nema dostupnih događaja za izbor."}
                </small>
              )}
            </div>
          </div>

          <div className="form-row">
            <div className="form-group">
              <label>Datum izdavanja *</label>
              <input
                type="date"
                className="form-control"
                value={form.datumIzdavanja}
                onChange={set("datumIzdavanja")}
                max={today}
              />
            </div>
            <div className="form-group">
              <label>Rok plaćanja *</label>
              <input
                type="date"
                className="form-control"
                value={form.rokPlacanja}
                onChange={set("rokPlacanja")}
                min={form.datumIzdavanja || today}
              />
            </div>
          </div>

          {isUlazna && (
            <div className="form-group">
              <label>Dobavljač ID *</label>
              <input
                type="number"
                min="1"
                className="form-control"
                value={form.dobavljacId}
                onChange={set("dobavljacId")}
              />
            </div>
          )}

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
              Kreiraj fakturu
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
