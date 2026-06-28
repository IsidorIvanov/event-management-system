import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import api from '@/shared/services/api';
import { useAuth } from '@/features/auth/context/AuthContext';
import { useToast } from '@/shared/components/ToastNotification';
import { formatDate as fmtDate, formatDateTime } from '@/shared/utils/format';
import * as analizaApi from '@/features/analiza/services/analizaProfitabilnostiService';
import * as nabavkaApi from '@/features/fakture/services/nabavkaService';

/** Isti filter kao backend (AnalizaProfitabilnostiService). */
const COMMITTED_NABAVKA_STATUSES = new Set(['POTVRDJENA', 'U_ISPORUCI', 'ZAVRSENA']);

const STATUS_NABAVKE_LABEL = {
  POTVRDJENA: 'Potvrđena',
  U_ISPORUCI: 'U isporuci',
  ZAVRSENA: 'Završena',
};

const ANALIZA_STATUS_DISPLAY = {
  DRAFT: 'Draft',
  FINALIZOVANA: 'Finalizovana',
};

const REZULTAT_OCENE_DISPLAY = {
  PROFITABILAN: 'Profitabilan',
  BREAK_EVEN: 'Break-even',
  GUBITAK: 'Gubitak',
};

const STATUS_STYLE = {
  DRAFT: { backgroundColor: '#6b7280', color: '#fff' },
  FINALIZOVANA: { backgroundColor: '#3478f6', color: '#fff' },
};

const REZULTAT_STYLE = {
  PROFITABILAN: { backgroundColor: '#16a34a', color: '#fff' },
  BREAK_EVEN: { backgroundColor: '#f59e0b', color: '#111827' },
  GUBITAK: { backgroundColor: '#dc2626', color: '#fff' },
};

const extractError = (error, fallback = 'Došlo je do greške.') => {
  const data = error?.response?.data;
  if (typeof data === 'string') return data;
  if (data?.details) return Object.values(data.details).join(', ');
  if (data?.message) return data.message;
  if (data?.error) return data.error;
  return error?.message || fallback;
};

const formatMoneyString = (value) => (value == null ? '—' : `${value} RSD`);

const formatCommitovaniTrosak = (analiza) => {
  if (analiza.status === 'FINALIZOVANA') {
    return '—';
  }
  return formatMoneyString(analiza.commitovaniTrosak);
};

/** Prihod: karte + izlazne fakture (B2B). Samo draft — trenutno stanje baze. */
const formatPrihodBreakdown = (analiza) => {
  if (analiza.status !== 'DRAFT') return null;
  const parts = [];
  if (analiza.prihodRegistracije != null) {
    parts.push(`karte ${formatMoneyString(analiza.prihodRegistracije)}`);
  }
  if (analiza.prihodIzlazneFakture != null) {
    parts.push(`izlazne fakture ${formatMoneyString(analiza.prihodIzlazneFakture)}`);
  }
  return parts.length > 0 ? parts.join(' · ') : null;
};

/** Rashod: evidentirani troškovi + honorari. Samo draft — trenutno stanje baze. */
const formatTrosakBreakdown = (analiza) => {
  if (analiza.status !== 'DRAFT') return null;
  const parts = [];
  if (analiza.trosakEvidentiran != null) {
    parts.push(`troškovi ${formatMoneyString(analiza.trosakEvidentiran)}`);
  }
  if (analiza.trosakHonorari != null) {
    parts.push(`honorari ${formatMoneyString(analiza.trosakHonorari)}`);
  }
  return parts.length > 0 ? parts.join(' · ') : null;
};

const formatPercent = (value) => {
  if (value == null) return '—';
  const number = Number(value);
  if (Number.isNaN(number)) return '—';
  return `${(number * 100).toFixed(2)}%`;
};

const formatDate = (value) =>
  fmtDate(value, { day: '2-digit', month: '2-digit', year: 'numeric' });

function Badge({ value, labelMap, styleMap }) {
  if (!value) return '—';

  return (
    <span className="status-badge" style={styleMap[value] || { backgroundColor: '#6b7280', color: '#fff' }}>
      {labelMap[value] || value}
    </span>
  );
}

function CommitNabavkaModal({ dogadjajId, dogadjajNaziv, ukupno, onClose }) {
  const toast = useToast();
  const [loading, setLoading] = useState(true);
  const [rows, setRows] = useState([]);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    nabavkaApi
      .getNabavkeByDogadjaj(dogadjajId)
      .then((res) => {
        if (cancelled) return;
        const nabavke = Array.isArray(res.data) ? res.data : [];
        const flat = [];
        nabavke
          .filter((n) => COMMITTED_NABAVKA_STATUSES.has(n.status))
          .forEach((n) => {
            (n.stavke || []).forEach((s, index) => {
              flat.push({
                key: `${n.nabavkaId}-${s.redniBroj ?? index}`,
                nabavkaId: n.nabavkaId,
                status: n.status,
                dobavljac: n.dobavljacNaziv || '—',
                naziv: s.nazivResursa,
                kolicina: s.kolicina,
                jedinicnaCena: s.jedinicnaCena,
                ukupnaCena: s.ukupnaCena,
              });
            });
          });
        setRows(flat);
      })
      .catch((err) => {
        if (!cancelled) {
          toast(extractError(err, 'Nije moguće učitati nabavke.'), 'error');
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [dogadjajId, toast]);

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-card modal-card-wide" onClick={(event) => event.stopPropagation()}>
        <div className="modal-header">
          <div>
            <h3 className="modal-title">Commitovane nabavke</h3>
            <p className="page-subtitle" style={{ marginTop: '0.35rem' }}>
              {dogadjajNaziv} · statusi POTVRĐENA, U ISPORUCI, ZAVRŠENA ·{' '}
              <strong>ne ulazi u neto/maržu/ROI</strong>
            </p>
          </div>
          <button className="modal-close" onClick={onClose} type="button">
            ✕
          </button>
        </div>

        <div style={{ padding: '0 1rem 1rem' }}>
          {loading ? (
            <p style={{ color: 'var(--text-muted)' }}>Učitavanje stavki...</p>
          ) : rows.length === 0 ? (
            <p style={{ color: 'var(--text-muted)' }}>Nema commitovanih stavki nabavke za ovaj događaj.</p>
          ) : (
            <div className="events-table-scroll">
              <table className="events-table">
                <thead>
                  <tr>
                    <th>Nabavka</th>
                    <th>Status</th>
                    <th>Dobavljač</th>
                    <th>Stavka</th>
                    <th>Kol.</th>
                    <th>Jed. cena</th>
                    <th>Ukupno</th>
                  </tr>
                </thead>
                <tbody>
                  {rows.map((row) => (
                    <tr key={row.key}>
                      <td>#{row.nabavkaId}</td>
                      <td>{STATUS_NABAVKE_LABEL[row.status] || row.status}</td>
                      <td>{row.dobavljac}</td>
                      <td>{row.naziv}</td>
                      <td>{row.kolicina}</td>
                      <td>{formatMoneyString(row.jedinicnaCena)}</td>
                      <td>{formatMoneyString(row.ukupnaCena)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          <div
            style={{
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              gap: '1rem',
              flexWrap: 'wrap',
              marginTop: '1rem',
              paddingTop: '1rem',
              borderTop: '1px solid var(--border)',
            }}
          >
            <div>
              <span style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>Ukupno (commit): </span>
              <strong>{formatMoneyString(ukupno)}</strong>
            </div>
            <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap' }}>
              <Link
                className="btn btn-outline btn-xs"
                to={`/dashboard/porudzbenice?dogadjajId=${dogadjajId}`}
                onClick={onClose}
              >
                Otvori porudžbenice
              </Link>
              <button className="btn btn-primary btn-xs" type="button" onClick={onClose}>
                Zatvori
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

function NapomeneModal({ title, initialValue = '', onClose, onSubmit, loading }) {
  const [napomene, setNapomene] = useState(initialValue || '');

  const handleSubmit = (event) => {
    event.preventDefault();
    onSubmit({ napomene: napomene.trim() || null });
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-card" onClick={(event) => event.stopPropagation()}>
        <div className="modal-header">
          <div>
            <h3 className="modal-title">{title}</h3>
            <p className="page-subtitle" style={{ marginTop: '0.35rem' }}>
              Napomene su opcione. Prihod = kupovina karata + izlazne fakture. Rashod = evidentirani
              troškovi (NETO) + honorari govornika. Obaveze iz nabavke nisu uključene u ocenu.
            </p>
          </div>
          <button className="modal-close" onClick={onClose} type="button">
            ✕
          </button>
        </div>

        <form onSubmit={handleSubmit} style={{ padding: '1rem' }}>
          <textarea
            className="form-control"
            placeholder="Napomene"
            value={napomene}
            onChange={(event) => setNapomene(event.target.value)}
            rows={5}
            style={{ width: '100%', marginBottom: '1rem' }}
          />
          <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'flex-end' }}>
            <button className="btn btn-outline" type="button" onClick={onClose}>
              Otkaži
            </button>
            <button className="btn btn-primary" type="submit" disabled={loading}>
              {loading ? 'Čuvanje...' : 'Sačuvaj'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default function AnalizaProfitabilnostiPage() {
  const toast = useToast();
  const { hasRole, getUserId } = useAuth();
  const isFinKontrolor = hasRole('FINANSIJSKI_KONTROLOR');
  const isMenadzer = hasRole('MENADZER_DOGADJAJA');
  const currentUserId = getUserId();

  const [dogadjaji, setDogadjaji] = useState([]);
  const [selectedDogadjajId, setSelectedDogadjajId] = useState('');
  const [analize, setAnalize] = useState([]);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  const [draftModalOpen, setDraftModalOpen] = useState(false);
  const [editNapomeneTarget, setEditNapomeneTarget] = useState(null);
  const [commitModalOpen, setCommitModalOpen] = useState(false);

  const selectedDogadjaj = useMemo(
    () => dogadjaji.find((dogadjaj) => String(dogadjaj.dogadjajId) === String(selectedDogadjajId)) || null,
    [dogadjaji, selectedDogadjajId],
  );

  const stats = useMemo(() => {
    const latest = analize[0] || null;
    return {
      total: analize.length,
      draft: analize.filter((analiza) => analiza.status === 'DRAFT').length,
      finalizovane: analize.filter((analiza) => analiza.status === 'FINALIZOVANA').length,
      latest,
    };
  }, [analize]);

  const loadAnalize = async (dogadjajId) => {
    if (!dogadjajId) {
      setAnalize([]);
      return;
    }
    const res = await analizaApi.getAnalizeByDogadjaj(dogadjajId);
    setAnalize(Array.isArray(res.data) ? res.data : []);
  };

  useEffect(() => {
    api
      .get('/dogadjaj')
      .then((res) => {
        const zavrseni = (Array.isArray(res.data) ? res.data : []).filter(
          (dogadjaj) => dogadjaj.status === 'ZAVRSEN',
        );
        setDogadjaji(zavrseni);
        setSelectedDogadjajId(String(zavrseni[0]?.dogadjajId || ''));
      })
      .catch((err) => {
        setError(extractError(err, 'Nije moguće učitati događaje.'));
      })
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    if (!selectedDogadjajId) {
      setAnalize([]);
      return;
    }
    loadAnalize(selectedDogadjajId).catch((err) => {
      toast(extractError(err, 'Nije moguće učitati analize profitabilnosti.'), 'error');
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedDogadjajId]);

  const handleCreateDraft = async (payload) => {
    if (!selectedDogadjajId) return;
    setBusy(true);
    try {
      await analizaApi.createDraft(selectedDogadjajId, payload);
      toast('Draft analiza je kreirana.', 'success');
      setDraftModalOpen(false);
      await loadAnalize(selectedDogadjajId);
    } catch (err) {
      toast(extractError(err, 'Kreiranje analize nije uspelo.'), 'error');
    } finally {
      setBusy(false);
    }
  };

  const handleUpdateNapomene = async (payload) => {
    if (!editNapomeneTarget) return;
    setBusy(true);
    try {
      await analizaApi.updateDraft(editNapomeneTarget.analizaId, payload);
      toast('Napomene su izmenjene.', 'success');
      setEditNapomeneTarget(null);
      await loadAnalize(selectedDogadjajId);
    } catch (err) {
      toast(extractError(err, 'Izmena napomena nije uspela.'), 'error');
    } finally {
      setBusy(false);
    }
  };

  const handleFinalize = async (analiza) => {
    if (!window.confirm('Da li želite da finalizujete ovu analizu? Posle finalizacije više nije izmenjiva.')) {
      return;
    }
    setBusy(true);
    try {
      await analizaApi.finalizeAnaliza(analiza.analizaId);
      toast('Analiza je finalizovana.', 'success');
      await loadAnalize(selectedDogadjajId);
    } catch (err) {
      toast(extractError(err, 'Finalizacija analize nije uspela.'), 'error');
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="program-page">
      {draftModalOpen && (
        <NapomeneModal
          title="Nova analiza (draft)"
          onClose={() => setDraftModalOpen(false)}
          onSubmit={handleCreateDraft}
          loading={busy}
        />
      )}
      {editNapomeneTarget && (
        <NapomeneModal
          title="Izmena napomena"
          initialValue={editNapomeneTarget.napomene || ''}
          onClose={() => setEditNapomeneTarget(null)}
          onSubmit={handleUpdateNapomene}
          loading={busy}
        />
      )}
      {commitModalOpen && selectedDogadjajId && (
        <CommitNabavkaModal
          dogadjajId={Number(selectedDogadjajId)}
          dogadjajNaziv={selectedDogadjaj?.naziv || `Događaj #${selectedDogadjajId}`}
          ukupno={analize.find((a) => a.status === 'DRAFT')?.commitovaniTrosak}
          onClose={() => setCommitModalOpen(false)}
        />
      )}

      <div className="program-header">
        <div>
          <h1>Analiza profitabilnosti</h1>
          <p className="page-subtitle">
            Prihod: kupovina karata i izlazne fakture. Rashod: evidentirani troškovi i honorari.
            Commitovane nabavke prikazuju se samo u draft fazi.
          </p>
        </div>
        {isFinKontrolor && selectedDogadjajId && (
          <button className="btn btn-primary" onClick={() => setDraftModalOpen(true)} disabled={busy}>
            Nova analiza (draft)
          </button>
        )}
      </div>

      {loading ? (
        <div className="events-table-card">Učitavanje...</div>
      ) : error ? (
        <div className="events-table-card" style={{ color: 'var(--danger)' }}>{error}</div>
      ) : dogadjaji.length === 0 ? (
        <div className="events-table-card">
          Nema završenih događaja. Analiza profitabilnosti se kreira tek nakon završetka događaja.
        </div>
      ) : (
        <>
          <div className="events-table-card" style={{ marginBottom: '1.5rem' }}>
            <div className="events-table-header">
              <h2>Izbor događaja</h2>
              <select
                className="status-select"
                value={selectedDogadjajId}
                onChange={(event) => setSelectedDogadjajId(event.target.value)}
              >
                {dogadjaji.map((dogadjaj) => (
                  <option key={dogadjaj.dogadjajId} value={dogadjaj.dogadjajId}>
                    {dogadjaj.naziv} · {formatDate(dogadjaj.datumZavrsetka)}
                  </option>
                ))}
              </select>
            </div>
            {selectedDogadjaj && (
              <p className="page-subtitle" style={{ padding: '0 1rem 1rem' }}>
                Izabrani događaj: {selectedDogadjaj.naziv}
              </p>
            )}
          </div>

          <div className="info-cards" style={{ marginBottom: '1.5rem' }}>
            <div className="info-card">
              <div className="label">Analize</div>
              <div className="value accent">{stats.total}</div>
            </div>
            <div className="info-card">
              <div className="label">Draft</div>
              <div className="value warning">{stats.draft}</div>
            </div>
            <div className="info-card">
              <div className="label">Finalizovane</div>
              <div className="value success">{stats.finalizovane}</div>
            </div>
            <div className="info-card">
              <div className="label">Poslednji neto</div>
              <div className="value accent">{formatMoneyString(stats.latest?.neto)}</div>
            </div>
          </div>

          <div className="events-table-card">
            <div className="events-table-header">
              <h2>Analize profitabilnosti</h2>
              <span className="page-subtitle">{analize.length} zapisa</span>
            </div>
            {analize.length === 0 ? (
              <p style={{ color: 'var(--text-muted)', padding: '1rem' }}>
                Nema analiza za izabrani događaj.
              </p>
            ) : (
              <div className="events-table-scroll">
                <table className="events-table">
                  <thead>
                    <tr>
                      <th>Datum</th>
                      <th>Prihod</th>
                      <th>Trošak (realizovan)</th>
                      <th>Commit. nabavka</th>
                      <th>Neto</th>
                      <th>Marža</th>
                      <th>ROI</th>
                      <th>Status</th>
                      <th>Rezultat</th>
                      <th>Kreirao</th>
                      <th>Napomene</th>
                      <th>Akcije</th>
                    </tr>
                  </thead>
                  <tbody>
                  {analize.map((analiza) => {
                    const canFinalize =
                      isMenadzer &&
                      analiza.status === 'DRAFT' &&
                      String(analiza.kreiraoId) !== String(currentUserId);
                    const canEditNapomene = isFinKontrolor && analiza.status === 'DRAFT';

                    return (
                      <tr key={analiza.analizaId}>
                        <td>
                          {formatDate(analiza.datumAnalize)}
                          {analiza.finalizovanoAt && (
                            <div className="card-hint">
                              Finalizovano: {formatDateTime(analiza.finalizovanoAt)}
                            </div>
                          )}
                        </td>
                        <td>
                          {formatMoneyString(analiza.ukupanPrihod)}
                          {formatPrihodBreakdown(analiza) && (
                            <div className="card-hint">
                              {formatPrihodBreakdown(analiza)}
                              <div style={{ marginTop: '0.15rem', opacity: 0.85 }}>
                                Raspodela prihoda (trenutno stanje)
                              </div>
                            </div>
                          )}
                        </td>
                        <td>
                          {formatMoneyString(analiza.ukupanTrosak)}
                          {formatTrosakBreakdown(analiza) && (
                            <div className="card-hint">
                              {formatTrosakBreakdown(analiza)}
                              <div style={{ marginTop: '0.15rem', opacity: 0.85 }}>
                                Raspodela rashoda (trenutno stanje)
                              </div>
                            </div>
                          )}
                        </td>
                        <td>
                          {analiza.status === 'DRAFT' &&
                          analiza.commitovaniTrosak != null &&
                          Number(analiza.commitovaniTrosak) > 0 ? (
                            <button
                              type="button"
                              className="btn-link"
                              onClick={() => setCommitModalOpen(true)}
                              title="Prikaži stavke commitovanih nabavki"
                            >
                              {formatMoneyString(analiza.commitovaniTrosak)}
                            </button>
                          ) : (
                            formatCommitovaniTrosak(analiza)
                          )}
                          {analiza.status === 'DRAFT' && (
                            <div className="card-hint">
                              {Number(analiza.commitovaniTrosak) > 0
                                ? 'Klikni iznos za detalje · van ocene profitabilnosti'
                                : 'Van ocene profitabilnosti'}
                            </div>
                          )}
                          {analiza.status === 'FINALIZOVANA' && (
                            <div className="card-hint">Samo u draft fazi</div>
                          )}
                        </td>
                        <td>{formatMoneyString(analiza.neto)}</td>
                        <td>{formatPercent(analiza.marza)}</td>
                        <td>{formatPercent(analiza.roi)}</td>
                        <td>
                          <Badge
                            value={analiza.status}
                            labelMap={ANALIZA_STATUS_DISPLAY}
                            styleMap={STATUS_STYLE}
                          />
                        </td>
                        <td>
                          <Badge
                            value={analiza.rezultatOcene}
                            labelMap={REZULTAT_OCENE_DISPLAY}
                            styleMap={REZULTAT_STYLE}
                          />
                        </td>
                        <td>{analiza.kreiraoImePrezime || `#${analiza.kreiraoId}`}</td>
                        <td>{analiza.napomene || '—'}</td>
                        <td>
                          <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
                            {canEditNapomene && (
                              <button
                                className="btn btn-outline btn-xs"
                                onClick={() => setEditNapomeneTarget(analiza)}
                                disabled={busy}
                              >
                                Edit napomene
                              </button>
                            )}
                            {canFinalize && (
                              <button
                                className="btn btn-primary btn-xs"
                                onClick={() => handleFinalize(analiza)}
                                disabled={busy}
                              >
                                Finalizuj
                              </button>
                            )}
                            {!canEditNapomene && !canFinalize && '—'}
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </>
      )}
    </div>
  );
}
