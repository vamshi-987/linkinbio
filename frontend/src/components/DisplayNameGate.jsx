import { useRef, useState } from 'react';
import { updateProfile, uploadAvatar } from '../api/profileApi';
import useAvatarPicker from '../hooks/useAvatarPicker';

const MAX_DISPLAY_NAME = 100;
const MAX_BIO = 280;

/**
 * Shown in place of the link tools until the account has a display name, so nobody can publish a
 * page headed by nothing. The photo and bio are offered here too — they are the rest of what a
 * visitor sees — but only the name is required to get through.
 */
export default function DisplayNameGate({ username, onSaved }) {
  const [displayName, setDisplayName] = useState('');
  const [bio, setBio] = useState('');
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);
  const avatar = useAvatarPicker();
  const fileInputRef = useRef(null);

  const submit = async (e) => {
    e.preventDefault();
    if (saving) return;

    const name = displayName.trim();
    if (!name) {
      setError('Please enter a display name.');
      return;
    }
    if (name.length > MAX_DISPLAY_NAME) {
      setError(`Display name cannot exceed ${MAX_DISPLAY_NAME} characters.`);
      return;
    }

    setError('');
    avatar.clearError();
    setSaving(true);
    try {
      // The photo goes first: if it is rejected, nothing is saved and the form stays put with the
      // picture still attached, rather than letting them through with it silently dropped.
      if (avatar.pending) await uploadAvatar(avatar.pending.file);

      // Bio is omitted entirely when left empty, so the stored value is not written over with "".
      const trimmedBio = bio.trim();
      await updateProfile(trimmedBio ? { displayName: name, bio: trimmedBio } : { displayName: name });
      onSaved(name);
    } catch (err) {
      setError(err.response?.data?.error || 'Could not save your details. Please try again.');
      setSaving(false);
    }
  };

  return (
    <section className="gate">
      <h1 className="gate-title">First, what should we call you?</h1>
      <p className="gate-lead">
        This is what visitors see at the top of <strong>/{username}</strong>. Everything here can be
        changed later in Settings.
      </p>

      <form onSubmit={submit} className="gate-form">
        <div className="gate-avatar-row">
          {avatar.pending ? (
            <img src={avatar.pending.preview} alt="Your profile" className="gate-avatar" />
          ) : (
            <div className="gate-avatar gate-avatar-empty" aria-hidden="true" />
          )}
          <div className="gate-avatar-actions">
            <input
              ref={fileInputRef}
              type="file"
              accept="image/png,image/jpeg"
              onChange={avatar.choose}
              hidden
            />
            <button
              type="button"
              className="gate-secondary"
              disabled={saving}
              onClick={() => fileInputRef.current?.click()}
            >
              {avatar.pending ? 'Choose another' : 'Add a photo'}
            </button>
            {avatar.pending && (
              <button
                type="button"
                className="gate-secondary is-remove"
                disabled={saving}
                onClick={avatar.clear}
              >
                Remove
              </button>
            )}
            <p className="gate-hint">Optional — PNG or JPEG, up to 2 MB.</p>
            {avatar.error && <p className="gate-error is-inline" role="alert">{avatar.error}</p>}
          </div>
        </div>

        <label className="gate-label" htmlFor="gate-display-name">Display name</label>
        <input
          id="gate-display-name"
          className="gate-input"
          placeholder="e.g. Alex Rivera"
          value={displayName}
          maxLength={MAX_DISPLAY_NAME}
          disabled={saving}
          autoFocus
          onChange={(e) => setDisplayName(e.target.value)}
        />

        <label className="gate-label" htmlFor="gate-bio">
          Bio <span className="gate-optional">(optional)</span>
        </label>
        <textarea
          id="gate-bio"
          className="gate-input gate-textarea"
          placeholder="Tell your audience about yourself..."
          value={bio}
          maxLength={MAX_BIO}
          disabled={saving}
          onChange={(e) => setBio(e.target.value)}
        />

        <button className="gate-submit" type="submit" disabled={saving}>
          {saving ? 'Saving…' : 'Continue'}
        </button>
        {error && <p className="gate-error" role="alert">{error}</p>}
      </form>

      <p className="gate-note">You'll be able to add links right after this.</p>
    </section>
  );
}
