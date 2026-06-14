import { useState, useEffect } from 'react';
import Modal from '@/shared/components/Modal';
import * as sesijaApi from '@/features/dogadjaji/services/sesijaService';

const VRSTA_OPTIONS = ['VISEDNEVNA', 'JEDNODNEVNA', 'POJEDINACNA_SESIJA', 'BESPLATNA'];
export const VRSTA_LABEL = {
  VISEDNEVNA: 'Višednevna',
  JEDNODNEVNA: 'Jednodnevna',
  POJEDINACNA_SESIJA: 'Pojedinačna sesija',
  BESPLATNA: 'Besplatna',
};

const formatDate = (s) =>
  s
    ? new Date(s + 'T00:00:00').toLocaleDateString('sr-Latn', {
        day: '2-digit',
        month: 'long',
        year: 'numeric',
      })
    : '—';

/** Generiše sve datume između datumPocetka i datumZavrsetka (inkluzivno) */
function buildEventDays(datumPocetka, datumZavrsetka) {
  const days = [];
  if (!datumPocetka || !datumZavrsetka) return days;
  let d = new Date(datumPocetka + 'T00:00:00');
  const end = new Date(datumZavrsetka + 'T00:00:00');
  while (d <= end) {
    const yyyy = d.getFullYear();
    const mm = String(d.getMonth() + 1).padStart(2, '0');
    const dd = String(d.getDate()).padStart(2, '0');
    days.push(`${yyyy}-${mm}-${dd}`);
    d = new Date(d.getFullYear(), d.getMonth(), d.getDate() + 1);
  }
  return days;
}

/**
 * Auto-generiše nazivTipa na osnovu vrste:
 *  VISEDNEVNA          → "VISEDNEVNA"
 *  JEDNODNEVNA         → datum (npr. "2024-05-22")
 *  POJEDINACNA_SESIJA  → sesija.naziv
 *  BESPLATNA           → "BESPLATNA"
 */
function buildNazivTipa(vrsta, datum, sesija) {
  if (vrsta === 'JEDNODNEVNA') return datum;
  if (vrsta === 'POJEDINACNA_SESIJA') return sesija?.naziv ?? '';
  return vrsta;
}

export default function UpsertKarteModal({ dogadjajId, event, tipKarte, onClose, onSaved }) {
  const isEdit = !!tipKarte;

  const [vrsta, setVrsta] = useState(tipKarte?.vrsta || 'VISEDNEVNA');
  const [datum, setDatum] = useState('');           // za JEDNODNEVNA
  const [selectedSesija, setSelectedSesija] = useState(null); // { sesijaId, naziv, ... }
  const [sesije, setSesije] = useState([]);
  const [cena, setCena] = useState(tipKarte?.cena ?? '');
  const [kvota, setKvota] = useState(tipKarte?.kvota ?? '');
  const [errors, setErrors] = useState({});
  const [saving, setSaving] = useState(false);

  const eventDays = buildEventDays(event?.datumPocetka, event?.datumZavrsetka);

  // Kada se vrsta promeni na POJEDINACNA_SESIJA → učitaj sesije
  useEffect(() => {
    if (vrsta === 'POJEDINACNA_SESIJA' && sesije.length === 0) {
      sesijaApi
        .getSesijeByDogadjaj(dogadjajId)
        .then((r) => setSesije(r.data))
        .catch(() => {});
    }
  }, [vrsta, dogadjajId]);

  // U edit modu — popuni datum/sesiju iz nazivTipa
  useEffect(() => {
    if (!isEdit || !tipKarte) return;
    if (tipKarte.vrsta === 'JEDNODNEVNA') {
      setDatum(tipKarte.nazivTipa);
    } else if (tipKarte.vrsta === 'POJEDINACNA_SESIJA') {
      sesijaApi.getSesijeByDogadjaj(dogadjajId).then((r) => {
        setSesije(r.data);
        const match = r.data.find((s) => s.naziv === tipKarte.nazivTipa);
        if (match) setSelectedSesija(match);
      });
    }
  }, [isEdit]);

  const validate = () => {
    const e = {};
    if (vrsta === 'JEDNODNEVNA' && !datum) e.datum = 'Izaberite dan važenja.';
    if (vrsta === 'POJEDINACNA_SESIJA' && !selectedSesija) e.sesija = 'Izaberite sesiju.';
    if (vrsta !== 'BESPLATNA') {
      if (cena === '' || isNaN(Number(cena)) || Number(cena) < 0)
        e.cena = 'Unesite ispravnu cenu (≥ 0).';
    }
    if (!kvota || isNaN(Number(kvota)) || Number(kvota) < 1)
      e.kvota = 'Kvota mora biti najmanje 1.';
    return e;
  };

  const handleSubmit = async (ev) => {
    ev.preventDefault();
    const errs = validate();
    if (Object.keys(errs).length) { setErrors(errs); return; }
    setSaving(true);
    try {
      const nazivTipa = isEdit
        ? tipKarte.nazivTipa
        : buildNazivTipa(vrsta, datum, selectedSesija);

      let opis = tipKarte?.opis || '';
      if (!isEdit) {
        if (vrsta === 'VISEDNEVNA') opis = 'Pristup svakoj sesiji tokom događaja';
        else if (vrsta === 'JEDNODNEVNA') opis = `Pristup svim sesijama ${formatDate(datum)}`;
        else if (vrsta === 'POJEDINACNA_SESIJA') opis = `Pristup sesiji: ${selectedSesija.naziv}`;
        else if (vrsta === 'BESPLATNA') opis = 'Besplatna ulaznica — pristup događaju';
      }

      await onSaved({
        dogadjajId,
        nazivTipa,
        vrsta,
        cena: vrsta === 'BESPLATNA' ? 0 : parseFloat(cena),
        kvota: parseInt(kvota),
        opis,
      });
    } finally {
      setSaving(false);
    }
  };

  return (
    <Modal boxClassName="modal-box modal-large" style={{ textAlign: 'left', maxWidth: 520 }} onClose={onClose}>
        {/* Header */}
        <div className="modal-header">
          <h3 className="modal-title">{isEdit ? 'Izmeni tip karte' : 'Dodaj tip karte'}</h3>
          <button className="modal-close" onClick={onClose}>✕</button>
        </div>

        <form
          onSubmit={handleSubmit}
          style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}
        >
          {/* Vrsta */}
          <div className="form-group">
            <label className="form-label">Vrsta karte *</label>
            <select
              className="form-input"
              value={vrsta}
              onChange={(e) => { setVrsta(e.target.value); setErrors({}); }}
              disabled={isEdit}
            >
              {VRSTA_OPTIONS.map((v) => (
                <option key={v} value={v}>{VRSTA_LABEL[v]}</option>
              ))}
            </select>
            {isEdit && (
              <span className="form-hint">Vrsta karte se ne može menjati.</span>
            )}
          </div>

          {/* JEDNODNEVNA — picker dana */}
          {vrsta === 'JEDNODNEVNA' && (
            <div className="form-group">
              <label className="form-label">Dan važenja *</label>
              <select
                className={`form-input${errors.datum ? ' input-error' : ''}`}
                value={datum}
                onChange={(e) => setDatum(e.target.value)}
                disabled={isEdit}
              >
                <option value="">— Izaberite dan —</option>
                {eventDays.map((d) => (
                  <option key={d} value={d}>{formatDate(d)}</option>
                ))}
              </select>
              {errors.datum && <span className="form-error">{errors.datum}</span>}
            </div>
          )}

          {/* POJEDINACNA_SESIJA — picker sesije */}
          {vrsta === 'POJEDINACNA_SESIJA' && (
            <div className="form-group">
              <label className="form-label">Sesija *</label>
              <select
                className={`form-input${errors.sesija ? ' input-error' : ''}`}
                value={selectedSesija?.sesijaId ?? ''}
                onChange={(e) => {
                  const s = sesije.find((s) => String(s.sesijaId) === e.target.value);
                  setSelectedSesija(s || null);
                }}
                disabled={isEdit}
              >
                <option value="">— Izaberite sesiju —</option>
                {sesije.map((s) => (
                  <option key={s.sesijaId} value={s.sesijaId}>
                    {s.naziv} · {s.datum} {s.vremePocetka?.slice(0, 5)}–{s.vremeZavrsetka?.slice(0, 5)}
                  </option>
                ))}
              </select>
              {errors.sesija && <span className="form-error">{errors.sesija}</span>}
            </div>
          )}

          {/* Cena + Kvota */}
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: vrsta === 'BESPLATNA' ? '1fr' : '1fr 1fr',
              gap: '1rem',
            }}
          >
            {vrsta !== 'BESPLATNA' ? (
              <div className="form-group">
                <label className="form-label">Cena (RSD) *</label>
                <input
                  className={`form-input${errors.cena ? ' input-error' : ''}`}
                  type="number"
                  min="0"
                  step="0.01"
                  value={cena}
                  onChange={(e) => setCena(e.target.value)}
                />
                {errors.cena && <span className="form-error">{errors.cena}</span>}
              </div>
            ) : (
              <div
                className="form-group"
                style={{
                  background: 'var(--success-subtle)',
                  border: '1px solid var(--success)',
                  borderRadius: 'var(--radius-sm)',
                  padding: '0.75rem 1rem',
                  color: 'var(--success)',
                  fontSize: '0.9rem',
                  fontWeight: 600,
                }}
              >
                🎟️ Besplatna ulaznica — cena se automatski postavlja na 0 RSD
              </div>
            )}

            <div className="form-group">
              <label className="form-label">Kvota (broj mesta) *</label>
              <input
                className={`form-input${errors.kvota ? ' input-error' : ''}`}
                type="number"
                min="1"
                value={kvota}
                onChange={(e) => setKvota(e.target.value)}
              />
              {errors.kvota && <span className="form-error">{errors.kvota}</span>}
            </div>
          </div>

          {/* Opis */}

          <div className="modal-actions">
            <button type="button" className="btn btn-outline" onClick={onClose}>Otkaži</button>
            <button type="submit" className="btn btn-primary" disabled={saving}>
              {saving ? 'Čuvanje...' : isEdit ? 'Sačuvaj izmene' : 'Dodaj kartu'}
            </button>
          </div>
        </form>
    </Modal>
  );
}
