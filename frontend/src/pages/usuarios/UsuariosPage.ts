import { renderAppLayout } from '../../components/layout/AppLayout';
import { renderBadge } from '../../components/ui/Badge';
import { renderTable } from '../../components/ui/Table';
import { router } from '../../router/router';
import { desactivarUsuario, listarUsuarios } from '../../services/usuariosService';
import type { Usuario } from '../../types/usuario';

export const UsuariosPage = {
  render(): HTMLElement {
    const content = document.createElement('div');
    content.innerHTML = `<div class="page-heading"><div><p class="muted">Accesos y permisos</p><h2>Usuarios</h2></div><button class="btn btn-primary" id="new-user">+ Nuevo usuario</button></div><section class="panel table-panel"><div class="toolbar"><input class="form-input search-input" aria-label="Buscar usuario" placeholder="Buscar por email o teléfono..." /><select class="form-input filter-select" aria-label="Filtrar por rol"><option value="">Todos los roles</option><option value="ADMINISTRADOR">Administrador</option><option value="TECNICO">Técnico</option><option value="RECEPCIONISTA">Recepcionista</option><option value="CLIENTE">Cliente</option></select><label class="checkbox-filter"><input type="checkbox" class="active-filter" value="active"> Solo activos</label><label class="checkbox-filter"><input type="checkbox" class="active-filter" value="inactive"> Solo no activos</label></div></section>`;
    content.querySelector('#new-user')?.addEventListener('click', () => router.navigate('/usuarios/nuevo'));
    const tablePanel = content.querySelector('.table-panel');
    const tableContainer = document.createElement('div');
    tableContainer.className = 'table-state';
    tableContainer.textContent = 'Cargando usuarios...';
    tablePanel?.appendChild(tableContainer);

    const search = content.querySelector('.search-input') as HTMLInputElement;
    const roleFilter = content.querySelector('.filter-select') as HTMLSelectElement;
    const statusFilters = [...content.querySelectorAll<HTMLInputElement>('.active-filter')];
    let users: Usuario[] = [];
    const updateTable = () => {
      const onDelete = async (id: number) => {
        if (!window.confirm('¿Querés eliminar este usuario? Se desactivará su acceso y no se perderá su historial.')) return;
        try {
          await desactivarUsuario(id);
          users = users.map((user) => user.id === id ? { ...user, activo: false } : user);
          updateTable();
        } catch (error) {
          window.alert(error instanceof Error ? error.message : 'No se pudo eliminar el usuario.');
        }
      };
      const table = renderUsersTable(
        users.filter((user) => {
          const query = search.value.trim().toLowerCase();
          const activeFilter = statusFilters
            .filter((filter) => filter.checked)
            .map((filter) => filter.value);
          return (!query || `${user.email ?? ''} ${user.telefono ?? ''}`.toLowerCase().includes(query))
            && (!roleFilter.value || user.roles.includes(roleFilter.value as Usuario['roles'][number]))
            && (activeFilter.length === 0
              || (activeFilter.includes('active') && user.activo)
              || (activeFilter.includes('inactive') && !user.activo));
        }),
      );
      table.querySelectorAll<HTMLButtonElement>('[data-delete-id]').forEach((button) => {
        button.addEventListener('click', () => void onDelete(Number(button.dataset.deleteId)));
      });
      tableContainer.replaceChildren(table);
    };
    search.addEventListener('input', updateTable);
    roleFilter.addEventListener('change', updateTable);
    statusFilters.forEach((filter) => filter.addEventListener('change', updateTable));
    void loadUsers(tableContainer, (loaded) => { users = loaded; updateTable(); });
    return renderAppLayout(content, 'Usuarios');
  },
};

async function loadUsers(container: HTMLElement, onLoaded: (users: Usuario[]) => void): Promise<void> {
  try {
    const users = await listarUsuarios();
    onLoaded(users);
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
      `<a class="icon-button" href="#/usuarios/editar?id=${user.id}" title="Editar usuario" aria-label="Editar usuario">✏</a>
       <button class="icon-button danger" type="button" data-delete-id="${user.id}" title="Eliminar usuario" aria-label="Eliminar usuario">🗑</button>`,
    ]),
  );
}

function formatRole(role: string): string {
  return role.charAt(0) + role.slice(1).toLowerCase();
}
