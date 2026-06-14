import { useEffect, useMemo, useState } from 'react';
import api from '@/shared/services/api';
import { useAuth } from '@/features/auth/context/AuthContext';
import { useToast } from '@/shared/components/ToastNotification';
import TrosakModal from '@/features/budzet/components/TrosakModal';
import * as budzetApi from '@/features/budzet/services/budzetService';

const TIP_LABEL = {
  RUCNI: 'Ručni',
  GOTOVINSKI: 'Gotovinski',
  AUTO_ULAZNA: 'Auto ulazna',
};

const TIP_CLASS = {
  RUCNI: 'status-published',
  GOTOVINSKI: 'status-ongoing',
  AUTO_ULAZNA: 'status-finished',
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

const toParams = (filters) =>
  Object.fromEntries(Object.entries(filters).filter(([, value]) => value));

export default function TroskoviPage() {
  const toast = useToast();
  const { hasRole } = useAuth();
  const canCreate =
    hasRole('FINANSIJSKI_KONTROLOR') ||
    hasRole('KOORDINATOR_RESURSA') ||
    hasRole('KOORDINATOR_PROGRAMA');

  const [dogadjaji, setDogadjaji] = useState([]);
  const [budzeti, setBudzeti] = useState([]);
  const [troskovi, setTroskovi] = useState([]);
  const [filters, setFilters] = useState({
    dogadjajId: '',
    budzetId: '',
    kategorijaId: '',
    tip: '',
  });
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [modalData, setModalData] = useState(null);

  const filteredBudzeti = useMemo(
    () =>
      filters.dogadjajId
        ? budzeti.filter((budzet) => String(budzet.dogadjajId) === String(filters.dogadjajId))
        : budzeti,
    [budzeti, filters.dogadjajId],
  );

  const selectedBudzet = useMemo(
    () => budzeti.find((budzet) => String(budzet.budzetId) === String(filters.budzetId)) || null,
    [budzeti, filters.budzetId],
  );

  const selectedStavka = useMemo(
    () =>
      selectedBudzet?.stavke?.find(
        (stavka) => String(stavka.kategorijaId) === String(filters.kategorijaId),
      ) || null,
    [selectedBudzet, filters.kategorijaId],
  );

  const stats = useMemo(() => {
    const bruto = troskovi.reduce((sum, trosak) => sum + Number(trosak.iznos || 0), 0);
    const refundirano = troskovi.reduce(
      (sum, trosak) => sum + Number(trosak.refundiraniIznos || 0),
      0,
    );
    const neto = troskovi.reduce((sum, trosak) => sum + Number(trosak.netoIznos ?? 0), 0);
    return { bruto, refundirano, neto };
  }, [troskovi]);

  const loadTroskovi = async (nextFilters = filters) => {
    const res = await budzetApi.getTroskovi(toParams(nextFilters));
    setTroskovi(res.data || []);
  };

  useEffect(() => {
    Promise.all([api.get('/dogadjaj'), budzetApi.getBudzeti(), budzetApi.getTroskovi()])
      .then(([dogadjajiRes, budzetiRes, troskoviRes]) => {
        setDogadjaji(dogadjajiRes.data || []);
        setBudzeti(budzetiRes.data || []);
        setTroskovi(troskoviRes.data || []);
      })
      .catch((err) => toast(extractError(err, 'Greška pri učitavanju troškova.'), 'error'))
      .finally(() => setLoading(false));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const updateFilter = async (field, value) => {
    const next = {
      ...filters,
      [field]: value,
      ...(field === 'dogadjajId' ? { budzetId: '', kategorijaId: '' } : {}),
      ...(field === 'budzetId' ? { kategorijaId: '' } : {}),
    };
    setFilters(next);
    try {
      await loadTroskovi(next);
    } catch (err) {
      toast(extractError(err, 'Greška pri filtriranju troškova.'), 'error');
    }
  };

  const openCreateModal = () => {
    if (!selectedBudzet || !selectedStavka) {
      toast('Izaberi budžet i kategoriju pre unosa troška.', 'error');
      return;
    }
    if (selectedBudzet.status !== 'ACTIVE') {
      toast('Ručni trošak se može evidentirati samo za ACTIVE budžet.', 'error');
      return;
    }
    setModalData({ budzet: selectedBudzet, stavka: selectedStavka });
  };

  const handleCreateTrosak = async (payload) => {
    setBusy(true);
    try {
      await budzetApi.createTrosak(payload);
      toast('Trošak je evidentiran.', 'success');
      setModalData(null);
      await loadTroskovi(filters);
    } catch (err) {
      toast(extractError(err, 'Greška pri unosu troška.'), 'error');
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="program-page">
      {modalData && (
        <TrosakModal
          budzet={modalData.budzet}
          stavka={modalData.stavka}
          onClose={() => setModalData(null)}
          onSubmit={handleCreateTrosak}
          loading={busy}
        />
      )}

      <div className="program-header">
        <div>
          <h1>Troškovi</h1>
          <p className="page-subtitle">pregled ručnih, gotovinskih i auto troškova</p>
        </div>
        {canCreate && (
          <button className="btn btn-primary" onClick={openCreateModal} style={{ width: 'auto' }}>
            Dodaj trošak
          </button>
        )}
      </div>

      <div className="info-cards">
        <div className="info-card">
          <div className="label">Broj troškova</div>
          <div className="value accent">{troskovi.length}</div>
        </div>
        <div className="info-card">
          <div className="label">Bruto iznos</div>
          <div className="value">{formatMoney(stats.bruto)}</div>
        </div>
        <div className="info-card">
          <div className="label">Refundirano</div>
          <div className="value warning">{formatMoney(stats.refundirano)}</div>
        </div>
        <div className="info-card">
          <div className="label">Neto trošak</div>
          <div className="value success">{formatMoney(stats.neto)}</div>
        </div>
      </div>

      <div className="events-table-card">
        <div className="events-table-header">
          <h2>Filteri</h2>
          <button
            className="btn btn-outline btn-sm"
            onClick={() => {
              const reset = { dogadjajId: '', budzetId: '', kategorijaId: '', tip: '' };
              setFilters(reset);
              loadTroskovi(reset).catch((err) =>
                toast(extractError(err, 'Greška pri resetovanju filtera.'), 'error'),
              );
            }}
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
              {dogadjaji.map((dogadjaj) => (
                <option key={dogadjaj.dogadjajId} value={dogadjaj.dogadjajId}>
                  {dogadjaj.naziv}
                </option>
              ))}
            </select>
          </div>
          <div className="form-group">
            <label>Budžet</label>
            <select value={filters.budzetId} onChange={(e) => updateFilter('budzetId', e.target.value)}>
              <option value="">Svi budžeti</option>
              {filteredBudzeti.map((budzet) => (
                <option key={budzet.budzetId} value={budzet.budzetId}>
                  {budzet.nazivBudzeta}
                </option>
              ))}
            </select>
          </div>
          <div className="form-group">
            <label>Kategorija</label>
            <select
              value={filters.kategorijaId}
              onChange={(e) => updateFilter('kategorijaId', e.target.value)}
              disabled={!selectedBudzet}
            >
              <option value="">Sve kategorije</option>
              {(selectedBudzet?.stavke || []).map((stavka) => (
                <option key={stavka.kategorijaId} value={stavka.kategorijaId}>
                  {stavka.kategorijaNaziv}
                </option>
              ))}
            </select>
          </div>
          <div className="form-group">
            <label>Tip</label>
            <select value={filters.tip} onChange={(e) => updateFilter('tip', e.target.value)}>
              <option value="">Svi tipovi</option>
              {Object.entries(TIP_LABEL).map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </select>
          </div>
        </div>
      </div>

      <div className="events-table-card">
        <div className="events-table-header">
          <h2>Lista troškova</h2>
          <div className="card-hint">AUTO_ULAZNA nastaje tek pri izdavanju ulazne fakture</div>
        </div>
        <table className="events-table">
          <thead>
            <tr>
              <th>OPIS</th>
              <th>TIP</th>
              <th>IZNOS</th>
              <th>REFUNDIRANO</th>
              <th>NETO</th>
              <th>DATUM</th>
              <th>BUDŽET</th>
              <th>KATEGORIJA</th>
              <th>FAKTURA/STAVKA</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan={9} style={{ textAlign: 'center', padding: '2rem' }}>
                  Učitavanje...
                </td>
              </tr>
            ) : troskovi.length === 0 ? (
              <tr>
                <td colSpan={9} style={{ textAlign: 'center', color: 'var(--text-muted)', padding: '2rem' }}>
                  Nema troškova za izabrane filtere.
                </td>
              </tr>
            ) : (
              troskovi.map((trosak) => {
                const budzet = budzeti.find((b) => Number(b.budzetId) === Number(trosak.budzetId));
                const stavka = budzet?.stavke?.find((s) => Number(s.kategorijaId) === Number(trosak.kategorijaId));
                return (
                  <tr key={trosak.trosakId}>
                    <td>
                      <strong>{trosak.opis}</strong>
                    </td>
                    <td>
                      <span className={`status-badge ${TIP_CLASS[trosak.tip] || 'status-draft'}`}>
                        {TIP_LABEL[trosak.tip] || trosak.tip}
                      </span>
                    </td>
                    <td>{formatMoney(trosak.iznos)}</td>
                    <td>{formatMoney(trosak.refundiraniIznos)}</td>
                    <td>{formatMoney(trosak.netoIznos)}</td>
                    <td>{formatDate(trosak.datumTroska || trosak.kreiranAt)}</td>
                    <td>{budzet?.nazivBudzeta || trosak.budzetId || '—'}</td>
                    <td>{stavka?.kategorijaNaziv || trosak.kategorijaId || '—'}</td>
                    <td>
                      {trosak.fakturaId ? `F#${trosak.fakturaId}` : '—'}
                      {trosak.stavkaFaktureId ? ` / S#${trosak.stavkaFaktureId}` : ''}
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
