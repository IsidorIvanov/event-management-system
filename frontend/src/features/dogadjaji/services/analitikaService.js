import api from '@/shared/services/api';

export const getAnalitika = (dogadjajId) => api.get(`/dogadjaj/${dogadjajId}/analitika`);

export const downloadPdf = async (dogadjajId, filename) => {
  const response = await api.get(`/dogadjaj/${dogadjajId}/analitika/pdf`, {
    responseType: 'blob',
  });
  const url = window.URL.createObjectURL(new Blob([response.data], { type: 'application/pdf' }));
  const link = document.createElement('a');
  link.href = url;
  link.setAttribute('download', filename || `izvestaj-dogadjaj-${dogadjajId}.pdf`);
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
};
