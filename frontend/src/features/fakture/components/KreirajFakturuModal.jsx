import { useEffect, useMemo, useState } from "react";
import Modal from "@/shared/components/Modal";
import api from "@/shared/services/api";

const EMPTY = {
  brojFakture: "",
  tip: "ULAZNA",
  datumIzdavanja: "",
  rokPlacanja: "",
  dogadjajId: "",
  klijentId: "",
  dobavljacId: "",
  ugovorId: "",
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
  const STATUS_DISPLAY = {
    OBJAVLJEN: "Objavljen",
    AKTIVAN: "Aktivan",
    DRAFT: "Nacrt",
    ZAVRSEN: "Završen",
  };

  const statusLabel = event?.status
    ? STATUS_DISPLAY[event.status] || event.status
    : null;

  return `${event?.naziv || "Nepoznat događaj"} — ${dateLabel}${statusLabel ? ` — ${statusLabel}` : ""}`;
};

const getClientId = (klijent) => klijent?.klijentId ?? klijent?.korisnikId ?? klijent?.id;

const formatClientLabel = (klijent) =>
  klijent?.nazivFirme ||
  [klijent?.ime, klijent?.prezime].filter(Boolean).join(" ") ||
  klijent?.email ||
  `Klijent #${getClientId(klijent)}`;

const formatContractLabel = (ugovor) =>
  `Ugovor #${ugovor.ugovorId}${ugovor.vaziDo ? ` — važi do ${ugovor.vaziDo}` : ""}`;

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
  const [loadedDobavljaci, setLoadedDobavljaci] = useState([]);
  const [dobavljaciLoading, setDobavljaciLoading] = useState(false);
  const [dobavljaciError, setDobavljaciError] = useState(null);
  const [loadedKlijenti, setLoadedKlijenti] = useState([]);
  const [klijentiLoading, setKlijentiLoading] = useState(false);
  const [klijentiError, setKlijentiError] = useState(null);
  const [loadedUgovori, setLoadedUgovori] = useState([]);
  const [ugovoriLoading, setUgovoriLoading] = useState(false);
  const [ugovoriError, setUgovoriError] = useState(null);
  const isUlazna = form.tip === "ULAZNA";

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

  useEffect(() => {
    let active = true;
    setKlijentiLoading(true);
    api
      .get("/klijenti")
      .then((res) => {
        if (!active) return;
        setLoadedKlijenti(Array.isArray(res.data) ? res.data : []);
        setKlijentiError(null);
      })
      .catch(() => {
        if (!active) return;
        setLoadedKlijenti([]);
        setKlijentiError("Nije moguće učitati klijente.");
      })
      .finally(() => {
        if (!active) return;
        setKlijentiLoading(false);
      });

    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    if (!isUlazna || !form.dobavljacId) {
      setLoadedUgovori([]);
      setUgovoriError(null);
      setForm((prev) => ({ ...prev, ugovorId: "" }));
      return;
    }

    let active = true;
    setUgovoriLoading(true);
    api
      .get("/ugovori", { params: { dobavljacId: form.dobavljacId } })
      .catch(() => api.get(`/nabavka/ugovor/dobavljac/${form.dobavljacId}`))
      .then((res) => {
        if (!active) return;
        const ugovori = Array.isArray(res.data) ? res.data : [];
        setLoadedUgovori(ugovori.filter((ugovor) => ugovor.status === "AKTIVAN"));
        setUgovoriError(null);
      })
      .catch(() => {
        if (!active) return;
        setLoadedUgovori([]);
        setUgovoriError("Nije moguće učitati ugovore za dobavljača.");
      })
      .finally(() => {
        if (!active) return;
        setUgovoriLoading(false);
      });

    return () => {
      active = false;
    };
  }, [form.dobavljacId, isUlazna]);

  useEffect(() => {
    let active = true;
    setDobavljaciLoading(true);
    api
      .get("/nabavka/dobavljac")
      .then((res) => {
        if (!active) return;
        setLoadedDobavljaci(Array.isArray(res.data) ? res.data : []);
        setDobavljaciError(null);
      })
      .catch(() => {
        if (!active) return;
        setLoadedDobavljaci([]);
        setDobavljaciError("Nije moguće učitati dobavljače.");
      })
      .finally(() => {
        if (!active) return;
        setDobavljaciLoading(false);
      });

    return () => {
      active = false;
    };
  }, []);

  const availableEvents = useMemo(
    () => (events.length ? events : loadedEvents),
    [events, loadedEvents],
  );

  const set = (field) => (e) => {
    const value = e.target.value;
    setForm((prev) => ({ ...prev, [field]: value }));
    if (error) setError(null);
  };

  const handleTipChange = (e) => {
    const tip = e.target.value;
    setForm((prev) => ({
      ...prev,
      tip,
      klijentId: "",
      dobavljacId: "",
      ugovorId: "",
    }));
    if (error) setError(null);
  };

  const handleDobavljacChange = (e) => {
    setForm((prev) => ({
      ...prev,
      dobavljacId: e.target.value,
      ugovorId: "",
    }));
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
    if (!isUlazna && !form.klijentId)
      return setError("Klijent je obavezan za izlaznu fakturu.");
    if (!form.dogadjajId) return setError("Događaj je obavezan.");

    onSubmit({
      brojFakture: form.brojFakture.trim(),
      tip: form.tip,
      ukupnaIznos: "0.00",
      placeniIznos: "0.00",
      datumIzdavanja: form.datumIzdavanja,
      rokPlacanja: form.rokPlacanja,
      dogadjajId: Number(form.dogadjajId),
      klijentId: isUlazna ? null : Number(form.klijentId),
      dobavljacId: isUlazna ? Number(form.dobavljacId) : null,
      ugovorId: isUlazna && form.ugovorId ? Number(form.ugovorId) : null,
      napomena: form.napomena || null,
    });
  };

  return (
    <Modal boxClassName="modal-card modal-card-wide" style={{ maxWidth: 680 }} onClose={onClose}>
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
                onChange={handleTipChange}
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
              <label>Dobavljač *</label>
              <select
                className="form-control"
                value={form.dobavljacId}
                onChange={handleDobavljacChange}
              >
                <option value="">Izaberi dobavljača</option>
                {loadedDobavljaci.map((d) => (
                  <option key={d.dobavljacId} value={d.dobavljacId}>
                    {d.naziv}
                  </option>
                ))}
              </select>
              {dobavljaciLoading && (
                <small style={{ color: "var(--text-muted)" }}>
                  Učitavanje dobavljača...
                </small>
              )}
              {!dobavljaciLoading && !loadedDobavljaci.length && (
                <small style={{ color: "var(--text-muted)" }}>
                  {dobavljaciError || "Nema dostupnih dobavljača."}
                </small>
              )}
            </div>
          )}

          {isUlazna && (
            <div className="form-group">
              <label>Ugovor</label>
              <select
                className="form-control"
                value={form.ugovorId}
                onChange={set("ugovorId")}
                disabled={!form.dobavljacId || ugovoriLoading}
              >
                <option value="">Bez vezanog ugovora</option>
                {loadedUgovori.map((ugovor) => (
                  <option key={ugovor.ugovorId} value={ugovor.ugovorId}>
                    {formatContractLabel(ugovor)}
                  </option>
                ))}
              </select>
              {ugovoriLoading && (
                <small style={{ color: "var(--text-muted)" }}>
                  Učitavanje ugovora...
                </small>
              )}
              {!ugovoriLoading && form.dobavljacId && !loadedUgovori.length && (
                <small style={{ color: "var(--text-muted)" }}>
                  {ugovoriError || "Nema aktivnih ugovora za dobavljača."}
                </small>
              )}
            </div>
          )}

          {!isUlazna && (
            <div className="form-group">
              <label>Klijent *</label>
              <select
                className="form-control"
                value={form.klijentId}
                onChange={set("klijentId")}
              >
                <option value="">Izaberi klijenta</option>
                {loadedKlijenti.map((klijent) => {
                  const id = getClientId(klijent);
                  return (
                    <option key={id} value={id}>
                      {formatClientLabel(klijent)}
                    </option>
                  );
                })}
              </select>
              {klijentiLoading && (
                <small style={{ color: "var(--text-muted)" }}>
                  Učitavanje klijenata...
                </small>
              )}
              {!klijentiLoading && !loadedKlijenti.length && (
                <small style={{ color: "var(--text-muted)" }}>
                  {klijentiError || "Nema dostupnih klijenata."}
                </small>
              )}
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
    </Modal>
  );
}
