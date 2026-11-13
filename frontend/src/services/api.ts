import type { Rol } from '../types/auth';
import { getAuthState } from '../utils/authState';
import { clearSession, decodeRoles, getAccessToken, saveAuthMessage } from '../utils/authStorage';

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

  const token = path === '/auth/login' ? null : getAccessToken();
  if (token) {
    try {
      decodeRoles(token);
    } catch {
      expireSession();
      throw new ApiError('Tu sesión expiró. Iniciá sesión nuevamente.', 401);
    }
    headers.set('Authorization', `Bearer ${token}`);
  }

  let response: Response;
  try {
    response = await fetch(buildApiUrl(path), {
      ...options,
      headers,
    });
  } catch {
    // fetch solo falla así cuando no hay respuesta: sin red o backend caído.
    throw new ApiError(
      'No se pudo conectar con el servidor. Verificá tu conexión e intentá nuevamente.',
      0,
    );
  }

  if (!response.ok) {
    const sessionExpired = response.status === 401 && token !== null && token === getAccessToken();
    if (sessionExpired) {
      expireSession();
    }

    const backendMessage = await readBackendMessage(response);
    throw new ApiError(
      backendMessage ?? fallbackMessage(response.status, path, sessionExpired),
      response.status,
    );
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return response.json() as Promise<T>;
}

function expireSession(): void {
  clearSession();
  saveAuthMessage('Tu sesión expiró. Iniciá sesión nuevamente.');
  window.dispatchEvent(new CustomEvent('fixhub:session-expired'));
}

// Mensajes genéricos de Spring/servlet que no aportan nada al usuario.
const GENERIC_MESSAGES = new Set([
  'access denied',
  'forbidden',
  'unauthorized',
  'bad request',
  'not found',
  'internal server error',
  'no static resource',
]);

/**
 * El backend responde con ProblemDetail ({ title, detail, errores? }).
 * Se prioriza `detail`, que es el mensaje de negocio en español.
 */
async function readBackendMessage(response: Response): Promise<string | null> {
  let body: unknown;
  try {
    body = await response.json();
  } catch {
    return null;
  }
  if (typeof body !== 'object' || body === null) return null;

  const record = body as Record<string, unknown>;
  const candidate = [record.detail, record.message].find(
    (value): value is string => typeof value === 'string' && value.trim().length > 0,
  );
  if (!candidate || GENERIC_MESSAGES.has(candidate.trim().toLowerCase())) return null;

  const fieldErrors = formatFieldErrors(record.errores);
  return fieldErrors ? `${candidate}. ${fieldErrors}` : candidate;
}

function formatFieldErrors(errores: unknown): string | null {
  if (typeof errores !== 'object' || errores === null) return null;
  const items = Object.entries(errores as Record<string, unknown>)
    .filter(([, message]) => typeof message === 'string' && message.trim())
    .map(([field, message]) => `${humanizeField(field)}: ${String(message)}`);
  return items.length ? `Revisá ${items.join('; ')}.` : null;
}

function humanizeField(field: string): string {
  const last = field.split('.').pop() ?? field;
  return last.replace(/\[\d+\]/g, '').replace(/([a-z])([A-Z])/g, '$1 $2').toLowerCase();
}

const ROLE_LABELS: Record<Rol, string> = {
  ADMINISTRADOR: 'Administrador',
  RECEPCIONISTA: 'Recepcionista',
  TECNICO: 'Técnico',
  CLIENTE: 'Cliente',
};

function forbiddenMessage(): string {
  const roles = getAuthState().roles;
  if (roles.length === 1 && roles[0] === 'CLIENTE') {
    return 'Solo podés consultar y gestionar tu propia información.';
  }
  if (roles.length === 1 && roles[0] === 'TECNICO') {
    return 'Como Técnico solo podés operar sobre las órdenes que tenés asignadas.';
  }
  const labels = roles.map((role) => ROLE_LABELS[role]).join(', ');
  return labels
    ? `Tu rol (${labels}) no tiene permiso para realizar esta acción.`
    : 'No tenés permiso para realizar esta acción.';
}

function fallbackMessage(status: number, path: string, sessionExpired: boolean): string {
  if (status === 400) return 'Los datos enviados no son válidos. Revisá el formulario e intentá nuevamente.';
  if (status === 401) {
    if (path.startsWith('/auth/login')) return 'El identificador o la contraseña son incorrectos.';
    return sessionExpired ? 'Tu sesión expiró. Iniciá sesión nuevamente.' : 'Necesitás iniciar sesión para continuar.';
  }
  if (status === 403) return forbiddenMessage();
  if (status === 404) return 'No se encontró la información solicitada.';
  if (status === 409) return 'La operación no se puede realizar en el estado actual.';
  if (status >= 500) return 'Ocurrió un problema en el servidor. Intentá nuevamente en unos minutos.';
  return 'No se pudo completar la operación. Intentá nuevamente.';
}
