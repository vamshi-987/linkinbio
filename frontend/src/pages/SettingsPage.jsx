import { useEffect, useRef, useState } from 'react';
import { deleteAvatar, getProfile, updateProfile, uploadAvatar } from '../api/profileApi';
import { Link } from 'react-router-dom';
import { themeFor, themeList } from '../theme/themes';
import useAvatarPicker from '../hooks/useAvatarPicker';

export default function SettingsPage() {
  const [displayName, setDisplayName] = useState('');
  const [bio, setBio] = useState('');
  const [theme, setTheme] = useState('default');
  const [avatarUrl, setAvatarUrl] = useState('');
  // A pick is held locally until Save, so choosing a photo and leaving leaves the stored one alone.
  const avatar = useAvatarPicker();
  const [avatarCleared, setAvatarCleared] = useState(false);
  const avatarInputRef = useRef(null);
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState(false);
  const [saveError, setSaveError] = useState('');
  // Until the current values arrive, saving would PATCH empty strings over them and wipe the
  // profile. Nothing is editable or submittable before then.
  const [loaded, setLoaded] = useState(false);
  const [loadError, setLoadError] = useState(false);

  useEffect(() => {
    let cancelled = false;
    getProfile()
      .then((profile) => {
        if (cancelled) return;
        setDisplayName(profile.displayName ?? '');
        setBio(profile.bio ?? '');
        setTheme(profile.theme ?? 'default');
        setAvatarUrl(profile.avatarUrl ?? '');
        setLoaded(true);
      })
      .catch(() => {
        if (!cancelled) setLoadError(true);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const handleSave = async (e) => {
    e.preventDefault();
    if (!loaded || saving) return;
    setSaveError('');
    avatar.clearError();

    // The name heads the public page, so it is the one field that cannot be cleared. An emptied
    // bio is a deliberate clear and is saved as one.
    if (!displayName.trim()) {
      setSaveError('Display name cannot be empty.');
      return;
    }

    setSaving(true);
    try {
      // The photo goes first: if it is rejected, the text fields are left unsaved too, so one Save
      // never half-applies.
      if (avatar.pending) {
        const profile = await uploadAvatar(avatar.pending.file);
        avatar.clear();
        setAvatarUrl(profile.avatarUrl ?? '');
      } else if (avatarCleared) {
        const profile = await deleteAvatar();
        setAvatarCleared(false);
        setAvatarUrl(profile.avatarUrl ?? '');
      }

      await updateProfile({ displayName, bio, theme });
      setSaved(true);
      setTimeout(() => setSaved(false), 2000);
    } catch (err) {
      setSaveError(err.response?.data?.error || 'Could not save your changes. Please try again.');
    } finally {
      setSaving(false);
    }
  };

  const handleAvatarChosen = (e) => {
    if (avatar.choose(e)) setAvatarCleared(false);
  };

  const handleAvatarRemove = () => {
    avatar.clearError();
    if (avatar.pending) {
      // Only the unsaved pick is dropped; whatever is stored server-side stays until Save.
      avatar.clear();
      return;
    }
    setAvatarCleared(true);
  };

  const shownAvatar = avatar.pending?.preview || (avatarCleared ? '' : avatarUrl);
  const avatarPending = Boolean(avatar.pending) || avatarCleared;
  const preview = themeFor(theme);

  return (
    <div className="settings-screen">
      <style>{settingsStyles}</style>
      <div className="settings-container">
        <Link to="/dashboard" className="settings-back">← Back</Link>
        <h1 className="settings-title">Account Settings</h1>

        <section className="settings-section">
          <h2 className="settings-heading">Profile Photo</h2>
          <div className="settings-avatar-row">
            {shownAvatar ? (
              <img src={shownAvatar} alt="Your profile" className="settings-avatar" />
            ) : (
              <div className="settings-avatar settings-avatar-empty" aria-hidden="true" />
            )}
            <div className="settings-avatar-actions">
              <input
                ref={avatarInputRef}
                type="file"
                accept="image/png,image/jpeg"
                onChange={handleAvatarChosen}
                hidden
              />
              <button
                type="button"
                className="settings-avatar-btn"
                disabled={!loaded || saving}
                onClick={() => avatarInputRef.current?.click()}
              >
                {shownAvatar ? 'Choose another photo' : 'Choose photo'}
              </button>
              {shownAvatar && (
                <button
                  type="button"
                  className="settings-avatar-btn is-remove"
                  disabled={!loaded || saving}
                  onClick={handleAvatarRemove}
                >
                  Remove
                </button>
              )}
              <p className="settings-help">PNG or JPEG, up to 2 MB.</p>
              {avatarPending && (
                <p className="settings-pending">
                  {avatar.pending ? 'New photo selected — ' : 'Photo will be removed — '}
                  save to apply.
                </p>
              )}
              {avatar.error && <p className="settings-error" role="alert">{avatar.error}</p>}
            </div>
          </div>
        </section>

        <form onSubmit={handleSave} className="settings-form">
          <section className="settings-section">
            <h2 className="settings-heading">Profile Information</h2>

            <label className="settings-label" htmlFor="displayName">Display Name</label>
            <input
              id="displayName"
              className="settings-input"
              placeholder="Your Name"
              value={displayName}
              disabled={!loaded}
              onChange={(e) => setDisplayName(e.target.value)}
            />

            <label className="settings-label" htmlFor="bio">Bio <span className="settings-optional">(optional)</span></label>
            <textarea
              id="bio"
              className="settings-input settings-textarea"
              placeholder="Tell your audience about yourself..."
              value={bio}
              disabled={!loaded}
              onChange={(e) => setBio(e.target.value)}
            />
          </section>

          <section className="settings-section">
            <h2 className="settings-heading">Theme Selection</h2>
            <div className="settings-themes">
              {themeList.map((option) => (
                <button
                  type="button"
                  key={option.id}
                  onClick={() => setTheme(option.id)}
                  disabled={!loaded}
                  aria-pressed={theme === option.id}
                  className={`settings-theme${theme === option.id ? ' is-selected' : ''}`}
                >
                  {/* The swatch is the palette itself, so the choices are told apart by colour
                      rather than by four near-identical labels. */}
                  <span
                    className="settings-theme-swatch"
                    style={{ background: option.bg, borderColor: option.border }}
                    aria-hidden="true"
                  >
                    <span style={{ background: option.accent }} />
                  </span>
                  {option.label}
                </button>
              ))}
            </div>

            {/* A real render of the palette: picking a theme changes something immediately instead
                of only after a save and a visit to the public page. */}
            <div className="settings-preview" style={{ background: preview.bg }}>
              <p className="settings-preview-name" style={{ color: preview.text }}>
                {displayName || 'Your Name'}
              </p>
              <p className="settings-preview-bio" style={{ color: preview.sub }}>
                {bio || 'Your bio appears here'}
              </p>
              <span
                className="settings-preview-link"
                style={{
                  background: preview.btn,
                  color: preview.btnText,
                  borderColor: preview.border,
                }}
              >
                Your first link
              </span>
              <span
                className="settings-preview-link"
                style={{
                  background: preview.btn,
                  color: preview.btnText,
                  borderColor: preview.border,
                }}
              >
                Your second link
              </span>
            </div>
            <p className="settings-preview-caption">Preview of your public page</p>
          </section>

          <button className="settings-save" type="submit" disabled={!loaded || saving}>
            {!loaded ? 'Loading…' : saving ? 'Saving…' : 'Save'}
          </button>
          {saved && <p className="settings-saved">Saved!</p>}
          {saveError && <p className="settings-error">{saveError}</p>}
          {loadError && (
            <p className="settings-error">
              Couldn't load your profile. Refresh before editing — saving now would overwrite it.
            </p>
          )}
        </form>

        <section className="settings-section settings-security">
          <h2 className="settings-heading">Password</h2>
          <p className="settings-help">
            We'll email a verification code to your registered address, then let you set a new password.
          </p>
          <Link to="/forgot-password" className="settings-change-password">Change Password</Link>
        </section>
      </div>
    </div>
  );
}

const settingsStyles = `
  .settings-screen {
    position: fixed;
    inset: 0;
    overflow-y: auto;
    background: #1e2127;
    font-family: system-ui, 'Segoe UI', Roboto, sans-serif;
    color: #e8eaee;
  }
  .settings-container {
    position: relative;
    width: 100%;
    max-width: 566px;
    margin: 0 auto;
    padding: 40px 20px 64px;
    box-sizing: border-box;
    text-align: left;
  }
  .settings-back {
    display: inline-block;
    margin-bottom: 8px;
    font-size: 14px;
    color: #9aa0ab;
    text-decoration: none;
  }
  .settings-back:hover {
    color: #e8eaee;
  }
  .settings-title {
    margin: 8px 0 44px;
    text-align: center;
    font-size: 44px;
    font-weight: 700;
    letter-spacing: -0.5px;
    color: #f2f4f7;
  }
  .settings-form {
    display: flex;
    flex-direction: column;
  }
  .settings-avatar-row {
    display: flex;
    align-items: center;
    gap: 20px;
  }
  .settings-avatar {
    width: 88px;
    height: 88px;
    flex-shrink: 0;
    object-fit: cover;
    border-radius: 50%;
    border: 1px solid #3a3e46;
  }
  .settings-avatar-empty {
    background: #23262d;
  }
  .settings-avatar-actions {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 10px;
  }
  .settings-avatar-btn {
    padding: 10px 18px;
    font-family: inherit;
    font-size: 15px;
    color: #e8eaee;
    background: #2b2f37;
    border: 1px solid #3a3e46;
    border-radius: 9px;
    cursor: pointer;
    transition: background 0.2s, color 0.2s;
  }
  .settings-avatar-btn:hover:not(:disabled) {
    background: #333944;
  }
  .settings-avatar-btn:disabled {
    opacity: 0.5;
    cursor: default;
  }
  .settings-avatar-btn.is-remove:hover:not(:disabled) {
    color: #f87171;
  }
  .settings-avatar-actions .settings-help,
  .settings-avatar-actions .settings-pending,
  .settings-avatar-actions .settings-error {
    flex-basis: 100%;
    margin: 0;
    text-align: left;
  }
  .settings-section {
    margin-bottom: 36px;
  }
  .settings-heading {
    margin: 0 0 18px;
    font-size: 22px;
    font-weight: 500;
    color: #e8eaee;
  }
  .settings-label {
    display: block;
    margin-bottom: 8px;
    font-size: 15px;
    color: #c8ccd3;
  }
  .settings-input {
    width: 100%;
    box-sizing: border-box;
    margin-bottom: 20px;
    padding: 13px 15px;
    font-family: inherit;
    font-size: 16px;
    color: #e8eaee;
    background: #23262d;
    border: 1px solid #444852;
    border-radius: 8px;
    outline: none;
    transition: border-color 0.2s, box-shadow 0.2s;
  }
  .settings-input:last-child {
    margin-bottom: 0;
  }
  .settings-input::placeholder {
    color: #868b95;
  }
  .settings-input:focus {
    border-color: #5b8def;
    box-shadow: 0 0 0 3px rgba(91, 141, 239, 0.2);
  }
  .settings-textarea {
    min-height: 130px;
    resize: vertical;
  }
  .settings-themes {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
  }
  .settings-theme {
    display: inline-flex;
    align-items: center;
    gap: 9px;
    padding: 10px 18px;
    font-family: inherit;
    font-size: 16px;
    color: #d3d7dd;
    background: #3a3e46;
    border: 1px solid #4c515b;
    border-radius: 999px;
    cursor: pointer;
    transition: background 0.2s, border-color 0.2s;
  }
  .settings-theme:hover {
    background: #454a54;
  }
  .settings-input:disabled,
  .settings-theme:disabled {
    opacity: 0.55;
    cursor: default;
  }
  .settings-theme:disabled:hover {
    background: #3a3e46;
  }
  .settings-theme.is-selected {
    background: #333944;
    border-color: #5b8def;
    color: #f2f4f7;
  }
  .settings-theme-swatch {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 18px;
    height: 18px;
    border-radius: 50%;
    border: 1px solid;
  }
  .settings-theme-swatch > span {
    width: 8px;
    height: 8px;
    border-radius: 50%;
  }
  .settings-preview {
    margin-top: 22px;
    padding: 22px 20px;
    text-align: center;
    border: 1px solid #444852;
    border-radius: 14px;
    transition: background 0.25s;
  }
  .settings-preview-name {
    margin: 0;
    font-size: 22px;
    font-weight: 700;
    overflow-wrap: anywhere;
  }
  .settings-preview-bio {
    margin: 6px 0 16px;
    font-size: 14px;
    overflow-wrap: anywhere;
  }
  .settings-preview-link {
    display: block;
    margin-bottom: 10px;
    padding: 11px 16px;
    font-size: 14px;
    border: 1px solid;
    border-radius: 10px;
  }
  .settings-preview-link:last-child {
    margin-bottom: 0;
  }
  .settings-preview-caption {
    margin: 10px 0 0;
    text-align: center;
    font-size: 13px;
    color: #868b95;
  }
  .settings-pending {
    margin: 0;
    font-size: 14px;
    color: #9dc0ff;
  }
  .settings-optional {
    color: #868b95;
  }
  .settings-save {
    padding: 15px 16px;
    font-family: inherit;
    font-size: 18px;
    font-weight: 500;
    color: #fff;
    background: #3b71d8;
    border: none;
    border-radius: 999px;
    cursor: pointer;
    transition: filter 0.2s;
  }
  .settings-save:hover {
    filter: brightness(1.08);
  }
  .settings-save:active {
    filter: brightness(0.95);
  }
  .settings-save:disabled {
    background: #33415e;
    color: #9aa0ab;
    cursor: default;
    filter: none;
  }
  .settings-saved {
    margin: 12px 0 0;
    text-align: center;
    font-size: 15px;
    color: #4ade80;
  }
  .settings-error {
    margin: 12px 0 0;
    text-align: center;
    font-size: 15px;
    line-height: 1.5;
    color: #f87171;
  }
  .settings-security {
    margin-top: 40px;
    padding-top: 32px;
    border-top: 1px solid #2f333c;
  }
  .settings-help {
    margin: 0 0 16px;
    font-size: 14px;
    line-height: 1.5;
    color: #9aa0ab;
  }
  .settings-change-password {
    display: inline-block;
    padding: 12px 22px;
    font-size: 15px;
    font-weight: 500;
    color: #e8eaee;
    text-decoration: none;
    background: #3a3e46;
    border: 1px solid #4c515b;
    border-radius: 999px;
    transition: background 0.2s;
  }
  .settings-change-password:hover {
    background: #454a54;
  }
  @media (max-width: 600px) {
    .settings-container {
      padding: 28px 18px 48px;
    }
    .settings-title {
      font-size: 30px;
      margin-bottom: 32px;
    }
    .settings-heading {
      font-size: 19px;
    }
    .settings-theme {
      flex: 1 1 calc(50% - 12px);
      justify-content: center;
    }
  }
`;
