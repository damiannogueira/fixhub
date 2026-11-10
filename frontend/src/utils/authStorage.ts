import type { LoginResponse, Rol } from '../types/auth';

const ACCESS_TOKEN_KEY = 'access_token';
const PASSWORD_CHANGE_KEY = 'fixhub.requires_password_change';
const AUTH_MESSAGE_KEY = 'fixhub.auth_message';

type JwtPayload = {
  roles?: unknown;
};

function decodeBase64Url(value: string): string {
  const base64 = value.replaceAll('-', '+').replaceAll('_', '/');
  const padded = base64.padEnd(Math.ceil(base64.length / 4) * 4, '=');
  return atob(padded);
}

export function decodeRoles(token: string): Rol[] {
  const parts = token.split('.');
  if (parts.length !== 3) {
    throw new Error('El token recibido no tiene un formato JWT válido.');
  }

  let payload: JwtPayload;
  try {
    payload = JSON.parse(decodeBase64Url(parts[1])) as JwtPayload;
  } catch {
    throw new Error('No se pudo decodificar el token de acceso.');
  }

  if (!Array.isArray(payload.roles)) {
    throw new Error('El token no contiene roles válidos.');
  }

  const validRoles: Rol[] = [
    'ADMINISTRADOR',
    'RECEPCIONISTA',
    'TECNICO',
    'CLIENTE',
  ];
  const roles = payload.roles.filter(
    (role): role is Rol => typeof role === 'string' && validRoles.includes(role as Rol),
  );

  if (roles.length === 0) {
    throw new Error('El token no contiene roles reconocidos.');
  }

  return roles;
}

export function saveAccessToken(token: string): void {
  localStorage.setItem(ACCESS_TOKEN_KEY, token);
}

export function getAccessToken(): string | null {
  return localStorage.getItem(ACCESS_TOKEN_KEY);
}

export function clearSession(): void {
  localStorage.removeItem(ACCESS_TOKEN_KEY);
  localStorage.removeItem(PASSWORD_CHANGE_KEY);
}

export function saveLoginResponse(response: LoginResponse): Rol[] {
  const roles = decodeRoles(response.token);
  saveAccessToken(response.token);
  localStorage.setItem(PASSWORD_CHANGE_KEY, String(response.requiereCambioClave));
  return roles;
}

export function getPasswordChangeRequired(): boolean {
  return localStorage.getItem(PASSWORD_CHANGE_KEY) === 'true';
}

export function clearPasswordChangeRequired(): void {
  localStorage.removeItem(PASSWORD_CHANGE_KEY);
}

export function saveAuthMessage(message: string): void {
  sessionStorage.setItem(AUTH_MESSAGE_KEY, message);
}

export function consumeAuthMessage(): string | null {
  const message = sessionStorage.getItem(AUTH_MESSAGE_KEY);
  sessionStorage.removeItem(AUTH_MESSAGE_KEY);
  return message;
}
