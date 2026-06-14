/**
 * Jedinstveni <tr> za loading / error / empty stanja u tabelama.
 * Vraća null kada ima podataka (pa se renderuju redovi).
 */
export default function TableStateRow({
  colSpan,
  loading,
  error,
  isEmpty,
  loadingText = 'Učitavanje...',
  emptyText = 'Nema rezultata.',
}) {
  let content = null;
  let color = 'var(--text-muted)';

  if (loading) content = loadingText;
  else if (error) {
    content = error;
    color = 'var(--danger)';
  } else if (isEmpty) content = emptyText;

  if (content === null) return null;

  return (
    <tr>
      <td colSpan={colSpan} style={{ textAlign: 'center', color, padding: '2rem' }}>
        {content}
      </td>
    </tr>
  );
}
