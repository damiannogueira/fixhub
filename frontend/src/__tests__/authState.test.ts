import { makeToken, loginResponse } from './fixtures/auth';
import type { Rol } from '../types/auth';
import { beforeEach, describe, expect, it } from 'vitest';
import {
  getAuthState,
  hasAnyRole,
  hasAuthenticatedSession,
  markPasswordChanged,
  requiresPasswordChange,
  resetAuthState,
  setAuthenticated,
} from '../utils/authState';
import { isHybridTechnician, isTechnicianOnly } from '../utils/technicianView';

function login(roles: Rol[], requiereCambioClave = false): void {
  setAuthenticated(loginResponse(roles, requiereCambioClave));
}

describe('authState', () => {
  beforeEach(() => {
    localStorage.clear();
    resetAuthState();
  });

  it('sin login no hay sesión', () => {
    expect(hasAuthenticatedSession()).toBe(false);
    expect(hasAnyRole(['ADMINISTRADOR'])).toBe(false);
  });

  it('tras setAuthenticated expone los roles', () => {
    login(['RECEPCIONISTA']);
    expect(hasAuthenticatedSession()).toBe(true);
    expect(hasAnyRole(['RECEPCIONISTA'])).toBe(true);
    expect(hasAnyRole(['ADMINISTRADOR'])).toBe(false);
    expect(getAuthState().roles).toEqual(['RECEPCIONISTA']);
  });

  it('el indicador del JWT mantiene el cambio obligatorio aunque se limpie el auxiliar', () => {
    login(['CLIENTE'], true);
    expect(requiresPasswordChange()).toBe(true);
    markPasswordChanged();
    expect(requiresPasswordChange()).toBe(true);
  });

  it('descarta una sesion vencida aunque conserve roles en memoria', () => {
    login(['ADMINISTRADOR']);
    localStorage.setItem('access_token', makeToken({ exp: 1 }));
    expect(hasAuthenticatedSession()).toBe(false);
    expect(localStorage.getItem('access_token')).toBeNull();
    expect(requiresPasswordChange()).toBe(false);
  });

  it('sincroniza el cierre y el reemplazo de token desde otra pestana', () => {
    login(['ADMINISTRADOR']);
    localStorage.removeItem('access_token');
    expect(hasAnyRole(['ADMINISTRADOR'])).toBe(false);
    localStorage.setItem('access_token', makeToken({ roles: ['CLIENTE'], requiereCambioClave: false }));
    localStorage.setItem('fixhub.requires_password_change', 'true');
    expect(hasAnyRole(['CLIENTE'])).toBe(true);
    expect(requiresPasswordChange()).toBe(false);
  });

  it('restaura la obligacion del JWT aunque falte la bandera local', () => {
    localStorage.setItem('access_token', makeToken({ roles: ['CLIENTE'], requiereCambioClave: true }));
    expect(requiresPasswordChange()).toBe(true);
  });

  it('distingue técnico puro de técnico híbrido', () => {
    login(['TECNICO']);
    expect(isTechnicianOnly()).toBe(true);
    expect(isHybridTechnician()).toBe(false);
    login(['TECNICO', 'ADMINISTRADOR']);
    expect(isTechnicianOnly()).toBe(false);
    expect(isHybridTechnician()).toBe(true);
  });
});
