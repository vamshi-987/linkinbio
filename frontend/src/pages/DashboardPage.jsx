import { useEffect, useState } from 'react';
import {
  getLinks, createLink, updateLink, deleteLink, reorderLinks, uploadThumbnail, deleteThumbnail,
} from '../api/linksApi';
import LinkForm from '../components/LinkForm';
import LinkList from '../components/LinkList';
import { useAuth } from '../context/useAuth';
import { Link } from 'react-router-dom';

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081/api';

export default function DashboardPage() {
  const [links, setLinks] = useState([]);
  const [showQr, setShowQr] = useState(false);
  const [reorderError, setReorderError] = useState('');
  const { username, logout } = useAuth();

  useEffect(() => {
    getLinks()
      .then((data) => setLinks(Array.isArray(data) ? data : []))
      .catch(() => setLinks([]));
  }, []);

  const handleAdd = async (data) => {
    const newLink = await createLink(data);
    setLinks((prev) => [...prev, newLink]);
  };

  const replace = (updated) =>
    setLinks((prev) => prev.map((l) => (l.id === updated.id ? updated : l)));

  const handleUpdate = async (id, data) => {
    const existing = links.find((l) => l.id === id);
    const updated = await updateLink(id, { active: existing?.active ?? true, ...data });
    replace(updated);
  };

  const handleUploadThumbnail = async (id, file) => replace(await uploadThumbnail(id, file));

  const handleRemoveThumbnail = async (id) => replace(await deleteThumbnail(id));

  const handleDelete = async (id) => {
    await deleteLink(id);
    setLinks((prev) => prev.filter((l) => l.id !== id));
  };

  const handleToggleActive = async (id) => {
    const link = links.find((l) => l.id === id);
    if (!link) return;
    // A full replacement, so every editable field has to be sent back, not just the flag.
    replace(await updateLink(id, {
      title: link.title,
      url: link.url,
      active: !link.active,
      visibleFrom: link.visibleFrom ?? null,
      visibleUntil: link.visibleUntil ?? null,
    }));
  };

  const swap = async (i, j) => {
    const previous = links;
    const updated = [...links];
    [updated[i], updated[j]] = [updated[j], updated[i]];

    // Applied optimistically so the arrows feel instant, then rolled back if the server rejects the
    // order — otherwise the list keeps showing an arrangement that was never saved.
    setLinks(updated);
    setReorderError('');
    try {
      await reorderLinks(updated.map((l) => l.id));
    } catch (err) {
      setLinks(previous);
      setReorderError(err.response?.data?.error || 'Could not save the new order. Please try again.');
    }
  };

  return (
    <div className="dash-screen">
      <style>{dashboardStyles}</style>

      <header className="dash-nav">
        <div className="dash-nav-inner">
          <div className="dash-nav-left">
            <span className="dash-brand">Link Management</span>
            <nav className="dash-nav-links">
              <Link to="/analytics" className="dash-nav-link">Analytics</Link>
              <Link to="/settings" className="dash-nav-link">Settings</Link>
              <Link to={`/${username}`} target="_blank" rel="noreferrer" className="dash-nav-link">
                View Page
              </Link>
            </nav>
          </div>
          <button onClick={logout} className="dash-logout" type="button">Logout</button>
        </div>
      </header>

      <main className="dash-container">
        <h1 className="dash-title">Add New Link</h1>
        <LinkForm onAdd={handleAdd} />
        {reorderError && <p className="link-list-error" role="alert">{reorderError}</p>}
        <LinkList
          links={links}
          onUpdate={handleUpdate}
          onDelete={handleDelete}
          onToggleActive={handleToggleActive}
          onMoveUp={(i) => i > 0 && swap(i, i - 1)}
          onMoveDown={(i) => i < links.length - 1 && swap(i, i + 1)}
          onUploadThumbnail={handleUploadThumbnail}
          onRemoveThumbnail={handleRemoveThumbnail}
        />

        <section className="dash-qr">
          <button
            type="button"
            className="dash-qr-toggle"
            aria-expanded={showQr}
            onClick={() => setShowQr((on) => !on)}
          >
            {showQr ? 'Hide QR code' : 'Show QR code for my page'}
          </button>
          {showQr && (
            <div className="dash-qr-body">
              {/* Generated server-side and cached, so this is a plain image request. */}
              <img
                src={`${API_BASE}/public/qr/profile/${username}?size=320`}
                alt={`QR code linking to ${username}'s page`}
                className="dash-qr-image"
                width="320"
                height="320"
              />
              <a
                className="dash-qr-download"
                href={`${API_BASE}/public/qr/profile/${username}?size=1024`}
                download={`${username}-qr.png`}
              >
                Download PNG
              </a>
            </div>
          )}
        </section>
      </main>
    </div>
  );
}

const dashboardStyles = `
  .dash-screen {
    position: fixed;
    inset: 0;
    overflow-y: auto;
    background: #1e2127;
    font-family: system-ui, 'Segoe UI', Roboto, sans-serif;
    color: #e8eaee;
  }
  .dash-nav {
    position: sticky;
    top: 0;
    z-index: 10;
    background: #23262d;
    border-bottom: 1px solid #2f333c;
  }
  .dash-nav-inner {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    max-width: 1180px;
    margin: 0 auto;
    padding: 18px 24px;
    box-sizing: border-box;
  }
  .dash-nav-left {
    display: flex;
    align-items: center;
    gap: 34px;
    min-width: 0;
  }
  .dash-brand {
    font-size: 26px;
    font-weight: 700;
    letter-spacing: -0.5px;
    color: #f2f4f7;
    white-space: nowrap;
  }
  .dash-nav-links {
    display: flex;
    align-items: center;
    gap: 30px;
  }
  .dash-nav-link {
    font-size: 16px;
    color: #c8ccd3;
    text-decoration: none;
    transition: color 0.2s;
    white-space: nowrap;
  }
  .dash-nav-link:hover {
    color: #f2f4f7;
  }
  .dash-logout {
    padding: 10px 22px;
    font-family: inherit;
    font-size: 15px;
    color: #e8eaee;
    background: #2b2f37;
    border: 1px solid #3a3e46;
    border-radius: 10px;
    cursor: pointer;
    white-space: nowrap;
    transition: background 0.2s, color 0.2s;
  }
  .dash-logout:hover {
    background: #333944;
    color: #f2f4f7;
  }
  .dash-container {
    width: 100%;
    max-width: 980px;
    margin: 0 auto;
    padding: 40px 24px 72px;
    box-sizing: border-box;
  }
  .dash-title {
    margin: 8px 0 32px;
    text-align: center;
    font-size: 52px;
    font-weight: 700;
    letter-spacing: -1px;
    color: #f2f4f7;
  }
  .link-form {
    display: flex;
    flex-direction: column;
    gap: 12px;
    margin-bottom: 32px;
  }
  .link-form-row {
    display: flex;
    align-items: stretch;
    gap: 16px;
  }
  .link-form-error,
  .link-list-error {
    margin: 0;
    font-size: 14px;
    color: #f87171;
  }
  .link-schedule-toggle {
    align-self: flex-start;
    padding: 0;
    font-family: inherit;
    font-size: 14px;
    color: #9aa0ab;
    background: none;
    border: none;
    cursor: pointer;
  }
  .link-schedule-toggle:hover {
    color: #e8eaee;
  }
  .link-schedule-fields {
    display: flex;
    flex-wrap: wrap;
    gap: 16px;
    padding: 16px 18px;
    background: #23262d;
    border: 1px solid #2f333c;
    border-radius: 14px;
  }
  .link-schedule-label {
    display: flex;
    flex-direction: column;
    gap: 6px;
    flex: 1;
    min-width: 200px;
    font-size: 13px;
    color: #9aa0ab;
  }
  .link-schedule-hint {
    flex-basis: 100%;
    margin: 0;
    font-size: 13px;
    color: #868b95;
  }
  .link-form-input {
    flex: 1;
    min-width: 0;
    box-sizing: border-box;
    padding: 16px 20px;
    font-family: inherit;
    font-size: 17px;
    color: #e8eaee;
    background: #23262d;
    border: 1px solid #3a3e46;
    border-radius: 14px;
    outline: none;
    transition: border-color 0.2s, box-shadow 0.2s;
  }
  .link-form-input::placeholder {
    color: #868b95;
  }
  .link-form-input:focus {
    border-color: #5b8def;
    box-shadow: 0 0 0 3px rgba(91, 141, 239, 0.2);
  }
  .link-form-submit {
    padding: 0 34px;
    font-family: inherit;
    font-size: 17px;
    font-weight: 500;
    color: #fff;
    background: #3b71d8;
    border: none;
    border-radius: 14px;
    cursor: pointer;
    white-space: nowrap;
    transition: filter 0.2s;
  }
  .link-form-submit:hover {
    filter: brightness(1.08);
  }
  .link-form-submit:active {
    filter: brightness(0.95);
  }
  .link-list {
    list-style: none;
    margin: 0;
    padding: 0;
    background: #23262d;
    border: 1px solid #2f333c;
    border-radius: 16px;
    overflow: hidden;
  }
  .link-list:empty {
    display: none;
  }
  .link-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    padding: 18px 24px;
    border-bottom: 1px solid #2f333c;
  }
  .link-row:last-child {
    border-bottom: none;
  }
  .link-info {
    flex: 1;
    min-width: 0;
  }
  .link-thumb {
    width: 44px;
    height: 44px;
    flex-shrink: 0;
    object-fit: cover;
    border-radius: 10px;
    border: 1px solid #3a3e46;
  }
  .link-info-schedule {
    margin: 4px 0 0;
    font-size: 13px;
    color: #868b95;
  }
  .link-badge {
    margin-left: 10px;
    padding: 2px 8px;
    font-size: 12px;
    font-weight: 500;
    vertical-align: middle;
    border-radius: 999px;
    background: #2f333c;
    color: #c8ccd3;
  }
  .link-badge.is-scheduled {
    background: rgba(91, 141, 239, 0.18);
    color: #9dc0ff;
  }
  .link-badge.is-expired {
    background: rgba(248, 113, 113, 0.16);
    color: #f8a5a5;
  }
  .link-info-title {
    margin: 0;
    font-size: 19px;
    font-weight: 500;
    color: #f2f4f7;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  .link-info-url {
    margin: 4px 0 0;
    font-size: 14px;
    color: #9aa0ab;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  .link-actions {
    display: flex;
    align-items: center;
    gap: 6px;
    flex-shrink: 0;
  }
  .link-icon-btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 38px;
    height: 38px;
    padding: 0;
    color: #9aa0ab;
    background: transparent;
    border: none;
    border-radius: 9px;
    cursor: pointer;
    transition: background 0.2s, color 0.2s;
  }
  .link-icon-btn:hover {
    background: #2f333c;
    color: #e8eaee;
  }
  .link-icon-btn.is-delete:hover {
    color: #f87171;
  }
  .link-icon-btn:disabled {
    opacity: 0.35;
    cursor: default;
    background: transparent;
    color: #9aa0ab;
  }
  .link-reorder {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    width: 38px;
    height: 38px;
  }
  .link-reorder-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 100%;
    height: 15px;
    padding: 0;
    color: #9aa0ab;
    background: transparent;
    border: none;
    border-radius: 5px;
    cursor: pointer;
    transition: background 0.2s, color 0.2s;
  }
  .link-reorder-btn:hover:not(:disabled) {
    background: #2f333c;
    color: #e8eaee;
  }
  .link-reorder-btn:disabled {
    opacity: 0.35;
    cursor: default;
  }
  .link-edit {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 10px;
    flex: 1;
    min-width: 0;
  }
  .link-edit-schedule {
    display: flex;
    gap: 10px;
    flex-basis: 100%;
  }
  .dash-qr {
    margin-top: 28px;
    text-align: center;
  }
  .dash-qr-toggle {
    padding: 10px 20px;
    font-family: inherit;
    font-size: 15px;
    color: #c8ccd3;
    background: #2b2f37;
    border: 1px solid #3a3e46;
    border-radius: 10px;
    cursor: pointer;
    transition: background 0.2s, color 0.2s;
  }
  .dash-qr-toggle:hover {
    background: #333944;
    color: #f2f4f7;
  }
  .dash-qr-body {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 14px;
    margin-top: 18px;
  }
  .dash-qr-image {
    max-width: 100%;
    height: auto;
    padding: 12px;
    background: #fff;
    border-radius: 14px;
  }
  .dash-qr-download {
    font-size: 15px;
    color: #9dc0ff;
    text-decoration: none;
  }
  .dash-qr-download:hover {
    text-decoration: underline;
  }
  .link-edit-input {
    flex: 1;
    min-width: 0;
    box-sizing: border-box;
    padding: 10px 12px;
    font-family: inherit;
    font-size: 15px;
    color: #e8eaee;
    background: #1e2127;
    border: 1px solid #3a3e46;
    border-radius: 9px;
    outline: none;
  }
  .link-edit-input:focus {
    border-color: #5b8def;
  }
  .link-edit-actions {
    display: flex;
    gap: 6px;
    flex-shrink: 0;
  }
  .link-text-btn {
    padding: 8px 14px;
    font-family: inherit;
    font-size: 14px;
    font-weight: 500;
    border: none;
    border-radius: 9px;
    cursor: pointer;
    transition: filter 0.2s, background 0.2s;
  }
  .link-text-btn.is-save {
    color: #fff;
    background: #3b71d8;
  }
  .link-text-btn.is-save:hover {
    filter: brightness(1.08);
  }
  .link-text-btn.is-cancel {
    color: #c8ccd3;
    background: #2f333c;
  }
  .link-text-btn.is-cancel:hover {
    background: #3a3e46;
  }
  @media (max-width: 640px) {
    .dash-nav-inner {
      padding: 14px 16px;
    }
    .dash-nav-left {
      gap: 16px;
    }
    .dash-brand {
      font-size: 20px;
    }
    .dash-nav-links {
      gap: 16px;
    }
    .dash-nav-link {
      font-size: 14px;
    }
    .dash-container {
      padding: 28px 16px 56px;
    }
    .dash-title {
      font-size: 34px;
      margin-bottom: 24px;
    }
    .link-form-row {
      flex-direction: column;
      gap: 12px;
    }
    .link-edit-schedule {
      flex-direction: column;
    }
    .link-form-submit {
      padding: 15px;
    }
    .link-row {
      padding: 14px 16px;
    }
  }
`;
