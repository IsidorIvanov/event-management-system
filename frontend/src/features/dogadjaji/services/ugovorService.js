import api from "@/shared/services/api.js";

export const ugovorService = {
  getAll: (params = {}) => api.get("/ugovori", { params }).then((res) => res.data),
  getAktivni: (dobavljacId) =>
    api.get("/ugovori/aktivni", { params: { dobavljacId } }).then((res) => res.data),
  create: (payload) => api.post("/ugovori", payload).then((res) => res.data),
  attachDocument: (id, dokumentUrl) =>
    api.put(`/ugovori/${id}/dokument`, { dokumentUrl }).then((res) => res.data),
  activate: (id) => api.put(`/ugovori/${id}/aktiviraj`).then((res) => res.data),
  terminate: (id) => api.put(`/ugovori/${id}/raskini`).then((res) => res.data),
};
