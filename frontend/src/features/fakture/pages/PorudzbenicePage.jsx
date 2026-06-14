import { useCallback, useEffect, useState } from 'react';
import api from '@/shared/services/api';
import { useToast } from '@/shared/components/ToastNotification';
import * as nabavkaApi from '@/features/fakture/services/nabavkaService';

const STATUS_PORUDZBENICE = {
  KREIRANA: { label: 'Kreirana', cls: 'status-draft' },
  POSLATA: { label: 'Poslata', cls: 'status-ongoing' },
  POTVRDJENA: { label: 'Potvrđena', cls: 'status-published' },
  U_ISPORUCI: { label: 'U isporuci', cls: 'status-ongoing' },
  ISPORUCENA: { label: 'Isporučena', cls: 'status-finished' },
  OTKAZANA: { label: 'Otkazana', cls: 'status-draft' },
};

const STATUS_NABAVKE = {
  NACRT: 'Nacrt',
  U_OBRADI: 'U obradi',
  PREDLOZENA: 'Predložena',
  POTVRDJENA: 'Potvrđena',
  U_ISPORUCI: 'U isporuci',
  ODBIJENA: 'Odbijena',
  ZAVRSENA: 'Završena',
};

const STATUS_OPTIONS = ['KREIRANA', 'POSLATA', 'POTVRDJENA', 'U_ISPORUCI', 'ISPORUCENA', 'OTKAZANA'];

function extractError(err) {
  const data = err.response?.data;
  if (data?.details) return Object.values(data.details).join(', ');
  return data?.error || err.message || 'Došlo je do greške';
}

function formatMoney(n) {
  if (n == null) return '—';
  return Number(n).toLocaleString('sr-Latn', { minimumFractionDigits: 2 }) + ' RSD';
}

function cenovnikLabel(c) {
  return `${c.nazivResursa} — ${c.dobavljacNaziv} (${formatMoney(c.cenaJedinicna)}/${c.jedinicaMere})`;
}

export default function PorudzbenicePage() {
  const toast = useToast();
  const [dogadjaji, setDogadjaji] = useState([]);
  const [selectedDogadjajId, setSelectedDogadjajId] = useState('');
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);

  const [cenovnik, setCenovnik] = useState([]);
  const [manualStavke, setManualStavke] = useState([]);
  const [autoStavke, setAutoStavke] = useState([]);
  const [autoUpozorenja, setAutoUpozorenja] = useState([]);

  const [nabavke, setNabavke] = useState([]);
  const [porudzbenice, setPorudzbenice] = useState([]);
  const [selectedNabavkaId, setSelectedNabavkaId] = useState('');

  const dostupniCenovnik = cenovnik.filter((c) => c.dostupnost !== false);

  const loadDogadjaji = useCallback(async () => {
    const res = await api.get('/dogadjaj');
    setDogadjaji(res.data);
  }, []);

  const loadCenovnik = useCallback(async () => {
    const res = await nabavkaApi.getCenovnik();
    setCenovnik(res.data);
  }, []);

  const loadNabavkeIPorudzbenice = useCallback(async (dogadjajId) => {
    if (!dogadjajId) {
      setNabavke([]);
      setPorudzbenice([]);
      return;
    }
    const [nRes, pRes] = await Promise.all([
      nabavkaApi.getNabavkeByDogadjaj(dogadjajId),
      nabavkaApi.getPorudzbeniceByDogadjaj(dogadjajId),
    ]);
    setNabavke(nRes.data);
    setPorudzbenice(pRes.data);
    setSelectedNabavkaId((prev) =>
      prev && nRes.data.some((n) => n.nabavkaId === Number(prev)) ? prev : (nRes.data[0]?.nabavkaId?.toString() || '')
    );
  }, []);

  useEffect(() => {
    Promise.all([loadDogadjaji(), loadCenovnik()])
      .catch((err) => toast(extractError(err), 'error'))
      .finally(() => setLoading(false));
  }, [loadDogadjaji, loadCenovnik, toast]);

  useEffect(() => {
    if (selectedDogadjajId) {
      loadNabavkeIPorudzbenice(selectedDogadjajId).catch((err) => toast(extractError(err), 'error'));
    }
  }, [selectedDogadjajId, loadNabavkeIPorudzbenice, toast]);

  const stavkeZaApi = (lista) =>
    lista
      .filter((s) => s.nazivResursa && Number(s.kolicina) >= 1)
      .map((s) => ({
        nazivResursa: s.nazivResursa,
        kolicina: Number(s.kolicina),
        ...(s.cenovnikId ? { cenovnikId: s.cenovnikId } : {}),
      }));

  const handleCenovnikMultiSelect = (e) => {
    const ids = Array.from(e.target.selectedOptions).map((o) => Number(o.value));
    const nove = ids.map((cenovnikId) => {
      const c = dostupniCenovnik.find((x) => x.cenovnikId === cenovnikId);
      const postojeca = manualStavke.find((s) => s.cenovnikId === cenovnikId);
      return {
        cenovnikId,
        nazivResursa: c?.nazivResursa || '',
        kolicina: postojeca?.kolicina ?? 1,
      };
    });
    setManualStavke(nove);
  };

  const updateManualKolicina = (cenovnikId, kolicina) => {
    setManualStavke((prev) =>
      prev.map((s) => (s.cenovnikId === cenovnikId ? { ...s, kolicina: Math.max(1, Number(kolicina) || 1) } : s))
    );
  };

  const handleKreirajNabavkuRucno = async () => {
    const stavke = stavkeZaApi(manualStavke);
    if (!selectedDogadjajId || stavke.length === 0) {
      toast('Izaberite bar jednu stavku iz cenovnika.', 'warning');
      return;
    }
    setBusy(true);
    try {
      const res = await nabavkaApi.createNabavka({ dogadjajId: Number(selectedDogadjajId), stavke });
      toast(`Nabavka #${res.data.nabavkaId} kreirana (ručno).`, 'success');
      setSelectedNabavkaId(String(res.data.nabavkaId));
      await loadNabavkeIPorudzbenice(selectedDogadjajId);
    } catch (err) {
      toast(extractError(err), 'error');
    } finally {
      setBusy(false);
    }
  };

  const handleDetekcija = async () => {
    if (!selectedDogadjajId) return;
    setBusy(true);
    try {
      const res = await nabavkaApi.detektujPotrebe(selectedDogadjajId);
      const stavke = res.data.map((p) => ({
        nazivResursa: p.nazivResursa,
        kolicina: p.kolicina,
        cenovnikId: p.preporuceniCenovnikId || null,
      }));
      setAutoStavke(stavke);
      setAutoUpozorenja(stavke.length === 0
        ? ['Nema odgovarajućih stavki u cenovniku za ovaj događaj.']
        : []);
      toast(stavke.length > 0
        ? `Pronađeno ${stavke.length} stavki iz cenovnika.`
        : 'Nema stavki u cenovniku za predložene potrebe.', stavke.length > 0 ? 'success' : 'warning');
    } catch (err) {
      toast(extractError(err), 'error');
    } finally {
      setBusy(false);
    }
  };

  const handleKreirajNabavkuAuto = async () => {
    const stavke = stavkeZaApi(autoStavke);
    if (!selectedDogadjajId || stavke.length === 0) {
      toast('Prvo pokrenite automatsko predlaganje potreba.', 'warning');
      return;
    }
    setBusy(true);
    try {
      const res = await nabavkaApi.createNabavka({ dogadjajId: Number(selectedDogadjajId), stavke });
      toast(`Nabavka #${res.data.nabavkaId} kreirana. Dobavljač: ${res.data.dobavljacNaziv || '—'}.`, 'success');
      setSelectedNabavkaId(String(res.data.nabavkaId));
      setAutoStavke([]);
      await loadNabavkeIPorudzbenice(selectedDogadjajId);
    } catch (err) {
      toast(extractError(err), 'error');
    } finally {
      setBusy(false);
    }
  };

  const handleGenerisiPorudzbenicu = async () => {
    if (!selectedNabavkaId) return;
    setBusy(true);
    try {
      const res = await nabavkaApi.generisiPorudzbenicu({ nabavkaId: Number(selectedNabavkaId) });
      toast(`Porudžbenica ${res.data.brojPorudzbenice} generisana.`, 'success');
      await loadNabavkeIPorudzbenice(selectedDogadjajId);
    } catch (err) {
      toast(extractError(err), 'error');
    } finally {
      setBusy(false);
    }
  };

  const handleStatusChange = async (porudzbenicaId, status) => {
    try {
      const res = await nabavkaApi.updatePorudzbenicaStatus(porudzbenicaId, status);
      setPorudzbenice((prev) =>
        prev.map((p) => (p.porudzbenicaId === porudzbenicaId ? res.data : p))
      );
      if (selectedDogadjajId) {
        await loadNabavkeIPorudzbenice(selectedDogadjajId);
      }
      toast('Status ažuriran.', 'success');
    } catch (err) {
      toast(extractError(err), 'error');
      await loadNabavkeIPorudzbenice(selectedDogadjajId);
    }
  };

  const selectedNabavka = nabavke.find((n) => n.nabavkaId === Number(selectedNabavkaId));
  const imaPorudzbenicu = porudzbenice.some((p) => p.nabavkaId === Number(selectedNabavkaId));
  const selectedCenovnikIds = manualStavke.map((s) => s.cenovnikId);

  return (
    <div className="program-page porudzbenice-page">
      <div className="program-header">
        <div>
          <h1>Porudžbenice</h1>
          <p className="page-subtitle">ručno i automatsko poručivanje opreme iz cenovnika</p>
        </div>
      </div>

      <div className="resursi-toolbar">
        <div className="form-group" style={{ minWidth: 280 }}>
          <label>Događaj</label>
          <select
            className="status-select"
            value={selectedDogadjajId}
            onChange={(e) => {
              setSelectedDogadjajId(e.target.value);
              setManualStavke([]);
              setAutoStavke([]);
              setAutoUpozorenja([]);
            }}
            disabled={loading}
          >
            <option value="">— izaberite događaj —</option>
            {dogadjaji.map((d) => (
              <option key={d.dogadjajId} value={d.dogadjajId}>{d.naziv}</option>
            ))}
          </select>
        </div>
      </div>

      {selectedDogadjajId && (
        <>
          <div className="events-table-card manual-stavke-card">
            <div className="events-table-header">
              <h2>Ručno poručivanje</h2>
            </div>
            <div className="porudzbenice-manual-body">
              <div className="form-group">
                <label>Stavke iz cenovnika (Ctrl+klik za više)</label>
                {dostupniCenovnik.length === 0 ? (
                  <p className="validacija-meta">Cenovnik je prazan — dodajte stavke u modulu nabavke.</p>
                ) : (
                  <select
                    multiple
                    size={Math.min(8, Math.max(4, dostupniCenovnik.length))}
                    className="cenovnik-multiselect"
                    value={selectedCenovnikIds.map(String)}
                    onChange={handleCenovnikMultiSelect}
                  >
                    {dostupniCenovnik.map((c) => (
                      <option key={c.cenovnikId} value={c.cenovnikId}>
                        {cenovnikLabel(c)}
                      </option>
                    ))}
                  </select>
                )}
              </div>

              {manualStavke.length > 0 && (
                <table className="events-table manual-stavke-table">
                  <thead>
                    <tr><th>STAVKA</th><th>DOBAVLJAČ</th><th>KOLIČINA</th></tr>
                  </thead>
                  <tbody>
                    {manualStavke.map((s) => {
                      const c = dostupniCenovnik.find((x) => x.cenovnikId === s.cenovnikId);
                      return (
                        <tr key={s.cenovnikId}>
                          <td><span className="event-name-badge">{s.nazivResursa}</span></td>
                          <td>{c?.dobavljacNaziv || '—'}</td>
                          <td style={{ width: 100 }}>
                            <input
                              type="number"
                              min={1}
                              className="search-input"
                              value={s.kolicina}
                              onChange={(e) => updateManualKolicina(s.cenovnikId, e.target.value)}
                            />
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              )}

              <div className="porudzbenice-actions">
                <button type="button" className="btn btn-primary btn-sm" onClick={handleKreirajNabavkuRucno} disabled={busy || manualStavke.length === 0}>
                  Kreiraj nabavku
                </button>
              </div>
            </div>
          </div>

          <div className="events-table-card manual-stavke-card">
            <div className="events-table-header">
              <h2>Automatsko poručivanje</h2>
              <button type="button" className="btn btn-outline btn-sm" onClick={handleDetekcija} disabled={busy}>
                Predloži potrebe
              </button>
            </div>
            <div className="porudzbenice-manual-body">
              {autoUpozorenja.length > 0 && (
                <ul className="validacija-lista">
                  {autoUpozorenja.map((u, i) => <li key={i}>{u}</li>)}
                </ul>
              )}

              {autoStavke.length > 0 ? (
                <table className="events-table">
                  <thead>
                    <tr><th>RESURS</th><th>KOLIČINA</th><th>IZVOR</th></tr>
                  </thead>
                  <tbody>
                    {autoStavke.map((s, i) => (
                      <tr key={i}>
                        <td><span className="event-name-badge">{s.nazivResursa}</span></td>
                        <td>{s.kolicina}</td>
                        <td style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>detekcija događaja</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              ) : (
                <p className="validacija-meta">Kliknite „Predloži potrebe“ da sistem predloži stavke na osnovu događaja.</p>
              )}

              <div className="porudzbenice-actions">
                <button type="button" className="btn btn-primary btn-sm" onClick={handleKreirajNabavkuAuto} disabled={busy || autoStavke.length === 0}>
                  Kreiraj nabavku
                </button>
              </div>
            </div>
          </div>

          <div className="porudzbenice-grid">
            <div className="events-table-card">
              <div className="events-table-header"><h2>Nabavke</h2></div>
              {nabavke.length === 0 ? (
                <p style={{ padding: '1.5rem', color: 'var(--text-muted)' }}>Nema nabavki.</p>
              ) : (
                <table className="events-table">
                  <thead>
                    <tr><th>ID</th><th>STAVKE</th><th>DOBAVLJAČ</th><th>STATUS</th><th>UKUPNO</th><th></th></tr>
                  </thead>
                  <tbody>
                    {nabavke.map((n) => (
                      <tr key={n.nabavkaId} className={Number(selectedNabavkaId) === n.nabavkaId ? 'row-selected' : ''}>
                        <td>#{n.nabavkaId}</td>
                        <td style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
                          {(n.stavke || []).map((s) => `${s.nazivResursa} ×${s.kolicina}`).join(', ') || '—'}
                        </td>
                        <td>{n.dobavljacNaziv || '—'}</td>
                        <td><span className="status-badge status-draft">{STATUS_NABAVKE[n.status] || n.status}</span></td>
                        <td>{formatMoney(n.ukupnaCena)}</td>
                        <td>
                          <button type="button" className="btn btn-outline btn-xs" onClick={() => setSelectedNabavkaId(String(n.nabavkaId))}>
                            Izaberi
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}

              {selectedNabavka && (
                <div className="porudzbenice-actions" style={{ padding: '0 1.25rem 1rem' }}>
                  <button
                    type="button"
                    className="btn btn-outline btn-sm"
                    onClick={handleGenerisiPorudzbenicu}
                    disabled={busy || !selectedNabavka.dobavljacId || imaPorudzbenicu}
                  >
                    Generiši porudžbenicu
                  </button>
                </div>
              )}
            </div>

            <div className="events-table-card">
              <div className="events-table-header"><h2>Praćenje porudžbenica</h2></div>
              {porudzbenice.length === 0 ? (
                <p style={{ padding: '1.5rem', color: 'var(--text-muted)' }}>Nema porudžbenica.</p>
              ) : (
                <table className="events-table">
                  <thead>
                    <tr><th>BROJ</th><th>DOBAVLJAČ</th><th>STATUS</th><th>UKUPNO</th><th>AKCIJA</th></tr>
                  </thead>
                  <tbody>
                    {porudzbenice.map((p) => {
                      const st = STATUS_PORUDZBENICE[p.status] || { label: p.status, cls: 'status-draft' };
                      return (
                        <tr key={p.porudzbenicaId}>
                          <td><span className="event-name-badge">{p.brojPorudzbenice}</span></td>
                          <td>{p.dobavljacNaziv}</td>
                          <td><span className={`status-badge ${st.cls}`}>{st.label}</span></td>
                          <td>{formatMoney(p.ukupnaCena)}</td>
                          <td>
                            <select
                              className="status-select"
                              value={p.status}
                              onChange={(e) => handleStatusChange(p.porudzbenicaId, e.target.value)}
                              style={{ fontSize: '0.8rem' }}
                            >
                              {STATUS_OPTIONS.map((s) => (
                                <option key={s} value={s}>{STATUS_PORUDZBENICE[s]?.label || s}</option>
                              ))}
                            </select>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              )}
            </div>
          </div>
        </>
      )}
    </div>
  );
}
