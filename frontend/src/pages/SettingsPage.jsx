import { useState } from 'react';
import { updateProfile } from '../api/profileApi';
import { Link } from 'react-router-dom';

const themes = ['default', 'dark', 'pastel', 'neon'];

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
    <div className="max-w-sm mx-auto mt-10 p-4">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold">Settings</h1>
        <Link to="/dashboard" className="text-sm underline">Back</Link>
      </div>
      <form onSubmit={handleSave} className="space-y-3">
        <input className="w-full border rounded px-3 py-2" placeholder="Display name"
               value={displayName} onChange={e => setDisplayName(e.target.value)} />
        <textarea className="w-full border rounded px-3 py-2" placeholder="Bio"
                  value={bio} onChange={e => setBio(e.target.value)} />
        <div className="flex gap-2">
          {themes.map(t => (
            <button type="button" key={t} onClick={() => setTheme(t)}
                    className={`px-3 py-1 rounded border ${theme === t ? 'bg-black text-white' : ''}`}>
              {t}
            </button>
          ))}
        </div>
        <button className="w-full bg-black text-white rounded py-2" type="submit">Save</button>
        {saved && <p className="text-green-600 text-sm">Saved!</p>}
      </form>
    </div>
  );
}