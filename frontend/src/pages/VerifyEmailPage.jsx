import { useEffect, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../context/useAuth';
import { resendVerification } from '../api/authApi';
import maskEmail from '../utils/maskEmail';

const KeyIcon = () => (
  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor"
       strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    <circle cx="7.5" cy="15.5" r="5.5" />
    <path d="m21 2-9.6 9.6" />
    <path d="m15.5 7.5 3 3L22 7l-3-3" />
  </svg>
);

export default function VerifyEmailPage() {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const { verifyEmail } = useAuth();

  const email = params.get('email') || '';
  const [otp, setOtp] = useState('');
  const [error, setError] = useState('');
  const [info, setInfo] = useState('');
  const [loading, setLoading] = useState(false);

  // The staged signup expires in Redis after a fixed window; once it lapses there is nothing left to
  // verify, so send the user back to log in (where they can sign up again) exactly when it does.
  useEffect(() => {
    const raw = localStorage.getItem('verifyExpiresAt');
    if (!raw) return undefined;
    const remaining = Number(raw) - Date.now();
    if (remaining <= 0) {
      localStorage.removeItem('verifyExpiresAt');
      navigate('/login', { replace: true });
      return undefined;
    }
    const timer = setTimeout(() => {
      localStorage.removeItem('verifyExpiresAt');
      navigate('/login', { replace: true });
    }, remaining);
    return () => clearTimeout(timer);
  }, [navigate]);

  const handleVerify = async (e) => {
    e.preventDefault();
    setError('');
    if (!/^[0-9]{6}$/.test(otp)) {
      setError('Enter the 6-digit code from your email.');
      return;
    }
    setLoading(true);
    try {
      await verifyEmail(email, otp);
      localStorage.removeItem('verifyExpiresAt');
      navigate('/dashboard');
    } catch (err) {
      setError(err.response?.data?.error || 'Invalid or expired code.');
    } finally {
      setLoading(false);
    }
  };

  const handleResend = async () => {
    setError('');
    setInfo('');
    setLoading(true);
    try {
      const { message } = await resendVerification(email);
      setInfo(message || 'A new code has been sent.');
    } catch (err) {
      setError(err.response?.data?.error || 'Could not resend the code.');
    } finally {
      setLoading(false);
    }
  };

  // Landing here without an email (e.g. a bookmarked URL) leaves nothing to verify against.
  if (!email) {
    return (
      <div className="ve-screen">
        <style>{styles}</style>
        <div className="ve-card">
          <h1 className="ve-title">Verify Email</h1>
          <p className="ve-subtitle">
            We don't know which account to verify. Please log in or sign up again.
          </p>
          <Link to="/login" className="ve-button ve-button-link">Go to Log In</Link>
        </div>
      </div>
    );
  }

  return (
    <div className="ve-screen">
      <style>{styles}</style>
      <div className="ve-card">
        <h1 className="ve-title">Verify Email</h1>
        <p className="ve-subtitle">
          Enter the 6-digit code sent to <strong>{maskEmail(email)}</strong>.
        </p>

        <form onSubmit={handleVerify} className="ve-form">
          <div className="ve-field">
            <span className="ve-field-icon"><KeyIcon /></span>
            <input
              className="ve-input ve-input-otp"
              inputMode="numeric"
              maxLength={6}
              placeholder="______"
              autoFocus
              value={otp}
              onChange={(e) => setOtp(e.target.value.replace(/\D/g, '').slice(0, 6))}
            />
          </div>
          {info && <p className="ve-info">{info}</p>}
          {error && <p className="ve-error">{error}</p>}
          <button className="ve-button" type="submit" disabled={loading}>
            {loading ? 'Verifying…' : 'Verify & Continue'}
          </button>
        </form>

        <p className="ve-footer">
          Didn't get it?{' '}
          <button type="button" className="ve-linkbtn" onClick={handleResend} disabled={loading}>
            Resend code
          </button>
        </p>
        <p className="ve-footer">
          <Link to="/login" className="ve-link">Back to Log In</Link>
        </p>
      </div>
    </div>
  );
}

const styles = `
  .ve-screen {
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
  .ve-card {
    width: 100%;
    max-width: 400px;
    box-sizing: border-box;
    padding: 40px 48px 44px;
    background: #2b2d34;
    border: 1px solid #363841;
    border-radius: 10px;
    box-shadow: 0 20px 50px rgba(0, 0, 0, 0.35);
  }
  .ve-title {
    margin: 0 0 12px;
    text-align: center;
    font-family: Georgia, 'Times New Roman', serif;
    font-weight: 400;
    font-size: 36px;
    letter-spacing: 0.5px;
    color: #eceef2;
  }
  .ve-subtitle {
    margin: 0 0 26px;
    text-align: center;
    font-size: 15px;
    line-height: 1.5;
    color: #9a9ea8;
    word-break: break-word;
  }
  .ve-subtitle strong {
    color: #cfd2d9;
  }
  .ve-form {
    display: flex;
    flex-direction: column;
    gap: 16px;
  }
  .ve-field {
    position: relative;
    display: flex;
    align-items: center;
  }
  .ve-field-icon {
    position: absolute;
    left: 16px;
    display: flex;
    color: #7d818b;
    pointer-events: none;
  }
  .ve-input {
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
  .ve-input::placeholder {
    color: #8b8f99;
  }
  .ve-input:focus {
    border-color: #3b6fe0;
    box-shadow: 0 0 0 3px rgba(59, 111, 224, 0.25);
  }
  .ve-input-otp {
    letter-spacing: 8px;
    font-size: 20px;
    text-align: center;
  }
  .ve-button {
    display: block;
    width: 100%;
    box-sizing: border-box;
    margin-top: 4px;
    padding: 15px 16px;
    font-family: inherit;
    font-size: 17px;
    font-weight: 500;
    text-align: center;
    text-decoration: none;
    color: #fff;
    background: linear-gradient(180deg, #2f6ef0 0%, #2159d6 100%);
    border: none;
    border-radius: 8px;
    cursor: pointer;
    transition: filter 0.2s;
  }
  .ve-button:hover:not(:disabled) {
    filter: brightness(1.07);
  }
  .ve-button:disabled {
    opacity: 0.6;
    cursor: default;
  }
  .ve-button-link {
    margin-top: 20px;
  }
  .ve-error {
    margin: 0;
    font-size: 14px;
    color: #ff6b6b;
  }
  .ve-info {
    margin: 0;
    font-size: 14px;
    color: #6fd08c;
  }
  .ve-footer {
    margin: 18px 0 0;
    text-align: center;
    font-size: 15px;
    color: #9a9ea8;
  }
  .ve-link {
    color: #eceef2;
    text-decoration: none;
  }
  .ve-link:hover {
    text-decoration: underline;
  }
  .ve-linkbtn {
    padding: 0;
    font-size: 15px;
    font-family: inherit;
    color: #eceef2;
    background: none;
    border: none;
    cursor: pointer;
    text-decoration: underline;
  }
  .ve-linkbtn:disabled {
    opacity: 0.6;
    cursor: default;
  }
  @media (max-width: 480px) {
    .ve-card {
      padding: 32px 24px 36px;
    }
    .ve-title {
      font-size: 30px;
    }
  }
`;
