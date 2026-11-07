import { renderAppLayout } from '../../components/layout/AppLayout';
import { renderBadge } from '../../components/ui/Badge';
import { renderTable } from '../../components/ui/Table';
import { users } from '../../mocks/data';

export const UsuariosPage = {
  render(): HTMLElement {
    const content = document.createElement('div');
    content.innerHTML = `<div class="page-heading"><div><p class="muted">Accesos y permisos</p><h2>Usuarios</h2></div><button class="btn btn-primary">+ Nuevo usuario</button></div><section class="panel table-panel"><div class="toolbar"><input class="form-input search-input" placeholder="Buscar usuario..." /><select class="form-input filter-select"><option>Todos los roles</option><option>Administrador</option><option>Técnico</option><option>Recepcionista</option></select></div></section>`;
    const table = renderTable(['Usuario', 'Contacto', 'Roles', 'Estado', 'Acciones'], users.map((user) => [
      `<strong>${user.name}</strong>`, user.contact, renderBadge(user.roles, 'info').outerHTML, renderBadge(user.status, 'success').outerHTML, `<button class="link-button">Administrar</button>`,
    ]));
    content.querySelector('.table-panel')?.appendChild(table);
    return renderAppLayout(content, 'Usuarios');
  },
};
