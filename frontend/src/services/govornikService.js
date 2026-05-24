import api from './api';

export const getAllGovornici = () => api.get('/govornik');
export const getGovornikByDogadjaj = (dogadjajId) => api.get(`/govornik/dogadjaj/${dogadjajId}`);
export const createGovornik = (data) => api.post('/govornik', data);
export const updateGovornik = (id, data) => api.put(`/govornik/${id}`, data);
export const deleteGovornik = (id) => api.delete(`/govornik/${id}`);
export const addGovornikToSesija = (sesijaId, govornikId) =>
  api.post(`/sesija/${sesijaId}/govornici/${govornikId}`);
export const removeGovornikFromSesija = (sesijaId, govornikId) =>
  api.delete(`/sesija/${sesijaId}/govornici/${govornikId}`);

