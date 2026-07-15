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
    <form onSubmit={handleSubmit} className="link-form">
      <input
        className="link-form-input"
        placeholder="Title"
        value={title}
        onChange={(e) => setTitle(e.target.value)}
      />
      <input
        className="link-form-input"
        placeholder="URL"
        value={url}
        onChange={(e) => setUrl(e.target.value)}
      />
      <button className="link-form-submit" type="submit">Add Link</button>
    </form>
  );
}
