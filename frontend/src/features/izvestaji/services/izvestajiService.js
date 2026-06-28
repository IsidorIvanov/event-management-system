import api from '@/shared/services/api';

export const generate = (data) => api.post('/izvestaji', data);

export const list = () => api.get('/izvestaji');

export const getById = (id) => api.get(`/izvestaji/${id}`);

export const download = async (id, filename) => {
  const response = await api.get(`/izvestaji/${id}/download`, {
    responseType: 'blob',
  });
  const url = window.URL.createObjectURL(new Blob([response.data]));
  const link = document.createElement('a');
  link.href = url;
  link.setAttribute('download', filename || `izvestaj-${id}.pdf`);
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
};
