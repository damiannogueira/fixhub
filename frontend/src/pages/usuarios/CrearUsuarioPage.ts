import { renderAppLayout } from '../../components/layout/AppLayout';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { crearUsuario } from '../../services/usuariosService';
import type { Rol } from '../../types/auth';
import { router } from '../../router/router';

const roles: Rol[] = [
  'ADMINISTRADOR',
  'RECEPCIONISTA',
  'TECNICO',
  'CLIENTE',
];

export const CrearUsuarioPage = {
  render(): HTMLElement {
    const content = document.createElement('div');
    content.innerHTML = `
      <div class="page-heading">
        <div>
          <p class="muted">Accesos y permisos</p>
          <h2>Crear usuario</h2>
        </div>
        <a class="btn btn-secondary" href="#/usuarios">Volver a usuarios</a>
      </div>
      <section class="panel form-panel">
        <p class="muted form-intro">Creá una cuenta y asignale un rol inicial.</p>
        <form id="create-user-form" class="user-form"></form>
      </section>
    `;

    const form = content.querySelector('#create-user-form') as HTMLFormElement;
    form.append(
      Input.render({
        label: 'Correo electrónico',
        type: 'email',
        id: 'email',
        placeholder: 'usuario@fixhub.com',
      }),
      Input.render({
        label: 'Teléfono',
        type: 'tel',
        id: 'telefono',
        placeholder: '1122334455',
      }),
      Input.render({
        label: 'Clave temporal',
        type: 'password',
        id: 'claveTemporal',
        placeholder: 'Mínimo 8 caracteres',
        required: true,
      }),
    );

    const roleGroup = document.createElement('div');
    roleGroup.className = 'form-group';
    roleGroup.innerHTML = `
      <label class="form-label" for="rol">Rol</label>
      <select id="rol" name="rol" class="form-input" required>
        <option value="">Seleccioná un rol</option>
        ${roles.map((role) => `<option value="${role}">${formatRole(role)}</option>`).join('')}
      </select>
    `;
    form.append(roleGroup);

    const actions = document.createElement('div');
    actions.className = 'form-actions';
    actions.append(
      Button.render({ text: 'Cancelar', variant: 'secondary' }),
      Button.render({ text: 'Crear usuario', type: 'submit' }),
    );
    actions.querySelector('button')?.addEventListener('click', () => router.navigate('/usuarios'));
    form.append(actions);

    form.addEventListener('submit', async (event) => {
      event.preventDefault();
      clearMessage(form);

      const values = new FormData(form);
      const email = String(values.get('email') ?? '').trim();
      const telefono = String(values.get('telefono') ?? '').trim();
      const claveTemporal = String(values.get('claveTemporal') ?? '');
      const rol = String(values.get('rol') ?? '') as Rol;

      if ((!email && !telefono) || claveTemporal.length < 8 || !roles.includes(rol)) {
        showMessage(form, 'Ingresá email o teléfono, una clave de al menos 8 caracteres y un rol.');
        return;
      }

      const submit = form.querySelector('button[type="submit"]') as HTMLButtonElement;
      submit.disabled = true;

      try {
        await crearUsuario({
          ...(email ? { email } : {}),
          ...(telefono ? { telefono } : {}),
          claveTemporal,
          roles: [rol],
        });
        router.navigate('/usuarios');
      } catch (error) {
        showMessage(form, error instanceof Error ? error.message : 'No se pudo crear el usuario.');
        submit.disabled = false;
      }
    });

    return renderAppLayout(content, 'Usuarios');
  },
};

function formatRole(role: Rol): string {
  return role.charAt(0) + role.slice(1).toLowerCase();
}

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
