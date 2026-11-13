import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import { Input } from '../../components/ui/Input';
import { changePassword } from '../../services/authService';
import { router } from '../../router/router';
import { resetAuthState } from '../../utils/authState';
import { clearSession, getAccessToken, saveAuthMessage } from '../../utils/authStorage';

export const ChangePasswordPage = {
  render(): HTMLElement {
    const wrapper = document.createElement('div');
    wrapper.className = 'login-wrapper';
    let exited = false;
    const card = Card.render(`
      <div class="login-header">
        <h1>Cambiar contraseña</h1>
        <p class="login-subtitle">Por seguridad, reemplazá tu credencial temporal. Si ya la cambiaste en otro navegador, cerrá sesión e ingresá con la nueva contraseña.</p>
      </div>
      <form id="change-password-form" class="login-form"></form>
    `);
    const form = card.querySelector('#change-password-form') as HTMLFormElement;
    form.append(
      Input.render({ label: 'Contraseña actual', type: 'password', id: 'current-password', required: true }),
      Input.render({ label: 'Nueva contraseña', type: 'password', id: 'new-password', required: true }),
      Input.render({ label: 'Confirmar nueva contraseña', type: 'password', id: 'confirm-password', required: true }),
      Button.render({ text: 'Guardar contraseña', type: 'submit', className: 'btn-full' }),
    );

    form.addEventListener('submit', async (event) => {
      event.preventDefault();
      const submit = form.querySelector('button[type="submit"]') as HTMLButtonElement;
      if (submit.disabled) return;
      const values = new FormData(form);
      const currentPassword = String(values.get('current-password') ?? '');
      const newPassword = String(values.get('new-password') ?? '');
      const confirmation = String(values.get('confirm-password') ?? '');

      if (newPassword !== confirmation) {
        showMessage(form, 'La nueva contraseña y su confirmación no coinciden.');
        return;
      }

      submit.disabled = true;
      const requestToken = getAccessToken();
      const requestHash = window.location.hash;
      const isCurrent = () => !exited && wrapper.isConnected
        && getAccessToken() === requestToken && window.location.hash === requestHash;
      clearMessage(form);
      try {
        await changePassword({
          claveActual: currentPassword,
          claveNueva: newPassword,
          confirmacionClave: confirmation,
        });
        if (!isCurrent()) return;
        clearSession();
        resetAuthState();
        saveAuthMessage('Contraseña actualizada correctamente. Iniciá sesión nuevamente.');
        router.navigate('/login');
      } catch (error) {
        if (!isCurrent()) return;
        showMessage(form, error instanceof Error ? error.message : 'No se pudo cambiar la contraseña.');
        submit.disabled = false;
      }
    });

    const exit = Button.render({ text: 'Cerrar sesión y volver al inicio', variant: 'secondary', className: 'btn-full' });
    exit.addEventListener('click', () => {
      exited = true;
      clearSession();
      resetAuthState();
      router.navigate('/');
    });
    card.appendChild(exit);
    wrapper.appendChild(card);
    return wrapper;
  },
};

function showMessage(form: HTMLFormElement, message: string): void {
  clearMessage(form);
  const alert = document.createElement('p');
  alert.className = 'form-alert';
  alert.textContent = message;
  form.prepend(alert);
}

function clearMessage(form: HTMLFormElement): void {
  form.querySelector('.form-alert')?.remove();
}
