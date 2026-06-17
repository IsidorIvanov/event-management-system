import api from '@/shared/services/api';

export const getPredlogCenaKarata = (dogadjajId) =>
  api.get(`/dogadjaj/${dogadjajId}/cene/predlog`);

export const primeniCenuKarte = (dogadjajId, nazivTipa, cena) =>
  api.patch(`/tip-karte/${dogadjajId}/${encodeURIComponent(nazivTipa)}/cena`, { cena });

export const primeniCenuSale = (lokacijaId, nazivSale, datum) =>
  api.patch(`/sala/${lokacijaId}/${encodeURIComponent(nazivSale)}/cena`, null, { params: { datum } });

export const getPredlogCeneSale = (lokacijaId, nazivSale, datum) =>
  api.get(`/sala/${lokacijaId}/${encodeURIComponent(nazivSale)}/cena/predlog`, { params: { datum } });

export const getPredlogSala = (params) =>
  api.get('/sala/predlog', { params });

export const postPredlogSala = (data) =>
  api.post('/sala/predlog', data);
