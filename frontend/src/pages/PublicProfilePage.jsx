import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import axiosClient from '../api/axiosClient';

const themes = {
  default: { bg: '#22262b', text: '#ffffff', sub: '#aeb4bd', btn: '#4a4f57', btnText: '#eef0f3' },
  dark: { bg: '#0f1114', text: '#ffffff', sub: '#9aa0ab', btn: '#242830', btnText: '#eef0f3' },
  pastel: { bg: '#fdf2f6', text: '#2d2a32', sub: '#6f6a76', btn: '#f4c9db', btnText: '#3a2b33' },
  neon: { bg: '#05060a', text: '#39ff14', sub: '#7bffb0', btn: '#0d1f16', btnText: '#39ff14' },
};

/**
 * Only a 404 means the page does not exist. Showing "not found" for a rate limit or a server error
 * tells the visitor the creator has no page, which is simply wrong.
 */
const errorMessage = (err) => {
  switch (err.response?.status) {
    case 404: return 'Profile not found';
    case 429: return 'Too many requests right now — please try again in a minute.';
    default: return 'Could not load this page. Please try again.';
  }
};

export default function PublicProfilePage() {
  const { username } = useParams();
  // The result carries the username it belongs to, so navigating to another profile shows the
  // loading state again instead of the previous page's profile or error.
  const [result, setResult] = useState(null);

  useEffect(() => {
    let cancelled = false;

    axiosClient.get(`/public/${username}`)
      .then((r) => !cancelled && setResult({ username, profile: r.data }))
      .catch((err) => !cancelled && setResult({ username, error: errorMessage(err) }));

    return () => { cancelled = true; };
  }, [username]);

  const current = result?.username === username ? result : null;

  if (current?.error) {
    return (
      <div className="pp-screen" style={themeVars(themes.default)}>
        <style>{styles}</style>
        <p className="pp-message" role="alert">{current.error}</p>
      </div>
    );
  }

  if (!current) {
    return (
      <div className="pp-screen" style={themeVars(themes.default)}>
        <style>{styles}</style>
        <p className="pp-message">Loading…</p>
      </div>
    );
  }

  const { profile } = current;
  const theme = themes[profile.theme] || themes.default;
  const apiBase = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081/api';
  const links = Array.isArray(profile.links) ? profile.links : [];

  return (
    <div className="pp-screen" style={themeVars(theme)}>
      <style>{styles}</style>
      <div className="pp-container">
        {profile.avatarUrl && (
          <img src={profile.avatarUrl} alt={profile.username} className="pp-avatar" />
        )}
        <h1 className="pp-name">{profile.displayName || profile.username}</h1>
        {profile.bio && <p className="pp-bio">{profile.bio}</p>}

        <div className="pp-links">
          {links.map((link) => (
            <a
              key={link.id}
              href={`${apiBase}/public/click/${link.id}`}
              className="pp-link"
            >
              {link.thumbnailUrl && (
                <img src={link.thumbnailUrl} alt="" className="pp-link-thumb" loading="lazy" />
              )}
              <span className="pp-link-title">{link.title}</span>
            </a>
          ))}
        </div>
      </div>
    </div>
  );
}

const themeVars = (t) => ({
  '--pp-bg': t.bg,
  '--pp-text': t.text,
  '--pp-sub': t.sub,
  '--pp-btn': t.btn,
  '--pp-btn-text': t.btnText,
});

const styles = `
  .pp-screen {
    position: fixed;
    inset: 0;
    overflow-y: auto;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 40px 20px;
    box-sizing: border-box;
    background: var(--pp-bg);
    font-family: system-ui, 'Segoe UI', Roboto, sans-serif;
    color: var(--pp-text);
  }
  .pp-container {
    width: 100%;
    max-width: 460px;
    text-align: center;
  }
  .pp-avatar {
    width: 96px;
    height: 96px;
    margin-bottom: 20px;
    border-radius: 50%;
    object-fit: cover;
  }
  .pp-name {
    margin: 0;
    font-size: 72px;
    font-weight: 800;
    letter-spacing: -1.5px;
    line-height: 1.05;
    color: var(--pp-text);
    word-break: break-word;
  }
  .pp-bio {
    margin: 14px 0 0;
    font-size: 30px;
    font-weight: 400;
    color: var(--pp-sub);
  }
  .pp-links {
    display: flex;
    flex-direction: column;
    gap: 18px;
    margin-top: 44px;
  }
  .pp-link {
    display: flex;
    align-items: center;
    gap: 14px;
    padding: 16px 20px;
    font-size: 19px;
    color: var(--pp-btn-text);
    text-decoration: none;
    background: var(--pp-btn);
    border: 1px solid rgba(255, 255, 255, 0.08);
    border-radius: 12px;
    transition: filter 0.2s, box-shadow 0.2s, transform 0.1s;
  }
  .pp-link:hover {
    filter: brightness(1.12);
    box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.14), 0 0 22px rgba(255, 255, 255, 0.12);
  }
  .pp-link:active {
    transform: translateY(1px);
    filter: brightness(0.97);
  }
  .pp-link-thumb {
    width: 36px;
    height: 36px;
    flex-shrink: 0;
    object-fit: cover;
    border-radius: 8px;
  }
  /* Centred with the thumbnail out of flow, so a link with an image and one without still line up. */
  .pp-link-title {
    flex: 1;
    text-align: center;
  }
  .pp-message {
    font-size: 18px;
    color: var(--pp-sub);
  }
  @media (max-width: 600px) {
    .pp-name {
      font-size: 48px;
    }
    .pp-bio {
      font-size: 22px;
    }
    .pp-links {
      margin-top: 32px;
      gap: 14px;
    }
    .pp-link {
      font-size: 17px;
      padding: 14px 18px;
    }
  }
`;
