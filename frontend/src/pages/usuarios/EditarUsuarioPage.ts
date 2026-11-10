import { renderAppLayout } from '../../components/layout/AppLayout';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { buscarUsuario, actualizarUsuario } from '../../services/usuariosService';
import { listarClientes } from '../../services/clientesService';
import type { Rol } from '../../types/auth';
import { router } from '../../router/router';

const roles: Rol[] = ['ADMINISTRADOR', 'RECEPCIONISTA', 'TECNICO', 'CLIENTE'];

export const EditarUsuarioPage = {
  render(): HTMLElement {
    const content = document.createElement('div');
    content.innerHTML = `
      <div class="page-heading"><div><p class="muted">Accesos y permisos</p><h2>Editar usuario</h2></div>
      <a class="btn btn-secondary" href="#/usuarios">Volver a usuarios</a></div>
      <section class="panel form-panel"><form id="edit-user-form" class="user-form"><p class="table-state">Cargando usuario...</p></form></section>
    `;
    const form = content.querySelector('#edit-user-form') as HTMLFormElement;
    const id = Number(new URLSearchParams(window.location.hash.split('?')[1] ?? '').get('id'));
    if (!Number.isInteger(id) || id < 1) {
      showMessage(form, 'El usuario seleccionado no es válido.');
      return renderAppLayout(content, 'Usuarios');
    }

    void loadUser(form, id);
    return renderAppLayout(content, 'Usuarios');
  },
};

async function loadUser(form: HTMLFormElement, id: number): Promise<void> {
  try {
    const [user, clients] = await Promise.all([buscarUsuario(id), listarClientes()]);
    const linkedClient = clients.some((client) => client.usuarioId === id);
    form.replaceChildren(
      Input.render({ label: 'Correo electrónico', type: 'email', id: 'email', value: user.email ?? '' }),
      Input.render({ label: 'Teléfono', type: 'tel', id: 'telefono', value: user.telefono ?? '' }),
      Input.render({ label: 'Nueva clave temporal (opcional)', type: 'password', id: 'claveTemporal', placeholder: 'Dejar vacío para conservarla' }),
    );

    const roleGroup = document.createElement('fieldset');
    roleGroup.className = 'form-group role-options';
    roleGroup.innerHTML = `<legend class="form-label">Roles</legend>${roles.map((role) =>
      `<label><input type="checkbox" name="roles" value="${role}" ${user.roles.includes(role) || (linkedClient && role === 'CLIENTE') ? 'checked' : ''} ${linkedClient && role === 'CLIENTE' ? 'disabled' : ''}> ${formatRole(role)}</label>`,
    ).join('')}`;
    form.append(roleGroup);
    if (linkedClient) {
      const notice = document.createElement('p');
      notice.className = 'muted';
      notice.textContent = 'Esta cuenta está vinculada a un Cliente: debe conservar CLIENTE y puede acumular otros roles.';
      form.append(notice);
    }

    const activeGroup = document.createElement('label');
    activeGroup.className = 'form-label';
    activeGroup.innerHTML = `<input type="checkbox" id="activo" ${user.activo ? 'checked' : ''}> Usuario activo`;
    form.append(activeGroup);

    const actions = document.createElement('div');
    actions.className = 'form-actions';
    actions.append(Button.render({ text: 'Cancelar', variant: 'secondary' }), Button.render({ text: 'Guardar cambios', type: 'submit' }));
    actions.querySelector('button')?.addEventListener('click', () => router.navigate('/usuarios'));
    form.append(actions);

    form.addEventListener('submit', async (event) => {
      event.preventDefault();
      clearMessage(form);
      const values = new FormData(form);
      const email = String(values.get('email') ?? '').trim();
      const telefono = String(values.get('telefono') ?? '').trim();
      const selectedRoles = values.getAll('roles') as Rol[];
      if (linkedClient && !selectedRoles.includes('CLIENTE')) selectedRoles.push('CLIENTE');
      if ((!email && !telefono) || selectedRoles.length === 0) {
        showMessage(form, 'Ingresá email o teléfono y al menos un rol.');
        return;
      }
      const submit = form.querySelector('button[type="submit"]') as HTMLButtonElement;
      submit.disabled = true;
      try {
        await actualizarUsuario(id, {
          ...(email ? { email } : {}),
          ...(telefono ? { telefono } : {}),
          ...(String(values.get('claveTemporal') ?? '').trim()
            ? { claveTemporal: String(values.get('claveTemporal')).trim() } : {}),
          roles: selectedRoles,
          activo: (form.querySelector('#activo') as HTMLInputElement).checked,
        });
        router.navigate('/usuarios');
      } catch (error) {
        showMessage(form, error instanceof Error ? error.message : 'No se pudo actualizar el usuario.');
        submit.disabled = false;
      }
    });
  } catch (error) {
    showMessage(form, error instanceof Error ? error.message : 'No se pudo cargar el usuario.');
  }
}

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
