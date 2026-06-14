import { useState } from 'react';

export default function BudzetKategorijeModal({
  kategorije,
  onClose,
  onCreate,
  onUpdate,
  onDelete,
  loading,
}) {
  const [form, setForm] = useState({ naziv: '', opis: '' });
  const [editId, setEditId] = useState(null);

  const reset = () => {
    setForm({ naziv: '', opis: '' });
    setEditId(null);
  };

  const startEdit = (kategorija) => {
    setEditId(kategorija.kategorijaId);
    setForm({ naziv: kategorija.naziv, opis: kategorija.opis || '' });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (editId) {
      await onUpdate(editId, form);
    } else {
      await onCreate(form);
    }
    reset();
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-box modal-box-wide" onClick={(e) => e.stopPropagation()}>
        <h3 className="modal-title">Kategorije budžeta</h3>

        <form onSubmit={handleSubmit}>
          <div className="form-row">
            <div className="form-group">
              <label>Naziv</label>
              <input
                value={form.naziv}
                onChange={(e) => setForm((prev) => ({ ...prev, naziv: e.target.value }))}
                maxLength={100}
                required
              />
            </div>
            <div className="form-group">
              <label>Opis</label>
              <input
                value={form.opis}
                onChange={(e) => setForm((prev) => ({ ...prev, opis: e.target.value }))}
                maxLength={500}
              />
            </div>
          </div>
          <div className="modal-actions" style={{ justifyContent: 'flex-start', marginBottom: '1rem' }}>
            <button type="submit" className="btn btn-primary btn-sm" disabled={loading}>
              {editId ? 'Sačuvaj izmenu' : 'Dodaj kategoriju'}
            </button>
            {editId && (
              <button type="button" className="btn btn-outline btn-sm" onClick={reset} disabled={loading}>Otkaži izmenu</button>
            )}
          </div>
        </form>

        <table className="events-table">
          <thead>
            <tr><th>Naziv</th><th>Opis</th><th>Akcije</th></tr>
          </thead>
          <tbody>
            {kategorije.length === 0 ? (
              <tr><td colSpan={3} style={{ textAlign: 'center', color: 'var(--text-muted)' }}>Nema kategorija.</td></tr>
            ) : kategorije.map((k) => (
              <tr key={k.kategorijaId}>
                <td>{k.naziv}</td>
                <td>{k.opis || '—'}</td>
                <td>
                  <div className="action-buttons">
                    <button type="button" className="btn btn-outline btn-xs" onClick={() => startEdit(k)} disabled={loading}>Uredi</button>
                    <button type="button" className="btn btn-danger-outline btn-xs" onClick={() => onDelete(k.kategorijaId)} disabled={loading}>Obriši</button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>

        <div className="modal-actions">
          <button type="button" className="btn btn-outline" onClick={onClose}>Zatvori</button>
        </div>
      </div>
    </div>
  );
}
