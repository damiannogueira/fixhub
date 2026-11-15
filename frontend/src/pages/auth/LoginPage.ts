import { Card } from '../../components/ui/Card';
import { Input } from '../../components/ui/Input';
import { Button } from '../../components/ui/Button';
import { router } from '../../router/router';
import { login } from '../../services/authService';
import { hasAnyRole, setAuthenticated } from '../../utils/authState';
import { consumeAuthMessage, getAccessToken } from '../../utils/authStorage';
import logoUrl from '../../assets/img/Logo tecnológico FixHub con soporte central.png';

export const LoginPage = {
  render(): HTMLElement {
    const wrapper = document.createElement('div');
    wrapper.className = 'login-wrapper';

    const cardContent = `
      <div class="login-header">
        <img src="${logoUrl}" alt="FixHub Logo" class="login-logo" />
        <p class="login-subtitle">Tu Plataforma de Soporte Central</p>
      </div>
      <form id="login-form" class="login-form"></form>
      <a href="#/" class="btn btn-secondary btn-full">Volver al inicio</a>
    `;
    
    const card = Card.render(cardContent);
    let exited = false;
    card.querySelector('a[href="#/"]')?.addEventListener('click', () => { exited = true; });
    const form = card.querySelector('#login-form') as HTMLFormElement;
    const authMessage = consumeAuthMessage();
    if (authMessage) {
      showMessage(form, authMessage, 'success');
    }

    form.appendChild(Input.render({ 
      label: 'Email o teléfono',
      type: 'text',
      id: 'username',
      placeholder: 'ejemplo@fixhub.com o 1122334455',
      required: true 
    }));
    
    form.appendChild(Input.render({ 
      label: 'Contraseña', 
      type: 'password', 
      id: 'password', 
      placeholder: '••••••••', 
      required: true 
    }));

    const submitBtn = Button.render({ 
      text: 'Ingresar', 
      type: 'submit', 
      variant: 'primary', 
      className: 'btn-full' 
    });
    form.appendChild(submitBtn);

    form.addEventListener('submit', (e) => {
      e.preventDefault();
      const formData = new FormData(form);
      const username = String(formData.get('username') ?? '').trim();
      const password = String(formData.get('password') ?? '');
      const submit = form.querySelector('button[type="submit"]') as HTMLButtonElement;
      if (submit.disabled) return;
      clearMessage(form);

      if (!username || password.length < 8) {
        showMessage(form, 'Ingresá un identificador y una contraseña de al menos 8 caracteres.');
        return;
      }

      submit.disabled = true;
      const requestToken = getAccessToken();
      const requestHash = window.location.hash;
      const isCurrent = () => !exited && wrapper.isConnected
        && window.location.hash === requestHash && getAccessToken() === requestToken;

      login({ identificador: username, clave: password })
        .then((response) => {
          if (!isCurrent()) return;
          setAuthenticated(response);
          const isClientOnly = hasAnyRole(['CLIENTE'])
            && !hasAnyRole(['ADMINISTRADOR', 'RECEPCIONISTA', 'TECNICO']);
          router.navigate(response.requiereCambioClave
            ? '/cambiar-clave'
            : isClientOnly ? '/mi-cuenta' : '/dashboard');
        })
        .catch((error: unknown) => {
          if (!isCurrent()) return;
          showMessage(form, error instanceof Error ? error.message : 'No se pudo iniciar sesión.');
          submit.disabled = false;
        });
    });

    wrapper.appendChild(card);
    return wrapper;
  },
};

function showMessage(
  form: HTMLFormElement,
  message: string,
  tone: 'error' | 'success' = 'error',
): void {
  const alert = document.createElement('p');
  alert.className = `form-alert form-alert-${tone}`;
  alert.textContent = message;
  form.prepend(alert);
}

function clearMessage(form: HTMLFormElement): void {
  form.querySelector('.form-alert')?.remove();
}
