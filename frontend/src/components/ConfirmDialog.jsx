import { useEffect, useRef } from 'react';

/**
 * A blocking confirmation for actions that cannot be undone. Cancel takes focus on open and Escape
 * dismisses, so the safe way out is always the easiest one — while a confirmed action is in flight
 * both are disabled, since dismissing then would hide an operation that is still running.
 */
export default function ConfirmDialog({
  title,
  message,
  confirmLabel = 'Delete',
  busy = false,
  onConfirm,
  onCancel,
}) {
  const cancelRef = useRef(null);

  useEffect(() => {
    cancelRef.current?.focus();
  }, []);

  useEffect(() => {
    const onKey = (e) => {
      if (e.key === 'Escape' && !busy) onCancel();
    };
    document.addEventListener('keydown', onKey);
    return () => document.removeEventListener('keydown', onKey);
  }, [busy, onCancel]);

  return (
    <div
      className="confirm-backdrop"
      onClick={() => !busy && onCancel()}
      role="presentation"
    >
      <style>{confirmStyles}</style>
      {/* The click guard stops a stray click inside the card from dismissing it. */}
      <div
        className="confirm-card"
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="confirm-title"
        aria-describedby="confirm-message"
        onClick={(e) => e.stopPropagation()}
      >
        <h2 className="confirm-title" id="confirm-title">{title}</h2>
        <p className="confirm-message" id="confirm-message">{message}</p>
        <div className="confirm-actions">
          <button
            ref={cancelRef}
            type="button"
            className="confirm-btn is-cancel"
            disabled={busy}
            onClick={onCancel}
          >
            Cancel
          </button>
          <button
            type="button"
            className="confirm-btn is-confirm"
            disabled={busy}
            onClick={onConfirm}
          >
            {busy ? 'Working…' : confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );
}

const confirmStyles = `
  .confirm-backdrop {
    position: fixed;
    inset: 0;
    z-index: 100;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 20px;
    background: rgba(8, 10, 14, 0.66);
  }
  .confirm-card {
    width: 100%;
    max-width: 420px;
    padding: 28px;
    box-sizing: border-box;
    font-family: system-ui, 'Segoe UI', Roboto, sans-serif;
    background: #23262d;
    border: 1px solid #3a3e46;
    border-radius: 16px;
    box-shadow: 0 24px 60px -12px rgba(0, 0, 0, 0.6);
  }
  .confirm-title {
    margin: 0;
    font-size: 21px;
    font-weight: 600;
    color: #f2f4f7;
    overflow-wrap: anywhere;
  }
  .confirm-message {
    margin: 12px 0 26px;
    font-size: 15px;
    line-height: 1.55;
    color: #9aa0ab;
  }
  .confirm-actions {
    display: flex;
    justify-content: flex-end;
    gap: 10px;
  }
  .confirm-btn {
    padding: 11px 20px;
    font-family: inherit;
    font-size: 15px;
    font-weight: 500;
    border: 1px solid transparent;
    border-radius: 10px;
    cursor: pointer;
    transition: background 0.2s, color 0.2s, filter 0.2s;
  }
  .confirm-btn:disabled {
    opacity: 0.6;
    cursor: default;
  }
  .confirm-btn.is-cancel {
    color: #c8ccd3;
    background: #2b2f37;
    border-color: #3a3e46;
  }
  .confirm-btn.is-cancel:hover:not(:disabled) {
    background: #333944;
    color: #f2f4f7;
  }
  .confirm-btn.is-confirm {
    color: #fff;
    background: #d24141;
  }
  .confirm-btn.is-confirm:hover:not(:disabled) {
    filter: brightness(1.1);
  }
  .confirm-btn:focus-visible {
    outline: 2px solid #5b8def;
    outline-offset: 2px;
  }
`;
