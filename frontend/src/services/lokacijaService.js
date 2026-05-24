import api from './api';

export const getLokacije = () => api.get('/lokacija');
export const getLokacija = (id) => api.get(`/lokacija/${id}`);
export const createLokacija = (data) => api.post('/lokacija', data);
export const updateLokacija = (id, data) => api.put(`/lokacija/${id}`, data);
export const deleteLokacija = (id) => api.delete(`/lokacija/${id}`);
