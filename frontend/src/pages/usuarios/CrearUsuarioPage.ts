import { renderAppLayout } from '../../components/layout/AppLayout';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { crearUsuario, crearUsuarioConCliente } from '../../services/usuariosService';
import type { Rol } from '../../types/auth';
import { router } from '../../router/router';
import { hasAnyRole } from '../../utils/authState';
import { NotFoundPage } from '../auth/NotFoundPage';

const roles: Rol[] = [
  'ADMINISTRADOR',
  'RECEPCIONISTA',
  'TECNICO',
  'CLIENTE',
];

export const CrearUsuarioPage = {
  render(): HTMLElement {
    const query = new URLSearchParams(window.location.hash.split('?')[1] ?? '');
    const clientOnly = query.get('rol') === 'CLIENTE';
    const returnPath = query.get('origen') === 'clientes' ? '/clientes' : '/usuarios';
    const receptionist = hasAnyRole(['RECEPCIONISTA']) && !hasAnyRole(['ADMINISTRADOR']);
    if (receptionist && !clientOnly) {
      return NotFoundPage.render();
    }
    const content = document.createElement('div');
    content.innerHTML = `
      <div class="page-heading">
        <div>
          <p class="muted">Accesos y permisos</p>
          <h2>${clientOnly ? 'Crear cliente' : 'Crear usuario'}</h2>
        </div>
        <a class="btn btn-secondary" href="#${returnPath}">Volver</a>
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
        ${roles.map((role) => `<option value="${role}" ${clientOnly && role === 'CLIENTE' ? 'selected' : ''}>${formatRole(role)}</option>`).join('')}
      </select>
    `;
    form.append(roleGroup);

    const clientFields = document.createElement('div');
    clientFields.className = 'client-fields';
    clientFields.hidden = true;
    clientFields.innerHTML = `
      <p class="muted form-intro">Datos del cliente</p>
      <div class="form-group"><label class="form-label" for="nombre">Nombre</label><input class="form-input" id="nombre" name="nombre" maxlength="255"></div>
      <div class="form-group"><label class="form-label" for="apellido">Apellido</label><input class="form-input" id="apellido" name="apellido" maxlength="255"></div>
      <div class="form-group"><label class="form-label" for="dni">DNI</label><input class="form-input" id="dni" name="dni" maxlength="255"></div>
      <div class="form-group"><label class="form-label" for="direccion">Dirección</label><input class="form-input" id="direccion" name="direccion" maxlength="255"></div>
    `;
    form.append(clientFields);
    const roleSelect = roleGroup.querySelector('#rol') as HTMLSelectElement;
    if (clientOnly) {
      roleSelect.disabled = true;
      clientFields.hidden = false;
    }
    roleSelect.addEventListener('change', () => {
      clientFields.hidden = roleSelect.value !== 'CLIENTE';
    });

    const actions = document.createElement('div');
    actions.className = 'form-actions';
    actions.append(
      Button.render({ text: 'Cancelar', variant: 'secondary' }),
      Button.render({ text: clientOnly ? 'Crear cliente' : 'Crear usuario', type: 'submit' }),
    );
    actions.querySelector('button')?.addEventListener('click', () => router.navigate(returnPath));
    form.append(actions);

    form.addEventListener('submit', async (event) => {
      event.preventDefault();
      clearMessage(form);

      const values = new FormData(form);
      const email = String(values.get('email') ?? '').trim();
      const telefono = String(values.get('telefono') ?? '').trim();
      const claveTemporal = String(values.get('claveTemporal') ?? '');
      const rol = (clientOnly ? 'CLIENTE' : String(values.get('rol') ?? '')) as Rol;
      const nombre = String(values.get('nombre') ?? '').trim();
      const apellido = String(values.get('apellido') ?? '').trim();

      if ((!email && !telefono) || claveTemporal.length < 8 || !roles.includes(rol)
        || (rol === 'CLIENTE' && (!nombre || !apellido))) {
        showMessage(form, rol === 'CLIENTE'
          ? 'Ingresá email o teléfono, una clave, un rol, nombre y apellido.'
          : 'Ingresá email o teléfono, una clave de al menos 8 caracteres y un rol.');
        return;
      }

      const submit = form.querySelector('button[type="submit"]') as HTMLButtonElement;
      submit.disabled = true;

      try {
        const request = {
          ...(email ? { email } : {}),
          ...(telefono ? { telefono } : {}),
          claveTemporal,
          roles: [rol],
        };
        if (rol === 'CLIENTE') {
          await crearUsuarioConCliente({
            ...request,
            nombre,
            apellido,
            ...(String(values.get('dni') ?? '').trim()
              ? { dni: String(values.get('dni')).trim() } : {}),
            ...(String(values.get('direccion') ?? '').trim()
              ? { direccion: String(values.get('direccion')).trim() } : {}),
          });
        } else {
          await crearUsuario(request);
        }
        router.navigate(returnPath);
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
