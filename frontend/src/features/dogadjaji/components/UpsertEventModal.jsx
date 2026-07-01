import { useState, useEffect } from 'react';
import Modal from '@/shared/components/Modal';
import api from '@/shared/services/api';

const today = new Date().toISOString().split('T')[0];

// Predloženi tagovi za brži unos - služe za sistem preporuka događaja
const PREDLOZENI_TAGOVI = [
  'Tehnologija',
  'Biznis',
  'Marketing',
  'Dizajn',
  'Edukacija',
  'Zdravlje',
  'Muzika',
  'Sport',
  'Umetnost',
  'Nauka',
];

// ISO datumi ('YYYY-MM-DD') se mogu porediti leksikografski. Dva intervala se
// preklapaju ako svaki počinje pre (ili na dan) kraja onog drugog.
function pronadjiPreklapanje(pocetak, zavrsetak, zauzetiTermini) {
  if (!pocetak || !zavrsetak) return null;
  return zauzetiTermini.find(
    t => t.datumPocetka <= zavrsetak && t.datumZavrsetka >= pocetak
  ) || null;
}

function validate(form, isEdit, zauzetiTermini) {
  const errors = {};
  if (!form.naziv.trim())                errors.naziv         = 'Naziv događaja je obavezan.';
  if (!form.lokacijaId)                  errors.lokacijaId    = 'Molimo izaberite lokaciju.';
  if (!form.datumPocetka)                errors.datumPocetka  = 'Datum početka je obavezan.';
  else if (!isEdit && form.datumPocetka < today) errors.datumPocetka = 'Datum početka ne može biti u prošlosti.';
  if (!form.datumZavrsetka)              errors.datumZavrsetka = 'Datum završetka je obavezan.';
  else if (form.datumZavrsetka < form.datumPocetka) errors.datumZavrsetka = 'Datum završetka ne može biti pre datuma početka.';
  else {
    const konflikt = pronadjiPreklapanje(form.datumPocetka, form.datumZavrsetka, zauzetiTermini);
    if (konflikt) {
      errors.datumZavrsetka = `Termin se preklapa sa već postojećim događajem na izabranoj lokaciji.`;
    }
  }
  if (!form.maksKapacitet)               errors.maksKapacitet = 'Kapacitet je obavezan.';
  else if (Number(form.maksKapacitet) < 1) errors.maksKapacitet = 'Kapacitet mora biti najmanje 1.';
  return errors;
}

function formatirajDatum(iso) {
  const [g, m, d] = iso.split('-');
  return `${d}.${m}.${g}.`;
}

function formatirajPeriod(pocetak, zavrsetak) {
  return pocetak === zavrsetak
    ? formatirajDatum(pocetak)
    : `${formatirajDatum(pocetak)} – ${formatirajDatum(zavrsetak)}`;
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
    tagovi:         event?.tagovi                    || [],
  });
  const [tagInput, setTagInput]       = useState('');
  const [fieldErrors, setFieldErrors] = useState({});
  const [lokacije, setLokacije]       = useState([]);
  const [zauzetiTermini, setZauzetiTermini] = useState([]);
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

  // Kad se izabere lokacija, dovuci zauzete termine na njoj da se preklapanje
  // prikaže i blokira pre slanja (sam događaj se izuzima pri izmeni).
  useEffect(() => {
    if (!form.lokacijaId) {
      setZauzetiTermini([]);
      return;
    }
    const params = { lokacijaId: form.lokacijaId };
    if (isEdit) params.excludeId = event.dogadjajId;
    api.get('/dogadjaj/zauzeti-termini', { params })
      .then(res => setZauzetiTermini(res.data))
      .catch(() => setZauzetiTermini([]));
  }, [form.lokacijaId]);

  const set = (field) => (e) => {
    setForm(f => ({ ...f, [field]: e.target.value }));
    if (fieldErrors[field]) setFieldErrors(fe => ({ ...fe, [field]: undefined }));
  };

  const dodajTag = (vrednost) => {
    const ocisceno = vrednost.trim();
    if (!ocisceno) return;
    setForm(f =>
      f.tagovi.some(t => t.toLowerCase() === ocisceno.toLowerCase())
        ? f
        : { ...f, tagovi: [...f.tagovi, ocisceno] }
    );
    setTagInput('');
  };

  const ukloniTag = (vrednost) => {
    setForm(f => ({ ...f, tagovi: f.tagovi.filter(t => t !== vrednost) }));
  };

  const handleTagKeyDown = (e) => {
    if (e.key === 'Enter' || e.key === ',') {
      e.preventDefault();
      dodajTag(tagInput);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const errors = validate(form, isEdit, zauzetiTermini);
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

          {form.lokacijaId && zauzetiTermini.length > 0 && (
            <div className="form-group">
              <p className="form-hint">Zauzeti termini na izabranoj lokaciji — izaberite datume van ovih opsega:</p>
              <div className="tag-chips">
                {zauzetiTermini.map(t => {
                  const sukob = !!form.datumPocetka && !!form.datumZavrsetka
                    && t.datumPocetka <= form.datumZavrsetka && t.datumZavrsetka >= form.datumPocetka;
                  return (
                    <span key={t.dogadjajId} className="tag-chip" title={t.naziv}
                      style={sukob ? { background: '#fde2e1', color: '#b42318', borderColor: '#f5b5b0' } : undefined}>
                      {formatirajPeriod(t.datumPocetka, t.datumZavrsetka)}
                    </span>
                  );
                })}
              </div>
            </div>
          )}

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

          <div className="form-group">
            <label>Tagovi (teme događaja)</label>
            <p className="form-hint">Opisuju o čemu se radi na događaju — koriste se za preporuke učesnicima.</p>

            {form.tagovi.length > 0 && (
              <div className="tag-chips">
                {form.tagovi.map(tag => (
                  <span key={tag} className="tag-chip">
                    {tag}
                    <button type="button" onClick={() => ukloniTag(tag)} aria-label={`Ukloni ${tag}`}>×</button>
                  </span>
                ))}
              </div>
            )}

            <input
              value={tagInput}
              onChange={(e) => setTagInput(e.target.value)}
              onKeyDown={handleTagKeyDown}
              onBlur={() => dodajTag(tagInput)}
              placeholder="Upišite tag i pritisnite Enter"
            />

            <div className="tag-suggestions">
              {PREDLOZENI_TAGOVI.map(predlog => {
                const vecDodato = form.tagovi.some(t => t.toLowerCase() === predlog.toLowerCase());
                return (
                  <button
                    key={predlog}
                    type="button"
                    className="tag-suggestion"
                    onClick={() => dodajTag(predlog)}
                    disabled={vecDodato}
                  >
                    {predlog}
                  </button>
                );
              })}
            </div>
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
