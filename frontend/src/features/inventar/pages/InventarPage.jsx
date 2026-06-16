import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import api from '@/shared/services/api';
import { useToast } from '@/shared/components/ToastNotification';
import ConfirmDialog from '@/shared/components/ConfirmDialog';
import * as inventarApi from '@/features/inventar/services/inventarService';

const STATUS_LABEL = {
  DOSTUPNO: { label: 'Dostupno', cls: 'status-published' },
  REZERVISANO: { label: 'Rezervisano', cls: 'status-ongoing' },
  NA_DOGADJAJU: { label: 'Na događaju', cls: 'status-ongoing' },
  U_SERVISU: { label: 'U servisu', cls: 'status-draft' },
  OTPISANO: { label: 'Otpisano', cls: 'status-draft' },
};

const DODELA_STATUS = {
  REZERVISANO: 'Rezervisano',
  NA_DOGADJAJU: 'Na događaju',
  ZAVRSENO: 'Završeno',
  OTKAZANO: 'Otkazano',
};

const EMPTY_FORM = {
  naziv: '',
  kategorija: 'AUDIO',
  kolicinaUkupno: '0',
  kolicinaDostupno: '0',
  lokacijaSkladista: '',
  napomena: '',
};

const EMPTY_DODELA = {
  opremaId: '',
  dogadjajId: '',
  sesijaId: '',
  kolicina: '1',
  datumOd: '',
  datumDo: '',
  napomena: '',
};

function extractError(err) {
  const data = err.response?.data;
  if (data?.details) return Object.values(data.details).join(', ');
  return data?.error || err.message || 'Došlo je do greške';
}

export default function InventarPage() {
  const toast = useToast();
  const [oprema, setOprema] = useState([]);
  const [dodele, setDodele] = useState([]);
  const [dogadjaji, setDogadjaji] = useState([]);
  const [sesije, setSesije] = useState([]);
  const [filterDogadjaj, setFilterDogadjaj] = useState('');
  const [form, setForm] = useState(EMPTY_FORM);
  const [dodelaForm, setDodelaForm] = useState(EMPTY_DODELA);
  const [editId, setEditId] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState(null);

  const loadOprema = useCallback(async () => {
    const res = await inventarApi.getAllOprema();
    setOprema(res.data);
  }, []);

  const loadDogadjaji = useCallback(async () => {
    const res = await api.get('/dogadjaj');
    setDogadjaji(res.data);
  }, []);

  const loadDodele = useCallback(async (dogadjajId) => {
    if (!dogadjajId) {
      setDodele([]);
      return;
    }
    const res = await inventarApi.getDodelePoDogadjaju(dogadjajId);
    setDodele(res.data);
  }, []);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      await Promise.all([loadOprema(), loadDogadjaji()]);
      if (filterDogadjaj) await loadDodele(filterDogadjaj);
    } catch (err) {
      toast(extractError(err), 'error');
    } finally {
      setLoading(false);
    }
  }, [filterDogadjaj, loadDogadjaji, loadDodele, loadOprema, toast]);

  useEffect(() => {
    load();
  }, [load]);

  useEffect(() => {
    if (!dodelaForm.dogadjajId) {
      setSesije([]);
      return;
    }
    api.get(`/sesija/dogadjaj/${dodelaForm.dogadjajId}`)
      .then((res) => setSesije(res.data))
      .catch(() => setSesije([]));
  }, [dodelaForm.dogadjajId]);

  const setField = (field) => (e) => setForm((prev) => ({ ...prev, [field]: e.target.value }));
  const setDodelaField = (field) => (e) => setDodelaForm((prev) => ({ ...prev, [field]: e.target.value }));

  const resetForm = () => {
    setForm(EMPTY_FORM);
    setEditId(null);
  };

  const startEdit = (o) => {
    setEditId(o.opremaId);
    setForm({
      naziv: o.naziv || '',
      kategorija: o.kategorija || 'OSTALO',
      kolicinaUkupno: String(o.kolicinaUkupno ?? 0),
      kolicinaDostupno: String(o.kolicinaDostupno ?? 0),
      lokacijaSkladista: o.lokacijaSkladista || '',
      napomena: o.napomena || '',
    });
  };

  const payload = () => ({
    naziv: form.naziv.trim(),
    kategorija: form.kategorija,
    kolicinaUkupno: Number(form.kolicinaUkupno),
    kolicinaDostupno: Number(form.kolicinaDostupno),
    lokacijaSkladista: form.lokacijaSkladista.trim() || null,
    napomena: form.napomena.trim() || null,
  });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);
    try {
      if (editId) {
        await inventarApi.updateOprema(editId, payload());
        toast('Oprema ažurirana.', 'success');
      } else {
        await inventarApi.createOprema(payload());
        toast('Oprema dodata u inventar.', 'success');
      }
      resetForm();
      await loadOprema();
    } catch (err) {
      toast(extractError(err), 'error');
    } finally {
      setSaving(false);
    }
  };

  const handleDodela = async (e) => {
    e.preventDefault();
    setSaving(true);
    try {
      await inventarApi.rezervisiOpremu({
        opremaId: Number(dodelaForm.opremaId),
        dogadjajId: Number(dodelaForm.dogadjajId),
        sesijaId: dodelaForm.sesijaId ? Number(dodelaForm.sesijaId) : null,
        kolicina: Number(dodelaForm.kolicina),
        datumOd: dodelaForm.datumOd,
        datumDo: dodelaForm.datumDo,
        napomena: dodelaForm.napomena.trim() || null,
      });
      toast('Oprema rezervisana.', 'success');
      setDodelaForm(EMPTY_DODELA);
      await Promise.all([loadOprema(), loadDodele(filterDogadjaj || dodelaForm.dogadjajId)]);
    } catch (err) {
      toast(extractError(err), 'error');
    } finally {
      setSaving(false);
    }
  };

  const dodelaAction = async (id, action) => {
    try {
      if (action === 'aktiviraj') await inventarApi.aktivirajDodelu(id);
      if (action === 'oslobodi') await inventarApi.oslobodiDodelu(id);
      if (action === 'otkazi') await inventarApi.otkaziDodelu(id);
      toast('Dodela ažurirana.', 'success');
      await Promise.all([loadOprema(), loadDodele(filterDogadjaj)]);
    } catch (err) {
      toast(extractError(err), 'error');
    }
  };

  const confirmDelete = async () => {
    if (!deleteTarget) return;
    try {
      await inventarApi.deleteOprema(deleteTarget.opremaId);
      toast('Oprema obrisana.', 'success');
      if (editId === deleteTarget.opremaId) resetForm();
      await loadOprema();
    } catch (err) {
      toast(extractError(err), 'error');
    } finally {
      setDeleteTarget(null);
    }
  };

  return (
    <div className="program-page">
      <div className="program-header">
        <div>
          <h1>Inventar opreme</h1>
          <p className="page-subtitle">Zalihe, rezervacije i dodele opreme događajima</p>
        </div>
        <Link to="/dashboard/sredstva-dogadjaja" className="btn btn-outline btn-sm">Sredstva događaja</Link>
      </div>

      <div className="events-table-card" style={{ marginBottom: '1.5rem' }}>
        <div className="events-table-header"><h2>{editId ? 'Izmena opreme' : 'Nova stavka inventara'}</h2></div>
        <form onSubmit={handleSubmit} className="form-grid" style={{ padding: '1rem' }}>
          <input className="form-control" placeholder="Naziv *" value={form.naziv} onChange={setField('naziv')} required />
          <select className="form-control" value={form.kategorija} onChange={setField('kategorija')}>
            {['AUDIO', 'RASVETA', 'SCENA', 'OSTALO'].map((k) => <option key={k} value={k}>{k}</option>)}
          </select>
          <input className="form-control" type="number" min="0" placeholder="Ukupno" value={form.kolicinaUkupno} onChange={setField('kolicinaUkupno')} />
          <input className="form-control" type="number" min="0" placeholder="Dostupno" value={form.kolicinaDostupno} onChange={setField('kolicinaDostupno')} />
          <input className="form-control" placeholder="Lokacija skladišta" value={form.lokacijaSkladista} onChange={setField('lokacijaSkladista')} />
          <input className="form-control" placeholder="Napomena" value={form.napomena} onChange={setField('napomena')} />
          <div style={{ display: 'flex', gap: '0.5rem' }}>
            <button type="submit" className="btn btn-primary" disabled={saving}>{editId ? 'Sačuvaj' : 'Dodaj'}</button>
            {editId && <button type="button" className="btn btn-outline" onClick={resetForm}>Otkaži</button>}
          </div>
        </form>
      </div>

      <div className="events-table-card" style={{ marginBottom: '1.5rem' }}>
        <div className="events-table-header"><h2>Stanje zaliha</h2></div>
        <table className="events-table">
          <thead>
            <tr><th>NAZIV</th><th>KATEGORIJA</th><th>UKUPNO</th><th>DOSTUPNO</th><th>REZ.</th><th>NA DOG.</th><th>STATUS</th><th>AKCIJE</th></tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan={8} style={{ textAlign: 'center', padding: '2rem' }}>Učitavanje...</td></tr>
            ) : oprema.length === 0 ? (
              <tr><td colSpan={8} style={{ textAlign: 'center', padding: '2rem' }}>Inventar je prazan.</td></tr>
            ) : oprema.map((o) => {
              const st = STATUS_LABEL[o.status] || { label: o.status, cls: 'status-draft' };
              return (
                <tr key={o.opremaId}>
                  <td><span className="event-name-badge">{o.naziv}</span></td>
                  <td>{o.kategorija}</td>
                  <td>{o.kolicinaUkupno}</td>
                  <td>{o.kolicinaDostupno}</td>
                  <td>{o.kolicinaRezervisano ?? 0}</td>
                  <td>{o.kolicinaNaDogadjaju ?? 0}</td>
                  <td><span className={`status-badge ${st.cls}`}>{st.label}</span></td>
                  <td>
                    <button type="button" className="btn btn-outline btn-sm" onClick={() => startEdit(o)}>Izmeni</button>
                    {' '}
                    <button type="button" className="btn btn-danger btn-sm" onClick={() => setDeleteTarget(o)}>Obriši</button>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>

      <div className="events-table-card">
        <div className="events-table-header">
          <h2>Dodele opreme</h2>
          <select className="status-select" value={filterDogadjaj} onChange={(e) => { setFilterDogadjaj(e.target.value); loadDodele(e.target.value); }}>
            <option value="">Izaberi događaj</option>
            {dogadjaji.map((d) => <option key={d.dogadjajId} value={d.dogadjajId}>{d.naziv}</option>)}
          </select>
        </div>

        <form onSubmit={handleDodela} className="form-grid" style={{ padding: '1rem' }}>
          <select className="form-control" value={dodelaForm.opremaId} onChange={setDodelaField('opremaId')} required>
            <option value="">Oprema *</option>
            {oprema.map((o) => <option key={o.opremaId} value={o.opremaId}>{o.naziv} (dostupno: {o.kolicinaDostupno})</option>)}
          </select>
          <select className="form-control" value={dodelaForm.dogadjajId} onChange={setDodelaField('dogadjajId')} required>
            <option value="">Događaj *</option>
            {dogadjaji.map((d) => <option key={d.dogadjajId} value={d.dogadjajId}>{d.naziv}</option>)}
          </select>
          <select className="form-control" value={dodelaForm.sesijaId} onChange={setDodelaField('sesijaId')}>
            <option value="">Sesija (opciono)</option>
            {sesije.map((s) => <option key={s.sesijaId} value={s.sesijaId}>{s.naziv}</option>)}
          </select>
          <input className="form-control" type="number" min="1" placeholder="Količina" value={dodelaForm.kolicina} onChange={setDodelaField('kolicina')} required />
          <input className="form-control" type="date" value={dodelaForm.datumOd} onChange={setDodelaField('datumOd')} required />
          <input className="form-control" type="date" value={dodelaForm.datumDo} onChange={setDodelaField('datumDo')} required />
          <button type="submit" className="btn btn-primary" disabled={saving}>Rezerviši</button>
        </form>

        <table className="events-table">
          <thead>
            <tr><th>OPREMA</th><th>SESIJA</th><th>KOL.</th><th>PERIOD</th><th>STATUS</th><th>AKCIJE</th></tr>
          </thead>
          <tbody>
            {!filterDogadjaj ? (
              <tr><td colSpan={6} style={{ textAlign: 'center', padding: '1.5rem' }}>Izaberite događaj.</td></tr>
            ) : dodele.length === 0 ? (
              <tr><td colSpan={6} style={{ textAlign: 'center', padding: '1.5rem' }}>Nema dodela.</td></tr>
            ) : dodele.map((d) => (
              <tr key={d.dodelaId}>
                <td>{d.opremaNaziv}</td>
                <td>{d.sesijaNaziv || '—'}</td>
                <td>{d.kolicina}</td>
                <td>{d.datumOd} → {d.datumDo}</td>
                <td>{DODELA_STATUS[d.status] || d.status}</td>
                <td>
                  {d.status === 'REZERVISANO' && (
                    <button type="button" className="btn btn-outline btn-sm" onClick={() => dodelaAction(d.dodelaId, 'aktiviraj')}>Aktiviraj</button>
                  )}
                  {(d.status === 'REZERVISANO' || d.status === 'NA_DOGADJAJU') && (
                    <>
                      {' '}
                      <button type="button" className="btn btn-outline btn-sm" onClick={() => dodelaAction(d.dodelaId, 'oslobodi')}>Oslobodi</button>
                      {' '}
                      <button type="button" className="btn btn-danger btn-sm" onClick={() => dodelaAction(d.dodelaId, 'otkazi')}>Otkaži</button>
                    </>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {deleteTarget && (
        <ConfirmDialog
          title="Brisanje opreme"
          message={`Obrisati „${deleteTarget.naziv}" iz inventara?`}
          onConfirm={confirmDelete}
          onCancel={() => setDeleteTarget(null)}
        />
      )}
    </div>
  );
}
