// Status događaja — prikazne oznake, CSS klase i opcije filtera.

export const STATUS_DISPLAY = {
  OBJAVLJEN: 'Objavljen',
  AKTIVAN: 'Aktivan',
  DRAFT: 'Nacrt',
  ZAVRSEN: 'Završen',
};

export const STATUS_CLASS = {
  OBJAVLJEN: 'status-badge status-published',
  AKTIVAN: 'status-badge status-ongoing',
  DRAFT: 'status-badge status-draft',
  ZAVRSEN: 'status-badge status-finished',
};

export const STATUS_OPTIONS = ['SVE', 'OBJAVLJEN', 'AKTIVAN', 'DRAFT', 'ZAVRSEN'];
