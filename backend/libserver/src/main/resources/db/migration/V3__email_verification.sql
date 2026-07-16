-- New signups must verify their email with an OTP. Existing accounts are grandfathered in as
-- verified so this rollout cannot lock anyone out of an account they already have.
ALTER TABLE users ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT false;
UPDATE users SET email_verified = true;

-- The OTP table now serves both password resets and email verification.
ALTER TABLE password_reset_otps RENAME TO email_otps;
ALTER INDEX idx_password_reset_otps_user_id RENAME TO idx_email_otps_user_id;
ALTER TABLE email_otps ADD COLUMN purpose VARCHAR(32) NOT NULL DEFAULT 'PASSWORD_RESET';
