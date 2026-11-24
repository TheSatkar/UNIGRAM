/**
 * Centralized API Client for UNIMART + Uni-Gram
 * Handles base URL configuration, Bearer token injection, JSON serialization,
 * error handling, and connection checks.
 */

import { isIntegratedMode } from '../lib/appMode.js';

export const API_BASE_URL = (
  import.meta.env.VITE_UNIGRAM_API_BASE_URL || 'http://127.0.0.1:8080'
).replace(/\/+$/, '');

const TOKEN_KEY = 'unimart_auth_token';
const IS_PROD_MODE = Boolean(
  import.meta.env.PROD ||
  import.meta.env.VITE_APP_MODE === 'production' ||
  import.meta.env.VITE_ENFORCE_PROD_AUTH === 'true'
);

export function getStoredToken() {
  try {
    return localStorage.getItem(TOKEN_KEY) || null;
  } catch {
    return null;
  }
}

export function setStoredToken(token) {
  try {
    if (token) {
      localStorage.setItem(TOKEN_KEY, token);
    } else {
      localStorage.removeItem(TOKEN_KEY);
    }
  } catch {
    // ignore
  }
}

export async function apiRequest(endpoint, options = {}) {
  const url = `${API_BASE_URL}${endpoint}`;
  const headers = {
    Accept: 'application/json',
    ...(options.headers || {})
  };

  const token = getStoredToken();
  if (token && !headers.Authorization) {
    headers.Authorization = `Bearer ${token}`;
  }

  const fetchOptions = { ...options, headers };

  if (fetchOptions.body && typeof fetchOptions.body === 'object') {
    headers['Content-Type'] = 'application/json';
    fetchOptions.body = JSON.stringify(fetchOptions.body);
  }

  let res;
  try {
    res = await fetch(url, fetchOptions);
  } catch (netErr) {
    if (isIntegratedMode() || IS_PROD_MODE) {
      throw new Error(
        `Cannot connect to Uni-Gram Java server at ${API_BASE_URL}. Ensure the backend is running on port 8080 (Option 15 / run-demo.bat). Error: ${netErr.message}`
      );
    }
    throw netErr;
  }

  let json = null;
  const contentType = res.headers.get('content-type') || '';
  if (contentType.includes('application/json')) {
    try {
      json = await res.json();
    } catch {
      json = null;
    }
  }

  if (!res.ok) {
    const errorMsg = json?.error || `HTTP ${res.status}: ${res.statusText}`;
    const err = new Error(errorMsg);
    err.status = res.status;
    err.data = json?.data || null;
    throw err;
  }

  return json;
}

export const apiClient = {
  get: (endpoint, options = {}) => apiRequest(endpoint, { ...options, method: 'GET' }),
  post: (endpoint, body, options = {}) => apiRequest(endpoint, { ...options, method: 'POST', body }),
  put: (endpoint, body, options = {}) => apiRequest(endpoint, { ...options, method: 'PUT', body }),
  delete: (endpoint, options = {}) => apiRequest(endpoint, { ...options, method: 'DELETE' }),
  getToken: getStoredToken,
  setToken: setStoredToken,
  getBaseUrl: () => API_BASE_URL
};

export default apiClient;
