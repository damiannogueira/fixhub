import { renderAppLayout } from '../../components/layout/AppLayout';
import { renderBadge } from '../../components/ui/Badge';
import { listarOrdenes } from '../../services/ordenesService';
import { crearPresupuesto, listarPresupuestos, publicarPresupuesto } from '../../services/presupuestosService';
import type { OrdenTrabajo } from '../../types/orden';
import type { Presupuesto } from '../../types/presupuesto';

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
    const [budgets, orders] = await Promise.all([listarPresupuestos(), listarOrdenes()]);
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
    ${budgets.length ? budgets.map((budget) => `<article class="budget-card"><div class="customer-order-header"><h3>Presupuesto #${budget.id} · Orden #${budget.ordenId}</h3>${renderBadge(formatStatus(budget.estado), budget.estado === 'APROBADO' ? 'success' : budget.estado === 'RECHAZADO' ? 'danger' : 'warning').outerHTML}</div><p>Total: <strong>${formatMoney(budget.total)}</strong></p>${budget.estado === 'BORRADOR' ? `<button class="btn btn-secondary" data-publish="${budget.id}">Publicar al cliente</button>` : ''}</article>`).join('') : '<p class="muted">No hay presupuestos registrados.</p>'}`;
  list.querySelector('#new-budget')?.addEventListener('click', () => renderBudgetForm(formPanel, availableOrders, () => loadBudgets(list, formPanel)));
  list.querySelectorAll<HTMLButtonElement>('[data-publish]').forEach((button) => button.addEventListener('click', () => {
    button.disabled = true;
    void publicarPresupuesto(Number(button.dataset.publish)).then(() => loadBudgets(list, formPanel)).catch((error) => { button.disabled = false; list.prepend(errorMessage(error)); });
  }));
}

function renderBudgetForm(panel: HTMLElement, orders: OrdenTrabajo[], onSaved: () => void): void {
  panel.hidden = false;
  panel.innerHTML = `<h3>Nuevo presupuesto</h3><form class="user-form"><div class="form-group"><label class="form-label">Orden</label><select class="form-input" name="ordenId" required><option value="">Seleccioná una orden en diagnóstico</option>${orders.map((order) => `<option value="${order.id}">Orden #${order.id} — ${escapeHtml(order.cliente.apellido)}, ${escapeHtml(order.cliente.nombre)}</option>`).join('')}</select></div><div class="form-group"><label class="form-label">Mano de obra</label><input class="form-input" name="costoManoObra" type="number" min="0" step="0.01" required></div><div class="form-actions"><button class="btn btn-secondary" type="button" id="close-budget">Cancelar</button><button class="btn btn-primary" type="submit">Guardar borrador</button></div></form>`;
  const form = panel.querySelector('form') as HTMLFormElement;
  panel.querySelector('#close-budget')?.addEventListener('click', () => { panel.hidden = true; });
  form.addEventListener('submit', (event) => {
    event.preventDefault();
    const values = new FormData(form);
    const button = form.querySelector('button[type="submit"]') as HTMLButtonElement;
    button.disabled = true;
    void crearPresupuesto(Number(values.get('ordenId')), { costoManoObra: Number(values.get('costoManoObra')), detalles: [] })
      .then(onSaved).catch((error) => { button.disabled = false; form.prepend(errorMessage(error)); });
  });
}
function errorMessage(error: unknown): HTMLElement { const element = document.createElement('p'); element.className = 'form-alert'; element.textContent = error instanceof Error ? error.message : 'No se pudo completar la operación.'; return element; }
function formatStatus(status: string): string { return status.replaceAll('_', ' ').toLowerCase().replace(/(^| )\w/g, (letter) => letter.toUpperCase()); }
function formatMoney(value: number): string { return new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' }).format(value); }
function escapeHtml(value: string): string { return value.replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;'); }
