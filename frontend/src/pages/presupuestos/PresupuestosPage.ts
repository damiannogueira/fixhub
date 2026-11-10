import { renderAppLayout } from '../../components/layout/AppLayout';
import { renderBadge } from '../../components/ui/Badge';
import { listarMisOrdenesTecnico, listarOrdenes } from '../../services/ordenesService';
import { crearPresupuesto, editarPresupuesto, listarMisPresupuestosTecnico, listarPresupuestos, publicarPresupuesto } from '../../services/presupuestosService';
import type { OrdenTrabajo } from '../../types/orden';
import type { Presupuesto } from '../../types/presupuesto';
import { listarRepuestos } from '../../services/repuestosService';
import type { Repuesto } from '../../types/repuesto';
import { hasAnyRole } from '../../utils/authState';

export const PresupuestosPage = {
  render(): HTMLElement {
    const content = document.createElement('div');
    content.innerHTML = `
      <div class="page-heading"><div><p class="muted">Presupuestos y aprobación</p><h2>Presupuestos</h2></div></div>
      <section class="panel budget-form-panel" hidden></section>
      <section class="panel budget-list"><div class="table-state">Cargando presupuestos...</div></section>`;
    const list = content.querySelector('.budget-list') as HTMLElement;
    const formPanel = content.querySelector('.budget-form-panel') as HTMLElement;
    void loadBudgets(list, formPanel);
    return renderAppLayout(content, 'Presupuestos');
  },
};

async function loadBudgets(list: HTMLElement, formPanel: HTMLElement): Promise<void> {
  try {
    const isTechnician = hasAnyRole(['TECNICO']) && !hasAnyRole(['ADMINISTRADOR', 'RECEPCIONISTA']);
    const [budgets, orders, parts] = await Promise.all([
      isTechnician ? listarMisPresupuestosTecnico() : listarPresupuestos(),
      isTechnician ? listarMisOrdenesTecnico() : listarOrdenes(),
      listarRepuestos(),
    ]);
    renderBudgetList(list, formPanel, budgets, orders, parts);
    openRequestedOrder(list, formPanel, budgets, orders, parts);
  } catch (error) {
    list.textContent = error instanceof Error ? error.message : 'No se pudieron cargar los presupuestos.';
  }
}

/**
 * Atiende el acceso desde "Preparar presupuesto" (#/presupuestos?ordenId=N):
 * abre el formulario con la orden fijada, o el borrador existente de esa orden.
 */
function openRequestedOrder(list: HTMLElement, formPanel: HTMLElement, budgets: Presupuesto[], orders: OrdenTrabajo[], parts: Repuesto[]): void {
  const requestedOrderId = Number(new URLSearchParams(window.location.hash.split('?')[1] ?? '').get('ordenId'));
  if (!Number.isInteger(requestedOrderId) || requestedOrderId < 1) return;

  // Se quita el parámetro para que recargar la lista después de guardar no reabra el formulario.
  history.replaceState(null, '', '#/presupuestos');

  const reload = () => loadBudgets(list, formPanel);
  const requestedOrder = orders.find((order) => order.id === requestedOrderId);
  const existing = budgets.find((budget) => budget.ordenId === requestedOrderId);

  if (existing?.estado === 'BORRADOR' || existing?.estado === 'PUBLICADO') {
    renderBudgetForm(formPanel, orders, reload, existing, parts);
  } else if (existing) {
    list.prepend(noticeMessage(`La orden #${requestedOrderId} ya tiene un presupuesto ${formatStatus(existing.estado).toLowerCase()}.`));
    return;
  } else if (!requestedOrder || requestedOrder.estado !== 'EN_DIAGNOSTICO') {
    list.prepend(noticeMessage(`La orden #${requestedOrderId} no está disponible para presupuestar. Debe estar en diagnóstico.`));
    return;
  } else {
    renderBudgetForm(formPanel, [requestedOrder], reload, undefined, parts, requestedOrder.id);
  }
  formPanel.scrollIntoView({ behavior: 'smooth', block: 'start' });
  (formPanel.querySelector('#budget-labor') as HTMLInputElement | null)?.focus({ preventScroll: true });
}

function renderBudgetList(list: HTMLElement, formPanel: HTMLElement, budgets: Presupuesto[], orders: OrdenTrabajo[], parts: Repuesto[]): void {
  const existingOrderIds = new Set(budgets.map((budget) => budget.ordenId));
  const availableOrders = orders.filter((order) => !existingOrderIds.has(order.id) && order.estado === 'EN_DIAGNOSTICO');
  // Según el README, solo Administrador y Recepcionista publican presupuestos.
  const canPublish = hasAnyRole(['ADMINISTRADOR', 'RECEPCIONISTA']);
  list.innerHTML = `
    <div class="panel-header"><h3>Presupuestos registrados</h3><button class="btn btn-primary" id="new-budget">+ Nuevo presupuesto</button></div>
    ${budgets.length ? budgets.map((budget) => `<article class="budget-card"><div class="customer-order-header"><h3>Presupuesto #${budget.id} · Orden #${budget.ordenId}</h3>${renderBadge(formatStatus(budget.estado), budget.estado === 'APROBADO' ? 'success' : budget.estado === 'RECHAZADO' ? 'danger' : 'warning').outerHTML}</div><div class="budget-cost-breakdown"><div class="budget-cost-row"><span>Mano de obra</span><strong>${formatMoney(budget.costoManoObra)}</strong></div>${budget.detalles.map((detail) => `<div class="budget-cost-row"><span>${escapeHtml(detail.repuesto)} · ${detail.cantidad} x ${formatMoney(detail.precioUnitario)}${isPending(budget) ? stockWarningHtml(detail.repuestoId, detail.cantidad, parts) : ''}</span><strong>${formatMoney(detail.cantidad * detail.precioUnitario)}</strong></div>`).join('')}<div class="budget-cost-row budget-cost-total"><span>Total</span><strong>${formatMoney(budget.total)}</strong></div></div>${renderDraftActions(budget, orders, canPublish)}</article>`).join('') : '<p class="muted">No hay presupuestos registrados.</p>'}`;
  list.querySelector('#new-budget')?.addEventListener('click', () => {
    void listarRepuestos().then((parts) => renderBudgetForm(formPanel, availableOrders, () => loadBudgets(list, formPanel), undefined, parts));
  });
  list.querySelectorAll<HTMLButtonElement>('[data-edit]').forEach((button) => button.addEventListener('click', () => {
    const budget = budgets.find((item) => item.id === Number(button.dataset.edit));
    if (budget) void listarRepuestos().then((parts) => renderBudgetForm(formPanel, orders, () => loadBudgets(list, formPanel), budget, parts));
  }));
  list.querySelectorAll<HTMLButtonElement>('[data-publish]').forEach((button) => button.addEventListener('click', () => {
    button.disabled = true;
    void publicarPresupuesto(Number(button.dataset.publish)).then(() => loadBudgets(list, formPanel)).catch((error) => { button.disabled = false; list.prepend(errorMessage(error)); });
  }));
}

function renderDraftActions(budget: Presupuesto, orders: OrdenTrabajo[], canPublish: boolean): string {
  const orderStatus = orders.find((order) => order.id === budget.ordenId)?.estado;
  if (budget.estado === 'PUBLICADO' && orderStatus === 'PENDIENTE_APROBACION') {
    return `<div class="form-actions"><button class="btn btn-secondary" data-edit="${budget.id}">Editar</button></div>`;
  }
  if (budget.estado !== 'BORRADOR') return '';
  if (orderStatus !== 'EN_DIAGNOSTICO') {
    const detail = orderStatus ? ` (estado actual: ${formatStatus(orderStatus).toLowerCase()})` : '';
    return `<p class="muted">La orden ya no está en diagnóstico${detail}: este borrador no puede editarse ni publicarse.</p>`;
  }
  return `<div class="form-actions"><button class="btn btn-secondary" data-edit="${budget.id}">Editar</button>${canPublish ? `<button class="btn btn-secondary" data-publish="${budget.id}">Publicar al cliente</button>` : ''}</div>`;
}

function renderBudgetForm(panel: HTMLElement, orders: OrdenTrabajo[], onSaved: () => void, budget?: Presupuesto, parts: Repuesto[] = [], fixedOrderId?: number): void {
  panel.hidden = false;
  const selectedOrderId = budget?.ordenId ?? fixedOrderId;
  const orderLocked = selectedOrderId !== undefined;
  const orderOptions = orders.map((order) => `<option value="${order.id}" ${selectedOrderId === order.id ? 'selected' : ''}>Orden #${order.id} — ${escapeHtml(order.cliente.apellido)}, ${escapeHtml(order.cliente.nombre)}</option>`).join('');
  const details = budget?.detalles ?? [];
  panel.innerHTML = `<h3>${budget ? `Editar presupuesto #${budget.id}` : 'Nuevo presupuesto'}</h3><form class="user-form"><div class="form-group"><label class="form-label" for="budget-order">Orden</label><select class="form-input" id="budget-order" name="ordenId" ${orderLocked ? 'disabled' : ''} required><option value="">Seleccioná una orden en diagnóstico</option>${orderOptions}</select></div><div class="form-group"><label class="form-label" for="budget-labor">Costo de mano de obra</label><input class="form-input" id="budget-labor" name="costoManoObra" type="number" min="0" step="0.01" value="${budget?.costoManoObra ?? ''}" required></div><div class="form-group"><span class="form-label">Repuestos</span><div class="budget-detail-editor" id="budget-details">${details.map((detail) => detailRow(detail.repuestoId, detail.cantidad, parts)).join('')}</div><button class="btn btn-secondary" type="button" id="add-budget-detail">+ Agregar repuesto</button><small class="muted">Los repuestos presupuestados son una estimación: no modifican el stock. El stock se descuenta solo con los consumos reales del técnico.</small></div><div class="form-actions"><button class="btn btn-secondary" type="button" id="close-budget">Cancelar</button><button class="btn btn-primary" type="submit">${budget ? 'Guardar cambios' : 'Guardar borrador'}</button></div></form>`;
  const form = panel.querySelector('form') as HTMLFormElement;
  panel.querySelector('#close-budget')?.addEventListener('click', () => { panel.hidden = true; });
  panel.querySelector('#add-budget-detail')?.addEventListener('click', () => {
    panel.querySelector('#budget-details')?.insertAdjacentHTML('beforeend', detailRow('', 1, parts));
  });
  const detailsEditor = panel.querySelector('#budget-details') as HTMLElement;
  detailsEditor.addEventListener('click', (event) => {
    const target = event.target as HTMLElement;
    if (target.closest('[data-remove-detail]')) target.closest('.budget-detail-row')?.remove();
  });
  // Aviso informativo: el presupuesto no modifica stock, pero conviene saber si habrá que reponer.
  const refreshStockWarning = (event: Event): void => {
    const row = (event.target as HTMLElement).closest<HTMLElement>('.budget-detail-row');
    if (row) updateRowStockWarning(row, parts);
  };
  detailsEditor.addEventListener('input', refreshStockWarning);
  detailsEditor.addEventListener('change', refreshStockWarning);
  detailsEditor.querySelectorAll<HTMLElement>('.budget-detail-row').forEach((row) => updateRowStockWarning(row, parts));
  form.addEventListener('submit', (event) => {
    event.preventDefault();
    const values = new FormData(form);
    const button = form.querySelector('button[type="submit"]') as HTMLButtonElement;
    button.disabled = true;
    const request = {
      costoManoObra: Number(values.get('costoManoObra')),
      detalles: Array.from(form.querySelectorAll<HTMLElement>('.budget-detail-row')).map((row) => ({
        repuestoId: Number((row.querySelector('[name="repuestoId"]') as HTMLSelectElement).value),
        cantidad: Number((row.querySelector('[name="cantidad"]') as HTMLInputElement).value),
      })).filter((detail) => detail.repuestoId > 0 && detail.cantidad > 0),
    };
    const save = budget
      ? editarPresupuesto(budget.id, request)
      // Un select deshabilitado no viaja en FormData: si la orden está fijada se usa ese id.
      : crearPresupuesto(fixedOrderId ?? Number(values.get('ordenId')), request);
    void save
      .then(() => {
        // Tanto al crear el borrador como al editarlo, el formulario se cierra y se recarga la lista.
        panel.hidden = true;
        panel.innerHTML = '';
        return onSaved();
      })
      .catch((error) => { button.disabled = false; form.prepend(errorMessage(error)); });
  });
}
function detailRow(repuestoId: number | string, cantidad: number, parts: Repuesto[]): string {
  const options = parts.map((part) => `<option value="${part.id}" ${String(part.id) === String(repuestoId) ? 'selected' : ''}>${escapeHtml(part.nombre)} — stock ${part.stockActual} · ${formatMoney(part.precioVenta)}</option>`).join('');
  return `<div class="budget-detail-row"><select class="form-input" name="repuestoId" required><option value="">Seleccioná repuesto</option>${options}</select><input class="form-input" name="cantidad" type="number" min="1" value="${cantidad}" required><button class="icon-button danger" type="button" data-remove-detail aria-label="Quitar repuesto">×</button><small class="stock-warning" role="status" hidden></small></div>`;
}
function stockShortageMessage(repuestoId: number | string, cantidad: number, parts: Repuesto[]): string | null {
  const part = parts.find((item) => String(item.id) === String(repuestoId));
  if (!part || !(cantidad > part.stockActual)) return null;
  return `⚠ Stock disponible: ${part.stockActual}. Habrá que reponer antes de reparar.`;
}
function stockWarningHtml(repuestoId: number, cantidad: number, parts: Repuesto[]): string {
  const message = stockShortageMessage(repuestoId, cantidad, parts);
  return message ? `<small class="stock-warning">${message}</small>` : '';
}
function updateRowStockWarning(row: HTMLElement, parts: Repuesto[]): void {
  const repuestoId = (row.querySelector('[name="repuestoId"]') as HTMLSelectElement).value;
  const cantidad = Number((row.querySelector('[name="cantidad"]') as HTMLInputElement).value);
  const warning = row.querySelector('.stock-warning') as HTMLElement;
  const message = stockShortageMessage(repuestoId, cantidad, parts);
  warning.textContent = message ?? '';
  warning.hidden = !message;
}
function isPending(budget: Presupuesto): boolean {
  return budget.estado === 'BORRADOR' || budget.estado === 'PUBLICADO';
}
function noticeMessage(message: string): HTMLElement { const element = document.createElement('p'); element.className = 'form-alert'; element.setAttribute('role', 'status'); element.textContent = message; return element; }
function errorMessage(error: unknown): HTMLElement { const element = document.createElement('p'); element.className = 'form-alert'; element.textContent = error instanceof Error ? error.message : 'No se pudo completar la operación.'; return element; }
function formatStatus(status: string): string { return status.replaceAll('_', ' ').toLowerCase().replace(/(^| )\w/g, (letter) => letter.toUpperCase()); }
function formatMoney(value: number): string { return new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' }).format(value); }
function escapeHtml(value: string): string { return value.replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;'); }
