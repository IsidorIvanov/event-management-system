import api from './api';

export const porukeService = {
  getKontakti: () => api.get('/poruke/kontakti').then(r => r.data),
  getKonverzacija: (korisnikId) => api.get(`/poruke/konverzacija/${korisnikId}`).then(r => r.data),
  posaljiPoruku: (primalacId, sadrzaj) =>
    api.post('/poruke/posalji', { primalacId, sadrzaj }).then(r => r.data),
  getNeprocitane: () => api.get('/poruke/neprocitane').then(r => r.data),
};

