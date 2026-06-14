import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '@/features/auth/context/AuthContext';

const TIP_LABELS = {
  ZAPOSLENI: 'Zaposleni',
  KLIJENT: 'Klijent (firma)',
  UCESNIK: 'Učesnik',
};

const ULOGA_LABELS = {
  MENADZER_DOGADJAJA: 'Menadžer događaja',
  KOORDINATOR_RESURSA: 'Koordinator resursa',
  KOORDINATOR_PROGRAMA: 'Koordinator programa',
  FINANSIJSKI_KONTROLOR: 'Finansijski kontrolor',
};

const getRegisterErrorMessage = (err) => {
  if (!err.response) {
    return 'Server trenutno nije dostupan. Pokušajte ponovo kasnije.';
  }

  const data = err.response.data;
  if (data?.details) {
    return Object.values(data.details).join('. ');
  }

  const backendMessage = data?.error || data?.message;
  if (backendMessage?.includes('već registrovan')) {
    return 'Email adresa je već registrovana.';
  }

  return backendMessage || 'Registracija nije uspela. Proverite podatke i pokušajte ponovo.';
};

export default function RegisterPage() {
  const [formData, setFormData] = useState({
    ime: '',
    prezime: '',
    email: '',
    lozinka: '',
    telefon: '',
    tipKorisnika: 'ZAPOSLENI',
    // Zaposleni
    uloga: 'FINANSIJSKI_KONTROLOR',
    pozicija: '',
    datumZaposlenja: '',
    // Klijent
    nazivFirme: '',
    pib: '',
    adresa: '',
    grad: '',
    kontaktOsoba: '',
    // Ucesnik
    kompanija: '',
    datumRodjenja: '',
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const { register } = useAuth();
  const navigate = useNavigate();

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const payload = {
        ime: formData.ime,
        prezime: formData.prezime,
        email: formData.email,
        lozinka: formData.lozinka,
        telefon: formData.telefon || null,
        tipKorisnika: formData.tipKorisnika,
      };

      if (formData.tipKorisnika === 'ZAPOSLENI') {
        payload.uloga = formData.uloga;
        payload.pozicija = formData.pozicija || null;
        payload.datumZaposlenja = formData.datumZaposlenja || null;
      } else if (formData.tipKorisnika === 'KLIJENT') {
        payload.nazivFirme = formData.nazivFirme;
        payload.pib = formData.pib;
        payload.adresa = formData.adresa || null;
        payload.grad = formData.grad || null;
        payload.kontaktOsoba = formData.kontaktOsoba || null;
      } else if (formData.tipKorisnika === 'UCESNIK') {
        payload.kompanija = formData.kompanija || null;
        payload.pozicija = formData.pozicija || null;
        payload.datumRodjenja = formData.datumRodjenja || null;
      }

      await register(payload);
      navigate('/dashboard');
    } catch (err) {
      setError(getRegisterErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-container">
      <div className="auth-card" style={{ maxWidth: '520px' }}>
        <h1>Registracija</h1>
        <p className="subtitle">Kreirajte nalog u sistemu za organizaciju događaja</p>

        {error && <div className="error-msg">{error}</div>}

        <form onSubmit={handleSubmit}>
          {/* Osnovna polja */}
          <div className="form-row">
            <div className="form-group">
              <label>Ime *</label>
              <input name="ime" value={formData.ime} onChange={handleChange} required placeholder="Vaše ime" />
            </div>
            <div className="form-group">
              <label>Prezime *</label>
              <input name="prezime" value={formData.prezime} onChange={handleChange} required placeholder="Vaše prezime" />
            </div>
          </div>

          <div className="form-group">
            <label>Email *</label>
            <input type="email" name="email" value={formData.email} onChange={handleChange} required placeholder="vas@email.com" />
          </div>

          <div className="form-row">
            <div className="form-group">
              <label>Lozinka *</label>
              <input type="password" name="lozinka" value={formData.lozinka} onChange={handleChange} required placeholder="Min. 6 karaktera" minLength={6} />
            </div>
            <div className="form-group">
              <label>Telefon</label>
              <input name="telefon" value={formData.telefon} onChange={handleChange} placeholder="+381..." />
            </div>
          </div>

          {/* Tip korisnika */}
          <div className="form-group">
            <label>Tip korisnika *</label>
            <select name="tipKorisnika" value={formData.tipKorisnika} onChange={handleChange}>
              {Object.entries(TIP_LABELS).map(([val, label]) => (
                <option key={val} value={val}>{label}</option>
              ))}
            </select>
          </div>

          {/* ---- Dinamička polja po tipu ---- */}

          {formData.tipKorisnika === 'ZAPOSLENI' && (
            <>
              <div className="form-row">
                <div className="form-group">
                  <label>Uloga *</label>
                  <select name="uloga" value={formData.uloga} onChange={handleChange}>
                    {Object.entries(ULOGA_LABELS).map(([val, label]) => (
                      <option key={val} value={val}>{label}</option>
                    ))}
                  </select>
                </div>
                <div className="form-group">
                  <label>Pozicija</label>
                  <input name="pozicija" value={formData.pozicija} onChange={handleChange} placeholder="Npr. Senior analitičar" />
                </div>
              </div>
              <div className="form-group">
                <label>Datum zaposlenja</label>
                <input type="date" name="datumZaposlenja" value={formData.datumZaposlenja} onChange={handleChange} placeholder="Opciono - koristi se danasnji datum ako nije upisano" />
              </div>
            </>
          )}

          {formData.tipKorisnika === 'KLIJENT' && (
            <>
              <div className="form-row">
                <div className="form-group">
                  <label>Naziv firme *</label>
                  <input name="nazivFirme" value={formData.nazivFirme} onChange={handleChange} required placeholder="DOO / AD" />
                </div>
                <div className="form-group">
                  <label>PIB *</label>
                  <input name="pib" value={formData.pib} onChange={handleChange} required placeholder="123456789" />
                </div>
              </div>
              <div className="form-row">
                <div className="form-group">
                  <label>Adresa</label>
                  <input name="adresa" value={formData.adresa} onChange={handleChange} placeholder="Ulica i broj" />
                </div>
                <div className="form-group">
                  <label>Grad</label>
                  <input name="grad" value={formData.grad} onChange={handleChange} placeholder="Beograd" />
                </div>
              </div>
              <div className="form-group">
                <label>Kontakt osoba</label>
                <input name="kontaktOsoba" value={formData.kontaktOsoba} onChange={handleChange} placeholder="Ime i prezime" />
              </div>
            </>
          )}

          {formData.tipKorisnika === 'UCESNIK' && (
            <>
              <div className="form-row">
                <div className="form-group">
                  <label>Kompanija</label>
                  <input name="kompanija" value={formData.kompanija} onChange={handleChange} placeholder="Opciono" />
                </div>
                <div className="form-group">
                  <label>Pozicija</label>
                  <input name="pozicija" value={formData.pozicija} onChange={handleChange} placeholder="Opciono" />
                </div>
              </div>
              <div className="form-group">
                <label>Datum rođenja</label>
                <input type="date" name="datumRodjenja" value={formData.datumRodjenja} onChange={handleChange} />
              </div>
            </>
          )}

          <button type="submit" className="btn btn-primary" disabled={loading} style={{ marginTop: '0.5rem' }}>
            {loading ? 'Registracija...' : 'Registruj se'}
          </button>
        </form>

        <div className="auth-link">
          Već imate nalog? <Link to="/login">Prijavite se</Link>
        </div>
      </div>
    </div>
  );
}
