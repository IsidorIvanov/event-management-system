import { useEffect, useState } from 'react';

const TIP_SALE = ['GLAVNA', 'WORKSHOP', 'PANEL', 'FOAJE'];

const EMPTY = {
  nazivSale: '',
  tipSale: 'GLAVNA',
  kapacitet: '',
  baznaCenaPoDanu: '',
};

export default function SalaForm({
  lokacijaId,
  initial,
  isEdit,
  onSubmit,
  onCancel,
  submitLabel = 'Sačuvaj salu',
}) {
  const [form, setForm] = useState(EMPTY);
  const [errors, setErrors] = useState({});

  useEffect(() => {
    if (initial) {
      setForm({
        nazivSale: initial.nazivSale || '',
        tipSale: initial.tipSale || 'GLAVNA',
        kapacitet: String(initial.kapacitet ?? ''),
        baznaCenaPoDanu: String(initial.baznaCenaPoDanu ?? ''),
      });
    } else {
      setForm(EMPTY);
    }
    setErrors({});
  }, [initial]);

  const validate = () => {
    const next = {};
    if (!isEdit) {
      if (!form.nazivSale.trim()) next.nazivSale = 'Naziv sale je obavezan';
      else if (form.nazivSale.length > 100) next.nazivSale = 'Maksimalno 100 karaktera';
    }
    const kap = Number(form.kapacitet);
    if (!form.kapacitet || Number.isNaN(kap) || kap < 1) next.kapacitet = 'Kapacitet mora biti najmanje 1';
    const cena = Number(form.baznaCenaPoDanu);
    if (!form.baznaCenaPoDanu || Number.isNaN(cena) || cena <= 0) {
      next.baznaCenaPoDanu = 'Cena mora biti veća od 0';
    }
    setErrors(next);
    return Object.keys(next).length === 0;
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!validate()) return;
    const payload = {
      lokacijaId,
      nazivSale: isEdit ? initial.nazivSale : form.nazivSale.trim(),
      tipSale: form.tipSale,
      kapacitet: Number(form.kapacitet),
      baznaCenaPoDanu: Number(form.baznaCenaPoDanu),
    };
    if (isEdit) {
      onSubmit({
        tipSale: payload.tipSale,
        kapacitet: payload.kapacitet,
        baznaCenaPoDanu: payload.baznaCenaPoDanu,
      });
    } else {
      onSubmit(payload);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="resource-form">
      {!isEdit && (
        <div className="form-group">
          <label htmlFor="nazivSale">Naziv sale</label>
          <input
            id="nazivSale"
            value={form.nazivSale}
            onChange={(e) => setForm({ ...form, nazivSale: e.target.value })}
            placeholder="npr. Sala A"
          />
          {errors.nazivSale && <span className="field-error">{errors.nazivSale}</span>}
        </div>
      )}
      <div className="form-group">
        <label htmlFor="tipSale">Tip sale</label>
        <select
          id="tipSale"
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
        <div className="form-group">
          <label htmlFor="kapacitet">Kapacitet</label>
          <input
            id="kapacitet"
            type="number"
            min="1"
            value={form.kapacitet}
            onChange={(e) => setForm({ ...form, kapacitet: e.target.value })}
          />
          {errors.kapacitet && <span className="field-error">{errors.kapacitet}</span>}
        </div>
        <div className="form-group">
          <label htmlFor="baznaCenaPoDanu">Osnovna cena / dan (RSD)</label>
          <input
            id="baznaCenaPoDanu"
            type="number"
            min="0.01"
            step="0.01"
            value={form.baznaCenaPoDanu}
            onChange={(e) => setForm({ ...form, baznaCenaPoDanu: e.target.value })}
          />
          {errors.baznaCenaPoDanu && <span className="field-error">{errors.baznaCenaPoDanu}</span>}
        </div>
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
