import { renderAppLayout } from '../../components/layout/AppLayout';
import { renderBadge } from '../../components/ui/Badge';
import { renderTable } from '../../components/ui/Table';
import { obtenerDatosDashboard } from '../../services/dashboardService';
import { getAuthState } from '../../utils/authState';
import type { EstadoOrden, OrdenTrabajo } from '../../types/orden';

export const DashboardPage = {
  render(): HTMLElement {
    const content = document.createElement('div');
    content.innerHTML = `
      <div class="page-heading"><div><p class="muted">Resumen general</p><h2>Buen día, ${getDashboardRole()}</h2></div><a class="btn btn-primary" href="#/ordenes">Ver órdenes</a></div>
      <div class="stats-grid dashboard-stats"><div class="table-state">Cargando indicadores...</div></div>
      <div class="content-grid">
        <section class="panel panel-wide"><div class="panel-header"><div><h3>Órdenes recientes</h3><p class="muted">Últimas órdenes ingresadas al taller</p></div><a href="#/ordenes" class="text-link">Ver todas</a></div><div class="dashboard-orders">Cargando órdenes...</div></section>
        <section class="panel"><div class="panel-header"><div><h3>Estado de órdenes</h3><p class="muted">Distribución actual</p></div></div><div class="dashboard-status">Cargando estados...</div></section>
      </div>
    `;
    const statsContainer = content.querySelector('.dashboard-stats') as HTMLElement;
    const ordersContainer = content.querySelector('.dashboard-orders') as HTMLElement;
    const statusContainer = content.querySelector('.dashboard-status') as HTMLElement;
    void loadDashboard(statsContainer, ordersContainer, statusContainer);
    return renderAppLayout(content, 'Dashboard');
  },
};

async function loadDashboard(
  statsContainer: HTMLElement,
  ordersContainer: HTMLElement,
  statusContainer: HTMLElement,
): Promise<void> {
  try {
    const data = await obtenerDatosDashboard();
    statsContainer.innerHTML = [
      statCard('Órdenes abiertas', data.openOrders.length, 'blue'),
      statCard('En diagnóstico', data.diagnosticOrders.length, 'orange'),
      statCard('Listas para retirar', data.readyOrders.length, 'green'),
      statCard('Stock bajo', data.lowStockParts.length, 'red'),
    ].join('');
    ordersContainer.replaceChildren(renderRecentOrders(data.orders));
    statusContainer.replaceChildren(renderStatusSummary(data.orders));
  } catch (error) {
    const message = error instanceof Error ? error.message : 'No se pudieron cargar los indicadores.';
    statsContainer.className = 'state-box table-state';
    statsContainer.textContent = message;
    ordersContainer.textContent = message;
    statusContainer.textContent = message;
  }
}

function statCard(label: string, value: number, tone: string): string {
  return `<article class="stat-card stat-${tone}"><div class="stat-icon">▦</div><p>${label}</p><strong>${value}</strong><small>Datos actuales</small></article>`;
}

function renderRecentOrders(orders: OrdenTrabajo[]): HTMLElement {
  const table = renderTable(
    ['Orden', 'Cliente', 'Equipo', 'Estado', 'Ingreso'],
    [...orders].sort((a, b) => b.id - a.id).slice(0, 5).map((order) => [
      `<strong>#${order.id}</strong>`,
      `${escapeHtml(order.cliente.apellido)}, ${escapeHtml(order.cliente.nombre)}`,
      escapeHtml([order.equipo.tipo, order.equipo.marca, order.equipo.modelo].filter(Boolean).join(' ')),
      renderBadge(formatStatus(order.estado), statusTone(order.estado)).outerHTML,
      formatDate(order.fechaIngreso),
    ]),
  );
  return table;
}

function renderStatusSummary(orders: OrdenTrabajo[]): HTMLElement {
  const counts = new Map<EstadoOrden, number>();
  orders.forEach((order) => counts.set(order.estado, (counts.get(order.estado) ?? 0) + 1));
  const summary = document.createElement('div');
  summary.className = 'progress-list';
  const total = orders.length || 1;
  summary.innerHTML = [...counts.entries()].map(([status, count]) => `
    <div><span>${formatStatus(status)}</span><strong>${count}</strong><i><b style="width:${Math.round((count / total) * 100)}%"></b></i></div>
  `).join('') || '<p class="muted">No hay órdenes registradas.</p>';
  return summary;
}

function getDashboardRole(): string {
  const labels: Partial<Record<string, string>> = {
    ADMINISTRADOR: 'Administrador',
    RECEPCIONISTA: 'Recepcionista',
    TECNICO: 'Técnico',
  };
  // Quien acumula roles los ve todos en el saludo.
  const names = getAuthState().roles.flatMap((role) => labels[role] ?? []);
  return names.join(' / ') || 'Administrador';
}

function formatStatus(status: EstadoOrden): string {
  return status.replaceAll('_', ' ').toLowerCase().replace(/(^| )\w/g, (letter) => letter.toUpperCase());
}

function statusTone(status: EstadoOrden): 'success' | 'warning' | 'danger' | 'info' | 'neutral' {
  if (status === 'CANCELADA') return 'danger';
  if (status === 'LISTA_PARA_RETIRAR') return 'warning';
  if (status === 'ENTREGADA' || status === 'FINALIZADA') return 'success';
  if (status === 'EN_REPARACION' || status === 'APROBADA') return 'info';
  return 'neutral';
}

function formatDate(value: string): string {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleDateString('es-AR');
}

function escapeHtml(value: string): string {
  return value.replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;').replaceAll('\'', '&#039;');
}
