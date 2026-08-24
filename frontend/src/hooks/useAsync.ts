import { useCallback, useEffect, useState } from 'react';

interface AsyncState<T> {
  data: T | null;
  isLoading: boolean;
  error: unknown;
}

/**
 * Small shared fetch-state hook so every module page doesn't hand-roll its
 * own loading/error booleans. Deliberately not a data-fetching library
 * (no cache, no retries) — this app's scope doesn't warrant one.
 */
export function useAsync<T>(fetcher: () => Promise<T>, deps: unknown[] = []) {
  const [state, setState] = useState<AsyncState<T>>({
    data: null,
    isLoading: true,
    error: null,
  });

  const load = useCallback(() => {
    let cancelled = false;
    setState((prev) => ({ ...prev, isLoading: true, error: null }));
    fetcher()
      .then((data) => {
        if (!cancelled) setState({ data, isLoading: false, error: null });
      })
      .catch((error: unknown) => {
        if (!cancelled) setState({ data: null, isLoading: false, error });
      });
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);

  useEffect(() => load(), [load]);

  return { ...state, reload: load };
}
