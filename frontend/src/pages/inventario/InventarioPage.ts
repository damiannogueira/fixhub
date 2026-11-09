import { renderAppLayout } from '../../components/layout/AppLayout';
import { renderBadge } from '../../components/ui/Badge';
import { renderTable } from '../../components/ui/Table';
import { listarRepuestos, listarRepuestosConStockBajo } from '../../services/repuestosService';
import type { Repuesto } from '../../types/repuesto';

export const InventarioPage = {
  render(): HTMLElement {
    const content = document.createElement('div');
    content.innerHTML = `
      <div class="page-heading"><div><p class="muted">Repuestos y existencias</p><h2>Inventario</h2></div></div>
      <section class="panel"><div class="toolbar"><input class="form-input search-input" aria-label="Buscar repuesto" placeholder="Buscar por código o nombre..." /><label class="checkbox-filter"><input type="checkbox" class="low-stock-filter"> Solo stock bajo</label></div></section>
      <section class="panel table-panel"><div class="table-state">Cargando repuestos...</div></section>
    `;
    const tablePanel = content.querySelector('.table-panel') as HTMLElement;
    const search = content.querySelector('.search-input') as HTMLInputElement;
    const lowStock = content.querySelector('.low-stock-filter') as HTMLInputElement;
    let parts: Repuesto[] = [];

    const updateTable = () => {
      const query = search.value.trim().toLowerCase();
      const filtered = parts.filter((part) => {
        const searchable = `${part.codigo} ${part.nombre} ${part.descripcion ?? ''}`.toLowerCase();
        return (!query || searchable.includes(query))
          && (!lowStock.checked || part.stockActual <= part.stockMinimo);
      });
      tablePanel.replaceChildren(renderPartsTable(filtered));
    };

    const load = async () => {
      try {
        parts = lowStock.checked
          ? await listarRepuestosConStockBajo()
          : await listarRepuestos();
        updateTable();
      } catch (error) {
        tablePanel.className = 'state-box table-state';
        tablePanel.textContent = error instanceof Error ? error.message : 'No se pudieron cargar los repuestos.';
      }
    };
    search.addEventListener('input', updateTable);
    lowStock.addEventListener('change', () => void load());
    void load();
    return renderAppLayout(content, 'Inventario');
  },
};

function renderPartsTable(parts: Repuesto[]): HTMLElement {
  return renderTable(
    ['Código', 'Repuesto', 'Stock', 'Precio venta', 'Estado'],
    parts.map((part) => [
      `<strong>${escapeHtml(part.codigo)}</strong>`,
      escapeHtml(part.nombre),
      `${part.stockActual} / mínimo ${part.stockMinimo}`,
      formatCurrency(part.precioVenta),
      part.stockActual <= part.stockMinimo
        ? renderBadge('Stock bajo', 'danger').outerHTML
        : renderBadge('Disponible', 'success').outerHTML,
    ]),
  );
}

function formatCurrency(value: number): string {
  return new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' }).format(value);
}

function escapeHtml(value: string): string {
  return value.replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;').replaceAll('\'', '&#039;');
}
