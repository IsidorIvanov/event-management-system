import { useState, useEffect } from 'react';
import * as sesijaApi from '@/features/dogadjaji/services/sesijaService';
import * as govornikApi from '@/features/dogadjaji/services/govornikService';
import UpsertGovornikModal from '@/features/dogadjaji/components/UpsertGovornikModal';
import { useToast } from '@/shared/components/ToastNotification';
import ConfirmDialog from '@/shared/components/ConfirmDialog';

export default function GovorniciTab({ event }) {
  const toast = useToast();
  const [govornici, setGovornici] = useState([]);
  const [sesije, setSesije] = useState([]);
  const [loading, setLoading] = useState(true);
  const [modal, setModal] = useState(null); // null=closed | 'add' | govornik obj
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [search, setSearch] = useState('');

  const load = async () => {
    setLoading(true);
    try {
      const [gRes, sRes] = await Promise.all([
        govornikApi.getGovornikByDogadjaj(event.dogadjajId),
        sesijaApi.getSesijeByDogadjaj(event.dogadjajId),
      ]);
      setGovornici(gRes.data);
      setSesije(sRes.data);
    } catch {
      toast('Greška pri učitavanju govornika.', 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, [event.dogadjajId]);

  const handleSave = async (formData, sesijaIds) => {
    let saved;
    if (modal?.govornikId) {
      // edit
      const res = await govornikApi.updateGovornik(modal.govornikId, formData);
      saved = res.data;

      // sync sessions: add new, remove removed
      const oldIds = new Set(modal.sesijaIds || []);
      const newIds = new Set(sesijaIds);
      for (const sid of newIds) {
        if (!oldIds.has(sid)) await govornikApi.addGovornikToSesija(sid, saved.govornikId);
      }
      for (const sid of oldIds) {
        if (!newIds.has(sid)) await govornikApi.removeGovornikFromSesija(sid, saved.govornikId);
      }
      toast('Govornik je izmenjen.', 'success');
    } else {
      // create
      const res = await govornikApi.createGovornik(formData);
      saved = res.data;
      for (const sid of sesijaIds) {
        await govornikApi.addGovornikToSesija(sid, saved.govornikId);
      }
      toast('Govornik je dodan.', 'success');
    }
    setModal(null);
    await load();
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    try {
      await govornikApi.deleteGovornik(deleteTarget.govornikId);
      toast(`Govornik „${deleteTarget.ime} ${deleteTarget.prezime}" je obrisan.`, 'success');
      await load();
    } catch {
      toast('Greška pri brisanju govornika.', 'error');
    } finally {
      setDeleteTarget(null);
    }
  };

  const filtered = govornici.filter((g) => {
    const q = search.toLowerCase();
    return (
      !q ||
      `${g.ime} ${g.prezime}`.toLowerCase().includes(q) ||
      (g.kompanija || '').toLowerCase().includes(q) ||
      (g.pozicija || '').toLowerCase().includes(q)
    );
  });

  const getSesijeForGovornik = (g) =>
    sesije.filter((s) => s.govornici?.some((sg) => sg.govornikId === g.govornikId) ||
      (g.sesijaIds || []).includes(s.sesijaId));

  return (
    <div className="govornici-tab">
      {/* Delete confirm */}
      {deleteTarget && (
        <ConfirmDialog
          title="Obriši govornika"
          onConfirm={handleDelete}
          onCancel={() => setDeleteTarget(null)}
        >
          Da li ste sigurni da želite da obrišete govornika <strong>„{deleteTarget.ime} {deleteTarget.prezime}"</strong>?
          <br />
          <span style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>
            Ova akcija se ne može poništiti.
          </span>
        </ConfirmDialog>
      )}

      {/* Modal */}
      {modal !== null && (
        <UpsertGovornikModal
          govornik={modal === 'add' ? null : modal}
          dogadjajId={event.dogadjajId}
          onClose={() => setModal(null)}
          onSaved={handleSave}
        />
      )}

      {/* Toolbar */}
      <div className="govornici-toolbar">
        <input
          className="search-input"
          placeholder="pretraži govornike..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
        <button className="btn btn-primary" style={{ width: 'auto' }} onClick={() => setModal('add')}>
          + Novi govornik
        </button>
      </div>

      {/* Grid */}
      {loading ? (
        <p className="empty-hint">Učitavanje...</p>
      ) : filtered.length === 0 ? (
        <div className="sesije-empty">
          <p>Nema govornika za ovaj događaj.</p>
          <button className="btn btn-primary" style={{ width: 'auto', marginTop: '0.5rem' }} onClick={() => setModal('add')}>
            + Novi govornik
          </button>
        </div>
      ) : (
        <div className="govornici-grid">
          {filtered.map((g) => {
            const assignedSesije = getSesijeForGovornik(g);
            return (
              <div key={g.govornikId} className="govornik-card">
                <div className="govornik-card-avatar">
                  <div className="govornik-avatar-placeholder" />
                </div>
                <div className="govornik-card-body">
                  <div className="govornik-card-name">{g.ime} {g.prezime}</div>
                  <div className="govornik-card-meta">
                    {g.pozicija && <span>{g.pozicija}</span>}
                    {g.pozicija && g.kompanija && <span> · </span>}
                    {g.kompanija && <span>{g.kompanija}</span>}
                  </div>
                  {assignedSesije.length > 0 && (
                    <div className="govornik-card-sessions">
                      sesije: {assignedSesije.map((s) => s.naziv).join(', ').slice(0, 40)}
                      {assignedSesije.map((s) => s.naziv).join(', ').length > 40 ? '...' : ''}
                    </div>
                  )}
                  <div className="govornik-card-actions">
                    <button className="btn btn-outline btn-xs" onClick={() => setModal(g)}>Uredi</button>
                    <button className="btn btn-xs btn-danger-outline" onClick={() => setDeleteTarget(g)}>Ukloni</button>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
