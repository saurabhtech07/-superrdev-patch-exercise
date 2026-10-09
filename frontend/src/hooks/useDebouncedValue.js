import { useState, useEffect } from 'react';

// Returns `value` after it has stopped changing for `delay` ms.
// Used for the search box so we fire one request per pause in typing
// instead of one request per keystroke.
export function useDebouncedValue(value, delay = 300) {
  const [debounced, setDebounced] = useState(value);

  useEffect(() => {
    const timer = setTimeout(() => setDebounced(value), delay);
    return () => clearTimeout(timer);
  }, [value, delay]);

  return debounced;
}
