import api from '@/shared/services/api';

// Fakture
export const getAllFakture = () => api.get('/fakture');
export const getFakturaById = (id) => api.get(`/fakture/${id}`);
export const createFaktura = (data) => api.post('/fakture', data);
export const addStavka = (id, data) => api.post(`/fakture/${id}/stavke`, data);
export const deleteStavka = (fakturaId, stavkaId) =>
  api.delete(`/fakture/${fakturaId}/stavke/${stavkaId}`);
export const issueFaktura = (id) => api.post(`/fakture/${id}/issue`);
export const cancelFaktura = (id) => api.post(`/fakture/${id}/cancel`);

// Placanja
export const createPlacanje = (fakturaId, data) =>
  api.post(`/placanja/faktura/${fakturaId}`, data);
export const confirmPlacanje = (id) => api.post(`/placanja/${id}/confirm`);
export const failPlacanje = (id) => api.post(`/placanja/${id}/fail`);

// Refundacije — vezane za placanje, ne fakturu
export const requestRefundacija = (placanjeId, data) =>
  api.post(`/refundacije/placanje/${placanjeId}`, data);
export const approveRefundacija = (id) => api.post(`/refundacije/${id}/approve`);
export const rejectRefundacija = (id) => api.post(`/refundacije/${id}/reject`);
export const executeRefundacija = (id) =>
  api.post(`/refundacije/${id}/execute`);
