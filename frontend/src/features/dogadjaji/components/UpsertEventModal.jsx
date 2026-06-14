import { useState, useEffect } from 'react';
import Modal from '@/shared/components/Modal';
import api from '@/shared/services/api';

const today = new Date().toISOString().split('T')[0];

function validate(form, isEdit) {
  const errors = {};
  if (!form.naziv.trim())                errors.naziv         = 'Naziv događaja je obavezan.';
  if (!form.lokacijaId)                  errors.lokacijaId    = 'Molimo izaberite lokaciju.';
  if (!form.datumPocetka)                errors.datumPocetka  = 'Datum početka je obavezan.';
  else if (!isEdit && form.datumPocetka < today) errors.datumPocetka = 'Datum početka ne može biti u prošlosti.';
  if (!form.datumZavrsetka)              errors.datumZavrsetka = 'Datum završetka je obavezan.';
  else if (form.datumZavrsetka < form.datumPocetka) errors.datumZavrsetka = 'Datum završetka ne može biti pre datuma početka.';
  if (!form.maksKapacitet)               errors.maksKapacitet = 'Kapacitet je obavezan.';
  else if (Number(form.maksKapacitet) < 1) errors.maksKapacitet = 'Kapacitet mora biti najmanje 1.';
  return errors;
}

export default function UpsertEventModal({ onClose, onCreated, event }) {
  const isEdit = !!event;

  const [form, setForm] = useState({
    naziv:          event?.naziv                    || '',
    lokacijaId:     '',
    datumPocetka:   event?.datumPocetka             || '',
    datumZavrsetka: event?.datumZavrsetka           || '',
    maksKapacitet:  event ? String(event.maksKapacitet) : '',
    opis:           event?.opis                     || '',
  });
  const [fieldErrors, setFieldErrors] = useState({});
  const [lokacije, setLokacije]       = useState([]);
  const [loading, setLoading]         = useState(false);
  const [error, setError]             = useState(null);

  useEffect(() => {
    api.get('/lokacija').then(res => {
      setLokacije(res.data);
      if (isEdit) {
        const match = res.data.find(
          l => l.grad === event.lokacijaGrad && l.drzava === event.lokacijaDrzava
        );
        if (match) setForm(f => ({ ...f, lokacijaId: String(match.lokacijaId) }));
      }
    }).catch(() => {});
  }, []);

  const set = (field) => (e) => {
    setForm(f => ({ ...f, [field]: e.target.value }));
    if (fieldErrors[field]) setFieldErrors(fe => ({ ...fe, [field]: undefined }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const errors = validate(form, isEdit);
    if (Object.keys(errors).length > 0) { setFieldErrors(errors); return; }
    setLoading(true);
    setError(null);
    const payload = { ...form, lokacijaId: Number(form.lokacijaId), maksKapacitet: Number(form.maksKapacitet) };
    try {
      const res = isEdit
        ? await api.put(`/dogadjaj/${event.dogadjajId}`, payload)
        : await api.post('/dogadjaj', payload);
      onCreated(res.data);
      onClose();
    } catch (err) {
      setError(err.response?.data?.error || `Greška pri ${isEdit ? 'izmeni' : 'kreiranju'} događaja.`);
    } finally {
      setLoading(false);
    }
  };

  const F = ({ name }) => fieldErrors[name]
    ? <span className="field-error">⚠ {fieldErrors[name]}</span>
    : null;

  return (
    <Modal boxClassName="modal-box modal-large" onClose={onClose}>
        <div className="modal-header">
          <h3 className="modal-title">{isEdit ? 'Izmeni događaj' : 'Novi događaj'}</h3>
          <button className="modal-close" onClick={onClose}>✕</button>
        </div>

        {error && <div className="error-msg">{error}</div>}

        <form onSubmit={handleSubmit} noValidate>
          <div className="form-group">
            <label>Naziv događaja *</label>
            <input value={form.naziv} onChange={set('naziv')} placeholder="npr. DevConf Spring '26"
              maxLength={200} className={fieldErrors.naziv ? 'input-error' : ''} />
            <F name="naziv" />
          </div>

          <div className="form-group">
            <label>Lokacija *</label>
            <select value={form.lokacijaId} onChange={set('lokacijaId')} className={fieldErrors.lokacijaId ? 'input-error' : ''}>
              <option value="">— Izaberi lokaciju —</option>
              {lokacije.map(l => (
                <option key={l.lokacijaId} value={l.lokacijaId}>{l.naziv} — {l.grad}, {l.drzava}</option>
              ))}
            </select>
            <F name="lokacijaId" />
          </div>

          <div className="form-row">
            <div className="form-group">
              <label>Datum početka *</label>
              <input type="date" value={form.datumPocetka} onChange={set('datumPocetka')}
                min={isEdit ? undefined : today} className={fieldErrors.datumPocetka ? 'input-error' : ''} />
              <F name="datumPocetka" />
            </div>
            <div className="form-group">
              <label>Datum završetka *</label>
              <input type="date" value={form.datumZavrsetka} onChange={set('datumZavrsetka')}
                min={form.datumPocetka || today} className={fieldErrors.datumZavrsetka ? 'input-error' : ''} />
              <F name="datumZavrsetka" />
            </div>
          </div>

          <div className="form-group">
            <label>Maksimalni kapacitet *</label>
            <input type="number" value={form.maksKapacitet} onChange={set('maksKapacitet')}
              placeholder="npr. 500" min={1} className={fieldErrors.maksKapacitet ? 'input-error' : ''} />
            <F name="maksKapacitet" />
          </div>

          <div className="form-group">
            <label>Opis</label>
            <textarea value={form.opis} onChange={set('opis')} placeholder="Kratki opis događaja..." rows={3} />
          </div>

          <div className="modal-actions">
            <button type="button" className="btn btn-outline" onClick={onClose}>Otkaži</button>
            <button type="submit" className="btn btn-primary" style={{ width: 'auto' }} disabled={loading}>
              {loading ? (isEdit ? 'Čuvanje...' : 'Kreiranje...') : (isEdit ? 'Sačuvaj izmene' : 'Kreiraj događaj')}
            </button>
          </div>
        </form>
    </Modal>
  );
}
