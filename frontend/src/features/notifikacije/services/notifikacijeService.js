import api from '@/shared/services/api';

export const getMojeNotifikacije = () => api.get('/notifikacije/moje');
export const getNeprocitaneBroj = () => api.get('/notifikacije/neprocitane/broj');
export const oznaciKaoProcitano = (id) => api.post(`/notifikacije/${id}/procitano`);
