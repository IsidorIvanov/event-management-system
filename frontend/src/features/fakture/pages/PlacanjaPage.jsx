import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '@/features/auth/context/AuthContext';
import { useToast } from '@/shared/components/ToastNotification';
import * as fakturaApi from '@/features/fakture/services/fakturaService';

const STATUS_LABEL = {
  PENDING: 'Na čekanju',
  COMPLETED: 'Potvrđeno',
  FAILED: 'Odbijeno',
};

const STATUS_CLASS = {
  PENDING: 'status-draft',
  COMPLETED: 'status-ongoing',
  FAILED: 'status-finished',
};

const TIP_LABEL = {
  ULAZNA: 'Ulazna',
  IZLAZNA: 'Izlazna',
};

const formatMoney = (value) =>
  new Intl.NumberFormat('sr-Latn-RS', {
    style: 'currency',
    currency: 'RSD',
    maximumFractionDigits: 2,
  }).format(Number(value || 0));

const formatDate = (value) =>
  value
    ? new Date(value).toLocaleDateString('sr-Latn', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
      })
    : '—';

const extractError = (error, fallback) =>
  error?.response?.data?.message ||
  error?.response?.data?.error ||
  error?.message ||
  fallback;

const flattenPlacanja = (fakture) =>
  (fakture || []).flatMap((faktura) =>
    (faktura.placanja || []).map((placanje) => ({
      ...placanje,
      faktura,
    })),
  );

const sumIzvrseneRefundacije = (placanje) =>
  (placanje.refundacije || [])
    .filter((refundacija) => refundacija.status === 'IZVRSENA')
    .reduce((sum, refundacija) => sum + Number(refundacija.iznos || 0), 0);

export default function PlacanjaPage() {
  const toast = useToast();
  const navigate = useNavigate();
  const { hasRole } = useAuth();
  const canManage = hasRole('FINANSIJSKI_KONTROLOR');

  const [fakture, setFakture] = useState([]);
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState(null);
  const [filters, setFilters] = useState({
    status: '',
    tip: '',
    search: '',
  });

  const loadFakture = async () => {
    const res = await fakturaApi.getAllFakture();
    setFakture(res.data || []);
  };

  useEffect(() => {
    loadFakture()
      .catch((err) => toast(extractError(err, 'Greška pri učitavanju plaćanja.'), 'error'))
      .finally(() => setLoading(false));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const placanja = useMemo(() => flattenPlacanja(fakture), [fakture]);

  const filteredPlacanja = useMemo(
    () =>
      placanja.filter((placanje) => {
        const matchesStatus = !filters.status || placanje.status === filters.status;
        const matchesTip = !filters.tip || placanje.faktura?.tip === filters.tip;
        const q = filters.search.trim().toLowerCase();
        const matchesSearch =
          !q ||
          [
            placanje.referentniBroj,
            placanje.faktura?.brojFakture,
            placanje.napomena,
            String(placanje.fakturaId || ''),
          ].some((value) => String(value || '').toLowerCase().includes(q));
        return matchesStatus && matchesTip && matchesSearch;
      }),
    [placanja, filters],
  );

  const stats = useMemo(
    () => {
      const bruto = filteredPlacanja.reduce((sum, placanje) => sum + Number(placanje.iznos || 0), 0);
      const refundirano = filteredPlacanja.reduce(
        (sum, placanje) => sum + sumIzvrseneRefundacije(placanje),
        0,
      );
      return {
        ukupno: filteredPlacanja.length,
        pending: filteredPlacanja.filter((placanje) => placanje.status === 'PENDING').length,
        completed: filteredPlacanja.filter((placanje) => placanje.status === 'COMPLETED').length,
        bruto,
        refundirano,
        neto: bruto - refundirano,
      };
    },
    [filteredPlacanja],
  );

  const updateFilter = (field, value) => {
    setFilters((prev) => ({ ...prev, [field]: value }));
  };

  const handleAction = async (placanje, action) => {
    setBusyId(placanje.placanjeId);
    try {
      if (action === 'confirm') {
        await fakturaApi.confirmPlacanje(placanje.placanjeId);
        toast('Plaćanje je potvrđeno.', 'success');
      } else {
        await fakturaApi.failPlacanje(placanje.placanjeId);
        toast('Plaćanje je odbijeno.', 'success');
      }
      await loadFakture();
    } catch (err) {
      toast(extractError(err, 'Greška pri obradi plaćanja.'), 'error');
    } finally {
      setBusyId(null);
    }
  };

  return (
    <div className="program-page">
      <div className="program-header">
        <div>
          <h1>Plaćanja</h1>
          <p className="page-subtitle">centralni pregled plaćanja iz svih faktura</p>
        </div>
        <button className="btn btn-outline" onClick={() => navigate('/dashboard/fakture')} style={{ width: 'auto' }}>
          Otvori fakture
        </button>
      </div>

      <div className="info-cards">
        <div className="info-card">
          <div className="label">Ukupno plaćanja</div>
          <div className="value accent">{stats.ukupno}</div>
        </div>
        <div className="info-card">
          <div className="label">Na čekanju</div>
          <div className="value warning">{stats.pending}</div>
        </div>
        <div className="info-card">
          <div className="label">Potvrđena</div>
          <div className="value success">{stats.completed}</div>
        </div>
        <div className="info-card">
          <div className="label">Bruto plaćeno</div>
          <div className="value">{formatMoney(stats.bruto)}</div>
        </div>
        <div className="info-card">
          <div className="label">Izvršeno refundirano</div>
          <div className="value warning">{formatMoney(stats.refundirano)}</div>
        </div>
        <div className="info-card">
          <div className="label">Neto efekat</div>
          <div className="value success">{formatMoney(stats.neto)}</div>
        </div>
      </div>

      <div className="events-table-card">
        <div className="events-table-header">
          <h2>Filteri</h2>
          <button
            className="btn btn-outline btn-sm"
            onClick={() => setFilters({ status: '', tip: '', search: '' })}
            style={{ width: 'auto' }}
          >
            Resetuj
          </button>
        </div>
        <div className="form-row">
          <div className="form-group">
            <label>Status</label>
            <select value={filters.status} onChange={(e) => updateFilter('status', e.target.value)}>
              <option value="">Svi statusi</option>
              {Object.entries(STATUS_LABEL).map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </select>
          </div>
          <div className="form-group">
            <label>Tip fakture</label>
            <select value={filters.tip} onChange={(e) => updateFilter('tip', e.target.value)}>
              <option value="">Svi tipovi</option>
              {Object.entries(TIP_LABEL).map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </select>
          </div>
          <div className="form-group">
            <label>Pretraga</label>
            <input
              value={filters.search}
              onChange={(e) => updateFilter('search', e.target.value)}
              placeholder="Broj fakture, referenca ili napomena"
            />
          </div>
        </div>
      </div>

      <div className="events-table-card">
        <div className="events-table-header">
          <h2>Lista plaćanja</h2>
          <div className="card-hint">Plaćanja ne menjaju F3 trošak, već samo plaćeni iznos fakture</div>
        </div>
        <table className="events-table">
          <thead>
            <tr>
              <th>FAKTURA</th>
              <th>TIP</th>
              <th>STATUS</th>
              <th>BRUTO</th>
              <th>REFUNDIRANO</th>
              <th>NETO</th>
              <th>METOD</th>
              <th>REFERENCA</th>
              <th>DATUM</th>
              <th>AKCIJE</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan={10} style={{ textAlign: 'center', padding: '2rem' }}>
                  Učitavanje...
                </td>
              </tr>
            ) : filteredPlacanja.length === 0 ? (
              <tr>
                <td colSpan={10} style={{ textAlign: 'center', color: 'var(--text-muted)', padding: '2rem' }}>
                  Nema plaćanja za izabrane filtere.
                </td>
              </tr>
            ) : (
              filteredPlacanja.map((placanje) => {
                const refundirano = sumIzvrseneRefundacije(placanje);
                const neto = Number(placanje.iznos || 0) - refundirano;
                return (
                  <tr key={placanje.placanjeId}>
                    <td>
                      <strong>{placanje.faktura?.brojFakture || `#${placanje.fakturaId}`}</strong>
                      <div className="card-hint">
                        Neto fakture {formatMoney(placanje.faktura?.placeniIznos)} / {formatMoney(placanje.faktura?.ukupnaIznos)}
                      </div>
                    </td>
                    <td>{TIP_LABEL[placanje.faktura?.tip] || placanje.faktura?.tip || '—'}</td>
                    <td>
                      <span className={`status-badge ${STATUS_CLASS[placanje.status] || 'status-draft'}`}>
                        {STATUS_LABEL[placanje.status] || placanje.status}
                      </span>
                    </td>
                    <td>{formatMoney(placanje.iznos)}</td>
                    <td>{formatMoney(refundirano)}</td>
                    <td>{formatMoney(neto)}</td>
                    <td>{placanje.metod || placanje.metodPlacanja || '—'}</td>
                    <td>{placanje.referentniBroj || '—'}</td>
                    <td>{formatDate(placanje.datumPlacanja || placanje.kreiranoAt)}</td>
                    <td>
                      {canManage && placanje.status === 'PENDING' ? (
                        <div className="action-buttons">
                          <button
                            className="btn btn-outline btn-xs"
                            disabled={busyId === placanje.placanjeId}
                            onClick={() => handleAction(placanje, 'confirm')}
                          >
                            Potvrdi
                          </button>
                          <button
                            className="btn btn-xs btn-danger-outline"
                            disabled={busyId === placanje.placanjeId}
                            onClick={() => handleAction(placanje, 'fail')}
                          >
                            Odbij
                          </button>
                        </div>
                      ) : (
                        '—'
                      )}
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
