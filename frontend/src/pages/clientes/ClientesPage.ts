import { renderAppLayout } from '../../components/layout/AppLayout';
import { renderBadge } from '../../components/ui/Badge';
import { renderTable } from '../../components/ui/Table';
import { clients } from '../../mocks/data';

export const ClientesPage = {
  render(): HTMLElement {
    const content = document.createElement('div');
    content.innerHTML = `<div class="page-heading"><div><p class="muted">Gestión de personas</p><h2>Clientes</h2></div><button class="btn btn-primary">+ Nuevo cliente</button></div><section class="panel"><div class="toolbar"><input class="form-input search-input" placeholder="Buscar por nombre o contacto..." /><select class="form-input filter-select"><option>Todos los estados</option><option>Activos</option><option>Inactivos</option></select></div></section><section class="panel table-panel"></section>`;
    const table = renderTable(['Cliente', 'Contacto', 'Equipos', 'Estado', 'Acciones'], clients.map((client) => [
      `<strong>${client.name}</strong>`, client.contact, client.equipment, renderBadge(client.status, client.status === 'Activo' ? 'success' : 'neutral').outerHTML, `<button class="link-button">Ver detalle</button>`,
    ]));
    content.querySelector('.table-panel')?.appendChild(table);
    return renderAppLayout(content, 'Clientes');
  },
};
