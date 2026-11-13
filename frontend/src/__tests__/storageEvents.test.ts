import { beforeAll, beforeEach, expect, it, vi } from 'vitest';
import { makeToken } from './fixtures/auth';
import { hasAnyRole, hasAuthenticatedSession } from '../utils/authState';

const { render } = vi.hoisted(() => ({ render: vi.fn(() => {
  const form = document.createElement('form');
  form.innerHTML = '<input name="borrador">';
  return form;
}) }));

vi.mock('../router/router', () => ({ router: {
  renderCurrentRoute: render,
  navigate: (path: string) => { window.location.hash = path; },
} }));

beforeAll(async () => { await import('../main'); });

beforeEach(() => {
  localStorage.clear();
  sessionStorage.clear();
  window.history.replaceState(null, '', '#/clientes');
  document.body.innerHTML = '<div id="app"></div>';
  localStorage.setItem('access_token', makeToken());
  render.mockClear();
  document.dispatchEvent(new Event('DOMContentLoaded'));
});

function storage(key: string | null, area: Storage = localStorage): void {
  window.dispatchEvent(new StorageEvent('storage', { key, storageArea: area }));
}

it('no pierde un formulario al cambiar el indicador auxiliar con el mismo JWT', () => {
  const input = document.querySelector('input')!;
  input.value = 'borrador sin guardar';
  localStorage.setItem('fixhub.requires_password_change', 'true');
  storage('fixhub.requires_password_change');
  storage('access_token');
  expect(render).toHaveBeenCalledOnce();
  expect(document.querySelector('input')).toBe(input);
  expect(input.value).toBe('borrador sin guardar');
});

it('un token nuevo renderiza una sola vez aunque lleguen eventos de ambas claves', () => {
  localStorage.setItem('access_token', makeToken({ sub: '2', roles: ['TECNICO'] }));
  storage('access_token');
  localStorage.setItem('fixhub.requires_password_change', 'false');
  storage('fixhub.requires_password_change');
  window.dispatchEvent(new Event('hashchange'));
  expect(render).toHaveBeenCalledTimes(2);
  expect(hasAnyRole(['TECNICO'])).toBe(true);
  expect(hasAnyRole(['ADMINISTRADOR'])).toBe(false);
});

it('un cierre remoto se refleja una sola vez', () => {
  localStorage.removeItem('access_token');
  storage('access_token');
  localStorage.removeItem('fixhub.requires_password_change');
  storage('fixhub.requires_password_change');
  expect(render).toHaveBeenCalledTimes(2);
  expect(hasAuthenticatedSession()).toBe(false);
});

it('ignora cambios de otras claves y de sessionStorage', () => {
  storage('preferencia');
  storage(null, sessionStorage);
  expect(render).toHaveBeenCalledOnce();
});

it('refleja localStorage.clear de otra pestaña', () => {
  localStorage.clear();
  storage(null);
  expect(render).toHaveBeenCalledTimes(2);
});
