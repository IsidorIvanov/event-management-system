import { useEffect, useMemo, useState } from "react";
import api from "@/shared/services/api.js";
import { ugovorService } from "../services/ugovorService";
import { useToast } from "@/shared/components/ToastNotification.jsx";
import { useAuth } from "@/features/auth/context/AuthContext.jsx";

const STATUS_LABEL = {
    NACRT: "Nacrt",
    AKTIVAN: "Aktivan",
    ISTEKAO: "Istekao",
    RASKINUT: "Raskinut",
};

const EMPTY_FORM = {
    brojUgovora: "",
    dobavljacId: "",
    dogadjajId: "",
    datumPotpisivanja: "",
    vaziDo: "",
    vrednost: "",
    predmet: "",
    usloviPlacanja: "",
};

const formatDate = (value) =>
    value
        ? new Date(value).toLocaleDateString("sr-Latn", {
            day: "2-digit",
            month: "2-digit",
            year: "numeric",
        })
        : "—";

const formatMoney = (value) =>
    Number(value || 0).toLocaleString("sr-Latn", {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
    });

export default function UgovoriPage() {
    const toast = useToast();
    const { hasRole } = useAuth();
    const canManageContracts = hasRole("MENADZER_DOGADJAJA");
    const [ugovori, setUgovori] = useState([]);
    const [dobavljaci, setDobavljaci] = useState([]);
    const [dogadjaji, setDogadjaji] = useState([]);
    const [filters, setFilters] = useState({ dobavljacId: "", dogadjajId: "", status: "" });
    const [form, setForm] = useState(EMPTY_FORM);
    const [documentTarget, setDocumentTarget] = useState(null);
    const [documentUrl, setDocumentUrl] = useState("");
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState(null);

    const loadUgovori = () => {
        setLoading(true);
        const params = Object.fromEntries(
            Object.entries(filters).filter(([, value]) => value !== ""),
        );
        ugovorService
            .getAll(params)
            .then((data) => {
                setUgovori(Array.isArray(data) ? data : []);
                setError(null);
            })
            .catch(() => setError("Nije moguće učitati ugovore."))
            .finally(() => setLoading(false));
    };

    useEffect(() => {
        Promise.all([
            api.get("/nabavka/dobavljac"),
            api.get("/dogadjaj"),
        ])
            .then(([dobavljaciRes, dogadjajiRes]) => {
                setDobavljaci(Array.isArray(dobavljaciRes.data) ? dobavljaciRes.data : []);
                setDogadjaji(Array.isArray(dogadjajiRes.data) ? dogadjajiRes.data : []);
            })
            .catch(() => toast("Nije moguće učitati dobavljače ili događaje.", "error"));
    }, [toast]);

    useEffect(() => {
        loadUgovori();
    }, [filters.dobavljacId, filters.dogadjajId, filters.status]);

    const stats = useMemo(
        () => ({
            total: ugovori.length,
            aktivni: ugovori.filter((u) => u.status === "AKTIVAN").length,
            bezDokumenta: ugovori.filter((u) => u.status === "AKTIVAN" && !u.dokumentUrl).length,
        }),
        [ugovori],
    );

    const setField = (field) => (event) => {
        setForm((prev) => ({ ...prev, [field]: event.target.value }));
    };

    const setFilter = (field) => (event) => {
        setFilters((prev) => ({ ...prev, [field]: event.target.value }));
    };

    const handleCreate = (event) => {
        event.preventDefault();
        setSaving(true);
        ugovorService
            .create({
                brojUgovora: form.brojUgovora.trim(),
                dobavljacId: Number(form.dobavljacId),
                dogadjajId: Number(form.dogadjajId),
                datumPotpisivanja: form.datumPotpisivanja,
                vaziDo: form.vaziDo,
                vrednost: Number(form.vrednost || 0).toFixed(2),
                predmet: form.predmet.trim(),
                usloviPlacanja: form.usloviPlacanja.trim() || null,
            })
            .then(() => {
                toast("Ugovor je kreiran u NACRT statusu.", "success");
                setForm(EMPTY_FORM);
                loadUgovori();
            })
            .catch((err) => {
                toast(err.response?.data?.error || err.response?.data?.message || "Kreiranje ugovora nije uspelo.", "error");
            })
            .finally(() => setSaving(false));
    };

    const updateRow = (updated) => {
        setUgovori((prev) =>
            prev.map((ugovor) => (ugovor.ugovorId === updated.ugovorId ? updated : ugovor)),
        );
    };

    const handleAttachDocument = (event) => {
        event.preventDefault();
        if (!documentTarget) return;
        ugovorService
            .attachDocument(documentTarget.ugovorId, documentUrl.trim())
            .then((updated) => {
                updateRow(updated);
                setDocumentTarget(null);
                setDocumentUrl("");
                toast("Dokument je povezan sa ugovorom.", "success");
            })
            .catch((err) => {
                toast(err.response?.data?.message || "Dodavanje dokumenta nije uspelo.", "error");
            });
    };

    const handleActivate = (id) => {
        ugovorService
            .activate(id)
            .then((updated) => {
                updateRow(updated);
                toast("Ugovor je aktiviran.", "success");
            })
            .catch((err) => {
                toast(err.response?.data?.message || "Aktivacija ugovora nije uspela.", "error");
            });
    };

    const handleTerminate = (id) => {
        ugovorService
            .terminate(id)
            .then((updated) => {
                updateRow(updated);
                toast("Ugovor je raskinut.", "success");
            })
            .catch((err) => {
                toast(err.response?.data?.message || "Raskid ugovora nije uspeo.", "error");
            });
    };

    return (
        <div>
            <div className="page-header">
                <div>
                    <h1>Ugovori</h1>
                    <p className="page-subtitle">
                        F4 upravljanje ugovorima sa dobavljačima za finansijski podsistem.
                    </p>
                </div>
            </div>

            <div className="info-cards" style={{ marginBottom: "1.5rem" }}>
                <div className="info-card">
                    <div className="label">Ukupno</div>
                    <div className="value accent">{stats.total}</div>
                </div>
                <div className="info-card">
                    <div className="label">Aktivni</div>
                    <div className="value success">{stats.aktivni}</div>
                </div>
                <div className="info-card">
                    <div className="label">Aktivni bez dokumenta</div>
                    <div className="value warning">{stats.bezDokumenta}</div>
                </div>
            </div>

            {canManageContracts && (
                <div className="events-table-card" style={{ marginBottom: "1.5rem" }}>
                    <div className="events-table-header">
                        <h2>Novi ugovor</h2>
                    </div>
                    <form onSubmit={handleCreate} className="form-grid" style={{ padding: "1rem" }}>
                        <input className="form-control" placeholder="Broj ugovora" value={form.brojUgovora} onChange={setField("brojUgovora")} required />
                        <select className="form-control" value={form.dobavljacId} onChange={setField("dobavljacId")} required>
                            <option value="">Dobavljač</option>
                            {dobavljaci.map((dobavljac) => (
                                <option key={dobavljac.dobavljacId} value={dobavljac.dobavljacId}>
                                    {dobavljac.naziv}
                                </option>
                            ))}
                        </select>
                        <select className="form-control" value={form.dogadjajId} onChange={setField("dogadjajId")} required>
                            <option value="">Događaj</option>
                            {dogadjaji.map((dogadjaj) => (
                                <option key={dogadjaj.dogadjajId} value={dogadjaj.dogadjajId}>
                                    {dogadjaj.naziv}
                                </option>
                            ))}
                        </select>
                        <input className="form-control" type="date" value={form.datumPotpisivanja} onChange={setField("datumPotpisivanja")} required />
                        <input className="form-control" type="date" value={form.vaziDo} onChange={setField("vaziDo")} min={form.datumPotpisivanja || undefined} required />
                        <input className="form-control" type="number" step="0.01" min="0" placeholder="Vrednost" value={form.vrednost} onChange={setField("vrednost")} required />
                        <input className="form-control" placeholder="Predmet" value={form.predmet} onChange={setField("predmet")} required />
                        <input className="form-control" placeholder="Uslovi plaćanja" value={form.usloviPlacanja} onChange={setField("usloviPlacanja")} />
                        <button className="btn btn-primary" disabled={saving} type="submit">
                            {saving ? "Čuvanje..." : "Kreiraj ugovor"}
                        </button>
                    </form>
                </div>
            )}

            <div className="events-table-card">
                <div className="events-table-header">
                    <h2>Lista ugovora</h2>
                    <div className="events-table-controls">
                        <select className="status-select" value={filters.dobavljacId} onChange={setFilter("dobavljacId")}>
                            <option value="">svi dobavljači</option>
                            {dobavljaci.map((dobavljac) => (
                                <option key={dobavljac.dobavljacId} value={dobavljac.dobavljacId}>
                                    {dobavljac.naziv}
                                </option>
                            ))}
                        </select>
                        <select className="status-select" value={filters.dogadjajId} onChange={setFilter("dogadjajId")}>
                            <option value="">svi događaji</option>
                            {dogadjaji.map((dogadjaj) => (
                                <option key={dogadjaj.dogadjajId} value={dogadjaj.dogadjajId}>
                                    {dogadjaj.naziv}
                                </option>
                            ))}
                        </select>
                        <select className="status-select" value={filters.status} onChange={setFilter("status")}>
                            <option value="">svi statusi</option>
                            {Object.entries(STATUS_LABEL).map(([value, label]) => (
                                <option key={value} value={value}>
                                    {label}
                                </option>
                            ))}
                        </select>
                    </div>
                </div>

                {error && <div className="error-msg" style={{ margin: "1rem" }}>{error}</div>}
                {canManageContracts && documentTarget && (
                    <form onSubmit={handleAttachDocument} style={{ padding: "1rem", display: "flex", gap: "0.75rem" }}>
                        <input
                            className="form-control"
                            value={documentUrl}
                            onChange={(event) => setDocumentUrl(event.target.value)}
                            placeholder={`Dokument URL za ${documentTarget.brojUgovora}`}
                            required
                        />
                        <button className="btn btn-primary" type="submit">Sačuvaj</button>
                        <button className="btn btn-outline" type="button" onClick={() => setDocumentTarget(null)}>Otkaži</button>
                    </form>
                )}

                <table className="events-table">
                    <thead>
                    <tr>
                        <th>Broj</th>
                        <th>Dobavljač / događaj</th>
                        <th>Predmet</th>
                        <th>Važenje</th>
                        <th>Vrednost</th>
                        <th>Status</th>
                        {canManageContracts && <th>Akcije</th>}
                    </tr>
                    </thead>
                    <tbody>
                    {loading ? (
                        <tr><td colSpan={canManageContracts ? 7 : 6} style={{ textAlign: "center", padding: "2rem" }}>Učitavanje...</td></tr>
                    ) : ugovori.length === 0 ? (
                        <tr><td colSpan={canManageContracts ? 7 : 6} style={{ textAlign: "center", padding: "2rem" }}>Nema ugovora.</td></tr>
                    ) : (
                        ugovori.map((ugovor) => (
                            <tr key={ugovor.ugovorId}>
                                <td>
                                    <strong>{ugovor.brojUgovora}</strong>
                                    {ugovor.status === "AKTIVAN" && !ugovor.dokumentUrl && (
                                        <div style={{ color: "var(--warning)", fontSize: "0.8rem", marginTop: "0.25rem" }}>
                                            Aktivan bez priloženog dokumenta
                                        </div>
                                    )}
                                </td>
                                <td>
                                    {ugovor.dobavljacNaziv || `Dobavljač #${ugovor.dobavljacId}`}
                                    <div style={{ color: "var(--text-muted)", fontSize: "0.85rem" }}>
                                        {ugovor.dogadjajNaziv || `Događaj #${ugovor.dogadjajId}`}
                                    </div>
                                </td>
                                <td>{ugovor.predmet}</td>
                                <td>{formatDate(ugovor.datumPotpisivanja)} - {formatDate(ugovor.vaziDo)}</td>
                                <td>{formatMoney(ugovor.vrednost)} RSD</td>
                                <td>{STATUS_LABEL[ugovor.status] || ugovor.status}</td>
                                {canManageContracts && (
                                    <td>
                                        <div style={{ display: "flex", gap: "0.4rem", flexWrap: "wrap" }}>
                                            <button className="btn btn-outline btn-sm" onClick={() => {
                                                setDocumentTarget(ugovor);
                                                setDocumentUrl(ugovor.dokumentUrl || "");
                                            }}>
                                                Dokument
                                            </button>
                                            {ugovor.status === "NACRT" && (
                                                <button className="btn btn-primary btn-sm" onClick={() => handleActivate(ugovor.ugovorId)}>
                                                    Aktiviraj
                                                </button>
                                            )}
                                            {(ugovor.status === "NACRT" || ugovor.status === "AKTIVAN") && (
                                                <button className="btn btn-danger btn-sm" onClick={() => handleTerminate(ugovor.ugovorId)}>
                                                    Raskini
                                                </button>
                                            )}
                                        </div>
                                    </td>
                                )}
                            </tr>
                        ))
                    )}
                    </tbody>
                </table>
            </div>
        </div>
    );
}
