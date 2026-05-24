import { useCallback, useEffect, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import LokacijaForm from '../components/resursi/LokacijaForm';
import SalaForm from '../components/resursi/SalaForm';
import LokacijaSalaList from '../components/resursi/LokacijaSalaList';
import SalaKalendar, { formatDate, addDays } from '../components/resursi/SalaKalendar';
import * as lokacijaApi from '../services/lokacijaService';
import * as salaApi from '../services/salaService';

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
  const canManageSala = hasRole('KOORDINATOR_RESURSA');

  const [lokacije, setLokacije] = useState([]);
  const [selectedLokacijaId, setSelectedLokacijaId] = useState(null);
  const [dostupnost, setDostupnost] = useState(null);
  const [loading, setLoading] = useState(true);
  const [kalendarLoading, setKalendarLoading] = useState(false);
  const [message, setMessage] = useState(null);
  const [error, setError] = useState(null);

  const [modal, setModal] = useState(null);
  const [datumOd, setDatumOd] = useState(formatDate(new Date()));
  const [datumDo, setDatumDo] = useState(addDays(formatDate(new Date()), 6));

  const loadLokacije = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await lokacijaApi.getLokacije();
      setLokacije(res.data);
      setSelectedLokacijaId((prev) => {
        if (prev && res.data.some((l) => l.lokacijaId === prev)) return prev;
        return res.data.length > 0 ? res.data[0].lokacijaId : null;
      });
    } catch (err) {
      setError(extractError(err));
    } finally {
      setLoading(false);
    }
  }, []);

  const loadDostupnost = useCallback(async () => {
    if (!selectedLokacijaId) return;
    setKalendarLoading(true);
    try {
      const res = await salaApi.getDostupnost(selectedLokacijaId, datumOd, datumDo);
      setDostupnost(res.data);
    } catch (err) {
      setError(extractError(err));
    } finally {
      setKalendarLoading(false);
    }
  }, [selectedLokacijaId, datumOd, datumDo]);

  useEffect(() => {
    loadLokacije();
  }, [loadLokacije]);

  useEffect(() => {
    loadDostupnost();
  }, [loadDostupnost]);

  const closeModal = () => setModal(null);

  const handleCreateLokacija = async (data) => {
    try {
      await lokacijaApi.createLokacija(data);
      setMessage('Lokacija je uspešno kreirana.');
      closeModal();
      await loadLokacije();
    } catch (err) {
      setError(extractError(err));
    }
  };

  const handleUpdateLokacija = async (data) => {
    try {
      await lokacijaApi.updateLokacija(modal.lokacija.lokacijaId, data);
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
      if (selectedLokacijaId === lok.lokacijaId) setSelectedLokacijaId(null);
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
      await loadDostupnost();
    } catch (err) {
      setError(extractError(err));
    }
  };

  const handleUpdateSala = async (data) => {
    try {
      await salaApi.updateSala(modal.sala.lokacijaId, modal.sala.nazivSale, data);
      setMessage('Sala je uspešno izmenjena.');
      closeModal();
      await loadLokacije();
      await loadDostupnost();
    } catch (err) {
      setError(extractError(err));
    }
  };

  const handleDeleteSala = async (sala) => {
    if (!window.confirm(`Obrisati salu "${sala.nazivSale}"?`)) return;
    try {
      await salaApi.deleteSala(sala.lokacijaId, sala.nazivSale);
      setMessage('Sala je obrisana.');
      await loadLokacije();
      await loadDostupnost();
    } catch (err) {
      setError(extractError(err));
    }
  };

  return (
    <>
      <h1>Resursi — lokacije i sale</h1>
      <p className="page-subtitle">
        Pregled objekata, upravljanje kapacitetima i vizuelni kalendar dostupnosti po satima.
      </p>

      {message && <div className="success-msg">{message}</div>}
      {error && <div className="error-msg">{error}</div>}

      <div className="resursi-toolbar">
        {canManageLokacija && (
          <button
            type="button"
            className="btn btn-primary"
            style={{ width: 'auto' }}
            onClick={() => setModal({ type: 'lokacija-create' })}
          >
            + Nova lokacija
          </button>
        )}
        <div className="date-range">
          <label>
            Od
            <input type="date" value={datumOd} onChange={(e) => setDatumOd(e.target.value)} />
          </label>
          <label>
            Do
            <input type="date" value={datumDo} onChange={(e) => setDatumDo(e.target.value)} />
          </label>
          <button type="button" className="btn btn-outline" style={{ width: 'auto' }} onClick={loadDostupnost}>
            Prikaži kalendar
          </button>
        </div>
      </div>

      {loading ? (
        <p className="empty-hint">Učitavanje lokacija...</p>
      ) : (
        <>
          <LokacijaSalaList
            lokacije={lokacije}
            selectedLokacijaId={selectedLokacijaId}
            onSelectLokacija={setSelectedLokacijaId}
            onEditLokacija={(lok) => setModal({ type: 'lokacija-edit', lokacija: lok })}
            onDeleteLokacija={handleDeleteLokacija}
            onAddSala={() => setModal({ type: 'sala-create' })}
            onEditSala={(sala) => setModal({ type: 'sala-edit', sala })}
            onDeleteSala={handleDeleteSala}
            canManageLokacija={canManageLokacija}
            canManageSala={canManageSala}
          />

          <section className="kalendar-section">
            <h2>Kalendar dostupnosti sala</h2>
            <SalaKalendar
              dostupnost={dostupnost}
              loading={kalendarLoading}
              onRefresh={loadDostupnost}
            />
          </section>
        </>
      )}

      {modal?.type === 'lokacija-create' && (
        <div className="modal-overlay" onClick={closeModal}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()}>
            <h3>Nova lokacija</h3>
            <LokacijaForm onSubmit={handleCreateLokacija} onCancel={closeModal} submitLabel="Kreiraj" />
          </div>
        </div>
      )}

      {modal?.type === 'lokacija-edit' && (
        <div className="modal-overlay" onClick={closeModal}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()}>
            <h3>Izmena lokacije</h3>
            <LokacijaForm
              initial={modal.lokacija}
              onSubmit={handleUpdateLokacija}
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
              onSubmit={handleUpdateSala}
              onCancel={closeModal}
              submitLabel="Sačuvaj izmene"
            />
          </div>
        </div>
      )}
    </>
  );
}
