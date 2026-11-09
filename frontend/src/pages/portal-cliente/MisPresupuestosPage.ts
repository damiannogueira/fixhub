import { renderAppLayout } from '../../components/layout/AppLayout';
import { renderBadge } from '../../components/ui/Badge';
import { listarMisPresupuestos, decidirPresupuesto } from '../../services/presupuestosService';
import type { Presupuesto } from '../../types/presupuesto';

export const MisPresupuestosPage = {
  render(): HTMLElement {
    const content = document.createElement('div');
    content.innerHTML = '<div class="page-heading"><div><p class="muted">Decisiones de reparación</p><h2>Mis presupuestos</h2></div></div><section class="panel budget-list"><div class="table-state">Cargando presupuestos...</div></section>';
    const list = content.querySelector('.budget-list') as HTMLElement;
    void listarMisPresupuestos().then((items) => renderBudgets(list, items)).catch((error) => {
      list.textContent = error instanceof Error ? error.message : 'No se pudieron cargar los presupuestos.';
    });
    return renderAppLayout(content, 'Mis presupuestos');
  },
};

function renderBudgets(container: HTMLElement, budgets: Presupuesto[]): void {
  if (!budgets.length) { container.innerHTML = '<p class="muted">No tenés presupuestos registrados.</p>'; return; }
  container.innerHTML = budgets.map((budget) => `
    <article class="budget-card">
      <div class="customer-order-header"><h3>Presupuesto #${budget.id} · Orden #${budget.ordenId}</h3>${renderBadge(formatStatus(budget.estado), budget.estado === 'RECHAZADO' ? 'danger' : budget.estado === 'APROBADO' ? 'success' : 'warning').outerHTML}</div>
      <div class="budget-cost-breakdown"><div class="budget-cost-row"><span>Mano de obra</span><strong>${formatMoney(budget.costoManoObra)}</strong></div>${budget.detalles.map((detail) => `<div class="budget-cost-row"><span>${escapeHtml(detail.repuesto)} · ${detail.cantidad} x ${formatMoney(detail.precioUnitario)}</span><strong>${formatMoney(detail.cantidad * detail.precioUnitario)}</strong></div>`).join('')}<div class="budget-cost-row budget-cost-total"><span>Total a aprobar</span><strong>${formatMoney(budget.total)}</strong></div></div>
      ${budget.estado === 'PUBLICADO' ? '<p class="muted">La aprobación confirma la mano de obra y todos los repuestos detallados.</p>' : ''}
      ${budget.estado === 'PUBLICADO' ? '<div class="form-actions"><button class="btn btn-primary" data-decision="APROBADO">Aprobar</button><button class="btn btn-secondary" data-decision="RECHAZADO">Rechazar</button></div>' : ''}
    </article>
  `).join('');
  container.querySelectorAll<HTMLButtonElement>('[data-decision]').forEach((button) => {
    button.addEventListener('click', () => {
      const card = button.closest('.budget-card') as HTMLElement;
      const budget = budgets[Array.from(container.children).indexOf(card)];
      void decidirPresupuesto(budget.id, button.dataset.decision as 'APROBADO' | 'RECHAZADO')
        .then(() => listarMisPresupuestos().then((items) => renderBudgets(container, items)))
        .catch((error) => { button.disabled = false; window.alert(error instanceof Error ? error.message : 'No se pudo registrar la decisión.'); });
    });
  });
}
function formatStatus(status: string): string { return status.replaceAll('_', ' ').toLowerCase().replace(/(^| )\w/g, (letter) => letter.toUpperCase()); }
function formatMoney(value: number): string { return new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' }).format(value); }
function escapeHtml(value: string): string { return value.replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;'); }
