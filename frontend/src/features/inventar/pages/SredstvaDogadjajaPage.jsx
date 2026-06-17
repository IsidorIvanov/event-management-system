import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import api from '@/shared/services/api';
import { useToast } from '@/shared/components/ToastNotification';
import * as inventarApi from '@/features/inventar/services/inventarService';

function extractError(err) {
  const data = err.response?.data;
  return data?.error || err.message || 'Došlo je do greške';
}

export default function SredstvaDogadjajaPage() {
  const toast = useToast();
  const [dogadjaji, setDogadjaji] = useState([]);
  const [selectedId, setSelectedId] = useState('');
  const [sredstva, setSredstva] = useState(null);
  const [validacija, setValidacija] = useState(null);
  const [loading, setLoading] = useState(false);
  const [predlogAlokacije, setPredlogAlokacije] = useState(null);
  const [predlogLoading, setPredlogLoading] = useState(false);
  const [primenaLoading, setPrimenaLoading] = useState(false);

  useEffect(() => {
    api.get('/dogadjaj').then((res) => setDogadjaji(res.data)).catch(() => toast('Greška pri učitavanju događaja.', 'error'));
  }, [toast]);

  const load = useCallback(async (dogadjajId) => {
    if (!dogadjajId) return;
    setLoading(true);
    try {
      const [sRes, vRes] = await Promise.all([
        inventarApi.getSredstvaDogadjaja(dogadjajId),
        inventarApi.validirajPotrebeInventarom(dogadjajId),
      ]);
      setSredstva(sRes.data);
      setValidacija(vRes.data);
    } catch (err) {
      toast(extractError(err), 'error');
      setSredstva(null);
      setValidacija(null);
    } finally {
      setLoading(false);
    }
  }, [toast]);

  useEffect(() => {
    setPredlogAlokacije(null);
    if (selectedId) load(selectedId);
  }, [selectedId, load]);

  const handlePredlogAlokacije = async () => {
    if (!selectedId) return;
    setPredlogLoading(true);
    setPredlogAlokacije(null);
    try {
      const res = await inventarApi.getPredlogAlokacijeOpreme(selectedId);
      setPredlogAlokacije(res.data);
      if (res.data.stavke?.length === 0) {
        toast('Nema detektovanih potreba za alokaciju.', 'warning');
      }
    } catch (err) {
      toast(extractError(err), 'error');
    } finally {
      setPredlogLoading(false);
    }
  };

  const handleOdbijPredlog = () => {
    setPredlogAlokacije(null);
    toast('Predlog alokacije odbijen.', 'info');
  };

  const handlePrihvatiPredlog = async () => {
    if (!selectedId || !predlogAlokacije) return;
    const imaNove = predlogAlokacije.stavke?.some((s) => s.pokriveno && s.predlozenaKolicina > 0);
    if (!imaNove) {
      toast('Nema novih stavki za rezervaciju — potrebe su već pokrivene ili nema zaliha.', 'warning');
      setPredlogAlokacije(null);
      return;
    }
    setPrimenaLoading(true);
    try {
      const res = await inventarApi.primeniAlokacijuOpreme(selectedId);
      const broj = res.data.kreiraneDodele?.length || 0;
      toast(`Alokacija prihvaćena — kreirano ${broj} rezervacija opreme.`, 'success');
      setPredlogAlokacije(null);
      await load(selectedId);
    } catch (err) {
      toast(extractError(err), 'error');
    } finally {
      setPrimenaLoading(false);
    }
  };

  const stavkeZaPrikaz = predlogAlokacije?.stavke?.filter((s) => s.predlozenaKolicina > 0 || !s.pokriveno || s.vecDodeljeno > 0) || [];

  return (
    <div className="program-page">
      <div className="program-header">
        <div>
          <h1>Sredstva događaja</h1>
          <p className="page-subtitle">Sale, nabavke i dodeljena oprema na jednom mestu</p>
        </div>
        <Link to="/dashboard/inventar" className="btn btn-outline btn-sm">Inventar</Link>
      </div>

      <div className="events-table-card" style={{ marginBottom: '1.5rem', padding: '1rem' }}>
        <label className="form-label">Događaj</label>
        <select className="form-control" value={selectedId} onChange={(e) => setSelectedId(e.target.value)}>
          <option value="">Izaberite događaj</option>
          {dogadjaji.map((d) => (
            <option key={d.dogadjajId} value={d.dogadjajId}>{d.naziv}</option>
          ))}
        </select>
      </div>

      {selectedId && (
        <div className="events-table-card" style={{ marginBottom: '1.5rem', padding: '1rem' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '0.75rem' }}>
            <div>
              <h2 style={{ margin: 0 }}>Optimizacija alokacije opreme</h2>
              <p className="validacija-meta" style={{ marginTop: '0.35rem' }}>
                Sistem detektuje potrebe događaja i predlaže dodelu iz inventara.
              </p>
            </div>
            <button
              type="button"
              className="btn btn-primary btn-sm"
              onClick={handlePredlogAlokacije}
              disabled={predlogLoading || primenaLoading}
              style={{ width: 'auto' }}
            >
              {predlogLoading ? 'Računam...' : 'Predloži alokaciju opreme'}
            </button>
          </div>

          {predlogAlokacije && (
            <div style={{ marginTop: '1.25rem' }}>
              <p className="validacija-meta" style={{ marginBottom: '0.75rem' }}>
                <strong>{predlogAlokacije.dogadjajNaziv}</strong>
                {' · '}{predlogAlokacije.pokrivenoStavki}/{predlogAlokacije.ukupnoPotreba} potreba pokriveno
                {predlogAlokacije.svePokriveno ? ' ✓' : ' (delimično)'}
              </p>

              {predlogAlokacije.upozorenja?.length > 0 && (
                <ul className="validacija-lista" style={{ marginBottom: '0.75rem' }}>
                  {predlogAlokacije.upozorenja.map((u, i) => <li key={i}>{u}</li>)}
                </ul>
              )}

              <table className="events-table">
                <thead>
                  <tr>
                    <th>POTREBA</th>
                    <th>POTREBNO</th>
                    <th>VEĆ DODELJENO</th>
                    <th>PREDLOG</th>
                    <th>OPREMA (INVENTAR)</th>
                    <th>PERIOD</th>
                    <th>STATUS</th>
                  </tr>
                </thead>
                <tbody>
                  {stavkeZaPrikaz.length === 0 ? (
                    <tr>
                      <td colSpan={7} style={{ textAlign: 'center', padding: '1rem' }}>
                        Nema stavki u predlogu.
                      </td>
                    </tr>
                  ) : (
                    stavkeZaPrikaz.map((s, i) => (
                      <tr key={`${s.nazivPotrebe}-${s.opremaId || i}`}>
                        <td>{s.nazivPotrebe}</td>
                        <td>{s.potrebnaKolicina}</td>
                        <td>{s.vecDodeljeno || 0}</td>
                        <td>{s.predlozenaKolicina > 0 ? `+${s.predlozenaKolicina}` : '—'}</td>
                        <td>{s.opremaNaziv || '—'}</td>
                        <td>{s.datumOd && s.datumDo ? `${s.datumOd} → ${s.datumDo}` : '—'}</td>
                        <td>
                          {s.pokriveno ? (
                            <span style={{ color: 'var(--success, #15803d)' }}>Pokriveno</span>
                          ) : (
                            <span style={{ color: 'var(--warning, #b45309)' }}>Nedostaje</span>
                          )}
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>

              {stavkeZaPrikaz.some((s) => s.obrazlozenje) && (
                <ul className="validacija-meta" style={{ marginTop: '0.75rem', fontSize: '0.85rem' }}>
                  {stavkeZaPrikaz.filter((s) => s.obrazlozenje).map((s, i) => (
                    <li key={i}>{s.obrazlozenje}</li>
                  ))}
                </ul>
              )}

              <div className="porudzbenice-actions" style={{ marginTop: '1rem' }}>
                <button
                  type="button"
                  className="btn btn-primary btn-sm"
                  onClick={handlePrihvatiPredlog}
                  disabled={primenaLoading}
                >
                  {primenaLoading ? 'Primena...' : 'Prihvati predlog'}
                </button>
                <button
                  type="button"
                  className="btn btn-outline btn-sm"
                  onClick={handleOdbijPredlog}
                  disabled={primenaLoading}
                >
                  Odbij
                </button>
              </div>
            </div>
          )}
        </div>
      )}

      {loading && <p style={{ padding: '1rem' }}>Učitavanje...</p>}

      {sredstva && !loading && (
        <>
          {validacija && (
            <div className="events-table-card" style={{ marginBottom: '1.5rem', padding: '1rem' }}>
              <h2>Pokrivenost inventarom ({validacija.tacnostProcenat}%)</h2>
              {validacija.upozorenja?.length > 0 ? (
                <ul style={{ color: 'var(--warning, #b45309)', marginTop: '0.5rem' }}>
                  {validacija.upozorenja.map((u) => <li key={u}>{u}</li>)}
                </ul>
              ) : (
                <p style={{ color: 'var(--success, #15803d)' }}>Sve detektovane potrebe su pokrivene zalihama.</p>
              )}
            </div>
          )}

          <div className="events-table-card" style={{ marginBottom: '1.5rem' }}>
            <div className="events-table-header"><h2>Sale i sesije</h2></div>
            <table className="events-table">
              <thead><tr><th>SESIJA</th><th>SALA</th><th>LOKACIJA</th><th>DATUM</th></tr></thead>
              <tbody>
                {sredstva.sale?.length ? sredstva.sale.map((s, i) => (
                  <tr key={`${s.sesijaId}-${i}`}>
                    <td>{s.sesijaNaziv}</td>
                    <td>{s.nazivSale}</td>
                    <td>{s.lokacijaNaziv || '—'}</td>
                    <td>{s.datum || '—'}</td>
                  </tr>
                )) : (
                  <tr><td colSpan={4} style={{ textAlign: 'center', padding: '1.5rem' }}>Nema sesija.</td></tr>
                )}
              </tbody>
            </table>
          </div>

          <div className="events-table-card" style={{ marginBottom: '1.5rem' }}>
            <div className="events-table-header"><h2>Nabavke</h2></div>
            <table className="events-table">
              <thead><tr><th>ID</th><th>STATUS</th><th>DOBAVLJAČ</th><th>STAVKE</th></tr></thead>
              <tbody>
                {sredstva.nabavke?.length ? sredstva.nabavke.map((n) => (
                  <tr key={n.nabavkaId}>
                    <td>#{n.nabavkaId}</td>
                    <td>{n.status}</td>
                    <td>{n.dobavljacNaziv || '—'}</td>
                    <td>{n.stavkeOpis}</td>
                  </tr>
                )) : (
                  <tr><td colSpan={4} style={{ textAlign: 'center', padding: '1.5rem' }}>Nema nabavki.</td></tr>
                )}
              </tbody>
            </table>
          </div>

          <div className="events-table-card">
            <div className="events-table-header"><h2>Dodeljena oprema (inventar)</h2></div>
            <table className="events-table">
              <thead><tr><th>OPREMA</th><th>SESIJA</th><th>KOL.</th><th>PERIOD</th><th>STATUS</th></tr></thead>
              <tbody>
                {sredstva.dodeleOpreme?.length ? sredstva.dodeleOpreme.map((d) => (
                  <tr key={d.dodelaId}>
                    <td>{d.opremaNaziv}</td>
                    <td>{d.sesijaNaziv || '—'}</td>
                    <td>{d.kolicina}</td>
                    <td>{d.datumOd} → {d.datumDo}</td>
                    <td>{d.status}</td>
                  </tr>
                )) : (
                  <tr><td colSpan={5} style={{ textAlign: 'center', padding: '1.5rem' }}>Nema dodela opreme.</td></tr>
                )}
              </tbody>
            </table>
          </div>
        </>
      )}
    </div>
  );
}
