import { useEffect, useMemo, useState } from 'react';
import api from '@/shared/services/api';
import { useAuth } from '@/features/auth/context/AuthContext';
import { useToast } from '@/shared/components/ToastNotification';
import { formatDate as fmtDate } from '@/shared/utils/format';
import * as scenarioApi from '@/features/analiza/services/scenarioPrognozeService';

const TIP_SCENARIJA_DISPLAY = {
  PESIMISTICNI: 'Pesimistični',
  REALISTICNI: 'Realistični',
  OPTIMISTICNI: 'Optimistični',
};

const TIP_SCENARIJA_ORDER = ['PESIMISTICNI', 'REALISTICNI', 'OPTIMISTICNI'];

const EMPTY_FORM = {
  nazivScenarija: '',
  tipScenarija: 'REALISTICNI',
  projektovaniPrihod: '',
  projektovaniTrosak: '',
  pretpostavke: '',
};

const extractError = (error, fallback = 'Došlo je do greške.') => {
  const data = error?.response?.data;
  if (typeof data === 'string') return data;
  if (data?.details) return Object.values(data.details).join(', ');
  if (data?.message) return data.message;
  if (data?.error) return data.error;
  return error?.message || fallback;
};

const formatMoneyString = (value) => (value == null ? '—' : `${value} RSD`);

const formatPercent = (value) => {
  if (value == null) return '—';
  const number = Number(value);
  if (Number.isNaN(number)) return '—';
  return `${(number * 100).toFixed(2)}%`;
};

const formatDate = (value) =>
  fmtDate(value, { day: '2-digit', month: '2-digit', year: 'numeric' });

function ScenarioModal({ onClose, onSubmit, loading }) {
  const [form, setForm] = useState(EMPTY_FORM);

  const setField = (field) => (event) => {
    setForm((prev) => ({ ...prev, [field]: event.target.value }));
  };

  const handleSubmit = (event) => {
    event.preventDefault();
    const prihod = Number(form.projektovaniPrihod);
    const trosak = Number(form.projektovaniTrosak);
    if (!form.nazivScenarija.trim()) {
      return;
    }
    if (Number.isNaN(prihod) || prihod < 0 || Number.isNaN(trosak) || trosak < 0) {
      return;
    }
    onSubmit({
      nazivScenarija: form.nazivScenarija.trim(),
      tipScenarija: form.tipScenarija,
      projektovaniPrihod: form.projektovaniPrihod,
      projektovaniTrosak: form.projektovaniTrosak,
      pretpostavke: form.pretpostavke.trim() || null,
    });
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-card" onClick={(event) => event.stopPropagation()}>
        <div className="modal-header">
          <div>
            <h3 className="modal-title">Novi scenario</h3>
            <p className="page-subtitle" style={{ marginTop: '0.35rem' }}>
              Projekcija prihoda i troškova pre završetka događaja.
            </p>
          </div>
          <button className="modal-close" onClick={onClose} type="button">
            ✕
          </button>
        </div>

        <form onSubmit={handleSubmit} className="form-grid" style={{ padding: '1rem' }}>
          <input
            className="form-control"
            placeholder="Naziv scenarija"
            value={form.nazivScenarija}
            onChange={setField('nazivScenarija')}
            required
          />
          <select
            className="form-control"
            value={form.tipScenarija}
            onChange={setField('tipScenarija')}
            required
          >
            {TIP_SCENARIJA_ORDER.map((tip) => (
              <option key={tip} value={tip}>
                {TIP_SCENARIJA_DISPLAY[tip]}
              </option>
            ))}
          </select>
          <input
            className="form-control"
            type="number"
            min="0"
            step="0.01"
            placeholder="Projektovani prihod"
            value={form.projektovaniPrihod}
            onChange={setField('projektovaniPrihod')}
            required
          />
          <input
            className="form-control"
            type="number"
            min="0"
            step="0.01"
            placeholder="Projektovani trošak"
            value={form.projektovaniTrosak}
            onChange={setField('projektovaniTrosak')}
            required
          />
          <textarea
            className="form-control"
            placeholder="Pretpostavke"
            value={form.pretpostavke}
            onChange={setField('pretpostavke')}
            rows={4}
            style={{ gridColumn: '1 / -1' }}
          />
          <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'flex-end', gridColumn: '1 / -1' }}>
            <button className="btn btn-outline" type="button" onClick={onClose}>
              Otkaži
            </button>
            <button className="btn btn-primary" type="submit" disabled={loading}>
              {loading ? 'Čuvanje...' : 'Sačuvaj scenario'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

function MetricsModal({ scenario, metrics, onClose }) {
  if (!scenario || !metrics) return null;

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-card" onClick={(event) => event.stopPropagation()}>
        <div className="modal-header">
          <div>
            <h3 className="modal-title">Metrike scenarija</h3>
            <p className="page-subtitle" style={{ marginTop: '0.35rem' }}>
              {scenario.nazivScenarija} · {TIP_SCENARIJA_DISPLAY[scenario.tipScenarija] || scenario.tipScenarija}
            </p>
          </div>
          <button className="modal-close" onClick={onClose} type="button">
            ✕
          </button>
        </div>
        <div className="info-cards" style={{ padding: '1rem' }}>
          <div className="info-card">
            <div className="label">Neto</div>
            <div className="value accent">{formatMoneyString(metrics.neto)}</div>
          </div>
          <div className="info-card">
            <div className="label">Marža</div>
            <div className="value success">{formatPercent(metrics.marza)}</div>
          </div>
          <div className="info-card">
            <div className="label">ROI</div>
            <div className="value warning">{formatPercent(metrics.roi)}</div>
          </div>
        </div>
      </div>
    </div>
  );
}

export default function ScenarioPrognozePage() {
  const toast = useToast();
  const { hasRole } = useAuth();
  const canCreate = hasRole('FINANSIJSKI_KONTROLOR') || hasRole('MENADZER_DOGADJAJA');

  const [dogadjaji, setDogadjaji] = useState([]);
  const [selectedDogadjajId, setSelectedDogadjajId] = useState('');
  const [scenariji, setScenariji] = useState([]);
  const [metricsByScenarioId, setMetricsByScenarioId] = useState({});
  const [metricsTarget, setMetricsTarget] = useState(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  const [showCreateModal, setShowCreateModal] = useState(false);

  const selectedDogadjaj = useMemo(
    () => dogadjaji.find((dogadjaj) => String(dogadjaj.dogadjajId) === String(selectedDogadjajId)) || null,
    [dogadjaji, selectedDogadjajId],
  );

  const latestByType = useMemo(() => {
    const result = {};
    TIP_SCENARIJA_ORDER.forEach((tip) => {
      result[tip] = scenariji
        .filter((scenario) => scenario.tipScenarija === tip)
        .sort((a, b) => new Date(b.createdAt || 0) - new Date(a.createdAt || 0))[0] || null;
    });
    return result;
  }, [scenariji]);

  const loadScenariji = async (dogadjajId) => {
    if (!dogadjajId) {
      setScenariji([]);
      return;
    }
    const res = await scenarioApi.getScenarijiByDogadjaj(dogadjajId);
    setScenariji(Array.isArray(res.data) ? res.data : []);
  };

  useEffect(() => {
    api
      .get('/dogadjaj')
      .then((res) => {
        const nezavrseni = (Array.isArray(res.data) ? res.data : []).filter(
          (dogadjaj) => dogadjaj.status !== 'ZAVRSEN',
        );
        setDogadjaji(nezavrseni);
        setSelectedDogadjajId(String(nezavrseni[0]?.dogadjajId || ''));
      })
      .catch((err) => {
        setError(extractError(err, 'Nije moguće učitati događaje.'));
      })
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    if (!selectedDogadjajId) {
      setScenariji([]);
      return;
    }
    loadScenariji(selectedDogadjajId).catch((err) => {
      toast(extractError(err, 'Nije moguće učitati scenarije.'), 'error');
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedDogadjajId]);

  const handleCreateScenario = async (payload) => {
    if (!selectedDogadjajId) return;
    setBusy(true);
    try {
      await scenarioApi.createScenario(selectedDogadjajId, payload);
      toast('Scenario je uspešno kreiran.', 'success');
      setShowCreateModal(false);
      await loadScenariji(selectedDogadjajId);
    } catch (err) {
      toast(extractError(err, 'Kreiranje scenarija nije uspelo.'), 'error');
    } finally {
      setBusy(false);
    }
  };

  const handleShowMetrics = async (scenario) => {
    setBusy(true);
    try {
      const cached = metricsByScenarioId[scenario.scenarioId];
      const metrics = cached || (await scenarioApi.getScenarioMetrics(scenario.scenarioId)).data;
      setMetricsByScenarioId((prev) => ({ ...prev, [scenario.scenarioId]: metrics }));
      setMetricsTarget(scenario);
    } catch (err) {
      toast(extractError(err, 'Nije moguće učitati metrike scenarija.'), 'error');
    } finally {
      setBusy(false);
    }
  };

  const selectedMetrics = metricsTarget ? metricsByScenarioId[metricsTarget.scenarioId] : null;

  return (
    <div className="program-page">
      {showCreateModal && (
        <ScenarioModal
          onClose={() => setShowCreateModal(false)}
          onSubmit={handleCreateScenario}
          loading={busy}
        />
      )}
      <MetricsModal
        scenario={metricsTarget}
        metrics={selectedMetrics}
        onClose={() => setMetricsTarget(null)}
      />

      <div className="program-header">
        <div>
          <h1>Scenario prognoze</h1>
          <p className="page-subtitle">
            F5 projekcije prihoda i troškova za događaje koji još nisu završeni.
          </p>
        </div>
        {canCreate && selectedDogadjajId && (
          <button className="btn btn-primary" onClick={() => setShowCreateModal(true)} disabled={busy}>
            Novi scenario
          </button>
        )}
      </div>

      {loading ? (
        <div className="events-table-card">Učitavanje...</div>
      ) : error ? (
        <div className="events-table-card" style={{ color: 'var(--danger)' }}>{error}</div>
      ) : dogadjaji.length === 0 ? (
        <div className="events-table-card">
          Nema događaja koji su dostupni za scenario prognoze. Scenario se kreira samo pre završetka događaja.
        </div>
      ) : (
        <>
          <div className="events-table-card" style={{ marginBottom: '1.5rem' }}>
            <div className="events-table-header">
              <h2>Izbor događaja</h2>
              <select
                className="status-select"
                value={selectedDogadjajId}
                onChange={(event) => setSelectedDogadjajId(event.target.value)}
              >
                {dogadjaji.map((dogadjaj) => (
                  <option key={dogadjaj.dogadjajId} value={dogadjaj.dogadjajId}>
                    {dogadjaj.naziv} · {dogadjaj.status}
                  </option>
                ))}
              </select>
            </div>
            {selectedDogadjaj && (
              <p className="page-subtitle" style={{ padding: '0 1rem 1rem' }}>
                Izabrani događaj: {selectedDogadjaj.naziv}
              </p>
            )}
          </div>

          <div className="info-cards" style={{ marginBottom: '1.5rem' }}>
            {TIP_SCENARIJA_ORDER.map((tip) => {
              const scenario = latestByType[tip];
              return (
                <div key={tip} className="info-card">
                  <div className="label">{TIP_SCENARIJA_DISPLAY[tip]}</div>
                  <div className="value accent" style={{ fontSize: '1.15rem' }}>
                    {scenario?.nazivScenarija || '—'}
                  </div>
                  <div className="card-hint">
                    {scenario
                      ? `${formatMoneyString(scenario.projektovaniPrihod)} / ${formatMoneyString(scenario.projektovaniTrosak)}`
                      : 'Nema scenarija ovog tipa'}
                  </div>
                </div>
              );
            })}
          </div>

          <div className="events-table-card">
            <div className="events-table-header">
              <h2>Scenariji</h2>
              <span className="page-subtitle">{scenariji.length} zapisa</span>
            </div>
            {scenariji.length === 0 ? (
              <p style={{ color: 'var(--text-muted)', padding: '1rem' }}>
                Nema scenarija za izabrani događaj.
              </p>
            ) : (
              <table className="events-table">
                <thead>
                  <tr>
                    <th>Naziv</th>
                    <th>Tip</th>
                    <th>Prihod</th>
                    <th>Trošak</th>
                    <th>Pretpostavke</th>
                    <th>Kreirano</th>
                    <th>Akcije</th>
                  </tr>
                </thead>
                <tbody>
                  {scenariji.map((scenario) => (
                    <tr key={scenario.scenarioId}>
                      <td><strong>{scenario.nazivScenarija}</strong></td>
                      <td>{TIP_SCENARIJA_DISPLAY[scenario.tipScenarija] || scenario.tipScenarija}</td>
                      <td>{formatMoneyString(scenario.projektovaniPrihod)}</td>
                      <td>{formatMoneyString(scenario.projektovaniTrosak)}</td>
                      <td>{scenario.pretpostavke || '—'}</td>
                      <td>{formatDate(scenario.createdAt)}</td>
                      <td>
                        <button
                          className="btn btn-outline btn-xs"
                          onClick={() => handleShowMetrics(scenario)}
                          disabled={busy}
                        >
                          Metrike
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </>
      )}
    </div>
  );
}
