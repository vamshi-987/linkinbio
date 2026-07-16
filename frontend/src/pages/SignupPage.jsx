import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/useAuth';

const UserIcon = () => (
  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor"
       strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
    <circle cx="12" cy="7" r="4" />
  </svg>
);

const MailIcon = () => (
  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor"
       strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    <rect x="2" y="4" width="20" height="16" rx="2" />
    <path d="m22 7-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7" />
  </svg>
);

const LockIcon = () => (
  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor"
       strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
    <path d="M7 11V7a5 5 0 0 1 10 0v4" />
  </svg>
);

export default function SignupPage() {
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const { signup } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    try {
      await signup(username, email, password);
      // The account exists but is unverified until the emailed code is confirmed.
      navigate(`/verify-email?email=${encodeURIComponent(email)}`);
    } catch (err) {
      setError(err.response?.data?.error || 'Signup failed');
    }
  };

  return (
    <div className="login-screen">
      <style>{loginStyles}</style>
      <div className="login-card">
        <h1 className="login-title">Sign Up</h1>
        <form onSubmit={handleSubmit} className="login-form">
          <div className="login-field">
            <span className="login-field-icon"><UserIcon /></span>
            <input
              className="login-input"
              placeholder="Username"
              autoComplete="username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
            />
          </div>
          <div className="login-field">
            <span className="login-field-icon"><MailIcon /></span>
            <input
              className="login-input"
              placeholder="Email"
              type="email"
              autoComplete="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
          </div>
          <div className="login-field">
            <span className="login-field-icon"><LockIcon /></span>
            <input
              className="login-input"
              placeholder="Password"
              type="password"
              autoComplete="new-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
          </div>
          {error && <p className="login-error">{error}</p>}
          <button className="login-button" type="submit">Sign Up</button>
        </form>
        <p className="login-footer">
          Already have an account? <Link to="/login" className="login-link">Log In</Link>
        </p>
      </div>
    </div>
  );
}

const loginStyles = `
  .login-screen {
    position: fixed;
    inset: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 24px;
    box-sizing: border-box;
    background: #24262d;
    font-family: system-ui, 'Segoe UI', Roboto, sans-serif;
    overflow-y: auto;
  }
  .login-card {
    width: 100%;
    max-width: 400px;
    box-sizing: border-box;
    padding: 40px 48px 44px;
    background: #2b2d34;
    border: 1px solid #363841;
    border-radius: 10px;
    box-shadow: 0 20px 50px rgba(0, 0, 0, 0.35);
  }
  .login-title {
    margin: 0 0 32px;
    text-align: center;
    font-family: Georgia, 'Times New Roman', serif;
    font-weight: 400;
    font-size: 40px;
    letter-spacing: 0.5px;
    color: #eceef2;
  }
  .login-form {
    display: flex;
    flex-direction: column;
    gap: 16px;
  }
  .login-field {
    position: relative;
    display: flex;
    align-items: center;
  }
  .login-field-icon {
    position: absolute;
    left: 16px;
    display: flex;
    color: #7d818b;
    pointer-events: none;
  }
  .login-input {
    width: 100%;
    box-sizing: border-box;
    padding: 14px 16px 14px 46px;
    font-size: 16px;
    color: #eceef2;
    background: #2f313a;
    border: 1px solid #3d404a;
    border-radius: 8px;
    outline: none;
    transition: border-color 0.2s, box-shadow 0.2s;
  }
  .login-input::placeholder {
    color: #8b8f99;
  }
  .login-input:focus {
    border-color: #3b6fe0;
    box-shadow: 0 0 0 3px rgba(59, 111, 224, 0.25);
  }
  .login-button {
    margin-top: 4px;
    padding: 15px 16px;
    font-size: 17px;
    font-weight: 500;
    color: #fff;
    background: linear-gradient(180deg, #2f6ef0 0%, #2159d6 100%);
    border: none;
    border-radius: 8px;
    cursor: pointer;
    transition: filter 0.2s;
  }
  .login-button:hover {
    filter: brightness(1.07);
  }
  .login-button:active {
    filter: brightness(0.95);
  }
  .login-error {
    margin: 0;
    font-size: 14px;
    color: #ff6b6b;
  }
  .login-footer {
    margin: 22px 0 0;
    text-align: center;
    font-size: 15px;
    color: #9a9ea8;
  }
  .login-link {
    color: #eceef2;
    text-decoration: none;
  }
  .login-link:hover {
    text-decoration: underline;
  }
  @media (max-width: 480px) {
    .login-card {
      padding: 32px 24px 36px;
    }
    .login-title {
      font-size: 34px;
      margin-bottom: 26px;
    }
  }
`;
