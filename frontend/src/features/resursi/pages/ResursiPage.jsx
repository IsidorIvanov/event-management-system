import { useCallback, useEffect, useState } from 'react';
import { useAuth } from '@/features/auth/context/AuthContext';
import LokacijaForm from '@/features/resursi/components/LokacijaForm';
import LokacijaIObjekatForm from '@/features/resursi/components/LokacijaIObjekatForm';
import SalaForm from '@/features/resursi/components/SalaForm';
import LokacijaSalaList from '@/features/resursi/components/LokacijaSalaList';
import SalaKalendar, { formatDate, addDays } from '@/features/resursi/components/SalaKalendar';
import SesijaDetaljModal from '@/features/resursi/components/SesijaDetaljModal';
import * as lokacijaApi from '@/features/resursi/services/lokacijaService';
import * as salaApi from '@/features/resursi/services/salaService';
import * as sesijaApi from '@/features/dogadjaji/services/sesijaService';

function extractError(err) {
  const data = err.response?.data;
  if (data?.details) {
    return Object.values(data.details).join(', ');
  }
  return data?.error || err.message || 'Došlo je do greške';
}

export default function ResursiPage() {
  const { hasRole } = useAuth();
  const canManageLokacija = hasRole('MENADZER_DOGADJAJA');
  const canManageSala = hasRole('KOORDINATOR_RESURSA') || hasRole('MENADZER_DOGADJAJA');

  const [lokacije, setLokacije] = useState([]);
  const [selectedLokacijaId, setSelectedLokacijaId] = useState(null);
  const [selectedSala, setSelectedSala] = useState(null);
  const [dostupnost, setDostupnost] = useState(null);
  const [loading, setLoading] = useState(true);
  const [kalendarLoading, setKalendarLoading] = useState(false);
  const [message, setMessage] = useState(null);
  const [error, setError] = useState(null);

  const [modal, setModal] = useState(null);
  const [sesijaDetalj, setSesijaDetalj] = useState(null);
  const [sesijaDetaljLoading, setSesijaDetaljLoading] = useState(false);
  const [sesijaDetaljError, setSesijaDetaljError] = useState(null);
  const [datumOd, setDatumOd] = useState(formatDate(new Date()));
  const [datumDo, setDatumDo] = useState(addDays(formatDate(new Date()), 6));

  const loadLokacije = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await lokacijaApi.getLokacije();
      setLokacije(res.data);
      setSelectedLokacijaId((prev) =>
        prev && res.data.some((l) => l.lokacijaId === prev) ? prev : null
      );
      setSelectedSala((prev) => {
        if (!prev) return null;
        const lok = res.data.find((l) => l.lokacijaId === prev.lokacijaId);
        if (!lok) return null;
        const exists = (lok.sale || []).some((s) => s.nazivSale === prev.nazivSale);
        return exists ? prev : null;
      });
    } catch (err) {
      setError(extractError(err));
    } finally {
      setLoading(false);
    }
  }, []);

  const loadDostupnost = useCallback(async (lokacijaId, nazivSale) => {
    if (!lokacijaId || !nazivSale) return;
    setKalendarLoading(true);
    setError(null);
    try {
      const res = await salaApi.getDostupnost(lokacijaId, datumOd, datumDo);
      const salaData = res.data.sale?.find((s) => s.nazivSale === nazivSale);
      setDostupnost(salaData ? { ...res.data, sale: [salaData] } : null);
    } catch (err) {
      setError(extractError(err));
      setDostupnost(null);
    } finally {
      setKalendarLoading(false);
    }
  }, [datumOd, datumDo]);

  useEffect(() => {
    loadLokacije();
  }, [loadLokacije]);

  const handleSelectLokacija = (lokacijaId) => {
    setSelectedLokacijaId(lokacijaId);
    setSelectedSala(null);
    setDostupnost(null);
  };

  const handleSelectSala = (sala) => {
    setSelectedSala({ lokacijaId: sala.lokacijaId, nazivSale: sala.nazivSale });
    setDostupnost(null);
    loadDostupnost(sala.lokacijaId, sala.nazivSale);
  };

  const handlePrikaziKalendar = () => {
    if (!selectedSala) return;
    loadDostupnost(selectedSala.lokacijaId, selectedSala.nazivSale);
  };

  const closeModal = () => setModal(null);

  const handleSlotClick = async (sesijaId) => {
    setSesijaDetalj(null);
    setSesijaDetaljError(null);
    setSesijaDetaljLoading(true);
    try {
      const res = await sesijaApi.getSesijaDetalj(sesijaId);
      setSesijaDetalj(res.data);
    } catch (err) {
      setSesijaDetaljError(extractError(err));
    } finally {
      setSesijaDetaljLoading(false);
    }
  };

  const closeSesijaDetalj = () => {
    setSesijaDetalj(null);
    setSesijaDetaljError(null);
  };

  const handleCreateLokacijaSaSalom = async ({ lokacija, sala }) => {
    try {
      const res = await lokacijaApi.createLokacija(lokacija);
      const newId = res.data.lokacijaId;
      if (sala && canManageSala) {
        await salaApi.createSala({ ...sala, lokacijaId: newId });
      }
      setMessage(
        sala
          ? 'Lokacija i sala su uspešno kreirane.'
          : 'Lokacija je uspešno kreirana.'
      );
      setSelectedLokacijaId(newId);
      closeModal();
      await loadLokacije();
    } catch (err) {
      setError(extractError(err));
    }
  };

  const handleUpdateLokacijaById = async (id, data) => {
    try {
      await lokacijaApi.updateLokacija(id, data);
      setMessage('Lokacija je uspešno izmenjena.');
      closeModal();
      await loadLokacije();
    } catch (err) {
      setError(extractError(err));
    }
  };

  const handleDeleteLokacija = async (lok) => {
    if (!window.confirm(`Obrisati lokaciju "${lok.naziv}"?`)) return;
    try {
      await lokacijaApi.deleteLokacija(lok.lokacijaId);
      setMessage('Lokacija je obrisana.');
      if (selectedLokacijaId === lok.lokacijaId) {
        setSelectedLokacijaId(null);
        setSelectedSala(null);
        setDostupnost(null);
      }
      await loadLokacije();
    } catch (err) {
      setError(extractError(err));
    }
  };

  const handleCreateSala = async (data) => {
    try {
      await salaApi.createSala(data);
      setMessage('Sala je uspešno kreirana.');
      closeModal();
      await loadLokacije();
      if (selectedSala) await loadDostupnost(selectedSala.lokacijaId, selectedSala.nazivSale);
    } catch (err) {
      setError(extractError(err));
    }
  };

  const handleUpdateSalaByRef = async (sala, data) => {
    try {
      await salaApi.updateSala(sala.lokacijaId, sala.nazivSale, data);
      setMessage('Sala je uspešno izmenjena.');
      closeModal();
      await loadLokacije();
      if (
        selectedSala?.lokacijaId === sala.lokacijaId &&
        selectedSala?.nazivSale === sala.nazivSale
      ) {
        await loadDostupnost(sala.lokacijaId, sala.nazivSale);
      }
    } catch (err) {
      setError(extractError(err));
    }
  };

  const handleDeleteSala = async (sala) => {
    if (!window.confirm(`Obrisati salu "${sala.nazivSale}"?`)) return;
    try {
      await salaApi.deleteSala(sala.lokacijaId, sala.nazivSale);
      setMessage('Sala je obrisana.');
      if (
        selectedSala?.lokacijaId === sala.lokacijaId &&
        selectedSala?.nazivSale === sala.nazivSale
      ) {
        setSelectedSala(null);
        setDostupnost(null);
      }
      await loadLokacije();
    } catch (err) {
      setError(extractError(err));
    }
  };

  const kalendarLokacija = lokacije.find((l) => l.lokacijaId === selectedSala?.lokacijaId);

  return (
    <>
      <h1>Resursi — lokacije i sale</h1>
      <p className="page-subtitle">
        Pregled objekata i kalendar dostupnosti sala po satima.
      </p>

      {message && <div className="success-msg">{message}</div>}
      {error && <div className="error-msg">{error}</div>}


      {loading ? (
        <p className="empty-hint">Učitavanje lokacija...</p>
      ) : (
        <>
          <LokacijaSalaList
            lokacije={lokacije}
            selectedLokacijaId={selectedLokacijaId}
            selectedSala={selectedSala}
            onSelectLokacija={handleSelectLokacija}
            onSelectSala={handleSelectSala}
            onEditLokacija={(lok) => setModal({ type: 'lokacija-edit', lokacija: lok })}
            onDeleteLokacija={handleDeleteLokacija}
            onAddSala={() => setModal({ type: 'sala-create' })}
            onEditSala={(sala) => setModal({ type: 'sala-edit', sala })}
            onDeleteSala={handleDeleteSala}
            canManageLokacija={canManageLokacija}
            canManageSala={canManageSala}
          />

          {selectedSala && (
            <section className="kalendar-section">
              <div className="kalendar-section-header">
                <h2>
                  Kalendar — {selectedSala.nazivSale}
                  {kalendarLokacija && (
                    <span className="kalendar-subtitle"> ({kalendarLokacija.naziv})</span>
                  )}
                </h2>
                <div className="date-range">
                  <label>
                    Od
                    <input type="date" value={datumOd} onChange={(e) => setDatumOd(e.target.value)} />
                  </label>
                  <label>
                    Do
                    <input type="date" value={datumDo} onChange={(e) => setDatumDo(e.target.value)} />
                  </label>
                  <button
                    type="button"
                    className="btn btn-outline"
                    style={{ width: 'auto' }}
                    onClick={handlePrikaziKalendar}
                  >
                    Osveži
                  </button>
                </div>
              </div>
              <SalaKalendar
                dostupnost={dostupnost}
                loading={kalendarLoading}
                onSlotClick={handleSlotClick}
              />
            </section>
          )}
        </>
      )}

      {(sesijaDetalj || sesijaDetaljLoading || sesijaDetaljError) && (
        <SesijaDetaljModal
          detalj={sesijaDetalj}
          loading={sesijaDetaljLoading}
          error={sesijaDetaljError}
          onClose={closeSesijaDetalj}
        />
      )}

      {modal?.type === 'lokacija-create' && (
        <div className="modal-overlay" onClick={closeModal}>
          <div className="modal-card modal-card-wide" onClick={(e) => e.stopPropagation()}>
            <h3>Nova lokacija</h3>
            {canManageSala ? (
              <LokacijaIObjekatForm
                onSubmit={handleCreateLokacijaSaSalom}
                onCancel={closeModal}
              />
            ) : (
              <LokacijaForm
                onSubmit={(data) => handleCreateLokacijaSaSalom({ lokacija: data, sala: null })}
                onCancel={closeModal}
                submitLabel="Kreiraj lokaciju"
              />
            )}
          </div>
        </div>
      )}

      {modal?.type === 'lokacija-edit' && (
        <div className="modal-overlay" onClick={closeModal}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()}>
            <h3>Izmena lokacije — {modal.lokacija.naziv}</h3>
            <LokacijaForm
              initial={modal.lokacija}
              onSubmit={(data) => handleUpdateLokacijaById(modal.lokacija.lokacijaId, data)}
              onCancel={closeModal}
              submitLabel="Sačuvaj izmene"
            />
          </div>
        </div>
      )}

      {modal?.type === 'sala-create' && selectedLokacijaId && (
        <div className="modal-overlay" onClick={closeModal}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()}>
            <h3>Nova sala</h3>
            <SalaForm
              lokacijaId={selectedLokacijaId}
              onSubmit={handleCreateSala}
              onCancel={closeModal}
              submitLabel="Kreiraj salu"
            />
          </div>
        </div>
      )}

      {modal?.type === 'sala-edit' && (
        <div className="modal-overlay" onClick={closeModal}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()}>
            <h3>Izmena sale — {modal.sala.nazivSale}</h3>
            <SalaForm
              lokacijaId={modal.sala.lokacijaId}
              initial={modal.sala}
              isEdit
              onSubmit={(data) => handleUpdateSalaByRef(modal.sala, data)}
              onCancel={closeModal}
              submitLabel="Sačuvaj izmene"
            />
          </div>
        </div>
      )}
    </>
  );
}
