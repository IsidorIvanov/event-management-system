import Modal from '@/shared/components/Modal';

/**
 * Potvrda akcije (najčešće brisanje).
 * Zamenjuje ručno kopirane delete-confirm modale.
 */
export default function ConfirmDialog({
  icon = '🗑️',
  title,
  children,
  confirmLabel = 'Obriši',
  cancelLabel = 'Otkaži',
  confirmClassName = 'btn btn-danger',
  onConfirm,
  onCancel,
  loading = false,
}) {
  return (
    <Modal onClose={onCancel} boxClassName="modal-box">
      {icon && <div className="modal-icon">{icon}</div>}
      <h3 className="modal-title">{title}</h3>
      <p className="modal-body">{children}</p>
      <div className="modal-actions">
        <button className="btn btn-outline" onClick={onCancel} disabled={loading}>
          {cancelLabel}
        </button>
        <button className={confirmClassName} onClick={onConfirm} disabled={loading}>
          {confirmLabel}
        </button>
      </div>
    </Modal>
  );
}
