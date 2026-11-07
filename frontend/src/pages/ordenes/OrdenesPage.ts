import { renderAppLayout } from '../../components/layout/AppLayout';
import { renderBadge } from '../../components/ui/Badge';
import { renderTable } from '../../components/ui/Table';
import { orders } from '../../mocks/data';

export const OrdenesPage = {
  render(): HTMLElement {
    const content = document.createElement('div');
    content.innerHTML = `<div class="page-heading"><div><p class="muted">Seguimiento del taller</p><h2>Órdenes de trabajo</h2></div><button class="btn btn-primary">+ Nueva orden</button></div><section class="panel table-panel"><div class="toolbar"><input class="form-input search-input" placeholder="Buscar orden, cliente o equipo..." /><select class="form-input filter-select"><option>Todos los estados</option><option>Recibida</option><option>En reparación</option><option>Lista para retirar</option></select></div></section>`;
    const table = renderTable(['Orden', 'Cliente', 'Equipo', 'Estado', 'Ingreso', 'Acciones'], orders.map((order) => [
      `<strong>${order.id}</strong>`, order.client, order.equipment, renderBadge(order.state, order.state === 'RECIBIDA' ? 'neutral' : order.state === 'EN_REPARACION' ? 'info' : 'warning').outerHTML, order.date, `<button class="link-button">Ver orden</button>`,
    ]));
    content.querySelector('.table-panel')?.appendChild(table);
    return renderAppLayout(content, 'Órdenes de trabajo');
  },
};
