import { useState, useEffect } from 'react';
import * as tipKarteApi from '@/features/dogadjaji/services/tipKarteService';
import UpsertKarteModal from '@/features/dogadjaji/components/UpsertKarteModal';
import { useToast } from '@/shared/components/ToastNotification';
import ConfirmDialog from '@/shared/components/ConfirmDialog';

const VRSTA_KARTE_LABEL = {
  VISEDNEVNA: 'Višednevna',
  JEDNODNEVNA: 'Jednodnevna',
  POJEDINACNA_SESIJA: 'Pojedinačna sesija',
  BESPLATNA: 'Besplatna',
};

function karteTitle(k) {
  if (k.vrsta === 'VISEDNEVNA') return 'Važi za ceo događaj';
  if (k.vrsta === 'BESPLATNA') return 'Besplatna ulaznica';
  if (k.vrsta === 'JEDNODNEVNA') {
    try {
      return new Date(k.nazivTipa + 'T00:00:00').toLocaleDateString('sr-Latn', {
        weekday: 'long', day: '2-digit', month: 'long', year: 'numeric',
      });
    } catch { return k.nazivTipa; }
  }
  // POJEDINACNA_SESIJA – nazivTipa je naziv sesije
  return k.nazivTipa;
}

export default function KarteTab({ event }) {
  const toast = useToast();
  const [karte, setKarte] = useState([]);
  const [loading, setLoading] = useState(true);
  const [modal, setModal] = useState(null); // null | 'add' | tipKarte obj
  const [deleteTarget, setDeleteTarget] = useState(null);

  const load = async () => {
    setLoading(true);
    try {
      const res = await tipKarteApi.getTipKarteByDogadjaj(event.dogadjajId);
      setKarte(res.data);
    } catch {
      toast('Greška pri učitavanju tipova karata.', 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, [event.dogadjajId]);

  const handleSave = async (data) => {
    try {
      if (modal?.nazivTipa) {
        await tipKarteApi.updateTipKarte(event.dogadjajId, modal.nazivTipa, data);
        toast('Tip karte je izmenjen.', 'success');
      } else {
        await tipKarteApi.createTipKarte(data);
        toast('Tip karte je dodat.', 'success');
      }
      setModal(null);
      await load();
    } catch (err) {
      toast(err?.response?.data?.message || 'Greška pri čuvanju tipa karte.', 'error');
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    try {
      await tipKarteApi.deleteTipKarte(event.dogadjajId, deleteTarget.nazivTipa);
      toast('Tip karte je obrisan.', 'success');
      await load();
    } catch {
      toast('Greška pri brisanju tipa karte.', 'error');
    } finally {
      setDeleteTarget(null);
    }
  };

  const totalKvota = karte.reduce((s, k) => s + k.kvota, 0);

  return (
    <div className="govornici-tab">
      {/* Delete confirm */}
      {deleteTarget && (
        <ConfirmDialog
          title="Obriši tip karte"
          onConfirm={handleDelete}
          onCancel={() => setDeleteTarget(null)}
        >
          Da li ste sigurni da želite da obrišete kartu{' '}
          <strong>„{VRSTA_KARTE_LABEL[deleteTarget.vrsta] || deleteTarget.vrsta}"</strong>?<br />
          <span style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>
            Ova akcija se ne može poništiti.
          </span>
        </ConfirmDialog>
      )}

      {modal !== null && (
        <UpsertKarteModal
          dogadjajId={event.dogadjajId}
          event={event}
          karte={karte}
          tipKarte={modal === 'add' ? null : modal}
          onClose={() => setModal(null)}
          onSaved={handleSave}
        />
      )}

      {/* Stats */}
      <div className="sesije-stats">
        <div className="sesija-stat-card">
          <div className="sesija-stat-label">Tipovi karata</div>
          <div className="sesija-stat-value">{karte.length}</div>
        </div>
        <div className="sesija-stat-card">
          <div className="sesija-stat-label">Ukupna kvota</div>
          <div className="sesija-stat-value">{totalKvota}</div>
        </div>
        <div className="sesija-stat-card">
          <div className="sesija-stat-label">Sesijske karte</div>
          <div className="sesija-stat-value">{karte.filter((k) => k.vrsta === 'POJEDINACNA_SESIJA').reduce((s, k) => s + (k.kvota || 0), 0)}</div>
        </div>
        <div className="sesija-stat-card">
          <div className="sesija-stat-label">Besplatne karte</div>
          <div className="sesija-stat-value">{karte.filter((k) => k.vrsta === 'BESPLATNA').reduce((s, k) => s + (k.kvota || 0), 0)}</div>
        </div>
      </div>

      {/* Toolbar */}
      <div className="govornici-toolbar">
        <div />
        <button className="btn btn-primary" style={{ width: 'auto' }} onClick={() => setModal('add')}>
          + Dodaj tip karte
        </button>
      </div>

      {/* List */}
      {loading ? (
        <p className="empty-hint">Učitavanje...</p>
      ) : karte.length === 0 ? (
        <div className="sesije-empty">
          <p>Nema tipova karata za ovaj događaj.</p>
          <button className="btn btn-primary" style={{ width: 'auto', marginTop: '0.5rem' }} onClick={() => setModal('add')}>
            + Dodaj tip karte
          </button>
        </div>
      ) : (
        <div className="karte-list">
          {/* Header row */}
          <div className="karte-list-header">
            <span>VRSTA</span>
            <span>DETALJI</span>
            <span>CENA</span>
            <span>KVOTA</span>
            <span></span>
          </div>

          {karte.map((k) => {
            const badge = k.vrsta === 'VISEDNEVNA'
              ? { label: 'ceo događaj', color: '#3b82f6' }
              : k.vrsta === 'JEDNODNEVNA'
              ? { label: (() => { try { return new Date(k.nazivTipa + 'T00:00:00').toLocaleDateString('sr-Latn', { day: 'numeric', month: 'short' }); } catch { return k.nazivTipa; } })(), color: '#f59e0b' }
              : k.vrsta === 'POJEDINACNA_SESIJA'
              ? { label: 'sesija', color: '#10b981' }
              : { label: 'besplatno', color: '#6b7280' };

            return (
              <div key={k.nazivTipa} className="karte-list-row">
                {/* Vrsta */}
                <div className="karte-vrsta-col">
                  <span className="karte-vrsta-label">{VRSTA_KARTE_LABEL[k.vrsta] || k.vrsta}</span>
                </div>

                {/* Naziv + opis */}
                <div className="karte-detalji-col">
                  <div className="karte-naziv-row">
                    <span className="karte-naziv">{karteTitle(k)}</span>
                    <span className="karte-badge" style={{ borderColor: badge.color, color: badge.color }}>
                      {badge.label}
                    </span>
                  </div>
                  {k.opis && <div className="karte-opis">{k.opis}</div>}
                </div>

                {/* Cena */}
                <div className="karte-cena-col">
                  {k.vrsta !== 'BESPLATNA' ? (
                    <span className="karte-cena">{Number(k.cena).toLocaleString('sr-Latn')} RSD</span>
                  ) : (
                    <span className="karte-cena" style={{ color: 'var(--success)' }}>Besplatno</span>
                  )}
                </div>

                {/* Kvota */}
                <div className="karte-kvota-col">
                  <span className="karte-kvota">{k.kvota}</span>
                  <span className="karte-kvota-hint">mesta</span>
                </div>

                {/* Akcije */}
                <div className="sesija-row-actions">
                  <button className="btn btn-outline btn-sm" onClick={() => setModal(k)}>Izmeni</button>
                  <button className="btn btn-outline btn-sm btn-danger-outline" onClick={() => setDeleteTarget(k)}>Obriši</button>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
