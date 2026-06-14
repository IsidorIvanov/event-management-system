// Centralizovani formatери za datume, vreme i novac.
// Zadržavaju isti izlaz kao prethodne per-fajl implementacije; pozivi koji
// koriste drugačije opcije prosleđuju ih kao argument.

export const DATE_OPTS = { day: '2-digit', month: 'short', year: 'numeric' };

/** Formatira datum/timestamp. Vraća placeholder ('—') za prazne vrednosti. */
export const formatDate = (value, opts = DATE_OPTS, locale = 'sr-Latn') =>
  value ? new Date(value).toLocaleDateString(locale, opts) : '—';

/** Formatira datum + vreme. */
export const formatDateTime = (
  value,
  opts = { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' },
  locale = 'sr-Latn',
) => (value ? new Date(value).toLocaleString(locale, opts) : '—');

/** Formatira vreme (HH:mm). */
export const formatTime = (value, locale = 'sr-Latn') =>
  value
    ? new Date(value).toLocaleTimeString(locale, { hour: '2-digit', minute: '2-digit' })
    : '';

/** Formatira iznos kao RSD valutu (npr. "1.234,00 RSD"). */
export const formatMoney = (value) => {
  const number = Number(value ?? 0);
  return new Intl.NumberFormat('sr-Latn-RS', {
    style: 'currency',
    currency: 'RSD',
    maximumFractionDigits: 2,
  }).format(Number.isNaN(number) ? 0 : number);
};
