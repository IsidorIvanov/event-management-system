import { useState, useEffect, useCallback } from 'react';
import Modal from '@/shared/components/Modal';
import * as lokacijaApi from '@/features/resursi/services/lokacijaService';
import * as optimizacijaApi from '@/features/optimizacija/services/optimizacijaService';

const TIP_OPTIONS = ['KEYNOTE', 'WORKSHOP', 'PANEL', 'NETWORKING'];

function formatMoneyShort(n) {
  if (n == null) return '—';
  return Number(n).toLocaleString('sr-Latn', { maximumFractionDigits: 0 }) + ' RSD';
}

const EMPTY = {
  naziv: '',
  datum: '',
  vremePocetka: '',
  vremeZavrsetka: '',
  tip: 'KEYNOTE',
  kapacitet: '',
  opis: '',
  lokacijaId: '',
  nazivSale: '',
};

export default function SesijaModal({ dogadjajId, event, sesija, preostaloKapacitet, onClose, onSaved }) {
  const [form, setForm] = useState(() => {
    if (sesija) {
      return {
        naziv: sesija.naziv || '',
        datum: sesija.datum || '',
        vremePocetka: sesija.vremePocetka || '',
        vremeZavrsetka: sesija.vremeZavrsetka || '',
        tip: sesija.tip || 'KEYNOTE',
        kapacitet: sesija.kapacitet || '',
        opis: sesija.opis || '',
        lokacijaId: sesija.lokacijaId || '',
        nazivSale: sesija.nazivSale || '',
      };
    }
    // Sesija uvek mora biti na lokaciji događaja — postavi je podrazumevano.
    return { ...EMPTY, datum: event?.datumPocetka || '', lokacijaId: event?.lokacijaId || '' };
  });

  const [sale, setSale] = useState([]);
  const [error, setError] = useState(null);
  const [saving, setSaving] = useState(false);
  const [predloziSala, setPredloziSala] = useState([]);
  const [upozorenjaOpreme, setUpozorenjaOpreme] = useState([]);

  useEffect(() => {
    lokacijaApi.getLokacije().then((res) => {
      // Sala se bira isključivo iz lokacije događaja — nađi je po ID-u,
      // a ako ID nije dostupan, po nazivu lokacije događaja.
      const lokId = sesija?.lokacijaId ?? event?.lokacijaId;
      const lok =
        res.data.find((l) => l.lokacijaId === lokId) ||
        res.data.find((l) => l.naziv === event?.lokacijaNaziv);
      if (lok) {
        setSale(lok.sale || []);
        setForm((f) => ({ ...f, lokacijaId: lok.lokacijaId }));
      }
    });
  }, []);

  const ucitajPredlogSala = useCallback(async () => {
    if (!form.lokacijaId || !form.datum || !form.vremePocetka || !form.vremeZavrsetka || !form.kapacitet) {
      setPredloziSala([]);
      return;
    }
    try {
      const res = await optimizacijaApi.postPredlogSala({
        dogadjajId,
        sesijaId: sesija?.sesijaId || null,
        lokacijaId: Number(form.lokacijaId),
        datum: form.datum,
        vremePocetka: form.vremePocetka,
        vremeZavrsetka: form.vremeZavrsetka,
        tip: form.tip,
        kapacitet: Number(form.kapacitet),
      });
      setPredloziSala(res.data.predlozi || []);
      setUpozorenjaOpreme(res.data.upozorenjaOpreme || []);
    } catch {
      setPredloziSala([]);
    }
  }, [dogadjajId, sesija?.sesijaId, form]);

  useEffect(() => {
    const t = setTimeout(ucitajPredlogSala, 400);
    return () => clearTimeout(t);
  }, [ucitajPredlogSala]);

  const set = (field) => (e) =>
    setForm((f) => ({ ...f, [field]: e.target.value }));

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    if (!form.lokacijaId || !form.nazivSale) {
      setError('Izaberite lokaciju i salu.');
      return;
    }
    if (preostaloKapacitet != null && Number(form.kapacitet) > preostaloKapacitet) {
      setError(`Kapacitet sesije prelazi preostali kapacitet događaja (${preostaloKapacitet}).`);
      return;
    }
    setSaving(true);
    try {
      await onSaved({
        dogadjajId,
        lokacijaId: Number(form.lokacijaId),
        nazivSale: form.nazivSale,
        naziv: form.naziv,
        datum: form.datum,
        vremePocetka: form.vremePocetka,
        vremeZavrsetka: form.vremeZavrsetka,
        tip: form.tip,
        kapacitet: Number(form.kapacitet),
        opis: form.opis || null,
      });
    } catch (err) {
      const data = err.response?.data;
      setError(data?.details ? Object.values(data.details).join(', ') : data?.error || err.message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <Modal boxClassName="modal-card modal-card-wide" style={{ maxWidth: 600 }} onClose={onClose}>
        <h3>{sesija ? 'Izmeni sesiju' : 'Dodaj sesiju'}</h3>

        {error && <div className="error-msg" style={{ marginBottom: '1rem' }}>{error}</div>}

        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '0.85rem' }}>
          <div className="form-group">
            <label>Naziv sesije *</label>
            <input className="form-control" value={form.naziv} onChange={set('naziv')} required maxLength={200} />
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
            <div className="form-group">
              <label>Datum *</label>
              <input type="date" className="form-control" value={form.datum} onChange={set('datum')} required
                min={event?.datumPocetka} max={event?.datumZavrsetka} />
            </div>
            <div className="form-group">
              <label>Tip *</label>
              <select className="form-control" value={form.tip} onChange={set('tip')}>
                {TIP_OPTIONS.map((t) => <option key={t} value={t}>{t}</option>)}
              </select>
            </div>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
            <div className="form-group">
              <label>Vreme početka *</label>
              <input type="time" className="form-control" value={form.vremePocetka} onChange={set('vremePocetka')} required />
            </div>
            <div className="form-group">
              <label>Vreme završetka *</label>
              <input type="time" className="form-control" value={form.vremeZavrsetka} onChange={set('vremeZavrsetka')} required />
            </div>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
            <div className="form-group">
              <label>Lokacija *</label>
              <input
                className="form-control"
                value={event?.lokacijaNaziv || ''}
                disabled
                title="Sesija se održava na lokaciji događaja"
              />
            </div>
            <div className="form-group">
              <label>Sala *</label>
              <select className="form-control" value={form.nazivSale} onChange={set('nazivSale')} required disabled={!sale.length}>
                <option value="">— izaberi salu —</option>
                {sale.map((s) => (
                  <option key={s.nazivSale} value={s.nazivSale}>{s.nazivSale}</option>
                ))}
              </select>
              {predloziSala.length > 0 && (
                <div style={{ marginTop: '0.5rem', fontSize: '0.85rem' }}>
                  <strong>Predložene sale:</strong>
                  <ul style={{ margin: '0.35rem 0 0', paddingLeft: '1.1rem' }}>
                    {predloziSala.slice(0, 3).map((p) => (
                      <li key={p.nazivSale}>
                        <button
                          type="button"
                          className="btn-link"
                          style={{ background: 'none', border: 'none', padding: 0, color: 'var(--accent)', cursor: 'pointer' }}
                          onClick={() => setForm((f) => ({ ...f, nazivSale: p.nazivSale }))}
                        >
                          {p.nazivSale}
                        </button>
                        {' '}{p.dostupna ? '✓ slobodna' : '✗ zauzeta'} — skor {p.skor}
                        {p.predlozenaCenaPoDanu != null && (
                          <span style={{ color: 'var(--text-secondary)' }}>
                            {' '}· {formatMoneyShort(p.predlozenaCenaPoDanu)}/dan
                            {p.baznaCenaPoDanu != null && p.predlozenaCenaPoDanu > p.baznaCenaPoDanu
                              ? ` (bazna ${formatMoneyShort(p.baznaCenaPoDanu)})`
                              : ''}
                          </span>
                        )}
                        {p.obrazlozenje && (
                          <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginTop: '0.15rem' }}>
                            {p.obrazlozenje}
                          </div>
                        )}
                      </li>
                    ))}
                  </ul>
                </div>
              )}
              {upozorenjaOpreme.length > 0 && (
                <ul className="validacija-lista" style={{ marginTop: '0.5rem' }}>
                  {upozorenjaOpreme.map((u, i) => <li key={i}>{u}</li>)}
                </ul>
              )}
            </div>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
            <div className="form-group">
              <label>Kapacitet *</label>
              <input type="number" className="form-control" value={form.kapacitet} onChange={set('kapacitet')}
                required min={1} max={preostaloKapacitet ?? undefined} />
              {preostaloKapacitet != null && (
                <small className="form-text">Preostali kapacitet događaja: {preostaloKapacitet}</small>
              )}
            </div>
          </div>

          <div className="form-group">
            <label>Opis</label>
            <textarea className="form-control" rows={3} value={form.opis} onChange={set('opis')} />
          </div>

          <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'flex-end', marginTop: '0.5rem' }}>
            <button type="button" className="btn btn-outline" onClick={onClose}>Otkaži</button>
            <button type="submit" className="btn btn-primary" disabled={saving} style={{ width: 'auto' }}>
              {saving ? 'Čuvanje...' : sesija ? 'Sačuvaj izmene' : 'Dodaj sesiju'}
            </button>
          </div>
        </form>
    </Modal>
  );
}





