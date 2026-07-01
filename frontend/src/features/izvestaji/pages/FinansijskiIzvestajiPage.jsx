import { useCallback, useEffect, useMemo, useState } from 'react';
import api from '@/shared/services/api';
import { useAuth } from '@/features/auth/context/AuthContext';
import { useToast } from '@/shared/components/ToastNotification';
import { formatDate, formatDateTime } from '@/shared/utils/format';
import * as izvestajiApi from '@/features/izvestaji/services/izvestajiService';
import * as nabavkaApi from '@/features/fakture/services/nabavkaService';

const TIP_LABEL = {
  PO_DOGADJAJU: 'Po događaju',
  RESURSI_I_TROSKOVI_PO_DOGADJAJU: 'Resursi i troškovi po događaju',
  PO_KLIJENTU: 'Po klijentu',
  PO_DOBAVLJACU: 'Po dobavljaču',
  PO_PERIODU: 'Po periodu',
};

const FORMAT_LABEL = {
  PDF: 'PDF',
  EXCEL: 'Excel',
};

const STATUS_LABEL = {
  PENDING: 'Na čekanju',
  GENERATING: 'Generisanje',
  READY: 'Spreman',
  FAILED: 'Neuspešno',
};

const STATUS_STYLE = {
  PENDING: { backgroundColor: '#6b7280', color: '#fff' },
  GENERATING: { backgroundColor: '#f59e0b', color: '#111827' },
  READY: { backgroundColor: '#16a34a', color: '#fff' },
  FAILED: { backgroundColor: '#dc2626', color: '#fff' },
};

const extractError = (error, fallback = 'Došlo je do greške.') => {
  const data = error?.response?.data;
  if (typeof data === 'string') return data;
  if (data?.error) return data.error;
  if (data?.message) return data.message;
  return error?.message || fallback;
};

function Badge({ value, labelMap, styleMap }) {
  const label = labelMap[value] || value;
  const style = styleMap[value] || {};
  return (
    <span
      style={{
        display: 'inline-block',
        padding: '0.2rem 0.55rem',
        borderRadius: '999px',
        fontSize: '0.78rem',
        fontWeight: 600,
        ...style,
      }}
    >
      {label}
    </span>
  );
}

function formatParametri(izvestaj) {
  const parts = [];
  if (izvestaj.dogadjajId) parts.push(`Događaj #${izvestaj.dogadjajId}`);
  if (izvestaj.klijentId) parts.push(`Klijent #${izvestaj.klijentId}`);
  if (izvestaj.dobavljacId) parts.push(`Dobavljač #${izvestaj.dobavljacId}`);
  if (izvestaj.periodOd && izvestaj.periodDo) {
    parts.push(`${formatDate(izvestaj.periodOd)} – ${formatDate(izvestaj.periodDo)}`);
  }
  return parts.join(' · ') || '—';
}

export default function FinansijskiIzvestajiPage() {
  const { hasRole } = useAuth();
  const toast = useToast();
  const isFinKontrolor = hasRole('FINANSIJSKI_KONTROLOR');

  const [tip, setTip] = useState('PO_PERIODU');
  const [format, setFormat] = useState('PDF');
  const [dogadjajId, setDogadjajId] = useState('');
  const [klijentId, setKlijentId] = useState('');
  const [dobavljacId, setDobavljacId] = useState('');
  const [periodOd, setPeriodOd] = useState('');
  const [periodDo, setPeriodDo] = useState('');

  const [dogadjaji, setDogadjaji] = useState([]);
  const [klijenti, setKlijenti] = useState([]);
  const [dobavljaci, setDobavljaci] = useState([]);
  const [izvestaji, setIzvestaji] = useState([]);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);

  const loadLookups = useCallback(async () => {
    const requests = [];
    if (tip === 'PO_DOGADJAJU' || tip === 'RESURSI_I_TROSKOVI_PO_DOGADJAJU') {
      requests.push(api.get('/dogadjaj').then((r) => setDogadjaji(r.data || [])));
    }
    if (tip === 'PO_KLIJENTU') {
      requests.push(api.get('/klijenti').then((r) => setKlijenti(r.data || [])));
    }
    if (tip === 'PO_DOBAVLJACU') {
      requests.push(nabavkaApi.getDobavljaci().then((r) => setDobavljaci(r.data || [])));
    }
    await Promise.all(requests);
  }, [tip]);

  const loadIzvestaji = useCallback(async () => {
    setLoading(true);
    try {
      const res = await izvestajiApi.list();
      setIzvestaji(res.data || []);
    } catch (error) {
      toast(extractError(error, 'Greška pri učitavanju izveštaja.'), 'error');
    } finally {
      setLoading(false);
    }
  }, [toast]);

  useEffect(() => {
    loadIzvestaji();
  }, [loadIzvestaji]);

  useEffect(() => {
    loadLookups().catch(() => toast('Greška pri učitavanju podataka za formu.', 'error'));
  }, [loadLookups, toast]);

  const needsPeriod = tip !== 'PO_DOGADJAJU' && tip !== 'RESURSI_I_TROSKOVI_PO_DOGADJAJU';

  const buildPayload = () => {
    const payload = { tip, format };
    if (tip === 'PO_DOGADJAJU' || tip === 'RESURSI_I_TROSKOVI_PO_DOGADJAJU') {
      payload.dogadjajId = Number(dogadjajId);
      if (tip === 'PO_DOGADJAJU' && periodOd && periodDo) {
        payload.periodOd = periodOd;
        payload.periodDo = periodDo;
      }
    } else if (tip === 'PO_KLIJENTU') {
      payload.klijentId = Number(klijentId);
      payload.periodOd = periodOd;
      payload.periodDo = periodDo;
    } else if (tip === 'PO_DOBAVLJACU') {
      payload.dobavljacId = Number(dobavljacId);
      payload.periodOd = periodOd;
      payload.periodDo = periodDo;
    } else {
      payload.periodOd = periodOd;
      payload.periodDo = periodDo;
    }
    return payload;
  };

  const handleGenerate = async () => {
    if (!isFinKontrolor) return;
    setBusy(true);
    try {
      const res = await izvestajiApi.generate(buildPayload());
      if (res.data?.status === 'FAILED') {
        toast(res.data.greskaPoruka || 'Generisanje nije uspelo.', 'error');
      } else {
        toast('Izveštaj je uspešno generisan.', 'success');
      }
      await loadIzvestaji();
    } catch (error) {
      toast(extractError(error), 'error');
    } finally {
      setBusy(false);
    }
  };

  const handleDownload = async (izvestaj) => {
    const ext = izvestaj.format === 'EXCEL' ? 'xlsx' : 'pdf';
    try {
      await izvestajiApi.download(izvestaj.izvestajId, `izvestaj-${izvestaj.izvestajId}.${ext}`);
    } catch (error) {
      toast(extractError(error, 'Preuzimanje nije uspelo.'), 'error');
    }
  };

  const stats = useMemo(() => ({
    total: izvestaji.length,
    ready: izvestaji.filter((i) => i.status === 'READY').length,
    failed: izvestaji.filter((i) => i.status === 'FAILED').length,
  }), [izvestaji]);

  return (
    <>
      <div className="program-header">
        <div>
          <h1>Finansijski izveštaji</h1>
          <p className="page-subtitle">
            Generišite PDF ili Excel izveštaje po događaju, klijentu, dobavljaču ili periodu.
          </p>
        </div>
      </div>

      {isFinKontrolor && (
        <div className="events-table-card" style={{ marginBottom: '1.5rem' }}>
          <div className="events-table-header">
            <h2>Novi izveštaj</h2>
          </div>
          <div style={{ padding: '1rem', display: 'grid', gap: '1rem', maxWidth: 640 }}>
            <label>
              Tip izveštaja
              <select
                className="status-select"
                style={{ display: 'block', width: '100%', marginTop: '0.35rem' }}
                value={tip}
                onChange={(e) => setTip(e.target.value)}
              >
                {Object.entries(TIP_LABEL).map(([value, label]) => (
                  <option key={value} value={value}>{label}</option>
                ))}
              </select>
            </label>

            {(tip === 'PO_DOGADJAJU' || tip === 'RESURSI_I_TROSKOVI_PO_DOGADJAJU') && (
              <label>
                Događaj
                <select
                  className="status-select"
                  style={{ display: 'block', width: '100%', marginTop: '0.35rem' }}
                  value={dogadjajId}
                  onChange={(e) => setDogadjajId(e.target.value)}
                >
                  <option value="">— izaberite —</option>
                  {dogadjaji.map((d) => (
                    <option key={d.dogadjajId} value={d.dogadjajId}>
                      {d.naziv} · {formatDate(d.datumPocetka)}
                    </option>
                  ))}
                </select>
              </label>
            )}

            {tip === 'PO_KLIJENTU' && (
              <label>
                Klijent
                <select
                  className="status-select"
                  style={{ display: 'block', width: '100%', marginTop: '0.35rem' }}
                  value={klijentId}
                  onChange={(e) => setKlijentId(e.target.value)}
                >
                  <option value="">— izaberite —</option>
                  {klijenti.map((k) => (
                    <option key={k.klijentId || k.korisnikId} value={k.klijentId || k.korisnikId}>
                      {k.nazivFirme || `${k.ime} ${k.prezime}`}
                    </option>
                  ))}
                </select>
              </label>
            )}

            {tip === 'PO_DOBAVLJACU' && (
              <label>
                Dobavljač
                <select
                  className="status-select"
                  style={{ display: 'block', width: '100%', marginTop: '0.35rem' }}
                  value={dobavljacId}
                  onChange={(e) => setDobavljacId(e.target.value)}
                >
                  <option value="">— izaberite —</option>
                  {dobavljaci.map((d) => (
                    <option key={d.dobavljacId} value={d.dobavljacId}>{d.naziv}</option>
                  ))}
                </select>
              </label>
            )}

            {needsPeriod && (
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
                <label>
                  Period od
                  <input
                    type="date"
                    className="status-select"
                    style={{ display: 'block', width: '100%', marginTop: '0.35rem' }}
                    value={periodOd}
                    onChange={(e) => setPeriodOd(e.target.value)}
                  />
                </label>
                <label>
                  Period do
                  <input
                    type="date"
                    className="status-select"
                    style={{ display: 'block', width: '100%', marginTop: '0.35rem' }}
                    value={periodDo}
                    onChange={(e) => setPeriodDo(e.target.value)}
                  />
                </label>
              </div>
            )}

            {tip === 'PO_DOGADJAJU' && (
              <p className="page-subtitle" style={{ margin: 0 }}>
                Period je opcion za izveštaj po događaju.
              </p>
            )}

            <fieldset style={{ border: 'none', padding: 0 }}>
              <legend style={{ fontWeight: 600, marginBottom: '0.5rem' }}>Format</legend>
              {Object.entries(FORMAT_LABEL).map(([value, label]) => (
                <label key={value} style={{ marginRight: '1.25rem' }}>
                  <input
                    type="radio"
                    name="format"
                    value={value}
                    checked={format === value}
                    onChange={(e) => setFormat(e.target.value)}
                  />
                  {' '}{label}
                </label>
              ))}
            </fieldset>

            <button
              type="button"
              className="btn btn-primary"
              onClick={handleGenerate}
              disabled={busy}
              style={{ width: 'fit-content' }}
            >
              {busy ? 'Generisanje...' : 'Generiši'}
            </button>
          </div>
        </div>
      )}

      <div className="info-cards" style={{ marginBottom: '1.5rem' }}>
        <div className="info-card">
          <div className="label">Ukupno</div>
          <div className="value accent">{stats.total}</div>
        </div>
        <div className="info-card">
          <div className="label">Spremni</div>
          <div className="value success">{stats.ready}</div>
        </div>
        <div className="info-card">
          <div className="label">Neuspešni</div>
          <div className="value warning">{stats.failed}</div>
        </div>
      </div>

      <div className="events-table-card">
        <div className="events-table-header">
          <h2>Istorija izveštaja</h2>
          <span className="page-subtitle">{izvestaji.length} zapisa</span>
        </div>
        {loading ? (
          <p style={{ padding: '1rem' }}>Učitavanje...</p>
        ) : izvestaji.length === 0 ? (
          <p style={{ color: 'var(--text-muted)', padding: '1rem' }}>Nema generisanih izveštaja.</p>
        ) : (
          <div className="events-table-scroll">
            <table className="events-table">
              <thead>
                <tr>
                  <th>Tip</th>
                  <th>Format</th>
                  <th>Parametri</th>
                  <th>Status</th>
                  <th>Kreiran</th>
                  <th>Generisan</th>
                  <th>Akcije</th>
                </tr>
              </thead>
              <tbody>
                {izvestaji.map((izvestaj) => (
                  <tr key={izvestaj.izvestajId}>
                    <td>{TIP_LABEL[izvestaj.tip] || izvestaj.tip}</td>
                    <td>{FORMAT_LABEL[izvestaj.format] || izvestaj.format}</td>
                    <td>{formatParametri(izvestaj)}</td>
                    <td>
                      <Badge value={izvestaj.status} labelMap={STATUS_LABEL} styleMap={STATUS_STYLE} />
                      {izvestaj.status === 'FAILED' && izvestaj.greskaPoruka && (
                        <div className="card-hint">{izvestaj.greskaPoruka}</div>
                      )}
                    </td>
                    <td>{formatDateTime(izvestaj.kreiranAt)}</td>
                    <td>{izvestaj.generisanoAt ? formatDateTime(izvestaj.generisanoAt) : '—'}</td>
                    <td>
                      <button
                        type="button"
                        className="btn btn-outline btn-sm"
                        disabled={izvestaj.status !== 'READY'}
                        onClick={() => handleDownload(izvestaj)}
                      >
                        Preuzmi
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </>
  );
}
