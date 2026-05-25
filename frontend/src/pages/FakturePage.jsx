import { useEffect, useMemo, useState } from "react";
import { useAuth } from "../context/AuthContext";
import { useToast } from "../components/ToastNotification";
import api from "../services/api";
import KreirajFakturuModal from "../components/fakture/KreirajFakturuModal.jsx";
import DodajStavkuModal from "../components/fakture/DodajStavkuModal.jsx";
import KreirajPlacanjeModal from "../components/fakture/KreirajPlacanjeModal.jsx";
import RefundacijaModal from "../components/fakture/RefundacijaModal.jsx";
import * as fakturaApi from "../services/fakturaService";

const TIP_DISPLAY = {
  ULAZNA: "Ulazna",
  IZLAZNA: "Izlazna",
};

const FAKTURA_STATUS_STYLE = {
  DRAFT: { backgroundColor: "#5b6573", color: "#fff" },
  IZDATA: { backgroundColor: "#3478f6", color: "#fff" },
  DELIMICNO_PLACENA: { backgroundColor: "#f59e0b", color: "#fff" },
  PLACENA: { backgroundColor: "#16a34a", color: "#fff" },
  DOSPELA: { backgroundColor: "#dc2626", color: "#fff" },
  OTKAZANA: { backgroundColor: "#374151", color: "#fff" },
};

const PLACANJE_STATUS_STYLE = {
  PENDING: { backgroundColor: "#f59e0b", color: "#fff" },
  COMPLETED: { backgroundColor: "#16a34a", color: "#fff" },
  FAILED: { backgroundColor: "#dc2626", color: "#fff" },
};

const REFUND_STATUS_STYLE = {
  TRAZENA: { backgroundColor: "#6b7280", color: "#fff" },
  ODOBRENA: { backgroundColor: "#3478f6", color: "#fff" },
  ODBIJENA: { backgroundColor: "#dc2626", color: "#fff" },
  IZVRSENA: { backgroundColor: "#16a34a", color: "#fff" },
};

const formatDate = (value) =>
  value
    ? new Date(value).toLocaleDateString("sr-Latn", {
        day: "2-digit",
        month: "2-digit",
        year: "numeric",
      })
    : "—";

const formatMoney = (value) => {
  const number = Number(value ?? 0);
  return new Intl.NumberFormat("sr-Latn-RS", {
    style: "currency",
    currency: "RSD",
    maximumFractionDigits: 2,
  }).format(Number.isNaN(number) ? 0 : number);
};

const extractError = (error, fallback) =>
  error?.response?.data?.message ||
  error?.response?.data?.error ||
  error?.message ||
  fallback;

function StatusBadge({ value, styleMap }) {
  return (
    <span
      className="status-badge"
      style={styleMap[value] || { backgroundColor: "#6b7280", color: "#fff" }}
    >
      {value}
    </span>
  );
}

function FakturaRow({ faktura, onOpen }) {
  return (
    <tr onClick={() => onOpen(faktura)} style={{ cursor: "pointer" }}>
      <td>
        <strong>{faktura.brojFakture}</strong>
      </td>
      <td>{TIP_DISPLAY[faktura.tip] || faktura.tip}</td>
      <td>
        <StatusBadge value={faktura.status} styleMap={FAKTURA_STATUS_STYLE} />
      </td>
      <td>{formatMoney(faktura.ukupnaIznos)}</td>
      <td>{formatMoney(faktura.placeniIznos)}</td>
      <td>{formatDate(faktura.rokPlacanja)}</td>
    </tr>
  );
}

function FakturaDetailModal({
  faktura,
  onClose,
  onAddStavka,
  onNewPlacanje,
  onConfirmPlacanje,
  onFailPlacanje,
  onRequestRefund,
  onApproveRefund,
  onRejectRefund,
  onExecuteRefund,
  onIssueFaktura,
  onCancelFaktura,
  hasRole,
  userId,
}) {
  if (!faktura) return null;

  const isFinansijski = hasRole("FINANSIJSKI_KONTROLOR");
  const isMenadzer = hasRole("MENADZER_DOGADJAJA");
  const canAddStavka = isFinansijski && faktura.status === "DRAFT";
  const canNewPlacanje =
    isFinansijski &&
    faktura.status !== "PLACENA" &&
    faktura.status !== "OTKAZANA";
  const canCancel = isFinansijski && Number(faktura.placeniIznos || 0) === 0;

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div
        className="modal-card modal-card-wide"
        style={{
          width: "min(1100px, 96vw)",
          maxHeight: "92vh",
          overflowY: "auto",
        }}
        onClick={(e) => e.stopPropagation()}
      >
        <div className="modal-header">
          <div>
            <h3 className="modal-title">Faktura {faktura.brojFakture}</h3>
            <p className="page-subtitle" style={{ marginTop: "0.35rem" }}>
              {TIP_DISPLAY[faktura.tip] || faktura.tip}
            </p>
          </div>
          <button className="modal-close" onClick={onClose}>
            ✕
          </button>
        </div>

        <div className="info-cards" style={{ marginBottom: "1.5rem" }}>
          <div className="info-card">
            <div className="label">Status</div>
            <StatusBadge
              value={faktura.status}
              styleMap={FAKTURA_STATUS_STYLE}
            />
          </div>
          <div className="info-card">
            <div className="label">Ukupan iznos</div>
            <div className="value accent">
              {formatMoney(faktura.ukupnaIznos)}
            </div>
          </div>
          <div className="info-card">
            <div className="label">Plaćeni iznos</div>
            <div className="value success">
              {formatMoney(faktura.placeniIznos)}
            </div>
          </div>
          <div className="info-card">
            <div className="label">Rok plaćanja</div>
            <div className="value">{formatDate(faktura.rokPlacanja)}</div>
          </div>
        </div>

        <div
          className="events-table-card"
          style={{ padding: "1.25rem", marginBottom: "1.25rem" }}
        >
          <h3 style={{ marginBottom: "1rem" }}>Stavke</h3>
          {faktura.stavke?.length ? (
            <table className="events-table">
              <thead>
                <tr>
                  <th>NAZIV</th>
                  <th>KOL</th>
                  <th>JED. CENA</th>
                  <th>UKUPNO</th>
                  <th>BUDŽET</th>
                  <th>KATEGORIJA</th>
                </tr>
              </thead>
              <tbody>
                {faktura.stavke.map((stavka) => (
                  <tr
                    key={
                      stavka.stavkaFaktureId ||
                      `${stavka.redniBroj}-${stavka.naziv}`
                    }
                  >
                    <td>{stavka.naziv}</td>
                    <td>{stavka.kolicina}</td>
                    <td>{formatMoney(stavka.jedinicnaCena)}</td>
                    <td>{formatMoney(stavka.ukupnaCena)}</td>
                    <td>{stavka.budzetId || "—"}</td>
                    <td>{stavka.kategorijaId || "—"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          ) : (
            <p style={{ color: "var(--text-muted)" }}>Nema stavki.</p>
          )}
        </div>

        <div
          className="events-table-card"
          style={{ padding: "1.25rem", marginBottom: "1.25rem" }}
        >
          <h3 style={{ marginBottom: "1rem" }}>Plaćanja</h3>
          {faktura.placanja?.length ? (
            faktura.placanja.map((placanje) => (
              <div
                key={placanje.placanjeId}
                style={{
                  border: "1px solid rgba(148,163,184,.18)",
                  borderRadius: 16,
                  padding: "1rem",
                  marginBottom: "1rem",
                }}
              >
                <div
                  style={{
                    display: "flex",
                    justifyContent: "space-between",
                    gap: "1rem",
                    flexWrap: "wrap",
                  }}
                >
                  <div>
                    <div style={{ fontWeight: 700 }}>
                      {formatMoney(placanje.iznos)} · {placanje.metod}
                    </div>
                    <div
                      style={{
                        color: "var(--text-secondary)",
                        marginTop: "0.25rem",
                      }}
                    >
                      Kreirano: {formatDate(placanje.kreiranoAt)}
                    </div>
                    {placanje.potvrdjenoAt && (
                      <div
                        style={{
                          color: "var(--text-secondary)",
                          marginTop: "0.25rem",
                        }}
                      >
                        Potvrđeno: {formatDate(placanje.potvrdjenoAt)}
                      </div>
                    )}
                  </div>
                  <StatusBadge
                    value={placanje.status}
                    styleMap={PLACANJE_STATUS_STYLE}
                  />
                </div>

                {placanje.napomena && (
                  <p
                    style={{
                      marginTop: "0.75rem",
                      color: "var(--text-secondary)",
                    }}
                  >
                    {placanje.napomena}
                  </p>
                )}

                <div
                  className="action-buttons"
                  style={{ marginTop: "0.75rem" }}
                >
                  {isFinansijski && placanje.status === "PENDING" && (
                    <>
                      <button
                        className="btn btn-outline btn-xs"
                        onClick={() => onConfirmPlacanje(placanje)}
                      >
                        Potvrdi plaćanje
                      </button>
                      <button
                        className="btn btn-xs btn-danger-outline"
                        onClick={() => onFailPlacanje(placanje)}
                      >
                        Odbij plaćanje
                      </button>
                    </>
                  )}
                  {isFinansijski && placanje.status === "COMPLETED" && (
                    <button
                      className="btn btn-outline btn-xs"
                      onClick={() => onRequestRefund(placanje)}
                    >
                      Zahtev za refundaciju
                    </button>
                  )}
                </div>

                <div style={{ marginTop: "1rem" }}>
                  <div style={{ fontWeight: 600, marginBottom: "0.5rem" }}>
                    Refundacije
                  </div>
                  {placanje.refundacije?.length ? (
                    placanje.refundacije.map((refundacija) => {
                      const canManage =
                        isMenadzer &&
                        Number(refundacija.kreiraoId) !== Number(userId);
                      return (
                        <div
                          key={refundacija.refundacijaId}
                          style={{
                            borderTop: "1px solid rgba(148,163,184,.16)",
                            paddingTop: "0.8rem",
                            marginTop: "0.8rem",
                          }}
                        >
                          <div
                            style={{
                              display: "flex",
                              justifyContent: "space-between",
                              gap: "1rem",
                              flexWrap: "wrap",
                            }}
                          >
                            <div>
                              <div style={{ fontWeight: 600 }}>
                                {formatMoney(refundacija.iznos)}
                              </div>
                              <div
                                style={{
                                  color: "var(--text-secondary)",
                                  marginTop: "0.25rem",
                                }}
                              >
                                {refundacija.razlog || "—"}
                              </div>
                              <div
                                style={{
                                  color: "var(--text-secondary)",
                                  marginTop: "0.25rem",
                                }}
                              >
                                Kreirano: {formatDate(refundacija.kreiranoAt)}
                              </div>
                            </div>
                            <StatusBadge
                              value={refundacija.status}
                              styleMap={REFUND_STATUS_STYLE}
                            />
                          </div>

                          <div
                            className="action-buttons"
                            style={{ marginTop: "0.75rem" }}
                          >
                            {canManage && refundacija.status === "TRAZENA" && (
                              <>
                                <button
                                  className="btn btn-outline btn-xs"
                                  onClick={() => onApproveRefund(refundacija)}
                                >
                                  Odobri
                                </button>
                                <button
                                  className="btn btn-xs btn-danger-outline"
                                  onClick={() => onRejectRefund(refundacija)}
                                >
                                  Odbij
                                </button>
                              </>
                            )}
                            {isFinansijski &&
                              refundacija.status === "ODOBRENA" && (
                                <button
                                  className="btn btn-outline btn-xs"
                                  onClick={() => onExecuteRefund(refundacija)}
                                >
                                  Izvrši refundaciju
                                </button>
                              )}
                          </div>
                        </div>
                      );
                    })
                  ) : (
                    <p style={{ color: "var(--text-muted)" }}>
                      Nema refundacija.
                    </p>
                  )}
                </div>
              </div>
            ))
          ) : (
            <p style={{ color: "var(--text-muted)" }}>Nema plaćanja.</p>
          )}
        </div>

        <div
          className="modal-actions"
          style={{ justifyContent: "space-between", flexWrap: "wrap" }}
        >
          <div className="action-buttons">
            {canAddStavka && (
              <button className="btn btn-primary" onClick={onAddStavka}>
                Dodaj stavku
              </button>
            )}
            {isFinansijski && faktura.status === "DRAFT" && (
              <button className="btn btn-outline" onClick={onIssueFaktura}>
                Izdaj fakturu
              </button>
            )}
            {canCancel && (
              <button className="btn btn-danger" onClick={onCancelFaktura}>
                Otkaži fakturu
              </button>
            )}
            {canNewPlacanje && (
              <button className="btn btn-outline" onClick={onNewPlacanje}>
                Novo plaćanje
              </button>
            )}
          </div>
          <button className="btn btn-outline" onClick={onClose}>
            Zatvori
          </button>
        </div>
      </div>
    </div>
  );
}

export default function FakturePage() {
  const { hasRole, getUserId } = useAuth();
  const toast = useToast();

  const [fakture, setFakture] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [createOpen, setCreateOpen] = useState(false);
  const [stavkaOpen, setStavkaOpen] = useState(false);
  const [placanjeOpen, setPlacanjeOpen] = useState(false);
  const [refundOpen, setRefundOpen] = useState(null);
  const [stavkaDogadjajId, setStavkaDogadjajId] = useState(null);
  const [stavkaFakturaId, setStavkaFakturaId] = useState(null);
  const [placanjeFakturaId, setPlacanjeFakturaId] = useState(null);
  const [selectedId, setSelectedId] = useState(null);
  const [selectedDetail, setSelectedDetail] = useState(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [dogadjaji, setDogadjaji] = useState([]);
  const [dogadjajiLoading, setDogadjajiLoading] = useState(false);

  const selectedSummary = useMemo(
    () => fakture.find((f) => f.fakturaId === selectedId) || null,
    [fakture, selectedId],
  );

  const loadAll = async () => {
    setLoading(true);
    try {
      const res = await fakturaApi.getAllFakture();
      console.log(
        "GET /api/fakture response (sample):",
        Array.isArray(res.data) && res.data.length ? res.data[0] : res.data,
      );
      setFakture(res.data);
      setError(null);
    } catch (err) {
      setError(extractError(err, "Greška pri učitavanju faktura."));
    } finally {
      setLoading(false);
    }
  };

  const loadDetail = async (id) => {
    if (!id) return;
    setDetailLoading(true);
    try {
      const res = await fakturaApi.getFakturaById(id);
      setSelectedDetail(res.data);
    } catch (err) {
      toast(
        extractError(err, "Greška pri učitavanju detalja fakture."),
        "error",
      );
    } finally {
      setDetailLoading(false);
    }
  };

  const refresh = async (keepDetail = true) => {
    await loadAll();
    if (keepDetail && selectedId) await loadDetail(selectedId);
  };

  useEffect(() => {
    loadAll();
  }, []);

  useEffect(() => {
    setDogadjajiLoading(true);
    api
      .get("/dogadjaj")
      .then((res) => setDogadjaji(res.data || []))
      .catch(() => {
        setDogadjaji([]);
      })
      .finally(() => setDogadjajiLoading(false));
  }, []);

  useEffect(() => {
    if (selectedId) loadDetail(selectedId);
  }, [selectedId]);

  useEffect(() => {
    console.log("selectedDetail:", selectedDetail);
    console.log("selectedSummary:", selectedSummary);
  }, [selectedDetail, selectedSummary, stavkaOpen]);

  const openDetail = (faktura) => {
    setSelectedId(faktura.fakturaId);
    setSelectedDetail(faktura);
  };

  const handleOpenStavka = () => {
    setStavkaDogadjajId(selectedDetail?.dogadjajId ?? null);
    setStavkaFakturaId(selectedDetail?.fakturaId ?? null);
    setStavkaOpen(true);
  };

  const handleOpenPlacanje = () => {
    setPlacanjeFakturaId(selectedDetail?.fakturaId ?? null);
    setPlacanjeOpen(true);
  };

  const handleCreateFaktura = async (payload) => {
    try {
      const res = await fakturaApi.createFaktura(payload);
      toast(`Faktura ${res.data.brojFakture} je kreirana.`, "success");
      setCreateOpen(false);
      await refresh(false);
      setSelectedId(res.data.fakturaId);
      await loadDetail(res.data.fakturaId);
    } catch (err) {
      toast(extractError(err, "Greška pri kreiranju fakture."), "error");
    }
  };

  const handleAddStavka = async (payload) => {
    const fakturaId = stavkaFakturaId;

    try {
      await fakturaApi.addStavka(fakturaId, payload);
      toast("Stavka je dodata.", "success");
      setStavkaOpen(false);
      setStavkaDogadjajId(null);
      setStavkaFakturaId(null);
      await loadAll();
      if (fakturaId) {
        setSelectedId(fakturaId);
        await loadDetail(fakturaId);
      }
    } catch (err) {
      toast(extractError(err, "Greška pri dodavanju stavke."), "error");
    }
  };

  const handleNewPlacanje = async (payload) => {
    const fakturaId = placanjeFakturaId;

    try {
      await fakturaApi.createPlacanje(fakturaId, payload);
      toast("Plaćanje je kreirano.", "success");
      setPlacanjeOpen(false);
      setPlacanjeFakturaId(null);
      await loadAll();
      if (fakturaId) {
        setSelectedId(fakturaId);
        await loadDetail(fakturaId);
      }
    } catch (err) {
      toast(extractError(err, "Greška pri kreiranju plaćanja."), "error");
    }
  };

  const handleConfirmPlacanje = async (placanje) => {
    try {
      await fakturaApi.confirmPlacanje(placanje.placanjeId);
      toast("Plaćanje je potvrđeno.", "success");
      await refresh();
    } catch (err) {
      toast(extractError(err, "Greška pri potvrdi plaćanja."), "error");
    }
  };

  const handleFailPlacanje = async (placanje) => {
    try {
      await fakturaApi.failPlacanje(placanje.placanjeId);
      toast("Plaćanje je odbijeno.", "success");
      await refresh();
    } catch (err) {
      toast(extractError(err, "Greška pri odbijanju plaćanja."), "error");
    }
  };

  const handleRequestRefund = (placanje) => {
    setRefundOpen({ placanjeId: placanje.placanjeId });
  };

  const handleRefundSubmit = async (placanjeId, payload) => {
    try {
      await fakturaApi.requestRefundacija(placanjeId, payload, getUserId());
      toast("Zahtev za refundaciju je poslat.", "success");
      setRefundOpen(null);
      await refresh();
    } catch (err) {
      toast(
        extractError(err, "Greška pri slanju zahteva za refundaciju."),
        "error",
      );
    }
  };

  const handleApproveRefund = async (refundacija) => {
    try {
      await fakturaApi.approveRefundacija(
        refundacija.refundacijaId,
        getUserId(),
      );
      toast("Refundacija je odobrena.", "success");
      await refresh();
    } catch (err) {
      toast(extractError(err, "Greška pri odobrenju refundacije."), "error");
    }
  };

  const handleRejectRefund = async (refundacija) => {
    try {
      await fakturaApi.rejectRefundacija(
        refundacija.refundacijaId,
        getUserId(),
      );
      toast("Refundacija je odbijena.", "success");
      await refresh();
    } catch (err) {
      toast(extractError(err, "Greška pri odbijanju refundacije."), "error");
    }
  };

  const handleExecuteRefund = async (refundacija) => {
    try {
      await fakturaApi.executeRefundacija(refundacija.refundacijaId);
      toast("Refundacija je izvršena.", "success");
      await refresh();
    } catch (err) {
      toast(extractError(err, "Greška pri izvršenju refundacije."), "error");
    }
  };

  const handleIssueFaktura = () => {
    if (!selectedId) return;

    fakturaApi
      .issueFaktura(selectedId)
      .then(async () => {
        toast("Faktura je izdata.", "success");
        await refresh();
      })
      .catch((err) => {
        toast(extractError(err, "Greška pri izdavanju fakture."), "error");
      });
  };

  const handleCancelFaktura = async () => {
    try {
      await fakturaApi.cancelFaktura(selectedId);
      toast("Faktura je otkazana.", "success");
      await refresh();
    } catch (err) {
      toast(extractError(err, "Greška pri otkazivanju fakture."), "error");
    }
  };

  return (
    <div className="program-page">
      {createOpen && (
        <KreirajFakturuModal
          onClose={() => setCreateOpen(false)}
          onSubmit={handleCreateFaktura}
          events={dogadjaji}
        />
      )}

      {stavkaOpen && (
        <DodajStavkuModal
          onClose={() => {
            setStavkaOpen(false);
            setStavkaDogadjajId(null);
            setStavkaFakturaId(null);
          }}
          onSubmit={handleAddStavka}
          dogadjajId={stavkaDogadjajId}
        />
      )}

      {placanjeOpen && (
        <KreirajPlacanjeModal
          onClose={() => {
            setPlacanjeOpen(false);
            setPlacanjeFakturaId(null);
          }}
          onSubmit={handleNewPlacanje}
        />
      )}

      {refundOpen && (
        <RefundacijaModal
          placanjeId={refundOpen.placanjeId}
          onClose={() => setRefundOpen(null)}
          onSubmit={handleRefundSubmit}
        />
      )}

      <div className="program-header">
        <div>
          <h1>Fakture</h1>
          <p className="page-subtitle">
            upravljanje fakturama, plaćanjima i refundacijama
          </p>
          <p className="page-subtitle" style={{ marginTop: "0.35rem" }}>
            {dogadjajiLoading
              ? "Učitavanje događaja za izbor..."
              : `${dogadjaji.length} događaja dostupno za kreiranje fakture`}
          </p>
        </div>

        {hasRole("FINANSIJSKI_KONTROLOR") && (
          <button
            className="btn btn-primary"
            onClick={() => setCreateOpen(true)}
            style={{ width: "auto" }}
          >
            Nova faktura
          </button>
        )}
      </div>

      <div className="events-table-card">
        <div className="events-table-header">
          <h2>Lista faktura</h2>
          <div className="card-hint">Kliknite na red za detalje</div>
        </div>

        <table className="events-table">
          <thead>
            <tr>
              <th>BROJ</th>
              <th>TIP</th>
              <th>STATUS</th>
              <th>UKUPNO</th>
              <th>PLAĆENO</th>
              <th>ROK PLAĆANJA</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td
                  colSpan={6}
                  style={{
                    textAlign: "center",
                    color: "var(--text-muted)",
                    padding: "2rem",
                  }}
                >
                  Učitavanje...
                </td>
              </tr>
            ) : error ? (
              <tr>
                <td
                  colSpan={6}
                  style={{
                    textAlign: "center",
                    color: "var(--danger)",
                    padding: "2rem",
                  }}
                >
                  {error}
                </td>
              </tr>
            ) : fakture.length === 0 ? (
              <tr>
                <td
                  colSpan={6}
                  style={{
                    textAlign: "center",
                    color: "var(--text-muted)",
                    padding: "2rem",
                  }}
                >
                  Nema faktura.
                </td>
              </tr>
            ) : (
              fakture.map((faktura) => (
                <FakturaRow
                  key={faktura.fakturaId}
                  faktura={faktura}
                  onOpen={openDetail}
                />
              ))
            )}
          </tbody>
        </table>
      </div>

      {selectedDetail && (
        <FakturaDetailModal
          faktura={selectedDetail}
          onClose={() => {
            setSelectedId(null);
            setSelectedDetail(null);
          }}
          onAddStavka={handleOpenStavka}
          onNewPlacanje={handleOpenPlacanje}
          onConfirmPlacanje={handleConfirmPlacanje}
          onFailPlacanje={handleFailPlacanje}
          onRequestRefund={handleRequestRefund}
          onApproveRefund={handleApproveRefund}
          onRejectRefund={handleRejectRefund}
          onExecuteRefund={handleExecuteRefund}
          onIssueFaktura={handleIssueFaktura}
          onCancelFaktura={handleCancelFaktura}
          hasRole={hasRole}
          userId={getUserId()}
          detailLoading={detailLoading}
        />
      )}
    </div>
  );
}
