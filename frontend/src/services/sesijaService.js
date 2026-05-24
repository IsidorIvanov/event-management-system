import api from './api';

export const getSesijaDetalj = (id) => api.get(`/sesija/${id}/detalj`);
export const getSesijeByDogadjaj = (dogadjajId) => api.get(`/sesija/dogadjaj/${dogadjajId}`);
export const createSesija = (data) => api.post('/sesija', data);
export const updateSesija = (id, data) => api.put(`/sesija/${id}`, data);
export const deleteSesija = (id) => api.delete(`/sesija/${id}`);
