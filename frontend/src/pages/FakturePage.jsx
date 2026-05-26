import { useEffect, useMemo, useState } from "react";
import { useAuth } from "../context/AuthContext";
import { useToast } from "../components/ToastNotification";
import api from "../services/api";
import KreirajFakturuModal from "../components/fakture/KreirajFakturuModal.jsx";
import DodajStavkuModal from "../components/fakture/DodajStavkuModal.jsx";
import KreirajPlacanjeModal from "../components/fakture/KreirajPlacanjeModal.jsx";
import RefundacijaModal from "../components/fakture/RefundacijaModal.jsx";
import * as fakturaApi from "../services/fakturaService";
import * as budzetApi from "../services/budzetService";
import { AlertLevelBadge } from "../components/budzet/BudzetStatusBadge.jsx";
import { notifyAlerts } from "../utils/alertUtils";

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

const FAKTURA_STATUS_DISPLAY = {
  DRAFT: "Nacrt",
  IZDATA: "Izdata",
  DELIMICNO_PLACENA: "Delimično plaćena",
  PLACENA: "Plaćena",
  DOSPELA: "Dospela",
  OTKAZANA: "Otkazana",
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

const PAYABLE_STATUSES = new Set(["IZDATA", "DELIMICNO_PLACENA", "DOSPELA"]);
const RESERVED_REFUND_STATUSES = new Set(["TRAZENA", "ODOBRENA", "IZVRSENA"]);

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

const getRemainingDebt = (faktura) =>
  Math.max(0, Number(faktura?.ukupnaIznos || 0) - Number(faktura?.placeniIznos || 0));

const getRefundableAmount = (placanje) => {
  const reserved = (placanje?.refundacije || [])
    .filter((refundacija) => RESERVED_REFUND_STATUSES.has(refundacija.status))
    .reduce((sum, refundacija) => sum + Number(refundacija.iznos || 0), 0);
  return Math.max(0, Number(placanje?.iznos || 0) - reserved);
};

function StatusBadge({ value, styleMap, labelMap = {} }) {
  return (
    <span
      className="status-badge"
      style={styleMap[value] || { backgroundColor: "#6b7280", color: "#fff" }}
    >
      {labelMap[value] || value}
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
        <StatusBadge
          value={faktura.status}
          styleMap={FAKTURA_STATUS_STYLE}
          labelMap={FAKTURA_STATUS_DISPLAY}
        />
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
  onDeleteStavka,
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
  stavkaAlerts,
}) {
  if (!faktura) return null;

  const isFinansijski = hasRole("FINANSIJSKI_KONTROLOR");
  const isMenadzer = hasRole("MENADZER_DOGADJAJA");
  const canAddStavka = isFinansijski && faktura.status === "DRAFT";
  const canNewPlacanje =
    isFinansijski &&
    PAYABLE_STATUSES.has(faktura.status) &&
    getRemainingDebt(faktura) > 0;
  const canCancel =
    isFinansijski &&
    faktura.status !== "OTKAZANA";

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div
        className="modal-card modal-card-wide faktura-detail-modal"
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

        <div className="faktura-detail-scroll">
          <div className="info-cards faktura-summary-cards">
            <div className="info-card">
              <div className="label">Status</div>
              <StatusBadge
                value={faktura.status}
                styleMap={FAKTURA_STATUS_STYLE}
                labelMap={FAKTURA_STATUS_DISPLAY}
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

          <div className="faktura-detail-main">
            <div className="events-table-card faktura-section-card">
              <h3>Stavke</h3>
              {faktura.stavke?.length ? (
                <div className="faktura-table-scroll">
                  <table className="events-table">
                    <thead>
                      <tr>
                        <th>NAZIV</th>
                        <th>KOL</th>
                        <th>JED. CENA</th>
                        <th>UKUPNO</th>
                        <th>BUDŽET</th>
                        <th>KATEGORIJA</th>
                        <th>ALERT</th>
                        {canAddStavka && <th>AKCIJE</th>}
                      </tr>
                    </thead>
                    <tbody>
                      {faktura.stavke.map((stavka) => {
                        const alert = stavkaAlerts?.[`${stavka.budzetId}:${stavka.kategorijaId}`];
                        return (
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
                            <td>
                              {stavka.trosakId ? (
                                <>
                                  <AlertLevelBadge level={alert?.alertLevel || "NONE"} />
                                  {alert?.poruka && <div className="card-hint">{alert.poruka}</div>}
                                </>
                              ) : (
                                "—"
                              )}
                            </td>
                            {canAddStavka && (
                              <td>
                                <button
                                  className="btn btn-xs btn-danger-outline"
                                  onClick={() => onDeleteStavka(stavka)}
                                >
                                  Obriši
                                </button>
                              </td>
                            )}
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              ) : (
                <p style={{ color: "var(--text-muted)" }}>Nema stavki.</p>
              )}
            </div>

            <div className="events-table-card faktura-section-card">
              <h3>Plaćanja</h3>
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
                    {placanje.referentniBroj && (
                      <div
                        style={{
                          color: "var(--text-secondary)",
                          marginTop: "0.25rem",
                        }}
                      >
                        Referenca: {placanje.referentniBroj}
                      </div>
                    )}
                    {placanje.datumPlacanja && (
                      <div
                        style={{
                          color: "var(--text-secondary)",
                          marginTop: "0.25rem",
                        }}
                      >
                        Datum plaćanja: {formatDate(placanje.datumPlacanja)}
                      </div>
                    )}
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
                    {placanje.dokumentUrl && (
                      <div
                        style={{
                          color: "var(--text-secondary)",
                          marginTop: "0.25rem",
                        }}
                      >
                        Dokument:{" "}
                        <a
                          href={placanje.dokumentUrl}
                          target="_blank"
                          rel="noreferrer"
                        >
                          otvori
                        </a>
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
                  {isFinansijski &&
                    placanje.status === "COMPLETED" &&
                    getRefundableAmount(placanje) > 0 && (
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
          </div>
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
  const [stavkaAlerts, setStavkaAlerts] = useState({});
  const [detailLoading, setDetailLoading] = useState(false);
  const [dogadjaji, setDogadjaji] = useState([]);
  const [dogadjajiLoading, setDogadjajiLoading] = useState(false);

  const selectedSummary = useMemo(
    () => fakture.find((f) => f.fakturaId === selectedId) || null,
    [fakture, selectedId],
  );

  const getBudgetIdsFromFaktura = (faktura) => [
    ...new Set((faktura?.stavke || []).map((stavka) => stavka.budzetId).filter(Boolean)),
  ];

  const loadAlertsForFaktura = async (faktura) => {
    const budzetIds = getBudgetIdsFromFaktura(faktura);
    if (budzetIds.length === 0) {
      setStavkaAlerts({});
      return [];
    }

    const alertLists = await Promise.all(
      budzetIds.map(async (budzetId) => {
        try {
          const res = await budzetApi.getBudzetAlerts(budzetId);
          return res.data || [];
        } catch {
          return [];
        }
      }),
    );
    const alerts = alertLists.flat();
    setStavkaAlerts(
      alerts.reduce((map, alert) => {
        map[`${alert.budzetId}:${alert.kategorijaId}`] = alert;
        return map;
      }, {}),
    );
    return alerts;
  };

  const notifyBudgetAlerts = async (budzetIds) => {
    const uniqueIds = [...new Set((budzetIds || []).filter(Boolean))];
    if (uniqueIds.length === 0) return;

    const alertLists = await Promise.all(
      uniqueIds.map(async (budzetId) => {
        try {
          const res = await budzetApi.getBudzetAlerts(budzetId);
          return res.data || [];
        } catch {
          return [];
        }
      }),
    );
    notifyAlerts(alertLists.flat(), toast);
  };

  const loadAll = async () => {
    setLoading(true);
    try {
      const res = await fakturaApi.getAllFakture();
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
      await loadAlertsForFaktura(res.data);
      return res.data;
    } catch (err) {
      toast(
        extractError(err, "Greška pri učitavanju detalja fakture."),
        "error",
      );
    } finally {
      setDetailLoading(false);
    }
    return null;
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
    const shouldNotifyAlerts = selectedDetail?.tip === "ULAZNA";

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
      if (shouldNotifyAlerts) {
        await notifyBudgetAlerts([payload.budzetId]);
      }
    } catch (err) {
      toast(extractError(err, "Greška pri dodavanju stavke."), "error");
    }
  };

  const handleDeleteStavka = async (stavka) => {
    const fakturaId = selectedDetail?.fakturaId;
    const stavkaId = stavka?.stavkaFaktureId;
    const shouldNotifyAlerts = selectedDetail?.tip === "ULAZNA";
    const budzetId = stavka?.budzetId;
    if (!fakturaId || !stavkaId) return;
    if (!window.confirm("Da li sigurno želiš da obrišeš ovu stavku?")) return;

    try {
      await fakturaApi.deleteStavka(fakturaId, stavkaId);
      toast("Stavka je obrisana.", "success");
      await loadAll();
      setSelectedId(fakturaId);
      await loadDetail(fakturaId);
      if (shouldNotifyAlerts) {
        await notifyBudgetAlerts([budzetId]);
      }
    } catch (err) {
      toast(extractError(err, "Greška pri brisanju stavke."), "error");
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
    setRefundOpen({ placanje });
  };

  const handleRefundSubmit = async (placanjeId, payload) => {
    try {
      await fakturaApi.requestRefundacija(placanjeId, payload);
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
      await fakturaApi.approveRefundacija(refundacija.refundacijaId);
      toast("Refundacija je odobrena.", "success");
      await refresh();
    } catch (err) {
      toast(extractError(err, "Greška pri odobrenju refundacije."), "error");
    }
  };

  const handleRejectRefund = async (refundacija) => {
    try {
      await fakturaApi.rejectRefundacija(refundacija.refundacijaId);
      toast("Refundacija je odbijena.", "success");
      await refresh();
    } catch (err) {
      toast(extractError(err, "Greška pri odbijanju refundacije."), "error");
    }
  };

  const handleExecuteRefund = async (refundacija) => {
    const shouldNotifyAlerts = selectedDetail?.tip === "ULAZNA";
    const affectedBudzetIds = getBudgetIdsFromFaktura(selectedDetail);
    try {
      await fakturaApi.executeRefundacija(refundacija.refundacijaId);
      toast("Refundacija je izvršena.", "success");
      await refresh();
      if (shouldNotifyAlerts) {
        await notifyBudgetAlerts(affectedBudzetIds);
      }
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
    const shouldNotifyAlerts = selectedDetail?.tip === "ULAZNA";
    const affectedBudzetIds = getBudgetIdsFromFaktura(selectedDetail);
    try {
      await fakturaApi.cancelFaktura(selectedId);
      toast("Faktura je otkazana.", "success");
      await refresh();
      if (shouldNotifyAlerts) {
        await notifyBudgetAlerts(affectedBudzetIds);
      }
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
          faktura={selectedDetail}
        />
      )}

      {refundOpen && (
        <RefundacijaModal
          placanje={refundOpen.placanje}
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
          onDeleteStavka={handleDeleteStavka}
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
          stavkaAlerts={stavkaAlerts}
          detailLoading={detailLoading}
        />
      )}
    </div>
  );
}
