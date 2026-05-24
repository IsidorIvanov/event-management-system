const STATUS_MAP = {
  DRAFT: { label: 'Nacrt', cls: 'status-draft' },
  APPROVED: { label: 'Odobren', cls: 'status-published' },
  ACTIVE: { label: 'Aktivan', cls: 'status-ongoing' },
  CLOSED: { label: 'Zatvoren', cls: 'status-finished' },
};

const KONTROLA_MAP = {
  ISPOD_PLANA: { label: 'Ispod plana', cls: 'status-published' },
  NA_PLANU: { label: 'Na planu', cls: 'status-ongoing' },
  PREKORACENJE: { label: 'Prekoračenje', cls: 'status-draft' },
};

export function statusLabel(status) {
  return STATUS_MAP[status]?.label || status || '—';
}

export function kontrolaLabel(status) {
  return KONTROLA_MAP[status]?.label || status || '—';
}

export function BudzetStatusBadge({ status }) {
  const meta = STATUS_MAP[status] || { label: status || '—', cls: 'status-draft' };
  return <span className={`status-badge ${meta.cls}`}>{meta.label}</span>;
}

export function StatusKontroleBadge({ status }) {
  const meta = KONTROLA_MAP[status] || { label: status || '—', cls: 'status-draft' };
  return <span className={`status-badge ${meta.cls}`}>{meta.label}</span>;
}
