import { useMemo, useState } from 'react';
import Modal from '@/shared/components/Modal';

const initialForm = (stavka) => ({
  kategorijaId: stavka?.kategorijaId || '',
  planiraniIznos: stavka?.planiraniIznos || '',
  pragUpozorenja: stavka?.pragUpozorenja || '0.8000',
  pragKriticnog: stavka?.pragKriticnog || '0.9500',
  komentar: stavka?.komentar || '',
});

export default function StavkaBudzetaModal({ stavka, kategorije, postojeceStavke, onClose, onSubmit, loading }) {
  const [form, setForm] = useState(() => initialForm(stavka));
  const [error, setError] = useState(null);

  const zauzeteKategorije = useMemo(
    () => new Set((postojeceStavke || []).map((s) => s.kategorijaId).filter((id) => id !== stavka?.kategorijaId)),
    [postojeceStavke, stavka?.kategorijaId]
  );

  const dostupneKategorije = kategorije.filter((k) => !zauzeteKategorije.has(k.kategorijaId));

  const update = (field, value) => {
    setForm((prev) => ({ ...prev, [field]: value }));
    if (error) setError(null);
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    const pragUpozorenja = Number(form.pragUpozorenja);
    const pragKriticnog = Number(form.pragKriticnog);
    if (pragUpozorenja < 0 || pragUpozorenja > 1 || pragKriticnog < 0 || pragKriticnog > 1) {
      return setError('Pragovi moraju biti u opsegu od 0 do 1.');
    }
    if (pragKriticnog <= pragUpozorenja) {
      return setError('Kritični prag mora biti veći od praga upozorenja.');
    }
    onSubmit({
      kategorijaId: Number(form.kategorijaId),
      planiraniIznos: String(form.planiraniIznos || '0'),
      pragUpozorenja: String(form.pragUpozorenja || '0.8000'),
      pragKriticnog: String(form.pragKriticnog || '0.9500'),
      komentar: form.komentar,
    });
  };

  return (
    <Modal as="form" boxClassName="modal-box" onSubmit={handleSubmit} onClose={onClose}>
        <h3 className="modal-title">{stavka ? 'Izmena stavke budžeta' : 'Nova stavka budžeta'}</h3>
        {error && <div className="error-msg" style={{ marginBottom: '1rem' }}>{error}</div>}

        <div className="form-group">
          <label>Kategorija</label>
          <select
            value={form.kategorijaId}
            onChange={(e) => update('kategorijaId', e.target.value)}
            disabled={!!stavka}
            required
          >
            <option value="">Izaberite kategoriju</option>
            {dostupneKategorije.map((k) => (
              <option key={k.kategorijaId} value={k.kategorijaId}>{k.naziv}</option>
            ))}
          </select>
        </div>

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

        <div className="form-row">
          <div className="form-group">
            <label>Prag upozorenja</label>
            <input
              type="number"
              min="0"
              max="1"
              step="0.0001"
              value={form.pragUpozorenja}
              onChange={(e) => update('pragUpozorenja', e.target.value)}
              required
            />
          </div>
          <div className="form-group">
            <label>Kritični prag</label>
            <input
              type="number"
              min="0"
              max="1"
              step="0.0001"
              value={form.pragKriticnog}
              onChange={(e) => update('pragKriticnog', e.target.value)}
              required
            />
          </div>
        </div>

        <div className="form-group">
          <label>Komentar</label>
          <input
            value={form.komentar}
            onChange={(e) => update('komentar', e.target.value)}
            maxLength={500}
            placeholder="Opcioni komentar"
          />
        </div>

        <div className="modal-actions">
          <button type="button" className="btn btn-outline" onClick={onClose} disabled={loading}>Otkaži</button>
          <button type="submit" className="btn btn-primary" disabled={loading || dostupneKategorije.length === 0}>
            {loading ? 'Čuvanje...' : 'Sačuvaj'}
          </button>
        </div>
    </Modal>
  );
}
