import { loginResponse, deferred } from './fixtures/auth';
import { beforeEach, expect, it, vi } from 'vitest';
import { LoginPage } from '../pages/auth/LoginPage';
import { ChangePasswordPage } from '../pages/auth/ChangePasswordPage';
import { renderAppLayout } from '../components/layout/AppLayout';
import { hasAuthenticatedSession, resetAuthState, setAuthenticated } from '../utils/authState';
import { changePassword } from '../services/authService';

vi.mock('../services/authService', () => ({ login: vi.fn(), changePassword: vi.fn() }));

beforeEach(() => {
  localStorage.clear();
  sessionStorage.clear();
  resetAuthState();
  window.history.replaceState(null, '', '#/cambiar-clave');
  vi.clearAllMocks();
});

function authenticate(): void {
  setAuthenticated(loginResponse(['ADMINISTRADOR'], true));
}

it('login ofrece un enlace visible al inicio', () => {
  const link = LoginPage.render().querySelector('a[href="#/"]');
  expect(link?.textContent).toContain('Volver al inicio');
});

it.each(['layout', 'cambio'])('salir desde %s limpia la sesion y vuelve a la landing', (source) => {
  authenticate();
  const page = source === 'layout'
    ? renderAppLayout(document.createElement('div'), 'Prueba')
    : ChangePasswordPage.render();
  const button = source === 'layout'
    ? page.querySelector('.logout-button')
    : Array.from(page.querySelectorAll('button')).find((item) => item.textContent?.includes('Cerrar'));
  (button as HTMLButtonElement).click();
  expect(window.location.hash).toBe('#/');
  expect(hasAuthenticatedSession()).toBe(false);
  expect(localStorage.getItem('access_token')).toBeNull();
  expect(localStorage.getItem('fixhub.requires_password_change')).toBeNull();
});

it('cambiar la clave cierra la sesion y pide un nuevo login', async () => {
  authenticate();
  vi.mocked(changePassword).mockResolvedValue(undefined);
  const page = ChangePasswordPage.render();
  document.body.replaceChildren(page);
  const inputs = page.querySelectorAll('input');
  inputs[0].value = 'Temporal123';
  inputs[1].value = inputs[2].value = 'Definitiva456';
  page.querySelector('form')!.dispatchEvent(new Event('submit', { cancelable: true }));
  await vi.waitFor(() => expect(window.location.hash).toBe('#/login'));
  expect(changePassword).toHaveBeenCalledWith({ claveActual: 'Temporal123', claveNueva: 'Definitiva456', confirmacionClave: 'Definitiva456' });
  expect(hasAuthenticatedSession()).toBe(false);
  expect(localStorage.getItem('fixhub.requires_password_change')).toBeNull();
});

it.each(['salir', 'otra sesión', 'pantalla reemplazada'])('ignora el cambio tardío después de %s', async (destination) => {
  authenticate();
  const pending = deferred<void>();
  vi.mocked(changePassword).mockReturnValue(pending.promise);
  const page = ChangePasswordPage.render();
  document.body.replaceChildren(page);
  window.history.replaceState(null, '', '#/cambiar-clave');
  const inputs = page.querySelectorAll('input');
  inputs[0].value = 'Temporal123';
  inputs[1].value = inputs[2].value = 'Definitiva456';
  page.querySelector('form')!.dispatchEvent(new Event('submit', { cancelable: true }));
  let expectedToken = localStorage.getItem('access_token');
  if (destination === 'salir') {
    (Array.from(page.querySelectorAll('button')).find((item) => item.textContent?.includes('Cerrar'))!).click();
    expectedToken = null;
  } else if (destination === 'otra sesión') {
    const response = loginResponse(['TECNICO']);
    setAuthenticated(response);
    expectedToken = response.token;
  } else {
    document.body.replaceChildren();
  }
  const expectedHash = window.location.hash;
  pending.resolve();
  await pending.promise;
  await Promise.resolve();
  expect(localStorage.getItem('access_token')).toBe(expectedToken);
  expect(window.location.hash).toBe(expectedHash);
  expect(sessionStorage.getItem('fixhub.auth_message')).toBeNull();
});

it('no muestra un error tardío después de salir del cambio de contraseña', async () => {
  authenticate();
  const pending = deferred<void>();
  vi.mocked(changePassword).mockReturnValue(pending.promise);
  const page = ChangePasswordPage.render();
  document.body.replaceChildren(page);
  const inputs = page.querySelectorAll('input');
  inputs[1].value = inputs[2].value = 'Definitiva456';
  page.querySelector('form')!.dispatchEvent(new Event('submit', { cancelable: true }));
  (Array.from(page.querySelectorAll('button')).find((item) => item.textContent?.includes('Cerrar'))!).click();
  pending.reject(new Error('Error anterior'));
  await pending.promise.catch(() => {});
  expect(page.querySelector('.form-alert')).toBeNull();
  expect(window.location.hash).toBe('#/');
});
