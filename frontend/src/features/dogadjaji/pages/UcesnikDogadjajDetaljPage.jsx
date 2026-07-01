import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import api from '@/shared/services/api';
import * as registracijaApi from '@/features/dogadjaji/services/registracijaService';
import { NOTIF_NEW_EVENT } from '@/features/notifikacije/hooks/useNotifikacije';
import RegistracijaModal from '@/features/dogadjaji/components/RegistracijaModal';
import InformacijeTab from '@/features/dogadjaji/components/ucesnik-detail/InformacijeTab';
import AgendaTab from '@/features/dogadjaji/components/ucesnik-detail/AgendaTab';
import SpeakersTab from '@/features/dogadjaji/components/ucesnik-detail/SpeakersTab';
import UcesniciTab from '@/features/dogadjaji/components/ucesnik-detail/UcesniciTab';
import { formatDate as fmtDate } from '@/shared/utils/format';

const formatDate = (s) => fmtDate(s, { day: '2-digit', month: 'short', year: 'numeric' });

const TABS = ['Informacije', 'Agenda', 'Govornici', 'Učesnici'];

export default function UcesnikDogadjajDetaljPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [event, setEvent] = useState(null);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState('Informacije');
  const [showRegModal, setShowRegModal] = useState(false);
  const [registered, setRegistered] = useState(false);
  const [userRegistration, setUserRegistration] = useState(null);

  useEffect(() => {
    Promise.all([
      api.get(`/dogadjaj/${id}`),
      registracijaApi.getMyRegistrations(),
    ])
      .then(([eventRes, regRes]) => {
        setEvent(eventRes.data);
        const activeReg = regRes.data.find(
          (r) => r.dogadjajId === Number(id) && r.status !== 'OTKAZANA'
        );
        setRegistered(!!activeReg);
        setUserRegistration(activeReg || null);
      })
      .catch(() => setEvent(null))
      .finally(() => setLoading(false));
  }, [id]);

  // Osveži događaj i registraciju kada stigne notifikacija, da se izmene odmah
  // odraze: promocija sa liste čekanja (D3) ili izmenjen datum/lokacija (D5).
  useEffect(() => {
    const onNotif = () => {
      api.get(`/dogadjaj/${id}`)
        .then((res) => setEvent(res.data))
        .catch(() => {});
      registracijaApi.getMyRegistrations()
        .then((res) => {
          const activeReg = res.data.find(
            (r) => r.dogadjajId === Number(id) && r.status !== 'OTKAZANA'
          );
          setRegistered(!!activeReg);
          setUserRegistration(activeReg || null);
        })
        .catch(() => {});
    };
    window.addEventListener(NOTIF_NEW_EVENT, onNotif);
    return () => window.removeEventListener(NOTIF_NEW_EVENT, onNotif);
  }, [id]);

  if (loading) {
    return <div className="ucesnik-empty" style={{ padding: '3rem' }}>Učitavanje...</div>;
  }

  if (!event) {
    return (
      <div className="ucesnik-empty-box" style={{ margin: '2rem 0' }}>
        <p>Događaj nije pronađen.</p>
        <button className="discover-btn-details" style={{ marginTop: '1rem' }} onClick={() => navigate(-1)}>
          ← Nazad
        </button>
      </div>
    );
  }

  const onWaitlist =
    registered &&
    (userRegistration?.statusKarte === 'NA_CEKANJU' ||
      userRegistration?.status === 'NA_CEKANJU');

  return (
    <div className="ev-detail-page">
      {showRegModal && (
        <RegistracijaModal
          event={event}
          onClose={() => setShowRegModal(false)}
          onSuccess={(newReg) => {
            setShowRegModal(false);
            setRegistered(true);
            if (newReg) setUserRegistration(newReg);
            else {
              // Reload registrations to get the full object
              registracijaApi.getMyRegistrations().then((res) => {
                const activeReg = res.data.find(
                  (r) => r.dogadjajId === Number(id) && r.status !== 'OTKAZANA'
                );
                if (activeReg) setUserRegistration(activeReg);
              }).catch(() => {});
            }
          }}
        />
      )}

      {/* Gornja traka */}
      <div className="ev-detail-topbar">
        <button className="ev-back-btn" onClick={() => navigate(-1)}>
          ← Nazad
        </button>
        {registered ? (
          onWaitlist ? (
            <span style={{ fontSize: '0.9rem', color: 'var(--warning)', fontWeight: 600 }}>
              ⏳ Na listi čekanja
            </span>
          ) : (
            <span style={{ fontSize: '0.9rem', color: 'var(--success)', fontWeight: 600 }}>
              ✓ Uspešno registrovani!
            </span>
          )
        ) : (
          <button className="discover-btn-register ev-register-btn" onClick={() => setShowRegModal(true)}>
            Registruj se za ovaj događaj
          </button>
        )}
      </div>

      {/* Naslov + meta */}
      <div className="ev-detail-hero">
        <h1 className="ev-detail-title">{event.naziv}</h1>
        <p className="ev-detail-meta">
          {formatDate(event.datumPocetka)}
          {event.lokacijaGrad ? ` · ${event.lokacijaGrad}` : ''}
          {event.lokacijaDrzava ? `, ${event.lokacijaDrzava}` : ''}
          {' · Uživo'}
        </p>
      </div>

      {/* Tabovi */}
      <div className="ev-tabs">
        {TABS.map((tab) => (
          <button
            key={tab}
            className={`ev-tab-btn${activeTab === tab ? ' active' : ''}`}
            onClick={() => setActiveTab(tab)}
          >
            {tab}
          </button>
        ))}
      </div>

      {/* Sadržaj taba */}
      {activeTab === 'Informacije' && (
        <InformacijeTab
          event={event}
          registered={registered}
          onWaitlist={onWaitlist}
          onRegister={() => setShowRegModal(true)}
        />
      )}

      {activeTab === 'Agenda' && (
        <AgendaTab event={event} isRegistered={registered} userRegistration={userRegistration} />
      )}

      {activeTab === 'Govornici' && <SpeakersTab event={event} />}

      {activeTab === 'Učesnici' && <UcesniciTab event={event} />}
    </div>
  );
}
