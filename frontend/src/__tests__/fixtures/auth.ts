import type { LoginResponse, Rol } from '../../types/auth';

const issuedAt = Math.floor(Date.now() / 1000);

export function makeToken(claims: Record<string, unknown> = {}): string {
  const encode = (value: unknown) => btoa(JSON.stringify(value))
    .replaceAll('+', '-').replaceAll('/', '_').replaceAll('=', '');
  return `${encode({ alg: 'HS256', typ: 'JWT' })}.${encode({
    iss: 'https://fixhub.local', sub: '1', identificador: 'demo@example.test',
    iat: issuedAt, exp: issuedAt + 3600,
    roles: ['ADMINISTRADOR'], requiereCambioClave: false, ...claims,
  })}.firma-simulada`;
}

export function loginResponse(roles: Rol[] = ['ADMINISTRADOR'], requiereCambioClave = false): LoginResponse {
  return {
    usuarioId: 1, identificador: 'demo@example.test',
    token: makeToken({ roles, requiereCambioClave }), tipo: 'Bearer',
    expiraEnSegundos: 3600, roles, requiereCambioClave,
  };
}

export function deferred<T>() {
  let resolve!: (value: T) => void;
  let reject!: (reason: unknown) => void;
  const promise = new Promise<T>((res, rej) => { resolve = res; reject = rej; });
  return { promise, resolve, reject };
}
