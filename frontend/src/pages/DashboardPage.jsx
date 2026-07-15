import { useEffect, useState } from 'react';
import { getLinks, createLink, updateLink, deleteLink, reorderLinks } from '../api/linksApi';
import LinkForm from '../components/LinkForm';
import LinkList from '../components/LinkList';
import { useAuth } from '../context/useAuth';
import { Link } from 'react-router-dom';

export default function DashboardPage() {
  const [links, setLinks] = useState([]);
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

  const handleUpdate = async (id, data) => {
    const existing = links.find((l) => l.id === id);
    const updated = await updateLink(id, { active: existing?.active ?? true, ...data });
    setLinks((prev) => prev.map((l) => (l.id === id ? updated : l)));
  };

  const handleDelete = async (id) => {
    await deleteLink(id);
    setLinks((prev) => prev.filter((l) => l.id !== id));
  };

  const swap = async (i, j) => {
    const updated = [...links];
    [updated[i], updated[j]] = [updated[j], updated[i]];
    setLinks(updated);
    await reorderLinks(updated.map((l) => l.id));
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
        <LinkList
          links={links}
          onUpdate={handleUpdate}
          onDelete={handleDelete}
          onMoveUp={(i) => i > 0 && swap(i, i - 1)}
          onMoveDown={(i) => i < links.length - 1 && swap(i, i + 1)}
        />
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
    align-items: stretch;
    gap: 16px;
    margin-bottom: 32px;
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
    min-width: 0;
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
    align-items: center;
    gap: 10px;
    flex: 1;
    min-width: 0;
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
    .link-form {
      flex-direction: column;
      gap: 12px;
    }
    .link-form-submit {
      padding: 15px;
    }
    .link-row {
      padding: 14px 16px;
    }
  }
`;
