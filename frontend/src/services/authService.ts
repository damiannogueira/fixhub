import type { LoginRequest, LoginResponse } from '../types/auth';
import { apiRequest, buildApiUrl } from './api';

export const authEndpoints = {
  login: buildApiUrl('/auth/login'),
  cambiarClave: buildApiUrl('/auth/cambiar-clave'),
} as const;

export type AuthServiceContract = {
  login(request: LoginRequest): Promise<LoginResponse>;
};

export function login(request: LoginRequest): Promise<LoginResponse> {
  return apiRequest<LoginResponse>('/auth/login', {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

export type ChangePasswordRequest = {
  claveActual: string;
  claveNueva: string;
  confirmacionClave: string;
};

export function changePassword(request: ChangePasswordRequest): Promise<void> {
  return apiRequest<void>('/auth/cambiar-clave', {
    method: 'POST',
    body: JSON.stringify(request),
  });
}
