import { useEffect, useState } from 'react';
import { getLinks, createLink, updateLink, deleteLink, reorderLinks } from '../api/linksApi';
import LinkForm from '../components/LinkForm';
import LinkList from '../components/LinkList';
import { useAuth } from '../context/useAuth';
import { Link } from 'react-router-dom';

export default function DashboardPage() {
  const [links, setLinks] = useState([]);
  const { username, logout } = useAuth();

  useEffect(() => { getLinks().then(setLinks); }, []);

  const handleAdd = async (data) => {
    const newLink = await createLink(data);
    setLinks([...links, newLink]);
  };

  const handleUpdate = async (id, data) => {
    const existing = links.find(l => l.id === id);
    const updated = await updateLink(id, { active: existing?.active ?? true, ...data });
    setLinks(links.map(l => (l.id === id ? updated : l)));
  };

  const handleDelete = async (id) => {
    await deleteLink(id);
    setLinks(links.filter(l => l.id !== id));
  };

  const swap = async (i, j) => {
    const updated = [...links];
    [updated[i], updated[j]] = [updated[j], updated[i]];
    setLinks(updated);
    await reorderLinks(updated.map(l => l.id));
  };

  return (
    <div className="max-w-xl mx-auto mt-10 p-4">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold">Your links</h1>
        <div className="flex gap-3 text-sm">
          <Link to="/analytics">Analytics</Link>
          <Link to="/settings">Settings</Link>
          <Link to={`/${username}`} target="_blank">View page</Link>
          <button onClick={logout}>Logout</button>
        </div>
      </div>
      <LinkForm onAdd={handleAdd} />
      <LinkList
        links={links}
        onUpdate={handleUpdate}
        onDelete={handleDelete}
        onMoveUp={(i) => i > 0 && swap(i, i - 1)}
        onMoveDown={(i) => i < links.length - 1 && swap(i, i + 1)}
      />
    </div>
  );
}