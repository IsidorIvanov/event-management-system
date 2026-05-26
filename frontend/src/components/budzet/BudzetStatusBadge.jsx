const STATUS_MAP = {
  DRAFT: { label: 'Nacrt', cls: 'status-draft' },
  APPROVED: { label: 'Odobren', cls: 'status-published' },
  ACTIVE: { label: 'Aktivan', cls: 'status-ongoing' },
  CLOSED: { label: 'Zatvoren', cls: 'status-finished' },
};

const KONTROLA_MAP = {
  ISPOD_PLANA: { label: 'Ispod plana', cls: 'kontrola-ispod' },
  NA_PLANU: { label: 'Na planu', cls: 'kontrola-na' },
  PREKORACENJE: { label: 'Prekoračenje', cls: 'kontrola-preko' },
};

const ALERT_MAP = {
  NONE: { label: 'Nema', cls: 'alert-none' },
  WARNING: { label: 'Upozorenje', cls: 'alert-warning' },
  CRITICAL: { label: 'Kritično', cls: 'alert-critical' },
  EXCEEDED: { label: 'Prekoračeno', cls: 'alert-exceeded' },
};

export function statusLabel(status) {
  return STATUS_MAP[status]?.label || status || '—';
}

export function kontrolaLabel(status) {
  return KONTROLA_MAP[status]?.label || status || '—';
}

export function alertLabel(level) {
  return ALERT_MAP[level]?.label || level || '—';
}

export function BudzetStatusBadge({ status }) {
  const meta = STATUS_MAP[status] || { label: status || '—', cls: 'status-draft' };
  return <span className={`status-badge ${meta.cls}`}>{meta.label}</span>;
}

export function StatusKontroleBadge({ status }) {
  const meta = KONTROLA_MAP[status] || { label: status || '—', cls: 'status-draft' };
  return <span className={`status-badge ${meta.cls}`}>{meta.label}</span>;
}

export function AlertLevelBadge({ level }) {
  const meta = ALERT_MAP[level] || { label: level || '—', cls: 'status-finished' };
  return <span className={`status-badge ${meta.cls}`}>{meta.label}</span>;
}
