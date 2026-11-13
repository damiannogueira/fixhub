import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiError, apiRequest } from '../services/api';
import { resetAuthState } from '../utils/authState';

function mockFetch(status: number, body?: unknown) {
  const fn = vi.fn().mockResolvedValue(
    new Response(body === undefined ? null : JSON.stringify(body), { status }),
  );
  vi.stubGlobal('fetch', fn);
  return fn;
}

describe('apiRequest', () => {
  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
    resetAuthState();
    window.location.hash = '#/ordenes';
  });
  afterEach(() => vi.unstubAllGlobals());

  it('envía el token Bearer y devuelve el JSON', async () => {
    localStorage.setItem('access_token', 'tok');
    const fetchMock = mockFetch(200, { ok: true });
    await expect(apiRequest('/x')).resolves.toEqual({ ok: true });
    const headers = fetchMock.mock.calls[0][1].headers as Headers;
    expect(headers.get('Authorization')).toBe('Bearer tok');
  });

  it('devuelve undefined en 204', async () => {
    mockFetch(204);
    await expect(apiRequest('/x')).resolves.toBeUndefined();
  });

  it('usa el detail del backend como mensaje de error', async () => {
    mockFetch(409, { detail: 'Stock insuficiente', errores: { 'items[0].cantidad': 'debe ser mayor a 0' } });
    await expect(apiRequest('/x')).rejects.toMatchObject({
      status: 409,
      message: 'Stock insuficiente. Revisá cantidad: debe ser mayor a 0.',
    });
  });

  it('usa mensajes de fallback según el status', async () => {
    mockFetch(404, {});
    await expect(apiRequest('/x')).rejects.toThrow('No se encontró la información solicitada.');
    mockFetch(500, { detail: 'Internal Server Error' });
    await expect(apiRequest('/x')).rejects.toThrow('Ocurrió un problema en el servidor.');
  });

  it('401 con sesión activa limpia el token y emite session-expired', async () => {
    localStorage.setItem('access_token', 'tok');
    const listener = vi.fn();
    window.addEventListener('fixhub:session-expired', listener);
    mockFetch(401, {});
    await expect(apiRequest('/x')).rejects.toBeInstanceOf(ApiError);
    expect(localStorage.getItem('access_token')).toBeNull();
    expect(listener).toHaveBeenCalledOnce();
    window.removeEventListener('fixhub:session-expired', listener);
  });

  it('error de red se traduce a ApiError con status 0', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('fail')));
    await expect(apiRequest('/x')).rejects.toMatchObject({ status: 0 });
  });
});
