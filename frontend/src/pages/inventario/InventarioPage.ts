import { renderAppLayout } from '../../components/layout/AppLayout';
import { renderBadge } from '../../components/ui/Badge';
import { renderTable } from '../../components/ui/Table';
import {
  actualizarRepuesto,
  crearRepuesto,
  desactivarRepuesto,
  listarMovimientos,
  listarRepuestos,
  listarRepuestosConStockBajo,
  registrarAjuste,
  registrarEntrada,
} from '../../services/repuestosService';
import type { Repuesto } from '../../types/repuesto';
import { hasAnyRole } from '../../utils/authState';

export const InventarioPage = {
  render(): HTMLElement {
    const content = document.createElement('div');
    content.innerHTML = `
      <div class="page-heading"><div><p class="muted">Repuestos y existencias</p><h2>Inventario</h2></div>${hasAnyRole(['ADMINISTRADOR']) ? '<button class="btn btn-primary" id="new-part">+ Nuevo repuesto</button>' : ''}</div>
      <section class="panel"><div class="toolbar"><input class="form-input search-input" aria-label="Buscar repuesto" placeholder="Buscar por código o nombre..." /><label class="checkbox-filter"><input type="checkbox" class="low-stock-filter"> Solo stock bajo</label></div></section>
      <section class="panel inventory-form-panel" hidden></section>
      <section class="panel table-panel"><div class="table-state">Cargando repuestos...</div></section>
    `;
    const tablePanel = content.querySelector('.table-panel') as HTMLElement;
    const search = content.querySelector('.search-input') as HTMLInputElement;
    const lowStock = content.querySelector('.low-stock-filter') as HTMLInputElement;
    const formPanel = content.querySelector('.inventory-form-panel') as HTMLElement;
    let parts: Repuesto[] = [];

    const updateTable = () => {
      const query = search.value.trim().toLowerCase();
      const filtered = parts.filter((part) => {
        const searchable = `${part.codigo} ${part.nombre} ${part.descripcion ?? ''}`.toLowerCase();
        return (!query || searchable.includes(query))
          && (!lowStock.checked || part.stockActual <= part.stockMinimo);
      });
      const table = renderPartsTable(filtered);
      tablePanel.replaceChildren(table);
      bindPartRows(table, filtered);
      bindAdminActions(tablePanel, formPanel, () => void load());
    };

    const load = async () => {
      try {
        parts = lowStock.checked
          ? await listarRepuestosConStockBajo()
          : await listarRepuestos();
        loadedParts = parts;
        updateTable();
      } catch (error) {
        tablePanel.className = 'state-box table-state';
        tablePanel.textContent = error instanceof Error ? error.message : 'No se pudieron cargar los repuestos.';
      }
    };
    search.addEventListener('input', updateTable);
    lowStock.addEventListener('change', () => void load());
    content.querySelector('#new-part')?.addEventListener('click', () => renderPartForm(formPanel, undefined, () => void load()));
    void load();
    return renderAppLayout(content, 'Inventario');
  },
};

function renderPartsTable(parts: Repuesto[]): HTMLElement {
  const admin = hasAnyRole(['ADMINISTRADOR']);
  return renderTable(
    admin ? ['Código', 'Repuesto', 'Stock', 'Precio venta', 'Estado', 'Acciones'] : ['Código', 'Repuesto', 'Stock', 'Precio venta', 'Estado'],
    parts.map((part) => [
      `<strong>${escapeHtml(part.codigo)}</strong>`,
      escapeHtml(part.nombre),
      `${part.stockActual} / mínimo ${part.stockMinimo}`,
      formatCurrency(part.precioVenta),
      part.stockActual <= part.stockMinimo
        ? renderBadge('Stock bajo', 'danger').outerHTML
        : renderBadge('Disponible', 'success').outerHTML,
      ...(admin ? [renderInventoryActions(part.id)] : []),
    ]),
  );
}

function bindPartRows(table: HTMLElement, parts: Repuesto[]): void {
  if (!hasAnyRole(['ADMINISTRADOR'])) return;
  table.querySelectorAll<HTMLTableRowElement>('tbody tr').forEach((row, index) => {
    const part = parts[index];
    if (!part) return;
    row.classList.add('inventory-row-link');
    row.addEventListener('click', (event) => {
      if ((event.target as HTMLElement).closest('button, a, details, summary')) return;
      window.location.hash = `/inventario/detalle?id=${part.id}`;
    });
  });
}

function renderInventoryActions(partId: number): string {
  return `<details class="action-menu inventory-action-menu">
    <summary>Acciones</summary>
    <div class="action-menu-content">
      <button type="button" class="action-menu-item" data-inventory-action="edit" data-part-id="${partId}">
        <strong>Editar repuesto</strong><small>Modificar sus datos, precios y stock mínimo.</small>
      </button>
      <button type="button" class="action-menu-item" data-inventory-action="entry" data-part-id="${partId}">
        <strong>Registrar entrada</strong><small>Agregar unidades recibidas al stock actual.</small>
      </button>
      <button type="button" class="action-menu-item" data-inventory-action="adjust" data-part-id="${partId}">
        <strong>Ajustar stock</strong><small>Corregir existencias por inventario o diferencia.</small>
      </button>
      <button type="button" class="action-menu-item action-menu-danger" data-inventory-action="delete" data-part-id="${partId}">
        <strong>Desactivar repuesto</strong><small>Ocultarlo del catálogo sin eliminar su historial.</small>
      </button>
    </div>
  </details>`;
}

function bindAdminActions(table: HTMLElement, panel: HTMLElement, reload: () => void): void {
  if (!hasAnyRole(['ADMINISTRADOR'])) return;
  table.querySelectorAll<HTMLButtonElement>('[data-part-detail]').forEach((button) => button.addEventListener('click', () => {
    renderHistory(panel, Number(button.dataset.partDetail));
  }));
  table.querySelectorAll<HTMLButtonElement>('[data-inventory-action]').forEach((button) => button.addEventListener('click', () => {
    const partId = Number(button.dataset.partId);
    const action = button.dataset.inventoryAction;
    button.closest('details')?.removeAttribute('open');
    if (action === 'edit') {
      const part = findPart(String(partId));
      if (part) renderPartForm(panel, part, reload);
    } else if (action === 'entry') {
      renderStockForm(panel, partId, true, reload);
    } else if (action === 'adjust') {
      renderStockForm(panel, partId, false, reload);
    } else if (action === 'delete') {
      if (!window.confirm('¿Desactivar este repuesto?')) return;
      void desactivarRepuesto(partId).then(reload).catch((error) => panel.prepend(errorMessage(error)));
    }
  }));
}

let loadedParts: Repuesto[] = [];
function findPart(id: string | undefined): Repuesto | undefined {
  return loadedParts.find((part) => part.id === Number(id));
}

function renderPartForm(panel: HTMLElement, part: Repuesto | undefined, reload: () => void): void {
  panel.hidden = false;
  panel.innerHTML = `<h3>${part ? 'Editar repuesto' : 'Nuevo repuesto'}</h3><form class="user-form inventory-form">
    <div class="form-grid">
      <div class="form-group"><label class="form-label" for="part-code">Código</label><input id="part-code" class="form-input" name="codigo" required value="${escapeHtml(part?.codigo ?? '')}"></div>
      <div class="form-group"><label class="form-label" for="part-name">Nombre</label><input id="part-name" class="form-input" name="nombre" required value="${escapeHtml(part?.nombre ?? '')}"></div>
      <div class="form-group"><label class="form-label" for="part-min-stock">Stock mínimo</label><input id="part-min-stock" class="form-input" name="stockMinimo" type="number" min="0" required value="${part?.stockMinimo ?? 0}"></div>
      <div class="form-group"><label class="form-label" for="part-cost">Precio de costo</label><input id="part-cost" class="form-input" name="precioCosto" type="number" min="0" step="0.01" required value="${part?.precioCosto ?? 0}"></div>
      <div class="form-group"><label class="form-label" for="part-sale-price">Precio de venta</label><input id="part-sale-price" class="form-input" name="precioVenta" type="number" min="0" step="0.01" required value="${part?.precioVenta ?? 0}"></div>
    </div>
    <div class="form-group"><label class="form-label" for="part-description">Descripción</label><textarea id="part-description" class="form-input" name="descripcion" rows="3">${escapeHtml(part?.descripcion ?? '')}</textarea></div>
    <div class="form-actions"><button type="button" class="btn btn-secondary" data-close-form>Cancelar</button><button class="btn btn-primary" type="submit">Guardar</button></div></form>`;
  bindForm(panel, async (form) => {
    const values = new FormData(form);
    const request = { codigo: String(values.get('codigo')), nombre: String(values.get('nombre')), descripcion: String(values.get('descripcion')), stockMinimo: Number(values.get('stockMinimo')), precioCosto: Number(values.get('precioCosto')), precioVenta: Number(values.get('precioVenta')) };
    if (part) await actualizarRepuesto(part.id, request); else await crearRepuesto(request);
    panel.hidden = true; reload();
  });
}

function renderStockForm(panel: HTMLElement, id: number, entry: boolean, reload: () => void): void {
  panel.hidden = false;
  panel.innerHTML = `<h3>${entry ? 'Registrar entrada' : 'Ajustar stock'}</h3><form class="user-form">
    <div class="form-group"><label class="form-label" for="stock-quantity">Cantidad ${entry ? '' : '(ingresá un valor negativo para disminuir el stock)'}</label><input id="stock-quantity" class="form-input" name="cantidad" type="number" ${entry ? 'min="1"' : ''} required></div>
    <div class="form-group"><label class="form-label" for="stock-reason">Motivo</label><input id="stock-reason" class="form-input" name="motivo" required></div>
    <div class="form-group"><label class="form-label" for="stock-reference">Referencia <span class="muted">(opcional)</span></label><input id="stock-reference" class="form-input" name="referencia"></div>
    <div class="form-actions"><button type="button" class="btn btn-secondary" data-close-form>Cancelar</button><button class="btn btn-primary" type="submit">Confirmar</button></div></form>`;
  bindForm(panel, async (form) => {
    const values = new FormData(form);
    const request = { cantidad: Number(values.get('cantidad')), motivo: String(values.get('motivo')), referencia: String(values.get('referencia')) };
    if (entry) await registrarEntrada(id, request); else await registrarAjuste(id, request);
    panel.hidden = true; reload();
  });
}

function renderHistory(panel: HTMLElement, id: number): void {
  panel.hidden = false; panel.innerHTML = '<div class="table-state">Cargando movimientos...</div>';
  void listarMovimientos(id).then((movements) => {
    panel.innerHTML = `<div class="panel-header"><div><p class="muted">Detalle del repuesto</p><h3>Movimientos de stock</h3></div><button type="button" class="btn btn-secondary" data-close-form>Cerrar detalle</button></div>${movements.length ? `<div class="table-wrapper"><table><thead><tr><th>Fecha</th><th>Tipo</th><th>Cantidad</th><th>Motivo</th><th>Referencia</th><th>Usuario</th></tr></thead><tbody>${movements.map((movement) => `<tr><td>${formatDate(movement.fecha)}</td><td>${movement.tipo}</td><td>${movement.cantidad}</td><td>${escapeHtml(movement.motivo)}</td><td>${escapeHtml(movement.referencia ?? '—')}</td><td>${escapeHtml(movement.usuarioNombre)}</td></tr>`).join('')}</tbody></table></div>` : '<p class="muted">No hay movimientos registrados para este repuesto.</p>'}`;
    panel.querySelector('[data-close-form]')?.addEventListener('click', () => { panel.hidden = true; });
  }).catch((error) => { panel.textContent = error instanceof Error ? error.message : 'No se pudieron cargar los movimientos.'; });
}

function bindForm(panel: HTMLElement, submit: (form: HTMLFormElement) => Promise<void>): void {
  panel.querySelector('[data-close-form]')?.addEventListener('click', () => { panel.hidden = true; });
  const form = panel.querySelector('form') as HTMLFormElement;
  form.addEventListener('submit', (event) => {
    event.preventDefault();
    const button = form.querySelector('button[type="submit"]') as HTMLButtonElement;
    button.disabled = true;
    void submit(form).catch((error) => { button.disabled = false; form.prepend(errorMessage(error)); });
  });
}

function errorMessage(error: unknown): HTMLElement {
  const element = document.createElement('p'); element.className = 'form-alert'; element.textContent = error instanceof Error ? error.message : 'No se pudo completar la operación.'; return element;
}

function formatCurrency(value: number): string {
  return new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' }).format(value);
}

function formatDate(value: string): string {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString('es-AR');
}

function escapeHtml(value: string): string {
  return value.replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;').replaceAll('\'', '&#039;');
}
