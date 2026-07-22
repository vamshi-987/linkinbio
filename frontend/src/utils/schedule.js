/**
 * Helpers for the link scheduling fields.
 *
 * `<input type="datetime-local">` speaks the visitor's wall clock with no zone attached, while the
 * API speaks UTC instants. Converting at this boundary — and only here — keeps a link scheduled for
 * "9am" going live at 9am wherever its owner happens to be.
 */

/** ISO instant → the `YYYY-MM-DDTHH:mm` string a datetime-local input expects, in local time. */
export const toLocalInput = (iso) => {
  if (!iso) return '';
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return '';

  const pad = (n) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
    + `T${pad(date.getHours())}:${pad(date.getMinutes())}`;
};

/** datetime-local value → ISO instant, or null when the field was left empty ("no bound"). */
export const toInstant = (localValue) => {
  if (!localValue) return null;
  const date = new Date(localValue);
  return Number.isNaN(date.getTime()) ? null : date.toISOString();
};

const formatter = new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' });

export const formatWhen = (iso) => (iso ? formatter.format(new Date(iso)) : '');

/** Human summary of a link's schedule, or '' when it has no bounds. */
export const describeSchedule = ({ visibleFrom, visibleUntil }) => {
  if (visibleFrom && visibleUntil) return `${formatWhen(visibleFrom)} → ${formatWhen(visibleUntil)}`;
  if (visibleFrom) return `From ${formatWhen(visibleFrom)}`;
  if (visibleUntil) return `Until ${formatWhen(visibleUntil)}`;
  return '';
};

export const STATUS_LABELS = {
  LIVE: 'Live',
  SCHEDULED: 'Scheduled',
  EXPIRED: 'Expired',
  HIDDEN: 'Hidden',
};
