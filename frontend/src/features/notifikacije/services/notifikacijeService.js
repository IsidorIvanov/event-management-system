import api from '@/shared/services/api';

/**
 * Stranica mojih obaveštenja.
 * @param {{ tip?: string, page?: number, size?: number }} params
 *   tip — filter po tipu (izostavljen → svi), page — 0-based, size — po strani.
 */
export const getMojeNotifikacije = ({ tip, page = 0, size = 10 } = {}) =>
  api.get('/notifikacije/moje', { params: { ...(tip ? { tip } : {}), page, size } });
export const getNeprocitaneBroj = () => api.get('/notifikacije/neprocitane/broj');
export const oznaciKaoProcitano = (id) => api.post(`/notifikacije/${id}/procitano`);
