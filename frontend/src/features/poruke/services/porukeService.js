import api from '@/shared/services/api';

export const getKontakti = () => api.get('/poruke/kontakti');
export const getKonverzacija = (korisnikId) => api.get(`/poruke/konverzacija/${korisnikId}`);
export const posaljiPoruku = (primalacId, sadrzaj) =>
  api.post('/poruke/posalji', { primalacId, sadrzaj });
export const getNeprocitane = () => api.get('/poruke/neprocitane');
