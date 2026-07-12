import { useCallback } from 'react';
import { MainLayout } from '../layouts/MainLayout';
import { StatusBadge } from '../components/StatusBadge';
import { useHealthCheck } from '../hooks/useHealthCheck';
import { fetchBackendHealth, fetchAiServiceHealth } from '../services/api';

export function HomePage() {
  const backendFetcher = useCallback(() => fetchBackendHealth(), []);
  const aiFetcher = useCallback(() => fetchAiServiceHealth(), []);

  const backend = useHealthCheck(backendFetcher);
  const aiService = useHealthCheck(aiFetcher);

  return (
    <MainLayout>
      <main className="flex flex-1 flex-col items-center justify-center px-4 py-16">
        {/* Logo mark */}
        <div className="mb-8 flex h-20 w-20 items-center justify-center rounded-2xl bg-gradient-to-br from-primary-500 to-primary-700 shadow-lg shadow-primary-500/30">
          <svg xmlns="http://www.w3.org/2000/svg" className="h-10 w-10 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M12 18v-5.25m0 0a6.01 6.01 0 0 0 1.5-.189m-1.5.189a6.01 6.01 0 0 1-1.5-.189m3.75 7.478a12.06 12.06 0 0 1-4.5 0m3.75 2.383a14.406 14.406 0 0 1-3 0M14.25 18v-.192c0-.983.658-1.823 1.508-2.316a7.5 7.5 0 1 0-7.517 0c.85.493 1.509 1.333 1.509 2.316V18" />
          </svg>
        </div>

        {/* Title */}
        <h1 className="bg-gradient-to-r from-white via-primary-200 to-primary-400 bg-clip-text text-5xl font-extrabold tracking-tight text-transparent sm:text-6xl">
          MemoraAI
        </h1>
        <p className="mt-4 max-w-md text-center text-lg text-surface-200/70">
          AI Learning Platform
        </p>

        {/* Divider */}
        <div className="my-10 h-px w-48 bg-gradient-to-r from-transparent via-primary-500/50 to-transparent" />

        {/* Service status cards */}
        <div className="w-full max-w-md space-y-4">
          <h2 className="mb-2 text-center text-xs font-semibold uppercase tracking-widest text-surface-200/50">
            Service Health
          </h2>
          <StatusBadge label="Backend Status" status={backend.status} error={backend.error} onRetry={backend.retry} />
          <StatusBadge label="AI Service Status" status={aiService.status} error={aiService.error} onRetry={aiService.retry} />
        </div>

        {/* Footer tagline */}
        <p className="mt-16 text-xs text-surface-200/30">
          Milestone 1 &middot; Foundation
        </p>
      </main>
    </MainLayout>
  );
}
