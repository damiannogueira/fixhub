import { clearSession } from '../utils/authStorage';

export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api';

export class ApiError extends Error {
  public readonly status: number;

  constructor(
    message: string,
    status: number,
  ) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
  }
}

export function buildApiUrl(path: string): string {
  return `${API_BASE_URL}${path}`;
}

export async function apiRequest<T>(
  path: string,
  options: RequestInit = {},
): Promise<T> {
  const headers = new Headers(options.headers);
  headers.set('Accept', 'application/json');

  if (options.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }

  const token = localStorage.getItem('access_token');
  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  const response = await fetch(buildApiUrl(path), {
    ...options,
    headers,
  });

  if (!response.ok) {
    if (response.status === 401 && token && window.location.hash !== '#/login') {
      clearSession();
      window.dispatchEvent(new CustomEvent('fixhub:session-expired'));
    }
    let message = `La solicitud falló con estado ${response.status}.`;

    try {
      const body: unknown = await response.json();
      if (typeof body === 'object' && body !== null && 'message' in body) {
        const apiMessage = body.message;
        if (typeof apiMessage === 'string' && apiMessage.trim()) {
          message = apiMessage;
        }
      }
    } catch {
      // Some error responses have no JSON body.
    }

    throw new ApiError(message, response.status);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return response.json() as Promise<T>;
}
