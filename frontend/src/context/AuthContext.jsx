import { useState } from 'react';
import axiosClient from '../api/axiosClient';
import { AuthContext } from './useAuth';

export function AuthProvider({ children }) {
  const [username, setUsername] = useState(localStorage.getItem('username'));

  const login = async (username, password) => {
    const { data } = await axiosClient.post('/auth/login', { username, password });
    localStorage.setItem('token', data.token);
    localStorage.setItem('username', data.username);
    setUsername(data.username);
  };

  const signup = async (username, email, password) => {
    const { data } = await axiosClient.post('/auth/signup', { username, email, password });
    localStorage.setItem('token', data.token);
    localStorage.setItem('username', data.username);
    setUsername(data.username);
  };

  const logout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    setUsername(null);
  };

  return (
    <AuthContext.Provider value={{ username, login, signup, logout }}>
      {children}
    </AuthContext.Provider>
  );
}