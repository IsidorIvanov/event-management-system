import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '@/shared/services/api';
import { useAuth } from '@/features/auth/context/AuthContext';
import { ULOGA_DISPLAY, TIP_DISPLAY } from '@/shared/constants/korisnik';
import { formatDate } from '@/shared/utils/format';
import { useToast } from '@/shared/components/ToastNotification';
import './profil.css';

const PREDLOZENI_INTERESI = [
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

export default function ProfilPage() {
  const { updateUser } = useAuth();
  const navigate = useNavigate();
  const toast = useToast();
  const [profil, setProfil] = useState(null);
  const [form, setForm] = useState(null);
  const [interesInput, setInteresInput] = useState('');
  const [editing, setEditing] = useState(false);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    api
      .get('/profil')
      .then((res) => {
        setProfil(res.data);
        setForm(res.data);
      })
      .catch(() => setError('Greška pri učitavanju profila.'))
      .finally(() => setLoading(false));
  }, []);

  const set = (field) => (e) =>
    setForm((f) => ({ ...f, [field]: e.target.value }));

  const isUcesnik = profil?.tipKorisnika === 'UCESNIK';
  const isKlijent = profil?.tipKorisnika === 'KLIJENT';

  const initials = profil
    ? `${profil.ime?.[0] || ''}${profil.prezime?.[0] || ''}`.toUpperCase()
    : '';
  // Organizacija prikazana u levoj koloni - zavisi od tipa korisnika
  const organizacija = isUcesnik
    ? profil?.kompanija
    : isKlijent
      ? profil?.nazivFirme
      : profil?.uloga && ULOGA_DISPLAY[profil.uloga];

  const dodajInteres = (vrednost) => {
    const ocisceno = vrednost.trim();
    if (!ocisceno) return;
    const lista = form.interesi || [];
    if (!lista.some((i) => i.toLowerCase() === ocisceno.toLowerCase())) {
      setForm((f) => ({ ...f, interesi: [...lista, ocisceno] }));
    }
    setInteresInput('');
  };

  const ukloniInteres = (vrednost) =>
    setForm((f) => ({
      ...f,
      interesi: (f.interesi || []).filter((i) => i !== vrednost),
    }));

  const handleInteresKeyDown = (e) => {
    if (e.key === 'Enter' || e.key === ',') {
      e.preventDefault();
      dodajInteres(interesInput);
    }
  };

  const handleCancel = () => {
    setForm(profil);
    setInteresInput('');
    setError(null);
    setEditing(false);
  };

  const handleSave = async (e) => {
    e.preventDefault();
    setSaving(true);
    setError(null);
    try {
      const payload = {
        ime: form.ime,
        prezime: form.prezime,
        email: form.email,
        telefon: form.telefon || null,
        pozicija: form.pozicija || null,
        nazivFirme: form.nazivFirme || null,
        pib: form.pib || null,
        adresa: form.adresa || null,
        grad: form.grad || null,
        kontaktOsoba: form.kontaktOsoba || null,
        kompanija: form.kompanija || null,
        datumRodjenja: form.datumRodjenja || null,
        interesi: form.interesi || [],
      };
      const res = await api.put('/profil', payload);
      setProfil(res.data);
      setForm(res.data);
      setEditing(false);
      // Osveži ime/prezime/email u sidebar-u i localStorage-u
      updateUser({
        ime: res.data.ime,
        prezime: res.data.prezime,
        email: res.data.email,
      });
      toast('Profil je uspešno sačuvan.', 'success');
    } catch (err) {
      setError(err.response?.data?.error || 'Greška pri čuvanju profila.');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="program-page">
      <div className="program-header-actions">
        <button className="btn btn-outline btn-sm" onClick={() => navigate(-1)}>
          ← Nazad
        </button>
      </div>
      <div className="program-header">
        <div>
          <h1>Moj profil</h1>
          <p className="page-subtitle">pregled i izmena vaših podataka</p>
        </div>
      </div>

      {error && <div className="error-msg">{error}</div>}

      {loading || !form ? (
        <div className="profil-card">
          <p style={{ color: 'var(--text-secondary)' }}>Učitavanje...</p>
        </div>
      ) : (
        <div className="profil-card profil-card-grid">
          <div className="profil-left">
            <div className="profil-avatar-lg">{initials}</div>
            <div className="profil-left-name">{profil.ime} {profil.prezime}</div>
            {organizacija && <div className="profil-left-org">{organizacija}</div>}
          </div>

          <div className="profil-right">
            {!editing ? (
          <>
            <div className="profil-view">
              <ProfilRed label="Email" value={profil.email} />
              <ProfilRed label="Telefon" value={profil.telefon || '—'} />
              <ProfilRed label="Tip naloga" value={TIP_DISPLAY[profil.tipKorisnika]} />
              {profil.uloga && <ProfilRed label="Uloga" value={ULOGA_DISPLAY[profil.uloga]} />}
              {profil.datumRegistracije && (
                <ProfilRed label="Datum registracije" value={formatDate(profil.datumRegistracije)} />
              )}

              {(profil.tipKorisnika === 'ZAPOSLENI' || isUcesnik) && profil.pozicija && (
                <ProfilRed label="Pozicija" value={profil.pozicija} />
              )}

              {isKlijent && (
                <>
                  <ProfilRed label="Naziv firme" value={profil.nazivFirme || '—'} />
                  <ProfilRed label="PIB" value={profil.pib || '—'} />
                  <ProfilRed label="Adresa" value={profil.adresa || '—'} />
                  <ProfilRed label="Grad" value={profil.grad || '—'} />
                  <ProfilRed label="Kontakt osoba" value={profil.kontaktOsoba || '—'} />
                </>
              )}

              {isUcesnik && (
                <>
                  {profil.datumRodjenja && (
                    <ProfilRed label="Datum rođenja" value={formatDate(profil.datumRodjenja)} />
                  )}
                  <div className="profil-red">
                    <span className="profil-label">Interesovanja</span>
                    <span className="profil-value">
                      {profil.interesi?.length > 0 ? (
                        <span className="interesi-tags">
                          {profil.interesi.map((i) => (
                            <span key={i} className="interes-tag">{i}</span>
                          ))}
                        </span>
                      ) : '—'}
                    </span>
                  </div>
                </>
              )}
            </div>

            <div className="modal-actions">
              <button type="button" className="btn btn-primary" style={{ width: 'auto' }} onClick={() => setEditing(true)}>
                Uredi profil
              </button>
            </div>
          </>
        ) : (
          <form onSubmit={handleSave} noValidate>
            <div className="form-row">
              <div className="form-group">
                <label>Ime *</label>
                <input value={form.ime} onChange={set('ime')} required />
              </div>
              <div className="form-group">
                <label>Prezime *</label>
                <input value={form.prezime} onChange={set('prezime')} required />
              </div>
            </div>

            <div className="form-group">
              <label>Email *</label>
              <input type="email" value={form.email} onChange={set('email')} required />
            </div>

            <div className="form-group">
              <label>Telefon</label>
              <input value={form.telefon || ''} onChange={set('telefon')} placeholder="+381..." />
            </div>

            {(profil.tipKorisnika === 'ZAPOSLENI' || isUcesnik) && (
              <div className="form-group">
                <label>Pozicija</label>
                <input value={form.pozicija || ''} onChange={set('pozicija')} />
              </div>
            )}

            {isKlijent && (
              <>
                <div className="form-row">
                  <div className="form-group">
                    <label>Naziv firme</label>
                    <input value={form.nazivFirme || ''} onChange={set('nazivFirme')} />
                  </div>
                  <div className="form-group">
                    <label>PIB</label>
                    <input value={form.pib || ''} onChange={set('pib')} />
                  </div>
                </div>
                <div className="form-row">
                  <div className="form-group">
                    <label>Adresa</label>
                    <input value={form.adresa || ''} onChange={set('adresa')} />
                  </div>
                  <div className="form-group">
                    <label>Grad</label>
                    <input value={form.grad || ''} onChange={set('grad')} />
                  </div>
                </div>
                <div className="form-group">
                  <label>Kontakt osoba</label>
                  <input value={form.kontaktOsoba || ''} onChange={set('kontaktOsoba')} />
                </div>
              </>
            )}

            {isUcesnik && (
              <>
                <div className="form-row">
                  <div className="form-group">
                    <label>Kompanija</label>
                    <input value={form.kompanija || ''} onChange={set('kompanija')} />
                  </div>
                  <div className="form-group">
                    <label>Datum rođenja</label>
                    <input type="date" value={form.datumRodjenja || ''} onChange={set('datumRodjenja')} />
                  </div>
                </div>

                <div className="form-group">
                  <label>Interesovanja</label>
                  <p className="page-subtitle" style={{ margin: '0 0 0.5rem', fontSize: '0.8rem' }}>
                    Koristimo ih da vam preporučimo događaje koji vas zanimaju.
                  </p>

                  {form.interesi?.length > 0 && (
                    <div className="interesi-tags">
                      {form.interesi.map((interes) => (
                        <span key={interes} className="interes-tag">
                          {interes}
                          <button type="button" onClick={() => ukloniInteres(interes)} aria-label={`Ukloni ${interes}`}>×</button>
                        </span>
                      ))}
                    </div>
                  )}

                  <input
                    value={interesInput}
                    onChange={(e) => setInteresInput(e.target.value)}
                    onKeyDown={handleInteresKeyDown}
                    onBlur={() => dodajInteres(interesInput)}
                    placeholder="Upišite interes i pritisnite Enter"
                  />

                  <div className="interesi-suggestions">
                    {PREDLOZENI_INTERESI.map((predlog) => {
                      const vecDodato = (form.interesi || []).some(
                        (i) => i.toLowerCase() === predlog.toLowerCase()
                      );
                      return (
                        <button
                          key={predlog}
                          type="button"
                          className="interes-suggestion"
                          onClick={() => dodajInteres(predlog)}
                          disabled={vecDodato}
                        >
                          + {predlog}
                        </button>
                      );
                    })}
                  </div>
                </div>
              </>
            )}

            <div className="modal-actions">
              <button type="button" className="btn btn-outline" onClick={handleCancel}>Otkaži</button>
              <button type="submit" className="btn btn-primary" style={{ width: 'auto' }} disabled={saving}>
                {saving ? 'Čuvanje...' : 'Sačuvaj izmene'}
              </button>
            </div>
          </form>
            )}
          </div>
        </div>
      )}
    </div>
  );
}

function ProfilRed({ label, value }) {
  return (
    <div className="profil-red">
      <span className="profil-label">{label}</span>
      <span className="profil-value">{value}</span>
    </div>
  );
}
