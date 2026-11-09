import { renderAppLayout } from '../../components/layout/AppLayout';
import { renderBadge } from '../../components/ui/Badge';
import { listarMisOrdenesTecnico, listarOrdenes } from '../../services/ordenesService';
import { crearPresupuesto, editarPresupuesto, listarMisPresupuestosTecnico, listarPresupuestos, publicarPresupuesto } from '../../services/presupuestosService';
import type { OrdenTrabajo } from '../../types/orden';
import type { Presupuesto } from '../../types/presupuesto';
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
    const [budgets, orders] = await Promise.all([
      isTechnician ? listarMisPresupuestosTecnico() : listarPresupuestos(),
      isTechnician ? listarMisOrdenesTecnico() : listarOrdenes(),
    ]);
    renderBudgetList(list, formPanel, budgets, orders);
  } catch (error) {
    list.textContent = error instanceof Error ? error.message : 'No se pudieron cargar los presupuestos.';
  }
}

function renderBudgetList(list: HTMLElement, formPanel: HTMLElement, budgets: Presupuesto[], orders: OrdenTrabajo[]): void {
  const existingOrderIds = new Set(budgets.map((budget) => budget.ordenId));
  const availableOrders = orders.filter((order) => !existingOrderIds.has(order.id) && order.estado === 'EN_DIAGNOSTICO');
  list.innerHTML = `
    <div class="panel-header"><h3>Presupuestos registrados</h3><button class="btn btn-primary" id="new-budget">+ Nuevo presupuesto</button></div>
    ${budgets.length ? budgets.map((budget) => `<article class="budget-card"><div class="customer-order-header"><h3>Presupuesto #${budget.id} · Orden #${budget.ordenId}</h3>${renderBadge(formatStatus(budget.estado), budget.estado === 'APROBADO' ? 'success' : budget.estado === 'RECHAZADO' ? 'danger' : 'warning').outerHTML}</div><div class="budget-cost-breakdown"><div class="budget-cost-row"><span>Mano de obra</span><strong>${formatMoney(budget.costoManoObra)}</strong></div>${budget.detalles.map((detail) => `<div class="budget-cost-row"><span>${escapeHtml(detail.repuesto)} · ${detail.cantidad} x ${formatMoney(detail.precioUnitario)}</span><strong>${formatMoney(detail.cantidad * detail.precioUnitario)}</strong></div>`).join('')}<div class="budget-cost-row budget-cost-total"><span>Total</span><strong>${formatMoney(budget.total)}</strong></div></div>${budget.estado === 'BORRADOR' ? `<div class="form-actions"><button class="btn btn-secondary" data-edit="${budget.id}">Editar</button><button class="btn btn-secondary" data-publish="${budget.id}">Publicar al cliente</button></div>` : ''}</article>`).join('') : '<p class="muted">No hay presupuestos registrados.</p>'}`;
  list.querySelector('#new-budget')?.addEventListener('click', () => renderBudgetForm(formPanel, availableOrders, () => loadBudgets(list, formPanel)));
  list.querySelectorAll<HTMLButtonElement>('[data-edit]').forEach((button) => button.addEventListener('click', () => {
    const budget = budgets.find((item) => item.id === Number(button.dataset.edit));
    if (budget) renderBudgetForm(formPanel, orders, () => loadBudgets(list, formPanel), budget);
  }));
  list.querySelectorAll<HTMLButtonElement>('[data-publish]').forEach((button) => button.addEventListener('click', () => {
    button.disabled = true;
    void publicarPresupuesto(Number(button.dataset.publish)).then(() => loadBudgets(list, formPanel)).catch((error) => { button.disabled = false; list.prepend(errorMessage(error)); });
  }));
}

function renderBudgetForm(panel: HTMLElement, orders: OrdenTrabajo[], onSaved: () => void, budget?: Presupuesto): void {
  panel.hidden = false;
  const orderOptions = orders.map((order) => `<option value="${order.id}" ${budget?.ordenId === order.id ? 'selected' : ''}>Orden #${order.id} — ${escapeHtml(order.cliente.apellido)}, ${escapeHtml(order.cliente.nombre)}</option>`).join('');
  const details = budget?.detalles ?? [];
  panel.innerHTML = `<h3>${budget ? `Editar presupuesto #${budget.id}` : 'Nuevo presupuesto'}</h3><form class="user-form"><div class="form-group"><label class="form-label" for="budget-order">Orden</label><select class="form-input" id="budget-order" name="ordenId" ${budget ? 'disabled' : ''} required><option value="">Seleccioná una orden en diagnóstico</option>${orderOptions}</select></div><div class="form-group"><label class="form-label" for="budget-labor">Costo de mano de obra</label><input class="form-input" id="budget-labor" name="costoManoObra" type="number" min="0" step="0.01" value="${budget?.costoManoObra ?? ''}" required></div>${details.length ? `<div class="form-group"><span class="form-label">Repuestos incluidos</span><ul>${details.map((detail) => `<li>${escapeHtml(detail.repuesto)} x ${detail.cantidad}</li>`).join('')}</ul><small class="muted">Los repuestos existentes se conservan al editar.</small></div>` : ''}<div class="form-actions"><button class="btn btn-secondary" type="button" id="close-budget">Cancelar</button><button class="btn btn-primary" type="submit">${budget ? 'Guardar cambios' : 'Guardar borrador'}</button></div></form>`;
  const form = panel.querySelector('form') as HTMLFormElement;
  panel.querySelector('#close-budget')?.addEventListener('click', () => { panel.hidden = true; });
  form.addEventListener('submit', (event) => {
    event.preventDefault();
    const values = new FormData(form);
    const button = form.querySelector('button[type="submit"]') as HTMLButtonElement;
    button.disabled = true;
    const request = {
      costoManoObra: Number(values.get('costoManoObra')),
      detalles: budget?.detalles.map((detail) => ({ repuestoId: detail.repuestoId, cantidad: detail.cantidad })) ?? [],
    };
    const save = budget
      ? editarPresupuesto(budget.id, request)
      : crearPresupuesto(Number(values.get('ordenId')), request);
    void save
      .then(() => {
        if (budget) panel.hidden = true;
        return onSaved();
      })
      .catch((error) => { button.disabled = false; form.prepend(errorMessage(error)); });
  });
}
function errorMessage(error: unknown): HTMLElement { const element = document.createElement('p'); element.className = 'form-alert'; element.textContent = error instanceof Error ? error.message : 'No se pudo completar la operación.'; return element; }
function formatStatus(status: string): string { return status.replaceAll('_', ' ').toLowerCase().replace(/(^| )\w/g, (letter) => letter.toUpperCase()); }
function formatMoney(value: number): string { return new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' }).format(value); }
function escapeHtml(value: string): string { return value.replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;'); }
