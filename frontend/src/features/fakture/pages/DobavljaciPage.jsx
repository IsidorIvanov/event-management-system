import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useToast } from '@/shared/components/ToastNotification';
import ConfirmDialog from '@/shared/components/ConfirmDialog';
import * as nabavkaApi from '@/features/fakture/services/nabavkaService';

const STATUS_LABEL = {
  AKTIVAN: { label: 'Aktivan', cls: 'status-published' },
  NEAKTIVAN: { label: 'Neaktivan', cls: 'status-draft' },
  SUSPENDOVAN: { label: 'Suspendovan', cls: 'status-ongoing' },
};

const EMPTY_FORM = {
  naziv: '',
  grad: '',
  kontaktEmail: '',
  telefon: '',
  pib: '',
};

function extractError(err) {
  const data = err.response?.data;
  if (data?.details) return Object.values(data.details).join(', ');
  return data?.error || err.message || 'Došlo je do greške';
}

export default function DobavljaciPage() {
  const toast = useToast();
  const [dobavljaci, setDobavljaci] = useState([]);
  const [statusFilter, setStatusFilter] = useState('');
  const [form, setForm] = useState(EMPTY_FORM);
  const [editId, setEditId] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const res = await nabavkaApi.getAllDobavljaci();
      setDobavljaci(res.data);
    } catch (err) {
      toast(extractError(err), 'error');
    } finally {
      setLoading(false);
    }
  }, [toast]);

  useEffect(() => {
    load();
  }, [load]);

  const filtered = dobavljaci.filter((d) => !statusFilter || d.status === statusFilter);

  const setField = (field) => (e) => setForm((prev) => ({ ...prev, [field]: e.target.value }));

  const resetForm = () => {
    setForm(EMPTY_FORM);
    setEditId(null);
  };

  const startEdit = (d) => {
    setEditId(d.dobavljacId);
    setForm({
      naziv: d.naziv || '',
      grad: d.grad || '',
      kontaktEmail: d.kontaktEmail || '',
      telefon: d.telefon || '',
      pib: d.pib || '',
      status: d.status || 'AKTIVAN',
      rejting: d.rejting != null ? String(d.rejting) : '',
      kriterijumi: d.kriterijumi || '',
    });
  };

  const payload = () => {
    const base = {
      naziv: form.naziv.trim(),
      grad: form.grad.trim(),
      kontaktEmail: form.kontaktEmail.trim(),
      telefon: form.telefon.trim() || null,
      pib: form.pib.trim(),
    };
    if (!editId) return base;
    return {
      ...base,
      status: form.status,
      rejting: form.rejting !== '' ? Number(form.rejting) : null,
      kriterijumi: form.kriterijumi?.trim() || null,
    };
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);
    try {
      if (editId) {
        await nabavkaApi.updateDobavljac(editId, payload());
        toast('Dobavljač ažuriran.', 'success');
      } else {
        await nabavkaApi.createDobavljac(payload());
        toast('Dobavljač kreiran.', 'success');
      }
      resetForm();
      await load();
    } catch (err) {
      toast(extractError(err), 'error');
    } finally {
      setSaving(false);
    }
  };

  const handleDeactivate = async (id) => {
    try {
      await nabavkaApi.deactivateDobavljac(id);
      toast('Dobavljač deaktiviran.', 'success');
      await load();
    } catch (err) {
      toast(extractError(err), 'error');
    }
  };

  const confirmDelete = async () => {
    if (!deleteTarget) return;
    try {
      await nabavkaApi.deleteDobavljac(deleteTarget.dobavljacId);
      toast('Dobavljač obrisan.', 'success');
      if (editId === deleteTarget.dobavljacId) resetForm();
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
          <h1>Dobavljači</h1>
          <p className="page-subtitle">Katalog partnera i dobavljača opreme</p>
        </div>
        <Link to="/dashboard/cenovnik" className="btn btn-outline btn-sm">Cenovnik</Link>
      </div>

      <div className="events-table-card" style={{ marginBottom: '1.5rem' }}>
        <div className="events-table-header">
          <h2>{editId ? 'Izmena dobavljača' : 'Novi dobavljač'}</h2>
        </div>
        <form onSubmit={handleSubmit} className="form-grid" style={{ padding: '1rem' }}>
          <input className="form-control" placeholder="Naziv *" value={form.naziv} onChange={setField('naziv')} required />
          <input className="form-control" placeholder="Grad *" value={form.grad} onChange={setField('grad')} required />
          <input className="form-control" type="email" placeholder="Email *" value={form.kontaktEmail} onChange={setField('kontaktEmail')} required />
          <input className="form-control" placeholder="Telefon" value={form.telefon} onChange={setField('telefon')} />
          <input className="form-control" placeholder="PIB *" value={form.pib} onChange={setField('pib')} required />
          {editId && (
            <>
              <select className="form-control" value={form.status} onChange={setField('status')}>
                {Object.keys(STATUS_LABEL).map((s) => (
                  <option key={s} value={s}>{STATUS_LABEL[s].label}</option>
                ))}
              </select>
              <input className="form-control" type="number" min="0" max="5" step="0.1" placeholder="Rejting (opciono)" value={form.rejting} onChange={setField('rejting')} />
              <input className="form-control" placeholder="Kriterijumi (opciono)" value={form.kriterijumi} onChange={setField('kriterijumi')} />
            </>
          )}
          <div style={{ display: 'flex', gap: '0.5rem' }}>
            <button type="submit" className="btn btn-primary" disabled={saving}>
              {saving ? 'Čuvanje...' : editId ? 'Sačuvaj izmene' : 'Dodaj dobavljača'}
            </button>
            {editId && (
              <button type="button" className="btn btn-outline" onClick={resetForm}>Otkaži</button>
            )}
          </div>
        </form>
      </div>

      <div className="events-table-card">
        <div className="events-table-header">
          <h2>Lista dobavljača</h2>
          <select className="status-select" value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
            <option value="">Svi statusi</option>
            {Object.entries(STATUS_LABEL).map(([value, { label }]) => (
              <option key={value} value={value}>{label}</option>
            ))}
          </select>
        </div>
        <table className="events-table">
          <thead>
            <tr><th>NAZIV</th><th>GRAD</th><th>EMAIL</th><th>PIB</th><th>REJTING</th><th>STATUS</th><th>AKCIJE</th></tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan={7} style={{ textAlign: 'center', padding: '2rem' }}>Učitavanje...</td></tr>
            ) : filtered.length === 0 ? (
              <tr><td colSpan={7} style={{ textAlign: 'center', padding: '2rem' }}>Nema dobavljača.</td></tr>
            ) : (
              filtered.map((d) => {
                const st = STATUS_LABEL[d.status] || { label: d.status, cls: 'status-draft' };
                return (
                  <tr key={d.dobavljacId}>
                    <td><span className="event-name-badge">{d.naziv}</span></td>
                    <td>{d.grad}</td>
                    <td>{d.kontaktEmail}</td>
                    <td>{d.pib}</td>
                    <td>{d.rejting ?? '—'}</td>
                    <td><span className={`status-badge ${st.cls}`}>{st.label}</span></td>
                    <td>
                      <div style={{ display: 'flex', gap: '0.35rem', flexWrap: 'wrap' }}>
                        <button type="button" className="btn btn-outline btn-sm" onClick={() => startEdit(d)}>Izmeni</button>
                        {d.status === 'AKTIVAN' && (
                          <button type="button" className="btn btn-outline btn-sm" onClick={() => handleDeactivate(d.dobavljacId)}>Deaktiviraj</button>
                        )}
                        <button type="button" className="btn btn-danger btn-sm" onClick={() => setDeleteTarget(d)}>Obriši</button>
                      </div>
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>

      {deleteTarget && (
        <ConfirmDialog
          title="Brisanje dobavljača"
          message={`Obrisati dobavljača „${deleteTarget.naziv}"?`}
          onConfirm={confirmDelete}
          onCancel={() => setDeleteTarget(null)}
        />
      )}
    </div>
  );
}
