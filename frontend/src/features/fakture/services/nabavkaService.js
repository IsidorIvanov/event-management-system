import api from '@/shared/services/api';

// Dobavljači
export const getAllDobavljaci = () => api.get('/nabavka/dobavljac');
export const getAktivniDobavljaci = () => api.get('/nabavka/dobavljac/aktivni');
export const getDobavljacById = (id) => api.get(`/nabavka/dobavljac/${id}`);
export const createDobavljac = (data) => api.post('/nabavka/dobavljac', data);
export const updateDobavljac = (id, data) => api.put(`/nabavka/dobavljac/${id}`, data);
export const deleteDobavljac = (id) => api.delete(`/nabavka/dobavljac/${id}`);
export const deactivateDobavljac = (id) => api.patch(`/nabavka/dobavljac/${id}/deaktiviraj`);

/** @deprecated koristiti getAktivniDobavljaci */
export const getDobavljaci = getAktivniDobavljaci;

// Cenovnik
export const getCenovnik = () => api.get('/nabavka/cenovnik');
export const getCenovnikById = (id) => api.get(`/nabavka/cenovnik/${id}`);
export const getCenovnikByDobavljac = (dobavljacId) => api.get(`/nabavka/cenovnik/dobavljac/${dobavljacId}`);
export const createCenovnik = (data) => api.post('/nabavka/cenovnik', data);
export const updateCenovnik = (id, data) => api.put(`/nabavka/cenovnik/${id}`, data);
export const deleteCenovnik = (id) => api.delete(`/nabavka/cenovnik/${id}`);

// Nabavka
export const getNabavke = () => api.get('/nabavka');
export const getNabavkeByDogadjaj = (dogadjajId) => api.get(`/nabavka/dogadjaj/${dogadjajId}`);
export const getNabavkaById = (id) => api.get(`/nabavka/${id}`);
export const createNabavka = (data) => api.post('/nabavka', data);
export const predloziDobavljaca = (data) => api.post('/nabavka/selekcija/predlog', data);
export const predloziDobavljacaDetaljno = (data) => api.post('/nabavka/selekcija/predlog-detaljno', data);
export const primeniSelekciju = (data) => api.post('/nabavka/selekcija/primeni', data);
export const optimizujAlokaciju = (data) => api.post('/nabavka/alokacija/optimizuj', data);
export const primeniAlokaciju = (data) => api.post('/nabavka/alokacija/primeni', data);
export const posaljiKontakt = (nabavkaId) => api.post(`/nabavka/${nabavkaId}/kontakt/posalji`);
export const detektujPotrebe = (dogadjajId) => api.get(`/nabavka/potrebe/dogadjaj/${dogadjajId}`);
export const validirajPotrebe = (dogadjajId) => api.get(`/nabavka/potrebe/dogadjaj/${dogadjajId}/validacija`);
export const getPorudzbenice = () => api.get('/nabavka/porudzbenica');
export const getPorudzbeniceByDogadjaj = (dogadjajId) => api.get(`/nabavka/porudzbenica/dogadjaj/${dogadjajId}`);
export const generisiPorudzbenicu = (data) => api.post('/nabavka/porudzbenica/generisi', data);
export const updatePorudzbenicaStatus = (id, status) =>
  api.patch(`/nabavka/porudzbenica/${id}/status`, { status });
