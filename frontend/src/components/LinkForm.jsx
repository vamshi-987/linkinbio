import { useState } from 'react';
import { toInstant } from '../utils/schedule';

export default function LinkForm({ onAdd }) {
  const [title, setTitle] = useState('');
  const [url, setUrl] = useState('');
  // Scheduling is opt-in: most links never need it, and two extra date fields on every add would
  // make the common case feel heavier than it is.
  const [scheduling, setScheduling] = useState(false);
  const [visibleFrom, setVisibleFrom] = useState('');
  const [visibleUntil, setVisibleUntil] = useState('');
  const [error, setError] = useState('');

  const reset = () => {
    setTitle('');
    setUrl('');
    setVisibleFrom('');
    setVisibleUntil('');
    setScheduling(false);
    setError('');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!title || !url) return;

    const from = scheduling ? toInstant(visibleFrom) : null;
    const until = scheduling ? toInstant(visibleUntil) : null;
    if (from && until && new Date(until) <= new Date(from)) {
      setError('The end time must be after the start time.');
      return;
    }

    try {
      await onAdd({ title, url, visibleFrom: from, visibleUntil: until });
      reset();
    } catch (err) {
      setError(err.response?.data?.error || 'Could not add that link.');
    }
  };

  return (
    <form onSubmit={handleSubmit} className="link-form">
      <div className="link-form-row">
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
      </div>

      <button
        type="button"
        className="link-schedule-toggle"
        aria-expanded={scheduling}
        onClick={() => setScheduling((on) => !on)}
      >
        {scheduling ? '− Scheduling' : '+ Schedule this link'}
      </button>

      {scheduling && (
        <div className="link-schedule-fields">
          <label className="link-schedule-label">
            Goes live
            <input
              type="datetime-local"
              className="link-form-input"
              value={visibleFrom}
              onChange={(e) => setVisibleFrom(e.target.value)}
            />
          </label>
          <label className="link-schedule-label">
            Expires
            <input
              type="datetime-local"
              className="link-form-input"
              value={visibleUntil}
              onChange={(e) => setVisibleUntil(e.target.value)}
            />
          </label>
          <p className="link-schedule-hint">
            Leave a field empty for no limit. Times are in your local time zone.
          </p>
        </div>
      )}

      {error && <p className="link-form-error" role="alert">{error}</p>}
    </form>
  );
}
