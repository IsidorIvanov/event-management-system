import api from './api';

export const getSesijaDetalj = (id) => api.get(`/sesija/${id}/detalj`);
