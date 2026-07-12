export interface HealthResponse {
  status: string;
  service: string;
}

export type ServiceStatus = 'loading' | 'up' | 'down';

export interface HealthCheckResult {
  status: ServiceStatus;
  error?: string;
  retry: () => void;
}

