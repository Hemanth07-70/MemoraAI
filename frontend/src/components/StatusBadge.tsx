import type { ServiceStatus } from '../types';

interface StatusBadgeProps {
  label: string;
  status: ServiceStatus;
  error?: string;
  onRetry: () => void;
}

const STATUS_CONFIG: Record<ServiceStatus, { color: string; bg: string; ring: string; text: string; dot: string }> = {
  loading: {
    color: 'text-warning',
    bg: 'bg-warning/10',
    ring: 'ring-warning/30',
    text: 'Checking...',
    dot: 'bg-warning animate-pulse',
  },
  up: {
    color: 'text-success',
    bg: 'bg-success/10',
    ring: 'ring-success/30',
    text: 'Connected',
    dot: 'bg-success',
  },
  down: {
    color: 'text-error',
    bg: 'bg-error/10',
    ring: 'ring-error/30',
    text: 'Unreachable',
    dot: 'bg-error',
  },
};

export function StatusBadge({ label, status, error, onRetry }: StatusBadgeProps) {
  const config = STATUS_CONFIG[status];

  return (
    <div
      className={`flex flex-col rounded-2xl ${config.bg} ring-1 ${config.ring} px-6 py-4 transition-all duration-300`}
    >
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <span className={`inline-block h-3 w-3 rounded-full ${config.dot}`} />
          <div>
            <p className="text-sm font-medium text-surface-700 dark:text-surface-200">{label}</p>
            <p className={`text-xs font-semibold ${config.color}`}>{config.text}</p>
          </div>
        </div>
        {status === 'down' && (
          <button
            onClick={onRetry}
            className="rounded-lg bg-surface-800 px-3 py-1 text-xs font-medium text-white transition hover:bg-surface-700"
          >
            Retry
          </button>
        )}
      </div>
      {status === 'down' && error && (
        <div className="mt-3 text-xs text-error/80 bg-error/5 p-2 rounded-lg border border-error/10">
          {error}
        </div>
      )}
    </div>
  );
}

