import axios from "axios";
import type { HealthResponse } from '../types';

export const BACKEND_URL = import.meta.env.VITE_BACKEND_URL || "http://localhost:8080";
export const AI_SERVICE_URL = import.meta.env.VITE_AI_SERVICE_URL || "http://localhost:8000";

const api = axios.create({
  baseURL: BACKEND_URL,
  timeout: 90000, // 90s — enough to survive a cold start wake-up
});

// Intercept requests to add the JWT token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("token");
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Intercept responses to handle 401 Unauthorized
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      // Clear token and optionally redirect to login
      localStorage.removeItem("token");
      // window.location.href = "/login";
    }
    return Promise.reject(error);
  }
);

export async function fetchBackendHealth(): Promise<HealthResponse> {
  const response = await api.get('/api/health');
  return response.data;
}

export async function fetchAiServiceHealth(): Promise<HealthResponse> {
  const response = await axios.get(`${AI_SERVICE_URL}/`);
  return response.data;
}

export default api;
