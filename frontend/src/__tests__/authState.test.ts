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

function login(roles: string[], requiereCambioClave = false): void {
  const payload = btoa(JSON.stringify({ roles })).replaceAll('=', '');
  setAuthenticated({ token: `h.${payload}.s`, requiereCambioClave } as never);
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

  it('requiresPasswordChange se limpia con markPasswordChanged', () => {
    login(['CLIENTE'], true);
    expect(requiresPasswordChange()).toBe(true);
    markPasswordChanged();
    expect(requiresPasswordChange()).toBe(false);
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
