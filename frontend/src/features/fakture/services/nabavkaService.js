import api from '@/shared/services/api';

export const getDobavljaci = () => api.get('/nabavka/dobavljac/aktivni');

export const getCenovnik = () => api.get('/nabavka/cenovnik');

export const getNabavke = () => api.get('/nabavka');

export const getNabavkeByDogadjaj = (dogadjajId) => api.get(`/nabavka/dogadjaj/${dogadjajId}`);

export const createNabavka = (data) => api.post('/nabavka', data);

export const predloziDobavljaca = (data) => api.post('/nabavka/selekcija/predlog', data);

export const primeniSelekciju = (data) => api.post('/nabavka/selekcija/primeni', data);

export const posaljiKontakt = (nabavkaId) => api.post(`/nabavka/${nabavkaId}/kontakt/posalji`);

export const detektujPotrebe = (dogadjajId) => api.get(`/nabavka/potrebe/dogadjaj/${dogadjajId}`);

export const validirajPotrebe = (dogadjajId) => api.get(`/nabavka/potrebe/dogadjaj/${dogadjajId}/validacija`);

export const getPorudzbenice = () => api.get('/nabavka/porudzbenica');

export const getPorudzbeniceByDogadjaj = (dogadjajId) => api.get(`/nabavka/porudzbenica/dogadjaj/${dogadjajId}`);

export const generisiPorudzbenicu = (data) => api.post('/nabavka/porudzbenica/generisi', data);

export const updatePorudzbenicaStatus = (id, status) =>
  api.patch(`/nabavka/porudzbenica/${id}/status`, { status });
