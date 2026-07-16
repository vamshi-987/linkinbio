import { useState } from 'react';
import axiosClient from '../api/axiosClient';
import { verifyEmail as verifyEmailApi } from '../api/authApi';
import { AuthContext } from './useAuth';

export function AuthProvider({ children }) {
  const [username, setUsername] = useState(localStorage.getItem('username'));

  const storeSession = (data) => {
    localStorage.setItem('token', data.token);
    localStorage.setItem('username', data.username);
    setUsername(data.username);
  };

  const login = async (username, password) => {
    const { data } = await axiosClient.post('/auth/login', { username, password });
    storeSession(data);
  };

  /** Creates the account and emails a code. No session yet — verifyEmail completes the signup. */
  const signup = async (username, email, password) => {
    const { data } = await axiosClient.post('/auth/signup', { username, email, password });
    return data;
  };

  const verifyEmail = async (email, otp) => {
    storeSession(await verifyEmailApi(email, otp));
  };

  const logout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    setUsername(null);
  };

  return (
    <AuthContext.Provider value={{ username, login, signup, verifyEmail, logout }}>
      {children}
    </AuthContext.Provider>
  );
}
