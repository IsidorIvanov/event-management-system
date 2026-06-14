import api from '@/shared/services/api';

export const getSaleByLokacija = (lokacijaId) =>
  api.get('/sala', { params: { lokacijaId } });

export const getSala = (lokacijaId, nazivSale) =>
  api.get(`/sala/${lokacijaId}/${encodeURIComponent(nazivSale)}`);

export const createSala = (data) => api.post('/sala', data);
export const updateSala = (lokacijaId, nazivSale, data) =>
  api.put(`/sala/${lokacijaId}/${encodeURIComponent(nazivSale)}`, data);
export const deleteSala = (lokacijaId, nazivSale) =>
  api.delete(`/sala/${lokacijaId}/${encodeURIComponent(nazivSale)}`);

export const getDostupnost = (lokacijaId, datumOd, datumDo) =>
  api.get('/sala/dostupnost', { params: { lokacijaId, datumOd, datumDo } });
