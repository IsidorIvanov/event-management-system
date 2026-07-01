import { useEffect } from 'react';

/**
 * Overlay + box wrapper za modale.
 * - klik na pozadinu ili Escape zatvara (onClose)
 * - klik unutar boxa se ne propagira
 * - `boxClassName` čuva postojeće stilove (modal-card / modal-box / ...)
 * - `as="form"` renderuje box kao <form> (prosledi onSubmit preko ...rest)
 */
export default function Modal({
  onClose,
  children,
  boxClassName = 'modal-card',
  as: Box = 'div',
  closeOnBackdrop = true,
  ...rest
}) {
  useEffect(() => {
    const onKey = (e) => {
      if (e.key === 'Escape') onClose?.();
    };
    document.addEventListener('keydown', onKey);
    return () => document.removeEventListener('keydown', onKey);
  }, [onClose]);

  return (
    <div className="modal-overlay" onClick={closeOnBackdrop ? onClose : undefined}>
      <Box
        className={boxClassName}
        onClick={(e) => e.stopPropagation()}
        {...rest}
      >
        {children}
      </Box>
    </div>
  );
}
