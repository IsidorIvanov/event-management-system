import { useEffect, useState } from 'react';

const EMPTY = { naziv: '', adresa: '', grad: '', drzava: '' };

export default function LokacijaForm({ initial, onSubmit, onCancel, submitLabel = 'Sačuvaj' }) {
  const [form, setForm] = useState(EMPTY);
  const [errors, setErrors] = useState({});

  useEffect(() => {
    if (initial) {
      setForm({
        naziv: initial.naziv || '',
        adresa: initial.adresa || '',
        grad: initial.grad || '',
        drzava: initial.drzava || '',
      });
    } else {
      setForm(EMPTY);
    }
    setErrors({});
  }, [initial]);

  const validate = () => {
    const next = {};
    if (!form.naziv.trim()) next.naziv = 'Naziv je obavezan';
    else if (form.naziv.length > 200) next.naziv = 'Maksimalno 200 karaktera';
    if (!form.adresa.trim()) next.adresa = 'Adresa je obavezna';
    if (!form.grad.trim()) next.grad = 'Grad je obavezan';
    if (!form.drzava.trim()) next.drzava = 'Država je obavezna';
    setErrors(next);
    return Object.keys(next).length === 0;
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!validate()) return;
    onSubmit(form);
  };

  const field = (name, label, placeholder) => (
    <div className="form-group">
      <label htmlFor={name}>{label}</label>
      <input
        id={name}
        value={form[name]}
        onChange={(e) => setForm({ ...form, [name]: e.target.value })}
        placeholder={placeholder}
      />
      {errors[name] && <span className="field-error">{errors[name]}</span>}
    </div>
  );

  return (
    <form onSubmit={handleSubmit} className="resource-form">
      {field('naziv', 'Naziv lokacije', 'npr. Kongresni centar')}
      {field('adresa', 'Adresa', 'Ulica i broj')}
      <div className="form-row">
        {field('grad', 'Grad', 'Beograd')}
        {field('drzava', 'Država', 'Srbija')}
      </div>
      <div className="form-actions">
        {onCancel && (
          <button type="button" className="btn btn-outline" onClick={onCancel}>
            Otkaži
          </button>
        )}
        <button type="submit" className="btn btn-primary" style={{ width: 'auto' }}>
          {submitLabel}
        </button>
      </div>
    </form>
  );
}
