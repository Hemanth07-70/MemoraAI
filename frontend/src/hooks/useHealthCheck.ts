import { useState, useEffect, useCallback } from 'react';
import type { ServiceStatus, HealthCheckResult } from '../types';

export function useHealthCheck(
  fetcher: () => Promise<unknown>,
  intervalMs: number = 15000
): HealthCheckResult {
  const [status, setStatus] = useState<ServiceStatus>('loading');
  const [error, setError] = useState<string | undefined>(undefined);

  const check = useCallback(async () => {
    try {
      await fetcher();
      setStatus('up');
      setError(undefined);
    } catch (err) {
      setStatus('down');
      if (err instanceof Error) {
        if (err.message === 'Failed to fetch') {
          setError('Network Error / Backend Offline or CORS Issue');
        } else {
          setError(err.message);
        }
      } else {
        setError('Unknown Error');
      }
    }
  }, [fetcher]);

  useEffect(() => {
    check();
    const id = setInterval(check, intervalMs);
    return () => clearInterval(id);
  }, [check, intervalMs]);

  return { status, error, retry: check };
}
