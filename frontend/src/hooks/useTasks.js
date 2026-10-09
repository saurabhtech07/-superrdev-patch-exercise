import { useState, useEffect } from 'react';
import { fetchTasks } from '../api';

export function useTasks(query, status, page, pageSize) {
  const [tasks, setTasks] = useState([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    // Ignore responses from a previous request once params change, so a slow
    // older response can never overwrite results of a newer one.
    let cancelled = false;

    setLoading(true);
    setError(null);

    fetchTasks({ query, status, page, pageSize })
      .then((data) => {
        if (cancelled) return;
        setTasks(data.items);
        setTotal(data.total);
      })
      .catch((err) => {
        if (cancelled) return;
        setError(err.message);
      })
      .finally(() => {
        // Always clear the spinner — previously this only ran on success,
        // so a failed request left the UI on "Loading tasks..." forever.
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [query, status, page, pageSize]);

  return { tasks, total, loading, error };
}
