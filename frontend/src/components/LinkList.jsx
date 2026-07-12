import { useState } from 'react';

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
    <ul className="space-y-2">
      {links.map((link, i) => (
        <li key={link.id} className="flex items-center justify-between border rounded px-3 py-2">
          {editingId === link.id ? (
            <>
              <div className="flex gap-2 flex-1 mr-2">
                <input className="border rounded px-2 py-1 flex-1" placeholder="Title"
                       value={draft.title} onChange={e => setDraft({ ...draft, title: e.target.value })} />
                <input className="border rounded px-2 py-1 flex-1" placeholder="https://..."
                       value={draft.url} onChange={e => setDraft({ ...draft, url: e.target.value })} />
              </div>
              <div className="flex gap-1">
                <button onClick={() => saveEdit(link.id)} className="px-2 text-green-600">Save</button>
                <button onClick={cancelEdit} className="px-2 text-gray-500">Cancel</button>
              </div>
            </>
          ) : (
            <>
              <div>
                <p className="font-medium">{link.title}</p>
                <p className="text-sm text-gray-500">{link.url}</p>
              </div>
              <div className="flex gap-1">
                <button onClick={() => onMoveUp(i)} disabled={i === 0} className="px-2">↑</button>
                <button onClick={() => onMoveDown(i)} disabled={i === links.length - 1} className="px-2">↓</button>
                <button onClick={() => startEdit(link)} className="px-2 text-blue-500">✎</button>
                <button onClick={() => onDelete(link.id)} className="px-2 text-red-500">✕</button>
              </div>
            </>
          )}
        </li>
      ))}
    </ul>
  );
}
