import { useRef, useState } from 'react';
import { describeSchedule, STATUS_LABELS, toInstant, toLocalInput } from '../utils/schedule';
import ConfirmDialog from './ConfirmDialog';

const EditIcon = () => (
  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor"
       strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    <path d="M12 20h9" />
    <path d="M16.5 3.5a2.12 2.12 0 0 1 3 3L7 19l-4 1 1-4Z" />
  </svg>
);

const ImageIcon = () => (
  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor"
       strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    <rect x="3" y="3" width="18" height="18" rx="2" />
    <circle cx="8.5" cy="8.5" r="1.5" />
    <path d="m21 15-5-5L5 21" />
  </svg>
);

const ChevronUpIcon = () => (
  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor"
       strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    <path d="m6 15 6-6 6 6" />
  </svg>
);

const ChevronDownIcon = () => (
  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor"
       strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    <path d="m6 9 6 6 6-6" />
  </svg>
);

const EyeIcon = () => (
  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor"
       strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    <path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7-10-7-10-7Z" />
    <circle cx="12" cy="12" r="3" />
  </svg>
);

const EyeOffIcon = () => (
  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor"
       strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    <path d="M10.7 5.1A10.6 10.6 0 0 1 12 5c6.5 0 10 7 10 7a18 18 0 0 1-2.9 3.9" />
    <path d="M6.6 6.6A18 18 0 0 0 2 12s3.5 7 10 7a10.4 10.4 0 0 0 5.4-1.4" />
    <path d="M9.9 9.9a3 3 0 0 0 4.2 4.2" />
    <path d="m2 2 20 20" />
  </svg>
);

const ImageOffIcon = () => (
  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor"
       strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    <path d="M21 15V5a2 2 0 0 0-2-2H9" />
    <path d="M3 7v12a2 2 0 0 0 2 2h12" />
    <path d="m5 19 6-6" />
    <path d="m2 2 20 20" />
  </svg>
);

const TrashIcon = () => (
  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor"
       strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    <path d="M3 6h18" />
    <path d="M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
    <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6" />
    <path d="M10 11v6" />
    <path d="M14 11v6" />
  </svg>
);

const emptyDraft = { title: '', url: '', visibleFrom: '', visibleUntil: '' };

export default function LinkList({
  links,
  onUpdate,
  onDelete,
  onToggleActive,
  onMoveUp,
  onMoveDown,
  onUploadThumbnail,
  onRemoveThumbnail,
}) {
  const [editingId, setEditingId] = useState(null);
  const [draft, setDraft] = useState(emptyDraft);
  const [error, setError] = useState('');
  // The link awaiting a delete confirmation, kept whole so the prompt can name it.
  const [pendingDelete, setPendingDelete] = useState(null);
  const [deleting, setDeleting] = useState(false);
  // One hidden file input, retargeted per row; the ref records which link is being uploaded to.
  const fileInputRef = useRef(null);
  const uploadingIdRef = useRef(null);

  const startEdit = (link) => {
    setEditingId(link.id);
    setError('');
    setDraft({
      title: link.title,
      url: link.url,
      visibleFrom: toLocalInput(link.visibleFrom),
      visibleUntil: toLocalInput(link.visibleUntil),
    });
  };

  const cancelEdit = () => {
    setEditingId(null);
    setDraft(emptyDraft);
    setError('');
  };

  const saveEdit = async (id) => {
    if (!draft.title || !draft.url) return;

    const from = toInstant(draft.visibleFrom);
    const until = toInstant(draft.visibleUntil);
    if (from && until && new Date(until) <= new Date(from)) {
      setError('The end time must be after the start time.');
      return;
    }

    try {
      // The schedule goes on every save, including when both fields are empty: the API replaces the
      // whole link, so leaving them out would clear a window the user never touched.
      await onUpdate(id, { title: draft.title, url: draft.url, visibleFrom: from, visibleUntil: until });
      cancelEdit();
    } catch (err) {
      setError(err.response?.data?.error || 'Could not save that link.');
    }
  };

  const toggleActive = async (id) => {
    try {
      setError('');
      await onToggleActive(id);
    } catch (err) {
      setError(err.response?.data?.error || 'Could not change that link’s visibility.');
    }
  };

  const pickThumbnail = (linkId) => {
    uploadingIdRef.current = linkId;
    fileInputRef.current?.click();
  };

  const handleFileChosen = async (e) => {
    const file = e.target.files?.[0];
    const linkId = uploadingIdRef.current;
    // Cleared first so picking the same file again still fires a change event.
    e.target.value = '';
    if (!file || !linkId) return;

    try {
      setError('');
      await onUploadThumbnail(linkId, file);
    } catch (err) {
      setError(err.response?.data?.error || 'Could not upload that image.');
    }
  };

  const removeThumbnail = async (linkId) => {
    try {
      setError('');
      await onRemoveThumbnail(linkId);
    } catch (err) {
      setError(err.response?.data?.error || 'Could not remove that thumbnail.');
    }
  };

  const confirmDelete = async () => {
    setDeleting(true);
    try {
      setError('');
      await onDelete(pendingDelete.id);
      setPendingDelete(null);
    } catch (err) {
      setError(err.response?.data?.error || 'Could not delete that link.');
      setPendingDelete(null);
    } finally {
      setDeleting(false);
    }
  };

  return (
    <>
      <input
        ref={fileInputRef}
        type="file"
        accept="image/png,image/jpeg"
        onChange={handleFileChosen}
        hidden
      />
      {error && <p className="link-list-error" role="alert">{error}</p>}

      <ul className="link-list">
        {links.map((link, i) => (
          <li key={link.id} className="link-row">
            {editingId === link.id ? (
              <div className="link-edit">
                <input
                  className="link-edit-input"
                  placeholder="Title"
                  value={draft.title}
                  onChange={(e) => setDraft({ ...draft, title: e.target.value })}
                />
                <input
                  className="link-edit-input"
                  placeholder="URL"
                  value={draft.url}
                  onChange={(e) => setDraft({ ...draft, url: e.target.value })}
                />
                <div className="link-edit-schedule">
                  <label className="link-schedule-label">
                    Goes live
                    <input
                      type="datetime-local"
                      className="link-edit-input"
                      value={draft.visibleFrom}
                      onChange={(e) => setDraft({ ...draft, visibleFrom: e.target.value })}
                    />
                  </label>
                  <label className="link-schedule-label">
                    Expires
                    <input
                      type="datetime-local"
                      className="link-edit-input"
                      value={draft.visibleUntil}
                      onChange={(e) => setDraft({ ...draft, visibleUntil: e.target.value })}
                    />
                  </label>
                </div>
                <div className="link-edit-actions">
                  <button onClick={() => saveEdit(link.id)} className="link-text-btn is-save" type="button">
                    Save
                  </button>
                  <button onClick={cancelEdit} className="link-text-btn is-cancel" type="button">
                    Cancel
                  </button>
                </div>
              </div>
            ) : (
              <>
                {link.thumbnailUrl && (
                  <img src={link.thumbnailUrl} alt="" className="link-thumb" />
                )}
                <div className="link-info">
                  <p className="link-info-title">
                    {link.title}
                    {link.status && link.status !== 'LIVE' && (
                      <span className={`link-badge is-${link.status.toLowerCase()}`}>
                        {STATUS_LABELS[link.status] ?? link.status}
                      </span>
                    )}
                  </p>
                  <p className="link-info-url">{link.url}</p>
                  {describeSchedule(link) && (
                    <p className="link-info-schedule">{describeSchedule(link)}</p>
                  )}
                </div>
                <div className="link-actions">
                  <button
                    onClick={() => toggleActive(link.id)}
                    className="link-icon-btn"
                    type="button"
                    aria-pressed={!link.active}
                    aria-label={link.active ? 'Hide link from your page' : 'Show link on your page'}
                    title={link.active ? 'Hide from your page' : 'Show on your page'}
                  >
                    {link.active ? <EyeIcon /> : <EyeOffIcon />}
                  </button>
                  {/* Always opens the picker, so an existing thumbnail can be swapped directly
                      rather than having to be removed first. */}
                  <button
                    onClick={() => pickThumbnail(link.id)}
                    className="link-icon-btn"
                    type="button"
                    aria-label={link.thumbnailUrl ? 'Change thumbnail' : 'Add thumbnail'}
                    title={link.thumbnailUrl ? 'Change thumbnail' : 'Add thumbnail'}
                  >
                    <ImageIcon />
                  </button>
                  {link.thumbnailUrl && (
                    <button
                      onClick={() => removeThumbnail(link.id)}
                      className="link-icon-btn is-delete"
                      type="button"
                      aria-label="Remove thumbnail"
                      title="Remove thumbnail"
                    >
                      <ImageOffIcon />
                    </button>
                  )}
                  <button
                    onClick={() => startEdit(link)}
                    className="link-icon-btn"
                    type="button"
                    aria-label="Edit link"
                  >
                    <EditIcon />
                  </button>
                  <div className="link-reorder">
                    <button
                      onClick={() => onMoveUp(i)}
                      disabled={i === 0}
                      className="link-reorder-btn"
                      type="button"
                      aria-label="Move link up"
                    >
                      <ChevronUpIcon />
                    </button>
                    <button
                      onClick={() => onMoveDown(i)}
                      disabled={i === links.length - 1}
                      className="link-reorder-btn"
                      type="button"
                      aria-label="Move link down"
                    >
                      <ChevronDownIcon />
                    </button>
                  </div>
                  <button
                    onClick={() => setPendingDelete(link)}
                    className="link-icon-btn is-delete"
                    type="button"
                    aria-label="Delete link"
                    title="Delete link"
                  >
                    <TrashIcon />
                  </button>
                </div>
              </>
            )}
          </li>
        ))}
      </ul>

      {pendingDelete && (
        <ConfirmDialog
          title={`Delete “${pendingDelete.title}”?`}
          message="This link and its click history will be removed from your page. This cannot be undone."
          confirmLabel="Delete link"
          busy={deleting}
          onConfirm={confirmDelete}
          onCancel={() => setPendingDelete(null)}
        />
      )}
    </>
  );
}
