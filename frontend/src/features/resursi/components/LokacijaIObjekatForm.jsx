import { useEffect, useState } from 'react';

const TIP_SALE = ['GLAVNA', 'WORKSHOP', 'PANEL', 'FOAJE'];

const EMPTY = {
  naziv: '',
  adresa: '',
  grad: '',
  drzava: '',
  dodajSalu: true,
  nazivSale: '',
  tipSale: 'GLAVNA',
  kapacitet: '',
  baznaCenaPoDanu: '',
};

export default function LokacijaIObjekatForm({ onSubmit, onCancel = null }) {
  const [form, setForm] = useState(EMPTY);
  const [errors, setErrors] = useState({});

  useEffect(() => {
    setForm(EMPTY);
    setErrors({});
  }, []);

  const validate = () => {
    const next = {};
    if (!form.naziv.trim()) next.naziv = 'Naziv lokacije je obavezan';
    if (!form.adresa.trim()) next.adresa = 'Adresa je obavezna';
    if (!form.grad.trim()) next.grad = 'Grad je obavezan';
    if (!form.drzava.trim()) next.drzava = 'Država je obavezna';

    if (form.dodajSalu) {
      if (!form.nazivSale.trim()) next.nazivSale = 'Naziv sale/objekta je obavezan';
      const kap = Number(form.kapacitet);
      if (!form.kapacitet || Number.isNaN(kap) || kap < 1) {
        next.kapacitet = 'Kapacitet mora biti najmanje 1';
      }
      const cena = Number(form.baznaCenaPoDanu);
      if (!form.baznaCenaPoDanu || Number.isNaN(cena) || cena <= 0) {
        next.baznaCenaPoDanu = 'Osnovna cena mora biti veća od 0';
      }
    }

    setErrors(next);
    return Object.keys(next).length === 0;
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!validate()) return;

    onSubmit({
      lokacija: {
        naziv: form.naziv.trim(),
        adresa: form.adresa.trim(),
        grad: form.grad.trim(),
        drzava: form.drzava.trim(),
      },
      sala: form.dodajSalu
        ? {
            nazivSale: form.nazivSale.trim(),
            tipSale: form.tipSale,
            kapacitet: Number(form.kapacitet),
            baznaCenaPoDanu: Number(form.baznaCenaPoDanu),
          }
        : null,
    });
  };

  const field = (name, label, placeholder, type = 'text') => (
    <div className="form-group">
      <label htmlFor={`lok-${name}`}>{label}</label>
      <input
        id={`lok-${name}`}
        type={type}
        value={form[name]}
        onChange={(e) => setForm({ ...form, [name]: e.target.value })}
        placeholder={placeholder}
      />
      {errors[name] && <span className="field-error">{errors[name]}</span>}
    </div>
  );

  return (
    <form onSubmit={handleSubmit} className="resource-form">
      <p className="form-section-label">Podaci o lokaciji</p>
      {field('naziv', 'Naziv lokacije', 'npr. Kongresni centar')}
      {field('adresa', 'Adresa', 'Ulica i broj')}
      <div className="form-row">
        {field('grad', 'Grad', 'Beograd')}
        {field('drzava', 'Država', 'Srbija')}
      </div>

      <div className="form-divider" />

      <label className="checkbox-label">
        <input
          type="checkbox"
          checked={form.dodajSalu}
          onChange={(e) => setForm({ ...form, dodajSalu: e.target.checked })}
        />
        Dodaj salu / objekat uz lokaciju
      </label>

      {form.dodajSalu && (
        <>
          <p className="form-section-label">Sala / objekat — kapacitet i cena</p>
          {field('nazivSale', 'Naziv sale', 'npr. Sala A')}
          <div className="form-group">
            <label htmlFor="lok-tipSale">Tip sale</label>
            <select
              id="lok-tipSale"
              value={form.tipSale}
              onChange={(e) => setForm({ ...form, tipSale: e.target.value })}
            >
              {TIP_SALE.map((t) => (
                <option key={t} value={t}>
                  {t}
                </option>
              ))}
            </select>
          </div>
          <div className="form-row">
            {field('kapacitet', 'Kapacitet', '100', 'number')}
            {field('baznaCenaPoDanu', 'Osnovna cena / dan (RSD)', '5000', 'number')}
          </div>
        </>
      )}

      <div className="form-actions">
        {onCancel && (
          <button type="button" className="btn btn-outline" onClick={onCancel}>
            Otkaži
          </button>
        )}
        <button type="submit" className="btn btn-primary" style={{ width: 'auto' }}>
          Kreiraj lokaciju
        </button>
      </div>
    </form>
  );
}
