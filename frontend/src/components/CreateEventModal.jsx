import { useState, useEffect } from 'react';
import api from '../services/api';

const EMPTY_FORM = {
  naziv: '',
  lokacijaId: '',
  datumPocetka: '',
  datumZavrsetka: '',
  maksKapacitet: '',
  opis: '',
};

export default function CreateEventModal({ onClose, onCreated }) {
  const [form, setForm]         = useState(EMPTY_FORM);
  const [lokacije, setLokacije] = useState([]);
  const [loading, setLoading]   = useState(false);
  const [error, setError]       = useState(null);

  useEffect(() => {
    api.get('/lokacija').then(res => setLokacije(res.data)).catch(() => {});
  }, []);

  const set = (field) => (e) => setForm(f => ({ ...f, [field]: e.target.value }));

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    try {
      const res = await api.post('/dogadjaj', {
        ...form,
        lokacijaId: Number(form.lokacijaId),
        maksKapacitet: Number(form.maksKapacitet),
      });
      onCreated(res.data);
      onClose();
    } catch (err) {
      setError(err.response?.data?.error || 'Greška pri kreiranju događaja.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-box modal-large" onClick={e => e.stopPropagation()}>
        <div className="modal-header">
          <h3 className="modal-title">Novi događaj</h3>
          <button className="modal-close" onClick={onClose}>✕</button>
        </div>

        {error && <div className="error-msg">{error}</div>}

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label>Naziv događaja *</label>
            <input value={form.naziv} onChange={set('naziv')} placeholder="npr. DevConf Spring '26" required maxLength={200} />
          </div>

          <div className="form-group">
            <label>Lokacija *</label>
            <select value={form.lokacijaId} onChange={set('lokacijaId')} required>
              <option value="">— Izaberi lokaciju —</option>
              {lokacije.map(l => (
                <option key={l.lokacijaId} value={l.lokacijaId}>
                  {l.naziv} — {l.grad}, {l.drzava}
                </option>
              ))}
            </select>
          </div>

          <div className="form-row">
            <div className="form-group">
              <label>Datum početka *</label>
              <input type="date" value={form.datumPocetka} onChange={set('datumPocetka')} required />
            </div>
            <div className="form-group">
              <label>Datum završetka *</label>
              <input type="date" value={form.datumZavrsetka} onChange={set('datumZavrsetka')} required />
            </div>
          </div>

          <div className="form-group">
            <label>Maksimalni kapacitet *</label>
            <input type="number" value={form.maksKapacitet} onChange={set('maksKapacitet')} placeholder="npr. 500" required min={1} />
          </div>

          <div className="form-group">
            <label>Opis</label>
            <textarea value={form.opis} onChange={set('opis')} placeholder="Kratki opis događaja..." rows={3} />
          </div>

          <div className="modal-actions">
            <button type="button" className="btn btn-outline" onClick={onClose}>Otkaži</button>
            <button type="submit" className="btn btn-primary" style={{ width: 'auto' }} disabled={loading}>
              {loading ? 'Kreiranje...' : 'Kreiraj događaj'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

