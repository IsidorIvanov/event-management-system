import api from './api';

export const getBudzeti = () => api.get('/budzet');

export const getBudzet = (id) => api.get(`/budzet/${id}`);

export const getBudzetiByDogadjaj = (dogadjajId) => api.get(`/budzet/dogadjaj/${dogadjajId}`);

export const createBudzet = (data) => api.post('/budzet', data);

export const updateBudzet = (id, data) => api.put(`/budzet/${id}`, data);

export const deleteBudzet = (id) => api.delete(`/budzet/${id}`);

export const addStavka = (budzetId, data) => api.post(`/budzet/${budzetId}/stavke`, data);

export const updateStavka = (budzetId, kategorijaId, data) =>
  api.put(`/budzet/${budzetId}/stavke/${kategorijaId}`, data);

export const deleteStavka = (budzetId, kategorijaId) =>
  api.delete(`/budzet/${budzetId}/stavke/${kategorijaId}`);

export const approveBudzet = (id) => api.post(`/budzet/${id}/approve`);

export const activateBudzet = (id) => api.post(`/budzet/${id}/activate`);

export const closeBudzet = (id) => api.post(`/budzet/${id}/close`);

export const getKategorije = () => api.get('/budzet/kategorije');

export const createKategorija = (data) => api.post('/budzet/kategorije', data);

export const updateKategorija = (id, data) => api.put(`/budzet/kategorije/${id}`, data);

export const deleteKategorija = (id) => api.delete(`/budzet/kategorije/${id}`);
