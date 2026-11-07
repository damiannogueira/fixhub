import { renderAppLayout } from '../../components/layout/AppLayout';
import { renderBadge } from '../../components/ui/Badge';
import { renderTable } from '../../components/ui/Table';
import { spareParts } from '../../mocks/data';

export const InventarioPage = {
  render(): HTMLElement {
    const content = document.createElement('div');
    content.innerHTML = `<div class="page-heading"><div><p class="muted">Repuestos y existencias</p><h2>Inventario</h2></div><button class="btn btn-primary">+ Nuevo repuesto</button></div><section class="panel table-panel"><div class="toolbar"><input class="form-input search-input" placeholder="Buscar repuesto..." /><button class="btn btn-secondary">Ver stock bajo</button></div></section>`;
    const table = renderTable(['Código', 'Repuesto', 'Categoría', 'Stock', 'Precio venta', 'Estado'], spareParts.map((part) => [
      `<strong>${part.code}</strong>`, part.name, part.category, part.stock <= part.minimum ? renderBadge(`${part.stock} unidades`, 'danger').outerHTML : renderBadge(`${part.stock} unidades`, 'success').outerHTML, part.price, part.stock <= part.minimum ? renderBadge('Stock bajo', 'danger').outerHTML : renderBadge('Disponible', 'success').outerHTML,
    ]));
    content.querySelector('.table-panel')?.appendChild(table);
    return renderAppLayout(content, 'Inventario');
  },
};
