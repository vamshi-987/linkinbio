/**
 * Masks the local part of an email for display next to an OTP prompt.
 *
 * The screen has to identify which inbox to check, but the full address should not be readable
 * by anyone glancing at the screen. The mask is a fixed width so it doesn't leak the real length.
 *
 * abhiram1.chittampally@gmail.com -> ab•••••y@gmail.com
 */
export default function maskEmail(email) {
  if (typeof email !== 'string') return '';

  const at = email.lastIndexOf('@');
  // Not an address we can split; showing it raw would defeat the point, so show nothing.
  if (at <= 0) return '';

  const local = email.slice(0, at);
  const domain = email.slice(at);
  const mask = '•••••';

  // Too short to reveal anything without giving away most of the local part.
  if (local.length <= 2) return mask + domain;

  return local[0] + local[1] + mask + local[local.length - 1] + domain;
}
