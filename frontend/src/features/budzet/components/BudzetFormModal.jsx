import { useState } from 'react';
import Modal from '@/shared/components/Modal';

const initialForm = (budzet, selectedDogadjajId) => ({
  dogadjajId: budzet?.dogadjajId || selectedDogadjajId || '',
  nazivBudzeta: budzet?.nazivBudzeta || '',
  planiraniIznos: budzet?.planiraniIznos || '',
  odobreniIznos: budzet?.odobreniIznos || '',
});

export default function BudzetFormModal({ budzet, dogadjaji, selectedDogadjajId, onClose, onSubmit, loading }) {
  const [form, setForm] = useState(() => initialForm(budzet, selectedDogadjajId));

  const update = (field, value) => setForm((prev) => ({ ...prev, [field]: value }));

  const handleSubmit = (e) => {
    e.preventDefault();
    onSubmit({
      ...form,
      dogadjajId: Number(form.dogadjajId),
      planiraniIznos: String(form.planiraniIznos || '0'),
      ...(budzet ? { odobreniIznos: String(form.odobreniIznos || form.planiraniIznos || '0') } : {}),
    });
  };

  return (
    <Modal as="form" boxClassName="modal-box" onSubmit={handleSubmit} onClose={onClose}>
        <h3 className="modal-title">{budzet ? 'Izmena budžeta' : 'Novi budžet'}</h3>

        <div className="form-group">
          <label>Događaj</label>
          <select
            value={form.dogadjajId}
            disabled={!!budzet}
            onChange={(e) => update('dogadjajId', e.target.value)}
            required
          >
            <option value="">Izaberite događaj</option>
            {dogadjaji.map((d) => (
              <option key={d.dogadjajId} value={d.dogadjajId}>
                {d.naziv} ({d.status})
              </option>
            ))}
          </select>
        </div>

        <div className="form-group">
          <label>Naziv budžeta</label>
          <input
            value={form.nazivBudzeta}
            onChange={(e) => update('nazivBudzeta', e.target.value)}
            placeholder="npr. Glavni budžet događaja"
            maxLength={255}
            required
          />
        </div>

        <div className="form-row">
          <div className="form-group">
            <label>Planirani iznos</label>
            <input
              type="number"
              min="0"
              step="0.01"
              value={form.planiraniIznos}
              onChange={(e) => update('planiraniIznos', e.target.value)}
              required
            />
          </div>
          <div className="form-group">
            <label>Odobreni iznos</label>
            <input
              type="number"
              min="0"
              step="0.01"
              value={form.odobreniIznos}
              onChange={(e) => update('odobreniIznos', e.target.value)}
              disabled={!budzet}
              placeholder="automatski = planirani"
            />
          </div>
        </div>

        <div className="modal-actions">
          <button type="button" className="btn btn-outline" onClick={onClose} disabled={loading}>Otkaži</button>
          <button type="submit" className="btn btn-primary" disabled={loading}>
            {loading ? 'Čuvanje...' : 'Sačuvaj'}
          </button>
        </div>
    </Modal>
  );
}
