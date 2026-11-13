import { makeToken, loginResponse, deferred } from './fixtures/auth';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const { loginMock, navigateMock } = vi.hoisted(() => ({
  loginMock: vi.fn(),
  navigateMock: vi.fn(),
}));

vi.mock('../services/authService', () => ({ login: loginMock }));
vi.mock('../router/router', () => ({ router: { navigate: navigateMock } }));

import { LoginPage } from '../pages/auth/LoginPage';
import { resetAuthState } from '../utils/authState';
import { saveAuthMessage } from '../utils/authStorage';

function tokenFor(roles: string[], requiereCambioClave = false): string {
  return makeToken({ roles, requiereCambioClave });
}

function setup(username: string, password: string) {
  const page = LoginPage.render();
  document.body.replaceChildren(page);
  (page.querySelector('#username') as HTMLInputElement).value = username;
  (page.querySelector('#password') as HTMLInputElement).value = password;
  const form = page.querySelector('form') as HTMLFormElement;
  const submit = form.querySelector('button[type="submit"]') as HTMLButtonElement;
  const send = () => form.dispatchEvent(new Event('submit', { cancelable: true, bubbles: true }));
  return { page, form, submit, send };
}

describe('LoginPage', () => {
  beforeEach(() => {
    loginMock.mockReset();
    navigateMock.mockReset();
    localStorage.clear();
    sessionStorage.clear();
    resetAuthState();
  });

  it('renderiza los campos y el botón de ingreso', () => {
    const { page, submit } = setup('', '');
    expect(page.querySelector('#username')).not.toBeNull();
    expect(page.querySelector('#password')).not.toBeNull();
    expect(submit.textContent).toBe('Ingresar');
  });

  it('muestra el mensaje de sesión expirada guardado', () => {
    saveAuthMessage('Tu sesión expiró. Iniciá sesión nuevamente.');
    const { form } = setup('', '');
    expect(form.querySelector('.form-alert-success')?.textContent).toBe('Tu sesión expiró. Iniciá sesión nuevamente.');
  });

  it('rechaza contraseñas de menos de 8 caracteres sin llamar al backend', () => {
    const { form, send } = setup('ana@fixhub.com', '1234567');
    send();
    expect(loginMock).not.toHaveBeenCalled();
    expect(form.querySelector('.form-alert-error')).not.toBeNull();
  });

  it('rechaza un identificador vacío', () => {
    const { send } = setup('   ', '12345678');
    send();
    expect(loginMock).not.toHaveBeenCalled();
  });

  it('envía el identificador sin espacios y la clave tal cual', async () => {
    loginMock.mockResolvedValue({ token: tokenFor(['ADMINISTRADOR']), requiereCambioClave: false });
    const { send } = setup('  ana@fixhub.com ', 'clave-segura');
    send();
    await vi.waitFor(() => expect(navigateMock).toHaveBeenCalled());
    expect(loginMock).toHaveBeenCalledWith({ identificador: 'ana@fixhub.com', clave: 'clave-segura' });
  });

  it.each([
    [['ADMINISTRADOR'], false, '/dashboard'],
    [['TECNICO'], false, '/dashboard'],
    [['CLIENTE'], false, '/mi-cuenta'],
    [['RECEPCIONISTA', 'TECNICO'], false, '/dashboard'],
    [['ADMINISTRADOR'], true, '/cambiar-clave'],
    [['CLIENTE'], true, '/cambiar-clave'],
  ])('login con roles %j y cambio de clave=%s navega a %s', async (roles, requiereCambioClave, destino) => {
    loginMock.mockResolvedValue({ token: tokenFor(roles, requiereCambioClave), requiereCambioClave });
    const { send } = setup('ana@fixhub.com', 'clave-segura');
    send();
    await vi.waitFor(() => expect(navigateMock).toHaveBeenCalledWith(destino));
    expect(localStorage.getItem('access_token')).toBe(tokenFor(roles, requiereCambioClave));
  });

  it('si el login falla muestra el error y vuelve a habilitar el botón', async () => {
    loginMock.mockRejectedValue(new Error('El identificador o la contraseña son incorrectos.'));
    const { form, submit, send } = setup('ana@fixhub.com', 'clave-segura');
    send();
    expect(submit.disabled).toBe(true);
    await vi.waitFor(() => expect(form.querySelector('.form-alert-error')).not.toBeNull());
    expect(form.querySelector('.form-alert-error')?.textContent).toBe('El identificador o la contraseña son incorrectos.');
    expect(submit.disabled).toBe(false);
    expect(navigateMock).not.toHaveBeenCalled();
  });

  it.each(['inicio', 'otra sesión', 'pantalla reemplazada'])('ignora el login tardío después de %s', async (destination) => {
    const pending = deferred<ReturnType<typeof loginResponse>>();
    loginMock.mockReturnValue(pending.promise);
    const { page, send } = setup('demo@example.test', 'Temporal123');
    send();
    let activeToken: string | null = null;
    if (destination === 'inicio') {
      page.querySelector('a')!.dispatchEvent(new MouseEvent('click', { cancelable: true }));
    } else if (destination === 'otra sesión') {
      activeToken = makeToken({ sub: '2' });
      localStorage.setItem('access_token', activeToken);
    } else {
      document.body.replaceChildren();
    }
    pending.resolve(loginResponse());
    await pending.promise;
    await Promise.resolve();
    expect(localStorage.getItem('access_token')).toBe(activeToken);
    expect(navigateMock).not.toHaveBeenCalled();
  });

  it('no muestra un error de login después de volver al inicio', async () => {
    const pending = deferred<ReturnType<typeof loginResponse>>();
    loginMock.mockReturnValue(pending.promise);
    const { page, send } = setup('demo@example.test', 'Temporal123');
    send();
    page.querySelector('a')!.dispatchEvent(new MouseEvent('click', { cancelable: true }));
    pending.reject(new Error('Credenciales incorrectas'));
    await pending.promise.catch(() => {});
    await Promise.resolve();
    expect(page.querySelector('.form-alert')).toBeNull();
    expect(navigateMock).not.toHaveBeenCalled();
  });
});
