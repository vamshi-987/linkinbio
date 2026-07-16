-- Issuing a code now counts recent codes per user+purpose to enforce the cooldown and hourly cap.
-- That runs on every signup, resend, login-while-unverified and forgot-password request, so it must
-- not fall back to scanning the user's whole OTP history.
CREATE INDEX IF NOT EXISTS idx_email_otps_user_purpose_created_at
    ON email_otps (user_id, purpose, created_at DESC);
