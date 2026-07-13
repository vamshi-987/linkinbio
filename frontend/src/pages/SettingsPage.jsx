import { useState } from 'react';
import { updateProfile } from '../api/profileApi';
import { Link } from 'react-router-dom';

const ContrastIcon = () => (
  <svg width="16" height="16" viewBox="0 0 24 24" aria-hidden="true">
    <circle cx="12" cy="12" r="9" fill="none" stroke="currentColor" strokeWidth="2" />
    <path d="M12 3a9 9 0 0 1 0 18Z" fill="currentColor" />
  </svg>
);

const MoonIcon = () => (
  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
       strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79Z" />
  </svg>
);

const DotIcon = () => (
  <svg width="16" height="16" viewBox="0 0 24 24" aria-hidden="true">
    <circle cx="12" cy="12" r="8" fill="currentColor" />
  </svg>
);

const themes = [
  { id: 'default', label: 'Default', Icon: ContrastIcon },
  { id: 'dark', label: 'Dark', Icon: MoonIcon },
  { id: 'pastel', label: 'Pastel', Icon: DotIcon },
  { id: 'neon', label: 'Neon', Icon: DotIcon },
];

export default function SettingsPage() {
  const [displayName, setDisplayName] = useState('');
  const [bio, setBio] = useState('');
  const [theme, setTheme] = useState('default');
  const [saved, setSaved] = useState(false);

  const handleSave = async (e) => {
    e.preventDefault();
    await updateProfile({ displayName, bio, theme });
    setSaved(true);
    setTimeout(() => setSaved(false), 2000);
  };

  return (
    <div className="settings-screen">
      <style>{settingsStyles}</style>
      <div className="settings-container">
        <Link to="/dashboard" className="settings-back">← Back</Link>
        <h1 className="settings-title">Account Settings</h1>

        <form onSubmit={handleSave} className="settings-form">
          <section className="settings-section">
            <h2 className="settings-heading">Profile Information</h2>

            <label className="settings-label" htmlFor="displayName">Display Name</label>
            <input
              id="displayName"
              className="settings-input"
              placeholder="Your Name"
              value={displayName}
              onChange={(e) => setDisplayName(e.target.value)}
            />

            <label className="settings-label" htmlFor="bio">Bio</label>
            <textarea
              id="bio"
              className="settings-input settings-textarea"
              placeholder="Tell your audience about yourself..."
              value={bio}
              onChange={(e) => setBio(e.target.value)}
            />
          </section>

          <section className="settings-section">
            <h2 className="settings-heading">Theme Selection</h2>
            <div className="settings-themes">
              {themes.map(({ id, label, Icon }) => (
                <button
                  type="button"
                  key={id}
                  onClick={() => setTheme(id)}
                  aria-pressed={theme === id}
                  className={`settings-theme${theme === id ? ' is-selected' : ''}`}
                >
                  <span className="settings-theme-icon"><Icon /></span>
                  {label}
                </button>
              ))}
            </div>
          </section>

          <button className="settings-save" type="submit">Save</button>
          {saved && <p className="settings-saved">Saved!</p>}
        </form>
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
  .settings-theme.is-selected {
    background: #333944;
    border-color: #5b8def;
    color: #f2f4f7;
  }
  .settings-theme-icon {
    display: flex;
    color: #9aa0ab;
  }
  .settings-theme.is-selected .settings-theme-icon {
    color: #5b8def;
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
  .settings-saved {
    margin: 12px 0 0;
    text-align: center;
    font-size: 15px;
    color: #4ade80;
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
