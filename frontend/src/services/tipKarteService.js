import api from './api';

export const getTipKarteByDogadjaj = (dogadjajId) =>
  api.get(`/tip-karte/dogadjaj/${dogadjajId}`);

export const createTipKarte = (data) =>
  api.post('/tip-karte', data);

export const updateTipKarte = (dogadjajId, nazivTipa, data) =>
  api.put(`/tip-karte/${dogadjajId}/${encodeURIComponent(nazivTipa)}`, data);

export const deleteTipKarte = (dogadjajId, nazivTipa) =>
  api.delete(`/tip-karte/${dogadjajId}/${encodeURIComponent(nazivTipa)}`);

