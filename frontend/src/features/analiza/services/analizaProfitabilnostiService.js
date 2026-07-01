import api from '@/shared/services/api';

export const createDraft = (dogadjajId, data) =>
  api.post(`/analiza/profitabilnost/dogadjaj/${dogadjajId}`, data);

export const finalizeAnaliza = (analizaId) =>
  api.put(`/analiza/profitabilnost/${analizaId}/finalizuj`);

export const updateDraft = (analizaId, data) =>
  api.put(`/analiza/profitabilnost/${analizaId}`, data);

export const getAnaliza = (analizaId) =>
  api.get(`/analiza/profitabilnost/${analizaId}`);

export const getAnalizeByDogadjaj = (dogadjajId) =>
  api.get(`/analiza/profitabilnost/dogadjaj/${dogadjajId}`);
