import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useToast } from '@/shared/components/ToastNotification';
import ConfirmDialog from '@/shared/components/ConfirmDialog';
import * as nabavkaApi from '@/features/fakture/services/nabavkaService';

const EMPTY_FORM = {
  dobavljacId: '',
  nazivResursa: '',
  opis: '',
  jedinicaMere: 'kom',
  cenaJedinicna: '',
  dostupnost: true,
};

function extractError(err) {
  const data = err.response?.data;
  if (data?.details) return Object.values(data.details).join(', ');
  return data?.error || err.message || 'Došlo je do greške';
}

function formatMoney(value) {
  return Number(value || 0).toLocaleString('sr-Latn', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

export default function CenovnikPage() {
  const toast = useToast();
  const [stavke, setStavke] = useState([]);
  const [dobavljaci, setDobavljaci] = useState([]);
  const [filterDobavljac, setFilterDobavljac] = useState('');
  const [filterDostupnost, setFilterDostupnost] = useState('');
  const [form, setForm] = useState(EMPTY_FORM);
  const [editId, setEditId] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [cenRes, dobRes] = await Promise.all([
        nabavkaApi.getCenovnik(),
        nabavkaApi.getAllDobavljaci(),
      ]);
      setStavke(cenRes.data);
      setDobavljaci(dobRes.data);
    } catch (err) {
      toast(extractError(err), 'error');
    } finally {
      setLoading(false);
    }
  }, [toast]);

  useEffect(() => {
    load();
  }, [load]);

  const filtered = stavke.filter((s) => {
    if (filterDobavljac && String(s.dobavljacId) !== filterDobavljac) return false;
    if (filterDostupnost === 'da' && !s.dostupnost) return false;
    if (filterDostupnost === 'ne' && s.dostupnost) return false;
    return true;
  });

  const setField = (field) => (e) => {
    const value = e.target.type === 'checkbox' ? e.target.checked : e.target.value;
    setForm((prev) => ({ ...prev, [field]: value }));
  };

  const resetForm = () => {
    setForm(EMPTY_FORM);
    setEditId(null);
  };

  const startEdit = (s) => {
    setEditId(s.cenovnikId);
    setForm({
      dobavljacId: String(s.dobavljacId),
      nazivResursa: s.nazivResursa || '',
      opis: s.opis || '',
      jedinicaMere: s.jedinicaMere || 'kom',
      cenaJedinicna: String(s.cenaJedinicna ?? ''),
      dostupnost: s.dostupnost !== false,
    });
  };

  const payload = () => ({
    dobavljacId: Number(form.dobavljacId),
    nazivResursa: form.nazivResursa.trim(),
    opis: form.opis.trim() || null,
    jedinicaMere: form.jedinicaMere.trim(),
    cenaJedinicna: Number(form.cenaJedinicna),
    dostupnost: form.dostupnost,
  });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);
    try {
      if (editId) {
        await nabavkaApi.updateCenovnik(editId, payload());
        toast('Stavka cenovnika ažurirana.', 'success');
      } else {
        await nabavkaApi.createCenovnik(payload());
        toast('Stavka dodata u cenovnik.', 'success');
      }
      resetForm();
      await load();
    } catch (err) {
      toast(extractError(err), 'error');
    } finally {
      setSaving(false);
    }
  };

  const confirmDelete = async () => {
    if (!deleteTarget) return;
    try {
      await nabavkaApi.deleteCenovnik(deleteTarget.cenovnikId);
      toast('Stavka obrisana.', 'success');
      if (editId === deleteTarget.cenovnikId) resetForm();
      await load();
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
          <h1>Cenovnik</h1>
          <p className="page-subtitle">Katalog resursa i cena po dobavljaču</p>
        </div>
        <div style={{ display: 'flex', gap: '0.5rem' }}>
          <Link to="/dashboard/dobavljaci" className="btn btn-outline btn-sm">Dobavljači</Link>
          <Link to="/dashboard/porudzbenice" className="btn btn-outline btn-sm">Porudžbenice</Link>
        </div>
      </div>

      <div className="events-table-card" style={{ marginBottom: '1.5rem' }}>
        <div className="events-table-header">
          <h2>{editId ? 'Izmena stavke' : 'Nova stavka cenovnika'}</h2>
        </div>
        <form onSubmit={handleSubmit} className="form-grid" style={{ padding: '1rem' }}>
          <select className="form-control" value={form.dobavljacId} onChange={setField('dobavljacId')} required>
            <option value="">Dobavljač *</option>
            {dobavljaci.map((d) => (
              <option key={d.dobavljacId} value={d.dobavljacId}>{d.naziv}</option>
            ))}
          </select>
          <input className="form-control" placeholder="Naziv resursa *" value={form.nazivResursa} onChange={setField('nazivResursa')} required />
          <input className="form-control" placeholder="Jedinica mere *" value={form.jedinicaMere} onChange={setField('jedinicaMere')} required />
          <input className="form-control" type="number" min="0.01" step="0.01" placeholder="Cena *" value={form.cenaJedinicna} onChange={setField('cenaJedinicna')} required />
          <input className="form-control" placeholder="Opis" value={form.opis} onChange={setField('opis')} />
          <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <input type="checkbox" checked={form.dostupnost} onChange={setField('dostupnost')} />
            Dostupno
          </label>
          <div style={{ display: 'flex', gap: '0.5rem' }}>
            <button type="submit" className="btn btn-primary" disabled={saving}>
              {saving ? 'Čuvanje...' : editId ? 'Sačuvaj' : 'Dodaj stavku'}
            </button>
            {editId && <button type="button" className="btn btn-outline" onClick={resetForm}>Otkaži</button>}
          </div>
        </form>
      </div>

      <div className="events-table-card">
        <div className="events-table-header">
          <h2>Stavke cenovnika</h2>
          <div className="events-table-controls">
            <select className="status-select" value={filterDobavljac} onChange={(e) => setFilterDobavljac(e.target.value)}>
              <option value="">Svi dobavljači</option>
              {dobavljaci.map((d) => (
                <option key={d.dobavljacId} value={d.dobavljacId}>{d.naziv}</option>
              ))}
            </select>
            <select className="status-select" value={filterDostupnost} onChange={(e) => setFilterDostupnost(e.target.value)}>
              <option value="">Sva dostupnost</option>
              <option value="da">Dostupno</option>
              <option value="ne">Nedostupno</option>
            </select>
          </div>
        </div>
        <table className="events-table">
          <thead>
            <tr><th>RESURS</th><th>DOBAVLJAČ</th><th>CENA</th><th>JED.</th><th>DOSTUPNO</th><th>AKCIJE</th></tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan={6} style={{ textAlign: 'center', padding: '2rem' }}>Učitavanje...</td></tr>
            ) : filtered.length === 0 ? (
              <tr><td colSpan={6} style={{ textAlign: 'center', padding: '2rem' }}>Nema stavki.</td></tr>
            ) : (
              filtered.map((s) => (
                <tr key={s.cenovnikId}>
                  <td><span className="event-name-badge">{s.nazivResursa}</span></td>
                  <td>{s.dobavljacNaziv}</td>
                  <td>{formatMoney(s.cenaJedinicna)} RSD</td>
                  <td>{s.jedinicaMere}</td>
                  <td>{s.dostupnost ? 'Da' : 'Ne'}</td>
                  <td>
                    <div style={{ display: 'flex', gap: '0.35rem' }}>
                      <button type="button" className="btn btn-outline btn-sm" onClick={() => startEdit(s)}>Izmeni</button>
                      <button type="button" className="btn btn-danger btn-sm" onClick={() => setDeleteTarget(s)}>Obriši</button>
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {deleteTarget && (
        <ConfirmDialog
          title="Brisanje stavke"
          message={`Obrisati „${deleteTarget.nazivResursa}"?`}
          onConfirm={confirmDelete}
          onCancel={() => setDeleteTarget(null)}
        />
      )}
    </div>
  );
}
