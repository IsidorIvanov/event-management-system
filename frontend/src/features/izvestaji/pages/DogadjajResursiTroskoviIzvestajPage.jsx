import { useCallback, useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import api from '@/shared/services/api';
import { useAuth } from '@/features/auth/context/AuthContext';
import { useToast } from '@/shared/components/ToastNotification';
import { formatDate, formatDateTime, formatMoney } from '@/shared/utils/format';
import * as izvestajiApi from '@/features/izvestaji/services/izvestajiService';

const REZULTAT_DISPLAY = {
  PROFITABILAN: 'Profitabilan',
  BREAK_EVEN: 'Break-even',
  GUBITAK: 'Gubitak',
};

const REZULTAT_STYLE = {
  PROFITABILAN: { backgroundColor: '#16a34a', color: '#fff' },
  BREAK_EVEN: { backgroundColor: '#f59e0b', color: '#111827' },
  GUBITAK: { backgroundColor: '#dc2626', color: '#fff' },
};

const extractError = (error, fallback = 'Došlo je do greške.') => {
  const data = error?.response?.data;
  if (typeof data === 'string') return data;
  if (data?.error) return data.error;
  if (data?.message) return data.message;
  return error?.message || fallback;
};

const formatPercent = (value) => (value == null ? '—' : `${value}%`);

function Badge({ value, labelMap, styleMap }) {
  if (!value) return '—';
  return (
    <span
      style={{
        display: 'inline-block',
        padding: '0.2rem 0.55rem',
        borderRadius: '999px',
        fontSize: '0.78rem',
        fontWeight: 600,
        ...(styleMap[value] || { backgroundColor: '#6b7280', color: '#fff' }),
      }}
    >
      {labelMap[value] || value}
    </span>
  );
}

export default function DogadjajResursiTroskoviIzvestajPage() {
  const { hasRole } = useAuth();
  const toast = useToast();
  const isFinKontrolor = hasRole('FINANSIJSKI_KONTROLOR');
  const [searchParams] = useSearchParams();

  const [dogadjaji, setDogadjaji] = useState([]);
  const [selectedId, setSelectedId] = useState(searchParams.get('dogadjajId') || '');
  const [izvestaj, setIzvestaj] = useState(null);
  const [loading, setLoading] = useState(false);
  const [exportBusy, setExportBusy] = useState(false);

  useEffect(() => {
    api.get('/dogadjaj')
      .then((res) => setDogadjaji(res.data || []))
      .catch(() => toast('Greška pri učitavanju događaja.', 'error'));
  }, [toast]);

  const load = useCallback(async (dogadjajId) => {
    if (!dogadjajId) return;
    setLoading(true);
    try {
      const res = await izvestajiApi.getDogadjajIzvestaj(dogadjajId);
      setIzvestaj(res.data);
    } catch (error) {
      toast(extractError(error), 'error');
      setIzvestaj(null);
    } finally {
      setLoading(false);
    }
  }, [toast]);

  useEffect(() => {
    if (selectedId) load(selectedId);
    else setIzvestaj(null);
  }, [selectedId, load]);

  const handleExport = async (format) => {
    if (!isFinKontrolor || !selectedId) return;
    setExportBusy(true);
    try {
      const res = await izvestajiApi.generate({
        tip: 'RESURSI_I_TROSKOVI_PO_DOGADJAJU',
        format,
        dogadjajId: Number(selectedId),
      });
      if (res.data?.status === 'READY' && res.data?.izvestajId) {
        const ext = format === 'EXCEL' ? 'xlsx' : 'pdf';
        await izvestajiApi.download(res.data.izvestajId, `izvestaj-resursi-${selectedId}.${ext}`);
        toast('Izveštaj je preuzet.', 'success');
      } else if (res.data?.status === 'FAILED') {
        toast(res.data.greskaPoruka || 'Generisanje nije uspelo.', 'error');
      } else {
        toast('Izveštaj je generisan.', 'success');
      }
    } catch (error) {
      toast(extractError(error), 'error');
    } finally {
      setExportBusy(false);
    }
  };

  const rezime = izvestaj?.rezime;
  const resursi = izvestaj?.resursi;
  const troskovi = izvestaj?.troskovi;

  return (
    <div className="program-page">
      <div className="program-header">
        <div>
          <h1>Izveštaj resursa i troškova</h1>
          <p className="page-subtitle">
            Iskorišćenost sala, pokrivenost opreme, nabavke i finansijski pregled po događaju
          </p>
        </div>
        <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
          <Link to="/dashboard/sredstva-dogadjaja" className="btn btn-outline btn-sm">
            Sredstva događaja
          </Link>
          <Link to="/dashboard/izvestaji" className="btn btn-outline btn-sm">
            Finansijski izveštaji
          </Link>
        </div>
      </div>

      <div className="events-table-card" style={{ marginBottom: '1.5rem', padding: '1rem' }}>
        <label className="form-label">Događaj</label>
        <select
          className="form-control"
          value={selectedId}
          onChange={(e) => setSelectedId(e.target.value)}
        >
          <option value="">Izaberite događaj</option>
          {dogadjaji.map((d) => (
            <option key={d.dogadjajId} value={d.dogadjajId}>
              {d.naziv} · {formatDate(d.datumPocetka)}
            </option>
          ))}
        </select>
      </div>

      {loading && <p style={{ padding: '1rem' }}>Učitavanje...</p>}

      {izvestaj && !loading && (
        <>
          <div className="info-cards" style={{ marginBottom: '1.5rem' }}>
            <div className="info-card">
              <div className="label">Iskorišćenost sala</div>
              <div className="value accent">{formatPercent(rezime?.prosecnaIskoriscenostSala)}</div>
            </div>
            <div className="info-card">
              <div className="label">Pokrivenost inventarom</div>
              <div className="value success">{formatPercent(rezime?.pokrivenostInventarProcenat)}</div>
            </div>
            <div className="info-card">
              <div className="label">Neto</div>
              <div className="value warning">{formatMoney(rezime?.neto)}</div>
            </div>
            <div className="info-card">
              <div className="label">Rezultat</div>
              <div className="value">
                <Badge
                  value={rezime?.rezultatOcene}
                  labelMap={REZULTAT_DISPLAY}
                  styleMap={REZULTAT_STYLE}
                />
              </div>
            </div>
            <div className="info-card">
              <div className="label">Upozorenja</div>
              <div className="value" style={{ color: rezime?.brojUpozorenja > 0 ? '#dc2626' : undefined }}>
                {rezime?.brojUpozorenja ?? 0}
              </div>
            </div>
          </div>

          <div className="events-table-card" style={{ marginBottom: '1.5rem', padding: '1rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '0.75rem' }}>
              <div>
                <h2 style={{ margin: 0 }}>{izvestaj.dogadjajNaziv}</h2>
                <p className="page-subtitle" style={{ margin: '0.25rem 0 0' }}>
                  Generisano: {formatDateTime(izvestaj.generisanoAt)}
                  {troskovi?.izvorOznaka && ` · ${troskovi.izvorOznaka}`}
                </p>
              </div>
              {isFinKontrolor && (
                <div style={{ display: 'flex', gap: '0.5rem' }}>
                  <button
                    type="button"
                    className="btn btn-outline btn-sm"
                    disabled={exportBusy}
                    onClick={() => handleExport('PDF')}
                  >
                    PDF
                  </button>
                  <button
                    type="button"
                    className="btn btn-outline btn-sm"
                    disabled={exportBusy}
                    onClick={() => handleExport('EXCEL')}
                  >
                    Excel
                  </button>
                </div>
              )}
            </div>
          </div>

          {resursi?.upozorenja?.length > 0 && (
            <div className="events-table-card" style={{ marginBottom: '1.5rem', padding: '1rem' }}>
              <h2>Upozorenja</h2>
              <ul style={{ color: 'var(--warning, #b45309)', marginTop: '0.5rem' }}>
                {resursi.upozorenja.map((u) => (
                  <li key={u}>{u}</li>
                ))}
              </ul>
            </div>
          )}

          <div className="events-table-card" style={{ marginBottom: '1.5rem' }}>
            <div className="events-table-header">
              <h2>Iskorišćenost sala</h2>
            </div>
            {resursi?.stavkeSala?.length > 0 ? (
              <div className="events-table-scroll">
                <table className="events-table">
                  <thead>
                    <tr>
                      <th>Sesija</th>
                      <th>Sala</th>
                      <th>Lokacija</th>
                      <th>Datum</th>
                      <th>Kapacitet</th>
                      <th>Popunjenost</th>
                      <th>Iskorišćenost</th>
                    </tr>
                  </thead>
                  <tbody>
                    {resursi.stavkeSala.map((s) => (
                      <tr key={s.sesijaId}>
                        <td>{s.sesijaNaziv}</td>
                        <td>{s.nazivSale}</td>
                        <td>{s.lokacijaNaziv || '—'}</td>
                        <td>{s.datum || '—'}</td>
                        <td>{s.kapacitet ?? '—'}</td>
                        <td>{s.popunjenost ?? '—'}</td>
                        <td>{formatPercent(s.iskoriscenostProcenat)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : (
              <p style={{ padding: '1rem', color: 'var(--text-muted)' }}>Nema sesija za ovaj događaj.</p>
            )}
          </div>

          <div className="events-table-card" style={{ marginBottom: '1.5rem' }}>
            <div className="events-table-header">
              <h2>Oprema i pokrivenost</h2>
            </div>
            {resursi?.stavkeOpreme?.length > 0 ? (
              <div className="events-table-scroll">
                <table className="events-table">
                  <thead>
                    <tr>
                      <th>Resurs</th>
                      <th>Potrebno</th>
                      <th>Dodeljeno</th>
                      <th>Na stanju</th>
                      <th>Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {resursi.stavkeOpreme.map((o) => (
                      <tr key={o.nazivResursa}>
                        <td>{o.nazivResursa}</td>
                        <td>{o.potrebnaKolicina}</td>
                        <td>{o.dodeljenoDogadjaju}</td>
                        <td>{o.dostupnoNaStanju}</td>
                        <td>
                          <span style={{ color: o.pokriveno ? 'var(--success, #15803d)' : 'var(--warning, #b45309)' }}>
                            {o.pokriveno ? 'Pokriveno' : 'Nedovoljno'}
                          </span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : (
              <p style={{ padding: '1rem', color: 'var(--text-muted)' }}>Nema detektovanih potreba za opremu.</p>
            )}
          </div>

          <div className="events-table-card" style={{ marginBottom: '1.5rem' }}>
            <div className="events-table-header">
              <h2>Nabavke</h2>
            </div>
            {resursi?.stavkeNabavke?.length > 0 ? (
              <div className="events-table-scroll">
                <table className="events-table">
                  <thead>
                    <tr>
                      <th>ID</th>
                      <th>Status</th>
                      <th>Dobavljač</th>
                      <th>Stavki</th>
                      <th>Vrednost</th>
                      <th>Commitovana</th>
                    </tr>
                  </thead>
                  <tbody>
                    {resursi.stavkeNabavke.map((n) => (
                      <tr key={n.nabavkaId}>
                        <td>#{n.nabavkaId}</td>
                        <td>{n.status}</td>
                        <td>{n.dobavljacNaziv || '—'}</td>
                        <td>{n.brojStavki}</td>
                        <td>{formatMoney(n.ukupnaVrednost)}</td>
                        <td>{n.commitovana ? 'Da' : 'Ne'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : (
              <p style={{ padding: '1rem', color: 'var(--text-muted)' }}>Nema nabavki za ovaj događaj.</p>
            )}
          </div>

          <div className="events-table-card">
            <div className="events-table-header">
              <h2>Finansije</h2>
            </div>
            <div style={{ padding: '1rem', display: 'grid', gap: '0.5rem', maxWidth: 480 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span>Ukupan prihod</span>
                <strong>{formatMoney(troskovi?.ukupanPrihod)}</strong>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span>Ukupan trošak</span>
                <strong>{formatMoney(troskovi?.ukupanTrosak)}</strong>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span>Neto</span>
                <strong>{formatMoney(troskovi?.neto)}</strong>
              </div>
              <hr style={{ border: 'none', borderTop: '1px solid var(--border)' }} />
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.9rem' }}>
                <span>Prihod od karata</span>
                <span>{formatMoney(troskovi?.prihodOdKarata)}</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.9rem' }}>
                <span>Prihod od izlaznih faktura</span>
                <span>{formatMoney(troskovi?.prihodOdIzlaznihFaktura)}</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.9rem' }}>
                <span>Evidentirani trošak</span>
                <span>{formatMoney(troskovi?.trosakEvidentiran)}</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.9rem' }}>
                <span>Honorari</span>
                <span>{formatMoney(troskovi?.trosakHonorari)}</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.9rem' }}>
                <span>Commitovana nabavka (informativno)</span>
                <span>{formatMoney(troskovi?.commitovanaNabavka)}</span>
              </div>
            </div>

            {troskovi?.iskoriscenostBudzeta?.length > 0 && (
              <div className="events-table-scroll" style={{ marginTop: '1rem' }}>
                <table className="events-table">
                  <thead>
                    <tr>
                      <th>Budžet</th>
                      <th>Kategorija</th>
                      <th>Planirano</th>
                      <th>Stvarno</th>
                      <th>Iskorišćenost</th>
                      <th>Kontrola</th>
                    </tr>
                  </thead>
                  <tbody>
                    {troskovi.iskoriscenostBudzeta.map((b, idx) => (
                      <tr key={`${b.budzetId}-${b.kategorijaNaziv}-${idx}`}>
                        <td>{b.nazivBudzeta}</td>
                        <td>{b.kategorijaNaziv}</td>
                        <td>{formatMoney(b.planirano)}</td>
                        <td>{formatMoney(b.stvarno)}</td>
                        <td>{b.iskoriscenostProcenat != null ? `${b.iskoriscenostProcenat}%` : '—'}</td>
                        <td>{b.statusKontrole || '—'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </>
      )}
    </div>
  );
}
