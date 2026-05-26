import { useState } from 'react';

const today = () => new Date().toISOString().slice(0, 10);

const initialForm = () => ({
  opis: '',
  iznos: '',
  datumTroska: today(),
  tip: 'RUCNI',
  dobavljacId: '',
});

export default function TrosakModal({ budzet, stavka, onClose, onSubmit, loading }) {
  const [form, setForm] = useState(initialForm);
  const [error, setError] = useState(null);

  const update = (field, value) => {
    setForm((prev) => ({ ...prev, [field]: value }));
    if (error) setError(null);
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!form.opis.trim()) return setError('Opis troška je obavezan.');
    if (Number(form.iznos) < 0) return setError('Iznos troška ne može biti negativan.');
    if (!form.datumTroska) return setError('Datum troška je obavezan.');

    onSubmit({
      budzetId: budzet.budzetId,
      kategorijaId: stavka.kategorijaId,
      dogadjajId: budzet.dogadjajId,
      opis: form.opis.trim(),
      iznos: String(form.iznos || '0'),
      datumTroska: form.datumTroska,
      tip: form.tip,
      dobavljacId: form.dobavljacId ? Number(form.dobavljacId) : null,
    });
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <form className="modal-box" onSubmit={handleSubmit} onClick={(e) => e.stopPropagation()}>
        <h3 className="modal-title">Novi trošak</h3>
        <p className="page-subtitle" style={{ marginTop: 0 }}>
          {budzet.nazivBudzeta} · {stavka.kategorijaNaziv}
        </p>
        {error && <div className="error-msg" style={{ marginBottom: '1rem' }}>{error}</div>}

        <div className="form-group">
          <label>Opis</label>
          <input
            value={form.opis}
            onChange={(e) => update('opis', e.target.value)}
            maxLength={500}
            placeholder="npr. Gotovinska kupovina materijala"
            required
          />
        </div>

        <div className="form-row">
          <div className="form-group">
            <label>Iznos</label>
            <input
              type="number"
              min="0"
              step="0.01"
              value={form.iznos}
              onChange={(e) => update('iznos', e.target.value)}
              required
            />
          </div>
          <div className="form-group">
            <label>Datum troška</label>
            <input
              type="date"
              value={form.datumTroska}
              onChange={(e) => update('datumTroska', e.target.value)}
              required
            />
          </div>
        </div>

        <div className="form-row">
          <div className="form-group">
            <label>Tip troška</label>
            <select value={form.tip} onChange={(e) => update('tip', e.target.value)}>
              <option value="RUCNI">Ručni</option>
              <option value="GOTOVINSKI">Gotovinski</option>
            </select>
          </div>
          <div className="form-group">
            <label>Dobavljač ID</label>
            <input
              type="number"
              min="1"
              value={form.dobavljacId}
              onChange={(e) => update('dobavljacId', e.target.value)}
              placeholder="Opciono"
            />
          </div>
        </div>

        <div className="modal-actions">
          <button type="button" className="btn btn-outline" onClick={onClose} disabled={loading}>Otkaži</button>
          <button type="submit" className="btn btn-primary" disabled={loading}>
            {loading ? 'Čuvanje...' : 'Sačuvaj trošak'}
          </button>
        </div>
      </form>
    </div>
  );
}
