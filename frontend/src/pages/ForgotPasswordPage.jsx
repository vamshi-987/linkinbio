import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { forgotPassword, verifyOtp, resetPassword } from '../api/authApi';
import { useAuth } from '../context/useAuth';

const UserIcon = () => (
  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor"
       strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
    <circle cx="12" cy="7" r="4" />
  </svg>
);

const KeyIcon = () => (
  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor"
       strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    <circle cx="7.5" cy="15.5" r="5.5" />
    <path d="m21 2-9.6 9.6" />
    <path d="m15.5 7.5 3 3L22 7l-3-3" />
  </svg>
);

const LockIcon = () => (
  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor"
       strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
    <path d="M7 11V7a5 5 0 0 1 10 0v4" />
  </svg>
);

const errorMessage = (err, fallback) => err?.response?.data?.error || fallback;

export default function ForgotPasswordPage() {
  const navigate = useNavigate();
  const { username: sessionUsername } = useAuth();
  const [step, setStep] = useState('username'); // username | otp | password | done
  // Prefilled when logged in (changing your password from Settings), so you never re-enter it.
  const [username, setUsername] = useState(sessionUsername || '');
  const [otp, setOtp] = useState('');
  const [resetToken, setResetToken] = useState('');
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [error, setError] = useState('');
  const [info, setInfo] = useState('');
  const [loading, setLoading] = useState(false);

  const submitUsername = async (e) => {
    e.preventDefault();
    setError('');
    setInfo('');
    if (!username.trim()) return;
    setLoading(true);
    try {
      const { message } = await forgotPassword(username.trim());
      setInfo(message || 'If an account exists for that username, a reset code has been sent to its registered email.');
      setStep('otp');
    } catch (err) {
      setError(errorMessage(err, 'Something went wrong. Please try again.'));
    } finally {
      setLoading(false);
    }
  };

  const submitOtp = async (e) => {
    e.preventDefault();
    setError('');
    if (!/^[0-9]{6}$/.test(otp)) {
      setError('Enter the 6-digit code from your email.');
      return;
    }
    setLoading(true);
    try {
      const { resetToken: token } = await verifyOtp(username.trim(), otp);
      setResetToken(token);
      setInfo('');
      setStep('password');
    } catch (err) {
      setError(errorMessage(err, 'Invalid or expired code.'));
    } finally {
      setLoading(false);
    }
  };

  const submitPassword = async (e) => {
    e.preventDefault();
    setError('');
    if (password.length < 8) {
      setError('Password must be at least 8 characters.');
      return;
    }
    if (password !== confirm) {
      setError('Passwords do not match.');
      return;
    }
    setLoading(true);
    try {
      await resetPassword(resetToken, password);
      setStep('done');
    } catch (err) {
      setError(errorMessage(err, 'Could not reset password. Please start again.'));
    } finally {
      setLoading(false);
    }
  };

  const resendCode = async () => {
    setError('');
    setLoading(true);
    try {
      const { message } = await forgotPassword(username.trim());
      setInfo(message || 'A new code has been sent.');
    } catch (err) {
      setError(errorMessage(err, 'Could not resend the code.'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fp-screen">
      <style>{styles}</style>
      <div className="fp-card">
        <h1 className="fp-title">Reset Password</h1>

        {step === 'username' && (
          <>
            <p className="fp-subtitle">
              Enter your username and we'll send a verification code to your registered email.
            </p>
            <form onSubmit={submitUsername} className="fp-form">
              <div className="fp-field">
                <span className="fp-field-icon"><UserIcon /></span>
                <input
                  className="fp-input"
                  type="text"
                  placeholder="Username"
                  autoComplete="username"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                />
              </div>
              {error && <p className="fp-error">{error}</p>}
              <button className="fp-button" type="submit" disabled={loading}>
                {loading ? 'Sending…' : 'Send Code'}
              </button>
            </form>
          </>
        )}

        {step === 'otp' && (
          <>
            <p className="fp-subtitle">
              Enter the 6-digit code sent to your registered email.
            </p>
            <form onSubmit={submitOtp} className="fp-form">
              <div className="fp-field">
                <span className="fp-field-icon"><KeyIcon /></span>
                <input
                  className="fp-input fp-input-otp"
                  inputMode="numeric"
                  maxLength={6}
                  placeholder="______"
                  value={otp}
                  onChange={(e) => setOtp(e.target.value.replace(/\D/g, '').slice(0, 6))}
                />
              </div>
              {info && <p className="fp-info">{info}</p>}
              {error && <p className="fp-error">{error}</p>}
              <button className="fp-button" type="submit" disabled={loading}>
                {loading ? 'Verifying…' : 'Verify Code'}
              </button>
            </form>
            <p className="fp-footer">
              Didn't get it?{' '}
              <button type="button" className="fp-linkbtn" onClick={resendCode} disabled={loading}>
                Resend code
              </button>
            </p>
          </>
        )}

        {step === 'password' && (
          <>
            <p className="fp-subtitle">Choose a new password for your account.</p>
            <form onSubmit={submitPassword} className="fp-form">
              <div className="fp-field">
                <span className="fp-field-icon"><LockIcon /></span>
                <input
                  className="fp-input"
                  type="password"
                  placeholder="New password"
                  autoComplete="new-password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                />
              </div>
              <div className="fp-field">
                <span className="fp-field-icon"><LockIcon /></span>
                <input
                  className="fp-input"
                  type="password"
                  placeholder="Confirm new password"
                  autoComplete="new-password"
                  value={confirm}
                  onChange={(e) => setConfirm(e.target.value)}
                />
              </div>
              {error && <p className="fp-error">{error}</p>}
              <button className="fp-button" type="submit" disabled={loading}>
                {loading ? 'Saving…' : 'Reset Password'}
              </button>
            </form>
          </>
        )}

        {step === 'done' && (
          <>
            <p className="fp-success">✓ Your password has been reset.</p>
            <button className="fp-button" type="button" onClick={() => navigate('/login')}>
              Back to Log In
            </button>
          </>
        )}

        {step !== 'done' && (
          <p className="fp-footer">
            Remembered it? <Link to="/login" className="fp-link">Log In</Link>
          </p>
        )}
      </div>
    </div>
  );
}

const styles = `
  .fp-screen {
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
  .fp-card {
    width: 100%;
    max-width: 400px;
    box-sizing: border-box;
    padding: 40px 48px 44px;
    background: #2b2d34;
    border: 1px solid #363841;
    border-radius: 10px;
    box-shadow: 0 20px 50px rgba(0, 0, 0, 0.35);
  }
  .fp-title {
    margin: 0 0 12px;
    text-align: center;
    font-family: Georgia, 'Times New Roman', serif;
    font-weight: 400;
    font-size: 36px;
    letter-spacing: 0.5px;
    color: #eceef2;
  }
  .fp-subtitle {
    margin: 0 0 26px;
    text-align: center;
    font-size: 15px;
    line-height: 1.5;
    color: #9a9ea8;
  }
  .fp-subtitle strong {
    color: #cfd2d9;
  }
  .fp-form {
    display: flex;
    flex-direction: column;
    gap: 16px;
  }
  .fp-field {
    position: relative;
    display: flex;
    align-items: center;
  }
  .fp-field-icon {
    position: absolute;
    left: 16px;
    display: flex;
    color: #7d818b;
    pointer-events: none;
  }
  .fp-input {
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
  .fp-input::placeholder {
    color: #8b8f99;
  }
  .fp-input:focus {
    border-color: #3b6fe0;
    box-shadow: 0 0 0 3px rgba(59, 111, 224, 0.25);
  }
  .fp-input-otp {
    letter-spacing: 8px;
    font-size: 20px;
    text-align: center;
    padding-left: 46px;
  }
  .fp-button {
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
  .fp-button:hover:not(:disabled) {
    filter: brightness(1.07);
  }
  .fp-button:active:not(:disabled) {
    filter: brightness(0.95);
  }
  .fp-button:disabled {
    opacity: 0.6;
    cursor: default;
  }
  .fp-error {
    margin: 0;
    font-size: 14px;
    color: #ff6b6b;
  }
  .fp-info {
    margin: 0;
    font-size: 14px;
    color: #6fd08c;
  }
  .fp-success {
    margin: 8px 0 24px;
    text-align: center;
    font-size: 17px;
    color: #6fd08c;
  }
  .fp-footer {
    margin: 22px 0 0;
    text-align: center;
    font-size: 15px;
    color: #9a9ea8;
  }
  .fp-link {
    color: #eceef2;
    text-decoration: none;
  }
  .fp-link:hover {
    text-decoration: underline;
  }
  .fp-linkbtn {
    padding: 0;
    font-size: 15px;
    font-family: inherit;
    color: #eceef2;
    background: none;
    border: none;
    cursor: pointer;
    text-decoration: underline;
  }
  .fp-linkbtn:disabled {
    opacity: 0.6;
    cursor: default;
  }
  @media (max-width: 480px) {
    .fp-card {
      padding: 32px 24px 36px;
    }
    .fp-title {
      font-size: 30px;
    }
  }
`;
