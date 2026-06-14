import api from '@/shared/services/api';

// Preporuke (recommendations) endpoints
export const getMojePreporuke = () => api.get('/preporuka/moje');
export const getKontaktiZaDogadjaj = (dogadjajId) => api.get(`/preporuka/${dogadjajId}/kontakti`);
