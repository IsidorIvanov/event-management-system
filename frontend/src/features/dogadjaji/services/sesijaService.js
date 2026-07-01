import api from '@/shared/services/api';

export const getSesijaDetalj = (id) => api.get(`/sesija/${id}/detalj`);
export const getSesijeByDogadjaj = (dogadjajId) => api.get(`/sesija/dogadjaj/${dogadjajId}`);
export const createSesija = (data) => api.post('/sesija', data);
export const updateSesija = (id, data) => api.put(`/sesija/${id}`, data);
export const deleteSesija = (id) => api.delete(`/sesija/${id}`);

// Raspored (schedule) endpoints
export const getMojRaspored = () => api.get('/raspored/moj');
export const getMojRasporedIds = () => api.get('/raspored/moj/ids');
export const addToRaspored = (sesijaId) => api.post(`/raspored/${sesijaId}`);
export const removeFromRaspored = (sesijaId) => api.delete(`/raspored/${sesijaId}`);
