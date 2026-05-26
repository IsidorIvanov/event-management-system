import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useToast } from '../components/ToastNotification';
import { AlertLevelBadge } from '../components/budzet/BudzetStatusBadge';
import * as budzetApi from '../services/budzetService';

const ALERT_LEVELS = ['WARNING', 'CRITICAL', 'EXCEEDED'];

const LEVEL_LABEL = {
  WARNING: 'Upozorenje',
  CRITICAL: 'Kritično',
  EXCEEDED: 'Prekoračeno',
};

const formatPercent = (value) => {
  if (value == null) return '—';
  return `${(Number(value) * 100).toLocaleString('sr-Latn', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 2,
  })}%`;
};

const extractError = (error, fallback) =>
  error?.response?.data?.message ||
  error?.response?.data?.error ||
  error?.message ||
  fallback;

export default function UpozorenjaPage() {
  const toast = useToast();
  const navigate = useNavigate();
  const [alerts, setAlerts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filters, setFilters] = useState({
    level: '',
    dogadjajId: '',
    budzetId: '',
  });

  const loadAlerts = async () => {
    const res = await budzetApi.getActiveBudzetAlerts();
    setAlerts(res.data || []);
  };

  useEffect(() => {
    loadAlerts()
      .catch((err) => toast(extractError(err, 'Greška pri učitavanju upozorenja.'), 'error'))
      .finally(() => setLoading(false));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const dogadjajOptions = useMemo(
    () =>
      Array.from(
        new Map(
          alerts
            .filter((alert) => alert.dogadjajId)
            .map((alert) => [alert.dogadjajId, alert.dogadjajNaziv || `#${alert.dogadjajId}`]),
        ).entries(),
      ),
    [alerts],
  );

  const budzetOptions = useMemo(
    () =>
      Array.from(
        new Map(
          alerts
            .filter((alert) => !filters.dogadjajId || String(alert.dogadjajId) === String(filters.dogadjajId))
            .map((alert) => [alert.budzetId, alert.nazivBudzeta || `#${alert.budzetId}`]),
        ).entries(),
      ),
    [alerts, filters.dogadjajId],
  );

  const filteredAlerts = useMemo(
    () =>
      alerts.filter((alert) => {
        const matchesLevel = !filters.level || alert.alertLevel === filters.level;
        const matchesDogadjaj = !filters.dogadjajId || String(alert.dogadjajId) === String(filters.dogadjajId);
        const matchesBudzet = !filters.budzetId || String(alert.budzetId) === String(filters.budzetId);
        return matchesLevel && matchesDogadjaj && matchesBudzet;
      }),
    [alerts, filters],
  );

  const stats = useMemo(
    () =>
      ALERT_LEVELS.reduce(
        (acc, level) => ({
          ...acc,
          [level]: alerts.filter((alert) => alert.alertLevel === level).length,
        }),
        {},
      ),
    [alerts],
  );

  const refresh = async () => {
    setLoading(true);
    try {
      await loadAlerts();
      toast('Upozorenja su osvežena.', 'success');
    } catch (err) {
      toast(extractError(err, 'Greška pri osvežavanju upozorenja.'), 'error');
    } finally {
      setLoading(false);
    }
  };

  const updateFilter = (field, value) => {
    setFilters((prev) => ({
      ...prev,
      [field]: value,
      ...(field === 'dogadjajId' ? { budzetId: '' } : {}),
    }));
  };

  return (
    <div className="program-page">
      <div className="program-header">
        <div>
          <h1>Upozorenja</h1>
          <p className="page-subtitle">trenutna aktivna F3 budžetska upozorenja</p>
        </div>
        <button className="btn btn-outline" onClick={refresh} style={{ width: 'auto' }}>
          Osveži
        </button>
      </div>

      <div className="info-card" style={{ padding: '1.25rem', marginBottom: '1rem' }}>
        <p style={{ color: 'var(--text-secondary)', lineHeight: 1.6, margin: 0 }}>
          Ova stranica prikazuje trenutno stanje upozorenja. Ne čuva istoriju notifikacija, pa alert nestaje
          iz liste kada potrošnja padne ispod praga.
        </p>
      </div>

      <div className="info-cards">
        <div className="info-card">
          <div className="label">Ukupno aktivnih</div>
          <div className="value accent">{alerts.length}</div>
        </div>
        <div className="info-card">
          <div className="label">Upozorenja</div>
          <div className="value warning">{stats.WARNING || 0}</div>
        </div>
        <div className="info-card">
          <div className="label">Kritično</div>
          <div className="value warning">{stats.CRITICAL || 0}</div>
        </div>
        <div className="info-card">
          <div className="label">Prekoračeno</div>
          <div className="value danger">{stats.EXCEEDED || 0}</div>
        </div>
      </div>

      <div className="events-table-card">
        <div className="events-table-header">
          <h2>Filteri</h2>
          <button
            className="btn btn-outline btn-sm"
            onClick={() => setFilters({ level: '', dogadjajId: '', budzetId: '' })}
            style={{ width: 'auto' }}
          >
            Resetuj
          </button>
        </div>
        <div className="form-row">
          <div className="form-group">
            <label>Događaj</label>
            <select value={filters.dogadjajId} onChange={(e) => updateFilter('dogadjajId', e.target.value)}>
              <option value="">Svi događaji</option>
              {dogadjajOptions.map(([dogadjajId, dogadjajNaziv]) => (
                <option key={dogadjajId} value={dogadjajId}>
                  {dogadjajNaziv}
                </option>
              ))}
            </select>
          </div>
          <div className="form-group">
            <label>Budžet</label>
            <select value={filters.budzetId} onChange={(e) => updateFilter('budzetId', e.target.value)}>
              <option value="">Svi budžeti</option>
              {budzetOptions.map(([budzetId, nazivBudzeta]) => (
                <option key={budzetId} value={budzetId}>
                  {nazivBudzeta}
                </option>
              ))}
            </select>
          </div>
          <div className="form-group">
            <label>Nivo upozorenja</label>
            <select value={filters.level} onChange={(e) => updateFilter('level', e.target.value)}>
              <option value="">Svi nivoi</option>
              {ALERT_LEVELS.map((level) => (
                <option key={level} value={level}>
                  {LEVEL_LABEL[level]}
                </option>
              ))}
            </select>
          </div>
        </div>
      </div>

      <div className="events-table-card">
        <div className="events-table-header">
          <h2>Aktivna upozorenja</h2>
          <div className="card-hint">read-only pregled, bez upisa u alert memoriju</div>
        </div>
        <table className="events-table">
          <thead>
            <tr>
              <th>DOGAĐAJ</th>
              <th>BUDŽET</th>
              <th>KATEGORIJA</th>
              <th>NIVO</th>
              <th>ISKORIŠĆENOST</th>
              <th>PORUKA</th>
              <th>AKCIJE</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan={7} style={{ textAlign: 'center', padding: '2rem' }}>
                  Učitavanje...
                </td>
              </tr>
            ) : filteredAlerts.length === 0 ? (
              <tr>
                <td colSpan={7} style={{ textAlign: 'center', color: 'var(--text-muted)', padding: '2rem' }}>
                  Nema aktivnih upozorenja za izabrani filter.
                </td>
              </tr>
            ) : (
              filteredAlerts.map((alert) => (
                <tr key={`${alert.budzetId}:${alert.kategorijaId}`}>
                  <td>{alert.dogadjajNaziv || `#${alert.dogadjajId}`}</td>
                  <td>{alert.nazivBudzeta || `#${alert.budzetId}`}</td>
                  <td>{alert.kategorijaNaziv || `#${alert.kategorijaId}`}</td>
                  <td>
                    <AlertLevelBadge level={alert.alertLevel} />
                  </td>
                  <td>{formatPercent(alert.iskoriscenost)}</td>
                  <td>{alert.poruka || alert.alertPoruka || '—'}</td>
                  <td>
                    <button
                      className="btn btn-outline btn-xs"
                      onClick={() => navigate('/dashboard/budzet')}
                    >
                      Otvori budžete
                    </button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
