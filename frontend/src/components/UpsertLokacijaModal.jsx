import { useState } from 'react';
import api from '../services/api';

function validate(form) {
  const errors = {};
  if (!form.naziv.trim())   errors.naziv   = 'Naziv lokacije je obavezan.';
  if (!form.adresa.trim())  errors.adresa  = 'Adresa je obavezna.';
  if (!form.grad.trim())    errors.grad    = 'Grad je obavezan.';
  if (!form.drzava.trim())  errors.drzava  = 'Država je obavezna.';
  return errors;
}

export default function UpsertLokacijaModal({ lokacija, onClose, onSaved }) {
  const isEdit = !!lokacija;

  const [form, setForm] = useState({
    naziv:  lokacija?.naziv  || '',
    adresa: lokacija?.adresa || '',
    grad:   lokacija?.grad   || '',
    drzava: lokacija?.drzava || '',
  });
  const [fieldErrors, setFieldErrors] = useState({});
  const [loading, setLoading]         = useState(false);
  const [error, setError]             = useState(null);

  const set = (field) => (e) => {
    setForm(f => ({ ...f, [field]: e.target.value }));
    if (fieldErrors[field]) setFieldErrors(fe => ({ ...fe, [field]: undefined }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const errors = validate(form);
    if (Object.keys(errors).length > 0) { setFieldErrors(errors); return; }
    setLoading(true);
    setError(null);
    try {
      const res = isEdit
        ? await api.put(`/lokacija/${lokacija.lokacijaId}`, form)
        : await api.post('/lokacija', form);
      onSaved(res.data);
      onClose();
    } catch (err) {
      setError(err.response?.data?.error || `Greška pri ${isEdit ? 'izmeni' : 'kreiranju'} lokacije.`);
    } finally {
      setLoading(false);
    }
  };

  const F = ({ name }) => fieldErrors[name]
    ? <span className="field-error">⚠ {fieldErrors[name]}</span>
    : null;

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-box modal-large" onClick={e => e.stopPropagation()}>
        <div className="modal-header">
          <h3 className="modal-title">{isEdit ? 'Izmeni lokaciju' : 'Nova lokacija'}</h3>
          <button className="modal-close" onClick={onClose}>✕</button>
        </div>

        {error && <div className="error-msg">{error}</div>}

        <form onSubmit={handleSubmit} noValidate>
          <div className="form-group">
            <label>Naziv *</label>
            <input value={form.naziv} onChange={set('naziv')} placeholder="npr. Convention Center Berlin"
              maxLength={200} className={fieldErrors.naziv ? 'input-error' : ''} />
            <F name="naziv" />
          </div>

          <div className="form-group">
            <label>Adresa *</label>
            <input value={form.adresa} onChange={set('adresa')} placeholder="npr. Messedamm 22"
              maxLength={200} className={fieldErrors.adresa ? 'input-error' : ''} />
            <F name="adresa" />
          </div>

          <div className="form-row">
            <div className="form-group">
              <label>Grad *</label>
              <input value={form.grad} onChange={set('grad')} placeholder="npr. Berlin"
                maxLength={100} className={fieldErrors.grad ? 'input-error' : ''} />
              <F name="grad" />
            </div>
            <div className="form-group">
              <label>Država *</label>
              <input value={form.drzava} onChange={set('drzava')} placeholder="npr. Nemačka"
                className={fieldErrors.drzava ? 'input-error' : ''} />
              <F name="drzava" />
            </div>
          </div>

          <div className="modal-actions">
            <button type="button" className="btn btn-outline" onClick={onClose}>Otkaži</button>
            <button type="submit" className="btn btn-primary" style={{ width: 'auto' }} disabled={loading}>
              {loading
                ? (isEdit ? 'Čuvanje...' : 'Kreiranje...')
                : (isEdit ? 'Sačuvaj izmene' : 'Kreiraj lokaciju')}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

