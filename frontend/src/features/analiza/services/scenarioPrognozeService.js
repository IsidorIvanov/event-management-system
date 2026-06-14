import api from '@/shared/services/api';

export const createScenario = (dogadjajId, data) =>
  api.post(`/analiza/scenariji-prognoze/dogadjaj/${dogadjajId}`, data);

export const getScenarijiByDogadjaj = (dogadjajId) =>
  api.get(`/analiza/scenariji-prognoze/dogadjaj/${dogadjajId}`);

export const getScenarioMetrics = (scenarioId) =>
  api.get(`/analiza/scenariji-prognoze/${scenarioId}/metrics`);
