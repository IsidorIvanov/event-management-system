import api from '@/shared/services/api';

export const getAllOprema = () => api.get('/inventar/oprema');
export const getOpremaById = (id) => api.get(`/inventar/oprema/${id}`);
export const createOprema = (data) => api.post('/inventar/oprema', data);
export const updateOprema = (id, data) => api.put(`/inventar/oprema/${id}`, data);
export const deleteOprema = (id) => api.delete(`/inventar/oprema/${id}`);

export const primiIzPorudzbenice = (porudzbenicaId) =>
  api.post(`/inventar/prijem/porudzbenica/${porudzbenicaId}`);
export const statusPrijemaPorudzbenice = (porudzbenicaId) =>
  api.get(`/inventar/prijem/porudzbenica/${porudzbenicaId}/status`);
export const getKretanjaZaOprema = (opremaId) => api.get(`/inventar/kretanje/oprema/${opremaId}`);

export const getDodelePoDogadjaju = (dogadjajId) => api.get(`/inventar/dodela/dogadjaj/${dogadjajId}`);
export const rezervisiOpremu = (data) => api.post('/inventar/dodela', data);
export const aktivirajDodelu = (id) => api.patch(`/inventar/dodela/${id}/aktiviraj`);
export const oslobodiDodelu = (id) => api.patch(`/inventar/dodela/${id}/oslobodi`);
export const otkaziDodelu = (id) => api.patch(`/inventar/dodela/${id}/otkazi`);

export const validirajPotrebeInventarom = (dogadjajId) =>
  api.get(`/inventar/potrebe/dogadjaj/${dogadjajId}`);
export const getPredlogAlokacijeOpreme = (dogadjajId) =>
  api.get(`/inventar/alokacija/dogadjaj/${dogadjajId}/predlog`);
export const primeniAlokacijuOpreme = (dogadjajId) =>
  api.post(`/inventar/alokacija/dogadjaj/${dogadjajId}/primeni`);
export const getSredstvaDogadjaja = (dogadjajId) => api.get(`/dogadjaj/${dogadjajId}/sredstva`);
