import { beforeEach, describe, expect, it } from 'vitest';
import {
  clearSession,
  consumeAuthMessage,
  decodeRoles,
  getAccessToken,
  getPasswordChangeRequired,
  saveAuthMessage,
  saveLoginResponse,
} from '../utils/authStorage';

import { makeToken } from './fixtures/auth';

describe('authStorage', () => {
  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
  });

  it('decodeRoles extrae los roles válidos del JWT', () => {
    expect(decodeRoles(makeToken({ roles: ['TECNICO', 'OTRO'] }))).toEqual(['TECNICO']);
  });

  it('decodeRoles rechaza tokens mal formados o sin roles', () => {
    expect(() => decodeRoles('abc')).toThrow();
    expect(() => decodeRoles(makeToken({ roles: undefined }))).toThrow();
    expect(() => decodeRoles(makeToken({ roles: ['X'] }))).toThrow();
  });

  it('saveLoginResponse guarda token y bandera de cambio de clave', () => {
    const token = makeToken({ roles: ['ADMINISTRADOR'], requiereCambioClave: true });
    const roles = saveLoginResponse({ token, requiereCambioClave: true } as never);
    expect(roles).toEqual(['ADMINISTRADOR']);
    expect(getAccessToken()).toBe(token);
    expect(getPasswordChangeRequired()).toBe(true);
  });

  it('clearSession borra token y bandera', () => {
    saveLoginResponse({ token: makeToken({ roles: ['CLIENTE'] }), requiereCambioClave: true } as never);
    clearSession();
    expect(getAccessToken()).toBeNull();
    expect(getPasswordChangeRequired()).toBe(false);
  });

  it('consumeAuthMessage devuelve el mensaje una sola vez', () => {
    saveAuthMessage('hola');
    expect(consumeAuthMessage()).toBe('hola');
    expect(consumeAuthMessage()).toBeNull();
  });

  it.each([undefined, null, '3600', 1])('rechaza vencimiento ausente o inválido: %s', (exp) => {
    expect(() => decodeRoles(makeToken({ exp }))).toThrow();
  });
});
