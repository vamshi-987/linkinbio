-- A user row is now created only after its email verification code is confirmed (the pending signup
-- lives in Redis until then), so every row is verified by construction and the column was always
-- true and never read. Drop it.
ALTER TABLE users DROP COLUMN email_verified;
