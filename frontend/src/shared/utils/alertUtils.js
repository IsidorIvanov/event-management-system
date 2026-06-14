export const notifyAlerts = (alerts, toast) => {
  const alertLevels = new Set(['WARNING', 'CRITICAL', 'EXCEEDED']);
  (alerts || [])
    .filter((alert) => alertLevels.has(alert.alertLevel))
    .forEach((alert) => {
      toast(
        alert.poruka || alert.alertPoruka || `Upozorenje za kategoriju ${alert.kategorijaNaziv}`,
        alert.alertLevel === 'WARNING' ? 'info' : 'error'
      );
    });
};
