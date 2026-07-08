import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import axiosClient from '../api/axiosClient';

const themeStyles = {
  default: 'bg-white text-black',
  dark: 'bg-gray-900 text-white',
  pastel: 'bg-pink-50 text-gray-800',
  neon: 'bg-black text-green-400',
};

export default function PublicProfilePage() {
  const { username } = useParams();
  const [profile, setProfile] = useState(null);
  const [notFound, setNotFound] = useState(false);

  useEffect(() => {
    axiosClient.get(`/public/${username}`)
      .then(r => setProfile(r.data))
      .catch(() => setNotFound(true));
  }, [username]);

  if (notFound) return <p className="text-center mt-20">Profile not found</p>;
  if (!profile) return <p className="text-center mt-20">Loading...</p>;

  const theme = themeStyles[profile.theme] || themeStyles.default;
  const apiBase = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081/api';

  return (
    <div className={`min-h-screen flex flex-col items-center pt-16 ${theme}`}>
      {profile.avatarUrl && (
        <img src={profile.avatarUrl} alt={profile.username} className="w-20 h-20 rounded-full object-cover mb-3" />
      )}
      <h1 className="text-xl font-semibold">{profile.displayName || profile.username}</h1>
      {profile.bio && <p className="mt-1 text-sm opacity-80">{profile.bio}</p>}
      <div className="w-full max-w-sm mt-6 space-y-3 px-4">
        {profile.links.map(link => (
          <a key={link.id} href={`${apiBase}/public/click/${link.id}`}
             className="block text-center border rounded-lg py-3 hover:opacity-80 transition">
            {link.title}
          </a>
        ))}
      </div>
    </div>
  );
}