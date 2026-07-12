import type { HealthResponse } from '../types';

const BACKEND_URL = import.meta.env.VITE_BACKEND_URL || 'http://localhost:8080';
const AI_SERVICE_URL = import.meta.env.VITE_AI_SERVICE_URL || 'http://localhost:8000';

export async function fetchBackendHealth(): Promise<HealthResponse> {
  const response = await fetch(`${BACKEND_URL}/api/health`);
  if (!response.ok) {
    throw new Error(`Backend health check failed: ${response.status}`);
  }
  return response.json();
}

export async function fetchAiServiceHealth(): Promise<HealthResponse> {
  const response = await fetch(`${AI_SERVICE_URL}/`);
  if (!response.ok) {
    throw new Error(`AI service health check failed: ${response.status}`);
  }
  return response.json();
}
