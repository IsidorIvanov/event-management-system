import { useEffect, useMemo, useState } from 'react';
import api from '../services/api';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../components/ToastNotification';
import BudzetFormModal from '../components/budzet/BudzetFormModal';
import StavkaBudzetaModal from '../components/budzet/StavkaBudzetaModal';
import TrosakModal from '../components/budzet/TrosakModal';
import BudzetKategorijeModal from '../components/budzet/BudzetKategorijeModal';
import { AlertLevelBadge, BudzetStatusBadge, StatusKontroleBadge } from '../components/budzet/BudzetStatusBadge';
import * as budzetApi from '../services/budzetService';
import { notifyAlerts } from '../utils/alertUtils';

const formatMoney = (value) => {
  if (value == null) return '—';
  return Number(value).toLocaleString('sr-Latn', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + ' RSD';
};

const formatPercent = (value) => {
  if (value == null) return '—';
  return (Number(value) * 100).toLocaleString('sr-Latn', { minimumFractionDigits: 0, maximumFractionDigits: 2 }) + '%';
};

const ALERT_PRIORITY = {
  NONE: 0,
  WARNING: 1,
  CRITICAL: 2,
  EXCEEDED: 3,
};

const summarizeBudzetAlerts = (budzet) => {
  const stavke = budzet?.stavke || [];
  const activeAlerts = stavke.filter((stavka) => stavka.alertLevel && stavka.alertLevel !== 'NONE');
  const highestAlert = activeAlerts.reduce((highest, stavka) => {
    const currentPriority = ALERT_PRIORITY[stavka.alertLevel] || 0;
    const highestPriority = ALERT_PRIORITY[highest] || 0;
    return currentPriority > highestPriority ? stavka.alertLevel : highest;
  }, 'NONE');

  return {
    activeAlertCount: activeAlerts.length,
    highestAlert,
  };
};

const extractError = (err) => {
  const data = err.response?.data;
  if (typeof data === 'string') return data;
  if (data?.details) return Object.values(data.details).join(', ');
  if (data?.error) return data.error;
  return err.message || 'Došlo je do greške.';
};

export default function BudzetPage() {
  const toast = useToast();
  const { hasRole } = useAuth();
  const isFinKontrolor = hasRole('FINANSIJSKI_KONTROLOR');
  const isMenadzer = hasRole('MENADZER_DOGADJAJA');
  const isKoordinatorResursa = hasRole('KOORDINATOR_RESURSA');
  const isKoordinatorPrograma = hasRole('KOORDINATOR_PROGRAMA');

  const [dogadjaji, setDogadjaji] = useState([]);
  const [selectedDogadjajId, setSelectedDogadjajId] = useState('');
  const [budzeti, setBudzeti] = useState([]);
  const [selectedBudzetId, setSelectedBudzetId] = useState('');
  const [kategorije, setKategorije] = useState([]);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [budzetModal, setBudzetModal] = useState(undefined);
  const [stavkaModal, setStavkaModal] = useState(undefined);
  const [trosakModal, setTrosakModal] = useState(undefined);
  const [showKategorije, setShowKategorije] = useState(false);

  const selectedBudzet = useMemo(
    () => budzeti.find((b) => b.budzetId === Number(selectedBudzetId)) || null,
    [budzeti, selectedBudzetId]
  );

  const loadKategorije = async () => {
    const res = await budzetApi.getKategorije();
    setKategorije(res.data);
  };

  const loadBudzeti = async (dogadjajId, preferredBudzetId = null) => {
    if (!dogadjajId) {
      setBudzeti([]);
      setSelectedBudzetId('');
      return;
    }

    const res = await budzetApi.getBudzetiByDogadjaj(dogadjajId);
    setBudzeti(res.data);
    const preferred = preferredBudzetId && res.data.some((b) => b.budzetId === Number(preferredBudzetId))
      ? String(preferredBudzetId)
      : String(res.data[0]?.budzetId || '');
    setSelectedBudzetId(preferred);
  };

  useEffect(() => {
    Promise.all([api.get('/dogadjaj'), budzetApi.getKategorije()])
      .then(([dogadjajiRes, kategorijeRes]) => {
        setDogadjaji(dogadjajiRes.data);
        setKategorije(kategorijeRes.data);
        setSelectedDogadjajId(String(dogadjajiRes.data[0]?.dogadjajId || ''));
      })
      .catch((err) => toast(extractError(err), 'error'))
      .finally(() => setLoading(false));
  // eslint-disable-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (selectedDogadjajId) {
      loadBudzeti(selectedDogadjajId).catch((err) => toast(extractError(err), 'error'));
    } else {
      setBudzeti([]);
      setSelectedBudzetId('');
    }
  }, [selectedDogadjajId]);

  const withBusy = async (action, successMessage) => {
    setBusy(true);
    try {
      const result = await action();
      if (successMessage) toast(successMessage, 'success');
      return result;
    } catch (err) {
      toast(extractError(err), 'error');
      return null;
    } finally {
      setBusy(false);
    }
  };

  const notifyAlertsForBudzet = async (budzetId) => {
    try {
      const res = await budzetApi.getBudzetAlerts(budzetId);
      notifyAlerts(res.data, toast);
    } catch (err) {
      toast(extractError(err), 'error');
    }
  };

  const handleSaveBudzet = async (payload) => {
    const saved = await withBusy(async () => {
      const res = budzetModal
        ? await budzetApi.updateBudzet(budzetModal.budzetId, payload)
        : await budzetApi.createBudzet(payload);
      return res.data;
    }, budzetModal ? 'Budžet je uspešno izmenjen.' : 'Budžet je uspešno kreiran.');

    if (saved) {
      setBudzetModal(undefined);
      setSelectedDogadjajId(String(saved.dogadjajId));
      await loadBudzeti(saved.dogadjajId, saved.budzetId);
    }
  };

  const handleDeleteBudzet = async (budzet) => {
    if (!window.confirm(`Da li želite da obrišete budžet "${budzet.nazivBudzeta}"?`)) return;
    const deleted = await withBusy(async () => budzetApi.deleteBudzet(budzet.budzetId), 'Budžet je obrisan.');
    if (deleted !== null) await loadBudzeti(selectedDogadjajId);
  };

  const handleSaveStavka = async (payload) => {
    if (!selectedBudzet) return;
    const saved = await withBusy(async () => {
      const res = stavkaModal
        ? await budzetApi.updateStavka(selectedBudzet.budzetId, stavkaModal.kategorijaId, payload)
        : await budzetApi.addStavka(selectedBudzet.budzetId, payload);
      return res.data;
    }, stavkaModal ? 'Stavka budžeta je izmenjena.' : 'Stavka budžeta je dodata.');

    if (saved) {
      setStavkaModal(undefined);
      await loadBudzeti(selectedDogadjajId, selectedBudzet.budzetId);
      notifyAlerts(saved.stavke, toast);
    }
  };

  const handleSaveTrosak = async (payload) => {
    if (!selectedBudzet) return;
    const saved = await withBusy(
      async () => {
        const res = await budzetApi.createTrosak(payload);
        return res.data;
      },
      'Trošak je uspešno evidentiran.'
    );

    if (saved) {
      setTrosakModal(undefined);
      await loadBudzeti(selectedDogadjajId, selectedBudzet.budzetId);
      await notifyAlertsForBudzet(selectedBudzet.budzetId);
    }
  };

  const handleDeleteStavka = async (stavka) => {
    if (!selectedBudzet) return;
    if (!window.confirm(`Obrisati stavku "${stavka.kategorijaNaziv}"?`)) return;
    const deleted = await withBusy(
      async () => budzetApi.deleteStavka(selectedBudzet.budzetId, stavka.kategorijaId),
      'Stavka budžeta je obrisana.'
    );
    if (deleted !== null) await loadBudzeti(selectedDogadjajId, selectedBudzet.budzetId);
  };

  const handleStatusAction = async (action, successMessage) => {
    if (!selectedBudzet) return;
    const saved = await withBusy(async () => {
      const res = await action(selectedBudzet.budzetId);
      return res.data;
    }, successMessage);
    if (saved) await loadBudzeti(selectedDogadjajId, saved.budzetId);
  };

  const handleCreateKategorija = async (payload) => {
    await withBusy(async () => budzetApi.createKategorija(payload), 'Kategorija je dodata.');
    await loadKategorije();
  };

  const handleUpdateKategorija = async (id, payload) => {
    await withBusy(async () => budzetApi.updateKategorija(id, payload), 'Kategorija je izmenjena.');
    await loadKategorije();
  };

  const handleDeleteKategorija = async (id) => {
    if (!window.confirm('Da li želite da obrišete kategoriju?')) return;
    await withBusy(async () => budzetApi.deleteKategorija(id), 'Kategorija je obrisana.');
    await loadKategorije();
  };

  const mozeDodatiStavku  = isFinKontrolor && selectedBudzet?.status === 'DRAFT';
  const mozeEditStavku    = isFinKontrolor && (selectedBudzet?.status === 'DRAFT' || selectedBudzet?.status === 'ACTIVE');
  const mozeBrisatiStavku = isFinKontrolor && selectedBudzet?.status === 'DRAFT';
  const mozeApprove = isMenadzer && selectedBudzet?.status === 'DRAFT';
  const mozeActivate = isFinKontrolor && selectedBudzet?.status === 'APPROVED';
  const mozeClose = isFinKontrolor && selectedBudzet?.status === 'ACTIVE';
  const mozeUnosTroska = selectedBudzet?.status === 'ACTIVE' && (isFinKontrolor || isKoordinatorResursa || isKoordinatorPrograma);
  const selectedBudzetIndex = budzeti.findIndex((b) => b.budzetId === selectedBudzet?.budzetId);
  const moveSelectedBudzet = (direction) => {
    if (budzeti.length === 0 || selectedBudzetIndex < 0) return;
    const nextIndex = selectedBudzetIndex + direction;
    if (nextIndex < 0 || nextIndex >= budzeti.length) return;
    setSelectedBudzetId(String(budzeti[nextIndex].budzetId));
  };

  return (
    <div className="program-page">
      {budzetModal !== undefined && (
        <BudzetFormModal
          budzet={budzetModal}
          dogadjaji={dogadjaji}
          selectedDogadjajId={selectedDogadjajId}
          onClose={() => setBudzetModal(undefined)}
          onSubmit={handleSaveBudzet}
          loading={busy}
        />
      )}

      {stavkaModal !== undefined && selectedBudzet && (
        <StavkaBudzetaModal
          stavka={stavkaModal}
          kategorije={kategorije}
          postojeceStavke={selectedBudzet.stavke || []}
          onClose={() => setStavkaModal(undefined)}
          onSubmit={handleSaveStavka}
          loading={busy}
        />
      )}

      {trosakModal !== undefined && selectedBudzet && (
        <TrosakModal
          budzet={selectedBudzet}
          stavka={trosakModal}
          onClose={() => setTrosakModal(undefined)}
          onSubmit={handleSaveTrosak}
          loading={busy}
        />
      )}

      {showKategorije && (
        <BudzetKategorijeModal
          kategorije={kategorije}
          onClose={() => setShowKategorije(false)}
          onCreate={handleCreateKategorija}
          onUpdate={handleUpdateKategorija}
          onDelete={handleDeleteKategorija}
          loading={busy}
        />
      )}

      <div className="program-header">
        <div>
          <h1>Budžeti</h1>
          <p className="page-subtitle">upravljanje budžetom i planiranje troškova po kategorijama</p>
        </div>
        <div className="program-header-actions">
          {isFinKontrolor && (
            <button className="btn btn-outline btn-sm" onClick={() => setShowKategorije(true)}>
              Kategorije
            </button>
          )}
          {isFinKontrolor && (
            <button className="btn btn-primary btn-sm" onClick={() => setBudzetModal(null)}>
              + Novi budžet
            </button>
          )}
        </div>
      </div>

      <div className="events-table-card" style={{ marginBottom: '1.5rem' }}>
        <div className="events-table-header">
          <h2>Izbor događaja</h2>
          <div className="events-table-controls">
            <select
              className="status-select"
              value={selectedDogadjajId}
              onChange={(e) => setSelectedDogadjajId(e.target.value)}
              disabled={loading}
            >
              <option value="">Izaberite događaj</option>
              {dogadjaji.map((d) => (
                <option key={d.dogadjajId} value={d.dogadjajId}>
                  {d.naziv} ({d.status})
                </option>
              ))}
            </select>
          </div>
        </div>

        {budzeti.length > 0 && (
          <div style={{ marginTop: '1rem' }}>
            <div className="events-table-header" style={{ marginBottom: '0.75rem' }}>
              <div>
                <h3 style={{ margin: 0 }}>Budžeti događaja</h3>
                <div className="card-hint">
                  {budzeti.length} budžet{budzeti.length === 1 ? '' : 'a'} za izabrani događaj
                </div>
              </div>
              <div className="action-buttons">
                <button
                  className="btn btn-outline btn-xs"
                  onClick={() => moveSelectedBudzet(-1)}
                  disabled={selectedBudzetIndex <= 0}
                >
                  Prethodni
                </button>
                <button
                  className="btn btn-outline btn-xs"
                  onClick={() => moveSelectedBudzet(1)}
                  disabled={selectedBudzetIndex < 0 || selectedBudzetIndex >= budzeti.length - 1}
                >
                  Sledeći
                </button>
              </div>
            </div>

            <div
              style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))',
                gap: '0.85rem',
              }}
            >
              {budzeti.map((budzet) => {
                const alertSummary = summarizeBudzetAlerts(budzet);
                const isSelected = budzet.budzetId === selectedBudzet?.budzetId;
                return (
                  <button
                    key={budzet.budzetId}
                    type="button"
                    onClick={() => setSelectedBudzetId(String(budzet.budzetId))}
                    className="info-card"
                    style={{
                      textAlign: 'left',
                      cursor: 'pointer',
                      borderColor: isSelected ? 'var(--primary)' : undefined,
                      boxShadow: isSelected ? '0 0 0 1px var(--primary)' : undefined,
                    }}
                  >
                    <div style={{ display: 'flex', justifyContent: 'space-between', gap: '0.75rem' }}>
                      <strong>{budzet.nazivBudzeta}</strong>
                      <BudzetStatusBadge status={budzet.status} />
                    </div>
                    <div className="card-hint" style={{ marginTop: '0.6rem' }}>
                      Plan: {formatMoney(budzet.planiraniIznos)} · Odobreno: {formatMoney(budzet.odobreniIznos)}
                    </div>
                    <div className="card-hint" style={{ marginTop: '0.35rem' }}>
                      Stvarno: {formatMoney(budzet.ukupnoStvarnoStavke)} · Alert stavki: {alertSummary.activeAlertCount}
                    </div>
                    <div style={{ marginTop: '0.65rem' }}>
                      <AlertLevelBadge level={alertSummary.highestAlert} />
                    </div>
                  </button>
                );
              })}
            </div>
          </div>
        )}

        <table className="events-table">
          <thead>
            <tr><th>Budžet</th><th>Status</th><th>Plan</th><th>Odobreno</th><th>Preostalo</th><th>Akcije</th></tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan={6} style={{ textAlign: 'center', color: 'var(--text-muted)' }}>Učitavanje...</td></tr>
            ) : budzeti.length === 0 ? (
              <tr><td colSpan={6} style={{ textAlign: 'center', color: 'var(--text-muted)' }}>Nema budžeta za izabrani događaj.</td></tr>
            ) : budzeti.map((b) => (
              <tr key={b.budzetId} className={b.budzetId === selectedBudzet?.budzetId ? 'row-selected' : ''}>
                <td>
                  <button className="link-button" onClick={() => setSelectedBudzetId(String(b.budzetId))}>
                    {b.nazivBudzeta}
                  </button>
                </td>
                <td><BudzetStatusBadge status={b.status} /></td>
                <td>{formatMoney(b.planiraniIznos)}</td>
                <td>{formatMoney(b.odobreniIznos)}</td>
                <td>{formatMoney(b.preostaloZaPlaniranje)}</td>
                <td>
                  <div className="action-buttons">
                    {isFinKontrolor && b.status === 'DRAFT' && (
                      <>
                        <button className="btn btn-outline btn-xs" onClick={() => setBudzetModal(b)}>Uredi</button>
                        <button className="btn btn-danger-outline btn-xs" onClick={() => handleDeleteBudzet(b)} disabled={busy}>Obriši</button>
                      </>
                    )}
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {selectedBudzet && (
        <>
          <div className="info-cards" style={{ marginBottom: '1.5rem' }}>
            <div className="info-card">
              <div className="label">Plan budžeta</div>
              <div className="value accent">{formatMoney(selectedBudzet.planiraniIznos)}</div>
            </div>
            <div className="info-card">
              <div className="label">Planirano po stavkama</div>
              <div className="value success">{formatMoney(selectedBudzet.ukupnoPlaniranoStavke)}</div>
            </div>
            <div className="info-card">
              <div className="label">Stvarni trošak</div>
              <div className="value warning">{formatMoney(selectedBudzet.ukupnoStvarnoStavke)}</div>
            </div>
            <div className="info-card">
              <div className="label">Status</div>
              <div className="value"><BudzetStatusBadge status={selectedBudzet.status} /></div>
            </div>
          </div>

          <div className="events-table-card">
            <div className="events-table-header">
              <div>
                <h2>{selectedBudzet.nazivBudzeta}</h2>
                <p className="page-subtitle" style={{ margin: 0 }}>
                  {selectedBudzet.dogadjajNaziv} · kreirao {selectedBudzet.kreiraoIme}
                </p>
              </div>
              <div className="events-table-controls">
                {mozeApprove && (
                  <button className="btn btn-outline btn-sm" onClick={() => handleStatusAction(budzetApi.approveBudzet, 'Budžet je odobren.')} disabled={busy}>
                    Odobri
                  </button>
                )}
                {mozeActivate && (
                  <button className="btn btn-outline btn-sm" onClick={() => handleStatusAction(budzetApi.activateBudzet, 'Budžet je aktiviran.')} disabled={busy}>
                    Aktiviraj
                  </button>
                )}
                {mozeClose && (
                  <button className="btn btn-outline btn-sm" onClick={() => handleStatusAction(budzetApi.closeBudzet, 'Budžet je zatvoren.')} disabled={busy}>
                    Zatvori
                  </button>
                )}
                {mozeDodatiStavku && (
                  <button className="btn btn-primary btn-sm" onClick={() => setStavkaModal(null)} disabled={busy || kategorije.length === 0}>
                    + Stavka
                  </button>
                )}
              </div>
            </div>

            <table className="events-table">
              <thead>
                <tr>
                  <th>Kategorija</th>
                  <th>Planirano</th>
                  <th>Stvarno</th>
                  <th>Iskorišćenost</th>
                  <th>Pragovi</th>
                  <th>Alert</th>
                  <th>Status kontrole</th>
                  <th>Akcije</th>
                </tr>
              </thead>
              <tbody>
                {selectedBudzet.stavke?.length ? selectedBudzet.stavke.map((s) => {
                  const fallbackRatio = Number(s.planiraniIznos) > 0 ? Number(s.stvarniIznos) / Number(s.planiraniIznos) : 0;
                  return (
                    <tr key={`${s.budzetId}-${s.kategorijaId}`}>
                      <td>
                        <div>{s.kategorijaNaziv}</div>
                        {s.komentar && <div className="card-hint">{s.komentar}</div>}
                      </td>
                      <td>{formatMoney(s.planiraniIznos)}</td>
                      <td>{formatMoney(s.stvarniIznos)}</td>
                      <td>{formatPercent(s.iskoriscenost ?? fallbackRatio)}</td>
                      <td>{formatPercent(s.pragUpozorenja)} / {formatPercent(s.pragKriticnog)}</td>
                      <td>
                        <AlertLevelBadge level={s.alertLevel || 'NONE'} />
                        {s.alertPoruka && <div className="card-hint">{s.alertPoruka}</div>}
                      </td>
                      <td><StatusKontroleBadge status={s.statusKontrole} /></td>
                      <td>
                        <div className="action-buttons">
                          {mozeUnosTroska && (
                            <button className="btn btn-primary btn-xs" onClick={() => setTrosakModal(s)} disabled={busy}>
                              Trošak
                            </button>
                          )}
                          {mozeEditStavku && (
                            <button className="btn btn-outline btn-xs" onClick={() => setStavkaModal(s)}>Uredi</button>
                          )}
                          {mozeBrisatiStavku && (
                            <button className="btn btn-danger-outline btn-xs" onClick={() => handleDeleteStavka(s)} disabled={busy}>Obriši</button>
                          )}
                        </div>
                      </td>
                    </tr>
                  );
                }) : (
                  <tr><td colSpan={8} style={{ textAlign: 'center', color: 'var(--text-muted)' }}>Budžet još nema stavke.</td></tr>
                )}
              </tbody>
            </table>
          </div>
        </>
      )}
    </div>
  );
}
