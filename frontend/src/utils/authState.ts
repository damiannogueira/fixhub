import type { LoginResponse, Rol } from '../types/auth';
import {
  clearPasswordChangeRequired,
  clearSession,
  decodeRoles,
  getAccessToken,
  getPasswordChangeRequired,
  saveLoginResponse,
} from './authStorage';

type AuthState = {
  response: LoginResponse | null;
  roles: Rol[];
};

const initialToken = getAccessToken();
let initialRoles: Rol[] = [];

if (initialToken) {
  try {
    initialRoles = decodeRoles(initialToken);
  } catch {
    initialRoles = [];
    clearSession();
  }
}

let state: AuthState = {
  response: null,
  roles: initialRoles,
};

export function setAuthenticated(response: LoginResponse): void {
  state = {
    response,
    roles: saveLoginResponse(response),
  };
}

function syncSession(): void {
  const token = getAccessToken();
  if (!token) {
    resetAuthState();
    return;
  }
  try {
    const roles = decodeRoles(token);
    if (state.response?.token !== token) state = { response: null, roles };
  } catch {
    clearSession();
    resetAuthState();
  }
}

export function getAuthState(): Readonly<AuthState> {
  syncSession();
  return state;
}

export function hasAnyRole(roles: readonly Rol[]): boolean {
  syncSession();
  return roles.some((role) => state.roles.includes(role));
}

export function hasAuthenticatedSession(): boolean {
  syncSession();
  return state.roles.length > 0;
}

export function requiresPasswordChange(): boolean {
  syncSession();
  return getPasswordChangeRequired();
}

export function markPasswordChanged(): void {
  clearPasswordChangeRequired();
  if (state.response) {
    state.response = {
      ...state.response,
      requiereCambioClave: false,
    };
  }
}

export function resetAuthState(): void {
  state = { response: null, roles: [] };
}
