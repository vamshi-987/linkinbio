import { useEffect, useState } from 'react';

export const MAX_AVATAR_BYTES = 2 * 1024 * 1024;
export const ALLOWED_AVATAR_TYPES = ['image/png', 'image/jpeg'];

/**
 * Holds a picked-but-not-yet-uploaded avatar and the blob URL previewing it. Nothing here touches
 * the network: the file reaches the server only when the surrounding form is submitted, so leaving
 * the page discards the pick rather than half-applying it.
 */
export default function useAvatarPicker() {
  const [pending, setPending] = useState(null);
  const [error, setError] = useState('');

  // Owns the lifetime of the preview's blob URL: released whenever the pick is replaced, cleared,
  // or the component unmounts.
  useEffect(() => {
    if (!pending) return undefined;
    return () => URL.revokeObjectURL(pending.preview);
  }, [pending]);

  /** Reads the file out of a change event. Returns whether it was accepted. */
  const choose = (e) => {
    const file = e.target.files?.[0];
    // Cleared up front so re-picking the same file still fires a change event.
    e.target.value = '';
    if (!file) return false;

    // A first pass only, to reject obviously wrong files without a round trip. The server re-checks
    // type, size and dimensions on upload and its verdict is the one that counts.
    if (!ALLOWED_AVATAR_TYPES.includes(file.type)) {
      setError('Please choose a PNG or JPEG image.');
      return false;
    }
    if (file.size > MAX_AVATAR_BYTES) {
      setError('That image is larger than 2 MB.');
      return false;
    }

    setError('');
    setPending({ file, preview: URL.createObjectURL(file) });
    return true;
  };

  return {
    pending,
    error,
    choose,
    clear: () => setPending(null),
    clearError: () => setError(''),
    setError,
  };
}
