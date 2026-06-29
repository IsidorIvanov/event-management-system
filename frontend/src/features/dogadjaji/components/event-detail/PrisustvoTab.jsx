import { useState, useEffect } from 'react';
import * as registracijaApi from '@/features/dogadjaji/services/registracijaService';
import { useToast } from '@/shared/components/ToastNotification';

const STATUS_REG_LABEL = {
  POTVRDJENA: 'Potvrđena',
  OTKAZANA: 'Otkazana',
  NA_CEKANJU: 'Na čekanju',
};

const STATUS_REG_CLASS = {
  POTVRDJENA: 'status-badge status-published',
  OTKAZANA: 'status-badge status-draft',
  NA_CEKANJU: 'status-badge status-ongoing',
};

const STATUS_KARTE_LABEL = {
  VALIDNA: 'Validna',
  NEVAZECA: 'Nevažeća',
  ISKORISCENA: 'Iskorišćena',
};

const STATUS_KARTE_CLASS = {
  VALIDNA: 'status-badge status-published',
  NEVAZECA: 'status-badge status-draft',
  ISKORISCENA: 'status-badge status-finished',
};

const PRISUSTVO_PAGE_SIZE = 10;

export default function PrisustvoTab({ event }) {
  const toast = useToast();
  const [registracije, setRegistracije] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [filterStatus, setFilterStatus] = useState('all');
  const [filterTipKarte, setFilterTipKarte] = useState('all');
  const [page, setPage] = useState(0);

  const load = async () => {
    setLoading(true);
    try {
      const res = await registracijaApi.getRegistracijeByDogadjaj(event.dogadjajId);
      setRegistracije(res.data);
    } catch {
      toast('Greška pri učitavanju učesnika.', 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, [event.dogadjajId]);

  const tipovi = [...new Set(registracije.map((r) => r.nazivTipa))];

  const filtered = registracije.filter((r) => {
    const q = search.toLowerCase();
    const matchSearch = !q ||
      `${r.ucesnikIme || ''} ${r.ucesnikPrezime || ''}`.toLowerCase().includes(q) ||
      (r.ucesnikEmail || '').toLowerCase().includes(q) ||
      (r.ucesnikKompanija || '').toLowerCase().includes(q) ||
      (r.brojKarte || '').toLowerCase().includes(q);
    const matchStatus = filterStatus === 'all' || r.status === filterStatus;
    const matchTip = filterTipKarte === 'all' || r.nazivTipa === filterTipKarte;
    return matchSearch && matchStatus && matchTip;
  });

  const totalPages = Math.ceil(filtered.length / PRISUSTVO_PAGE_SIZE);
  const pageItems = filtered.slice(
    page * PRISUSTVO_PAGE_SIZE,
    page * PRISUSTVO_PAGE_SIZE + PRISUSTVO_PAGE_SIZE,
  );

  // Reset to first page whenever filters/search change or data reloads.
  useEffect(() => { setPage(0); }, [search, filterStatus, filterTipKarte, registracije]);

  const statsPotvrdjena = registracije.filter((r) => r.status === 'POTVRDJENA').length;
  const statsOtkazana = registracije.filter((r) => r.status === 'OTKAZANA').length;

  return (
    <div className="govornici-tab">
      {/* Stats */}
      <div className="sesije-stats">
        <div className="sesija-stat-card">
          <div className="sesija-stat-label">Ukupno registracija</div>
          <div className="sesija-stat-value">{registracije.length}</div>
        </div>
        <div className="sesija-stat-card">
          <div className="sesija-stat-label">Potvrđene</div>
          <div className="sesija-stat-value">{statsPotvrdjena}</div>
        </div>
        <div className="sesija-stat-card">
          <div className="sesija-stat-label">Otkazane</div>
          <div className="sesija-stat-value">{statsOtkazana}</div>
        </div>
        <div className="sesija-stat-card">
          <div className="sesija-stat-label">Popunjenost</div>
          <div className="sesija-stat-value">
            {event.maksKapacitet > 0
              ? `${Math.round((statsPotvrdjena / event.maksKapacitet) * 100)}%`
              : '—'}
          </div>
        </div>
      </div>

      {/* Filters */}
      <div className="sesije-toolbar">
        <div className="sesije-filters">
          <div className="sesija-filter-input">
            <span className="filter-icon">🔍</span>
            <input
              placeholder="pretraži učesnike..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="sesija-search"
            />
          </div>
          <select
            className="sesija-filter-select"
            value={filterStatus}
            onChange={(e) => setFilterStatus(e.target.value)}
          >
            <option value="all">status: svi</option>
            <option value="POTVRDJENA">Potvrđena</option>
            <option value="OTKAZANA">Otkazana</option>
            <option value="NA_CEKANJU">Na čekanju</option>
          </select>
          <select
            className="sesija-filter-select"
            value={filterTipKarte}
            onChange={(e) => setFilterTipKarte(e.target.value)}
          >
            <option value="all">tip karte: svi</option>
            {tipovi.map((t) => (
              <option key={t} value={t}>{t}</option>
            ))}
          </select>
        </div>
        <button
          className="btn btn-outline btn-sm"
          onClick={load}
          style={{ width: 'auto' }}
        >
          ↻ Osveži
        </button>
      </div>

      {/* Table */}
      {loading ? (
        <p className="empty-hint">Učitavanje učesnika...</p>
      ) : filtered.length === 0 ? (
        <div className="sesije-empty">
          <p>Nema registrovanih učesnika za ovaj događaj.</p>
        </div>
      ) : (
        <div className="events-table-card">
          <div className="events-table-header">
            <h2>Registrovani učesnici</h2>
            <div className="card-hint">{filtered.length} od {registracije.length} ukupno</div>
          </div>
          <table className="events-table">
            <thead>
              <tr>
                <th>IME I PREZIME</th>
                <th>EMAIL</th>
                <th>KOMPANIJA / POZICIJA</th>
                <th>TIP KARTE</th>
                <th>BROJ KARTE</th>
                <th>DATUM REG.</th>
                <th>STATUS REG.</th>
                <th>STATUS KARTE</th>
              </tr>
            </thead>
            <tbody>
              {pageItems.map((r) => (
                <tr key={r.registracijaId}>
                  <td>
                    <span className="event-name-badge">
                      {r.ucesnikIme} {r.ucesnikPrezime}
                    </span>
                  </td>
                  <td>{r.ucesnikEmail || '—'}</td>
                  <td>
                    {[r.ucesnikKompanija, r.ucesnikPozicija].filter(Boolean).join(' · ') || '—'}
                  </td>
                  <td>{r.nazivTipa || '—'}</td>
                  <td>
                    <span style={{ fontFamily: 'monospace', fontSize: '0.85rem' }}>
                      {r.brojKarte || '—'}
                    </span>
                  </td>
                  <td>
                    {r.datumRegistracije
                      ? new Date(r.datumRegistracije).toLocaleDateString('sr-Latn', {
                          day: '2-digit', month: '2-digit', year: 'numeric',
                        })
                      : '—'}
                  </td>
                  <td>
                    <span className={STATUS_REG_CLASS[r.status] || 'status-badge status-draft'}>
                      {STATUS_REG_LABEL[r.status] || r.status || '—'}
                    </span>
                  </td>
                  <td>
                    <span className={STATUS_KARTE_CLASS[r.statusKarte] || 'status-badge status-draft'}>
                      {STATUS_KARTE_LABEL[r.statusKarte] || r.statusKarte || '—'}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {totalPages > 1 && (
            <div className="prisustvo-paginacija">
              <button
                className="btn btn-outline btn-sm"
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0}
              >
                ← Prethodna
              </button>
              <span className="prisustvo-paginacija-info">
                Strana {page + 1} od {totalPages}
              </span>
              <button
                className="btn btn-outline btn-sm"
                onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                disabled={page >= totalPages - 1}
              >
                Sledeća →
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
