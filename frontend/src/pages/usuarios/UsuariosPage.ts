import { renderAppLayout } from '../../components/layout/AppLayout';
import { renderBadge } from '../../components/ui/Badge';
import { renderTable } from '../../components/ui/Table';
import { router } from '../../router/router';
import { listarUsuarios } from '../../services/usuariosService';
import type { Usuario } from '../../types/usuario';

export const UsuariosPage = {
  render(): HTMLElement {
    const content = document.createElement('div');
    content.innerHTML = `<div class="page-heading"><div><p class="muted">Accesos y permisos</p><h2>Usuarios</h2></div><button class="btn btn-primary" id="new-user">+ Nuevo usuario</button></div><section class="panel table-panel"><div class="toolbar"><input class="form-input search-input" placeholder="Buscar usuario..." /><select class="form-input filter-select"><option>Todos los roles</option><option>Administrador</option><option>Técnico</option><option>Recepcionista</option></select></div></section>`;
    content.querySelector('#new-user')?.addEventListener('click', () => router.navigate('/usuarios/nuevo'));
    const tablePanel = content.querySelector('.table-panel');
    const tableContainer = document.createElement('div');
    tableContainer.className = 'table-state';
    tableContainer.textContent = 'Cargando usuarios...';
    tablePanel?.appendChild(tableContainer);

    void loadUsers(tableContainer);
    return renderAppLayout(content, 'Usuarios');
  },
};

async function loadUsers(container: HTMLElement): Promise<void> {
  try {
    const users = await listarUsuarios();
    container.replaceChildren(renderUsersTable(users));
  } catch (error) {
    container.className = 'state-box table-state';
    container.textContent = error instanceof Error
      ? error.message
      : 'No se pudieron cargar los usuarios.';
  }
}

function renderUsersTable(users: Usuario[]): HTMLElement {
  return renderTable(
    ['Usuario', 'Contacto', 'Roles', 'Estado', 'Acciones'],
    users.map((user) => [
      `<strong>${user.email ?? 'Sin email'}</strong>`,
      user.telefono ?? 'Sin teléfono',
      user.roles.map(formatRole).join(', '),
      renderBadge(user.activo ? 'Activo' : 'Inactivo', user.activo ? 'success' : 'neutral').outerHTML,
      `<button class="link-button">Administrar</button>`,
    ]),
  );
}

function formatRole(role: string): string {
  return role.charAt(0) + role.slice(1).toLowerCase();
}
