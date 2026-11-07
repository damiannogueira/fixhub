import type { LoginRequest, LoginResponse } from '../types/auth';
import { buildApiUrl } from './api';

export const authEndpoints = {
  login: buildApiUrl('/auth/login'),
  cambiarClave: buildApiUrl('/auth/cambiar-clave'),
} as const;

export type AuthServiceContract = {
  login(request: LoginRequest): Promise<LoginResponse>;
};
