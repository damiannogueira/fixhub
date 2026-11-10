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

export function getAuthState(): Readonly<AuthState> {
  return state;
}

export function hasAnyRole(roles: readonly Rol[]): boolean {
  return roles.some((role) => state.roles.includes(role));
}

export function hasAuthenticatedSession(): boolean {
  return state.roles.length > 0;
}

export function requiresPasswordChange(): boolean {
  return state.response?.requiereCambioClave === true || getPasswordChangeRequired();
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
