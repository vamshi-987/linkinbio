import { useState } from 'react';

export default function LinkForm({ onAdd }) {
  const [title, setTitle] = useState('');
  const [url, setUrl] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!title || !url) return;
    await onAdd({ title, url });
    setTitle('');
    setUrl('');
  };

  return (
    <form onSubmit={handleSubmit} className="flex gap-2 mb-4">
      <input className="border rounded px-3 py-2 flex-1" placeholder="Title"
             value={title} onChange={e => setTitle(e.target.value)} />
      <input className="border rounded px-3 py-2 flex-1" placeholder="https://..."
             value={url} onChange={e => setUrl(e.target.value)} />
      <button className="bg-black text-white rounded px-4" type="submit">Add</button>
    </form>
  );
}