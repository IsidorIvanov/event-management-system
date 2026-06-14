import api from '@/shared/services/api';

export const register = (data) => api.post('/registracija', data);
export const getMyRegistrations = () => api.get('/registracija/moje');
export const cancelRegistration = (id) => api.delete(`/registracija/${id}`);
export const getRegistracijeByDogadjaj = (dogadjajId) => api.get(`/registracija/dogadjaj/${dogadjajId}`);
