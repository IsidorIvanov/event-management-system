import { useState, useEffect } from 'react';
import Modal from '@/shared/components/Modal';
import * as sesijaApi from '@/features/dogadjaji/services/sesijaService';

const EMPTY = {
  ime: '',
  prezime: '',
  pozicija: '',
  kompanija: '',
  biografija: '',
  email: '',
  honorar: '',
};

export default function UpsertGovornikModal({ govornik, dogadjajId, onClose, onSaved }) {
  const isEdit = !!govornik;
  const [form, setForm] = useState(() => {
    if (govornik) {
      return {
        ime: govornik.ime || '',
        prezime: govornik.prezime || '',
        pozicija: govornik.pozicija || '',
        kompanija: govornik.kompanija || '',
        biografija: govornik.biografija || '',
        email: govornik.email || '',
        honorar: govornik.honorar ?? '',
      };
    }
    return { ...EMPTY };
  });

  const [sesije, setSesije] = useState([]);
  const [selectedSesijaIds, setSelectedSesijaIds] = useState(
    govornik?.sesijaIds ? new Set(govornik.sesijaIds) : new Set()
  );
  const [error, setError] = useState(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (dogadjajId) {
      sesijaApi.getSesijeByDogadjaj(dogadjajId).then((res) => setSesije(res.data));
    }
  }, [dogadjajId]);

  const set = (field) => (e) =>
    setForm((f) => ({ ...f, [field]: e.target.value }));

  const toggleSesija = (id) => {
    setSelectedSesijaIds((prev) => {
      const next = new Set(prev);
      next.has(id) ? next.delete(id) : next.add(id);
      return next;
    });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    setSaving(true);
    try {
      const payload = {
        ...form,
        honorar: form.honorar === '' ? 0 : Number(form.honorar),
        sesijaIds: [...selectedSesijaIds],
      };
      await onSaved(payload, [...selectedSesijaIds]);
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Greška pri čuvanju govornika.');
    } finally {
      setSaving(false);
    }
  };

  return (
    <Modal boxClassName="modal-sheet" style={{ maxWidth: 560, maxHeight: '90vh', overflowY: 'auto' }} onClose={onClose}>
        <div className="modal-sheet-header">
          <div>
            <div className="modal-sheet-sub">{isEdit ? 'Izmena govornika' : 'Novi govornik'}</div>
            <h2 className="modal-sheet-title">{isEdit ? 'Izmeni govornika' : 'Dodaj govornika'}</h2>
            <p className="modal-sheet-desc">
              Govornici se prikazuju na tabu govornika i na svakoj sesiji kojoj su dodeljeni.
            </p>
          </div>
          <button className="modal-sheet-close" onClick={onClose}>✕ Zatvori</button>
        </div>

        <form onSubmit={handleSubmit} className="modal-sheet-form">
          {/* Name row */}
          <div className="form-row-2">
            <div className="form-group">
              <label>Ime *</label>
              <input
                className="form-input"
                placeholder="npr. Mara"
                value={form.ime}
                onChange={set('ime')}
                required
              />
            </div>
            <div className="form-group">
              <label>Prezime *</label>
              <input
                className="form-input"
                placeholder="npr. Okafor"
                value={form.prezime}
                onChange={set('prezime')}
                required
              />
            </div>
          </div>

          {/* Job / Company row */}
          <div className="form-row-2">
            <div className="form-group">
              <label>Pozicija</label>
              <input
                className="form-input"
                placeholder="npr. Vođa dizajna"
                value={form.pozicija}
                onChange={set('pozicija')}
              />
            </div>
            <div className="form-group">
              <label>Kompanija / organizacija</label>
              <input
                className="form-input"
                placeholder="npr. Fjordline"
                value={form.kompanija}
                onChange={set('kompanija')}
              />
            </div>
          </div>

          {/* Bio */}
          <div className="form-group">
            <div className="bio-label-row">
              <label style={{ marginBottom: 0 }}>Kratka biografija</label>
              <span className="bio-char-count">{form.biografija.length} / 500</span>
            </div>
            <textarea
              className="form-input"
              placeholder="Jedan ili dva pasusa koja predstavljaju govornika učesnicima."
              value={form.biografija}
              onChange={set('biografija')}
              rows={4}
              maxLength={500}
            />
          </div>

          {/* Email / Honorar */}
          <div className="form-row-2">
            <div className="form-group">
              <label>Email (privatno)</label>
              <input
                className="form-input"
                type="email"
                placeholder="govornik@primer.com"
                value={form.email}
                onChange={set('email')}
              />
            </div>
            <div className="form-group">
              <label>Honorar (RSD)</label>
              <input
                className="form-input"
                type="number"
                min="0"
                step="0.01"
                placeholder="0.00"
                value={form.honorar}
                onChange={set('honorar')}
              />
            </div>
          </div>

          {/* Assigned sessions */}
          {sesije.length > 0 && (
            <div className="form-group">
              <div className="sessions-label-row">
                <label style={{ marginBottom: 0 }}>Dodeljene sesije</label>
                {selectedSesijaIds.size > 0 && (
                  <span className="sessions-count-badge">{selectedSesijaIds.size} izabrano</span>
                )}
              </div>
              <div className="sesija-checkbox-list">
                {sesije.map((s) => (
                  <label key={s.sesijaId} className="sesija-checkbox-item">
                    <div className="sesija-checkbox-inner">
                      <input
                        type="checkbox"
                        checked={selectedSesijaIds.has(s.sesijaId)}
                        onChange={() => toggleSesija(s.sesijaId)}
                      />
                      <span className="sesija-checkbox-naziv">{s.naziv}</span>
                      <span className="sesija-checkbox-time">
                        {s.vremePocetka?.slice(0, 5)} – {s.vremeZavrsetka?.slice(0, 5)}
                      </span>
                    </div>
                  </label>
                ))}
              </div>
              <p className="form-hint">
                Izaberite sesije koje ovaj govornik drži. Može se kasnije izmeniti sa taba sesija.
              </p>
            </div>
          )}

          {error && <p className="form-error">{error}</p>}

          <div className="modal-sheet-footer">
            <button type="button" className="btn btn-outline" onClick={onClose}>
              Otkaži
            </button>
            <button type="submit" className="btn btn-primary" disabled={saving}>
              {saving ? 'Čuvanje...' : isEdit ? 'Sačuvaj izmene' : 'Dodaj govornika'}
            </button>
          </div>
        </form>
    </Modal>
  );
}
