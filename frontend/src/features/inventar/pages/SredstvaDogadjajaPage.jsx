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
    if (selectedId) load(selectedId);
  }, [selectedId, load]);

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
