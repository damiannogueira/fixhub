import { makeToken, deferred } from './fixtures/auth';
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
    localStorage.setItem('access_token', makeToken());
    const fetchMock = mockFetch(200, { ok: true });
    await expect(apiRequest('/x')).resolves.toEqual({ ok: true });
    const headers = fetchMock.mock.calls[0][1].headers as Headers;
    expect(headers.get('Authorization')).toBe(`Bearer ${makeToken()}`);
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
    localStorage.setItem('access_token', makeToken());
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

  it.each(['nueva', 'cerrada'])('un 401 tardío no invalida una sesión %s', async (session) => {
    const pending = deferred<Response>();
    const listener = vi.fn();
    window.addEventListener('fixhub:session-expired', listener);
    localStorage.setItem('access_token', makeToken());
    vi.stubGlobal('fetch', vi.fn(() => pending.promise));
    const result = apiRequest('/x').catch((error: unknown) => error);
    const newToken = session === 'nueva' ? makeToken({ sub: '2' }) : null;
    if (newToken) localStorage.setItem('access_token', newToken);
    else localStorage.removeItem('access_token');
    pending.resolve(new Response(null, { status: 401 }));
    expect(await result).toBeInstanceOf(ApiError);
    expect(localStorage.getItem('access_token')).toBe(newToken);
    expect(sessionStorage.getItem('fixhub.auth_message')).toBeNull();
    expect(listener).not.toHaveBeenCalled();
    window.removeEventListener('fixhub:session-expired', listener);
  });

  it.each([1, undefined, 'mañana'])('no envía un JWT con exp inválido: %s', async (exp) => {
    localStorage.setItem('access_token', makeToken({ exp }));
    const fetchMock = mockFetch(200, {});
    await expect(apiRequest('/x')).rejects.toMatchObject({ status: 401 });
    expect(fetchMock).not.toHaveBeenCalled();
    expect(localStorage.getItem('access_token')).toBeNull();
  });

  it('login no envía el token anterior ni invalida esa sesión ante credenciales incorrectas', async () => {
    const token = makeToken();
    localStorage.setItem('access_token', token);
    const fetchMock = mockFetch(401, {});
    await expect(apiRequest('/auth/login', { method: 'POST' })).rejects.toMatchObject({ status: 401 });
    expect((fetchMock.mock.calls[0][1].headers as Headers).has('Authorization')).toBe(false);
    expect(localStorage.getItem('access_token')).toBe(token);
  });

  it('descarta el token que venció después de abrir la pantalla', async () => {
    const exp = Math.floor(Date.now() / 1000) + 60;
    localStorage.setItem('access_token', makeToken({ exp }));
    const fetchMock = mockFetch(200, {});
    const clock = vi.spyOn(Date, 'now').mockReturnValue(exp * 1000);
    try {
      await expect(apiRequest('/x')).rejects.toMatchObject({ status: 401 });
      expect(fetchMock).not.toHaveBeenCalled();
    } finally {
      clock.mockRestore();
    }
  });
});
