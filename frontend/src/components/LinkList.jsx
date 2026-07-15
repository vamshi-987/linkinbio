import { useState } from 'react';

const EditIcon = () => (
  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor"
       strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    <path d="M12 20h9" />
    <path d="M16.5 3.5a2.12 2.12 0 0 1 3 3L7 19l-4 1 1-4Z" />
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

export default function LinkList({ links, onUpdate, onDelete, onMoveUp, onMoveDown }) {
  const [editingId, setEditingId] = useState(null);
  const [draft, setDraft] = useState({ title: '', url: '' });

  const startEdit = (link) => {
    setEditingId(link.id);
    setDraft({ title: link.title, url: link.url });
  };

  const cancelEdit = () => {
    setEditingId(null);
    setDraft({ title: '', url: '' });
  };

  const saveEdit = async (id) => {
    if (!draft.title || !draft.url) return;
    await onUpdate(id, { title: draft.title, url: draft.url });
    cancelEdit();
  };

  return (
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
              <div className="link-info">
                <p className="link-info-title">{link.title}</p>
                <p className="link-info-url">{link.url}</p>
              </div>
              <div className="link-actions">
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
                  onClick={() => onDelete(link.id)}
                  className="link-icon-btn is-delete"
                  type="button"
                  aria-label="Delete link"
                >
                  <TrashIcon />
                </button>
              </div>
            </>
          )}
        </li>
      ))}
    </ul>
  );
}
