import { renderAppLayout } from '../../components/layout/AppLayout';
import { renderBadge } from '../../components/ui/Badge';
import { renderTable } from '../../components/ui/Table';
import { dashboardStats, orders } from '../../mocks/data';

export const DashboardPage = {
  render(): HTMLElement {
    const content = document.createElement('div');
    content.innerHTML = `
      <div class="page-heading"><div><p class="muted">Resumen general</p><h2>Buen día, Administrador</h2></div><button class="btn btn-primary">+ Nueva orden</button></div>
      <div class="stats-grid">${dashboardStats.map((stat) => `
        <article class="stat-card stat-${stat.tone}"><div class="stat-icon">▦</div><p>${stat.label}</p><strong>${stat.value}</strong><small>${stat.change}</small></article>
      `).join('')}</div>
      <div class="content-grid">
        <section class="panel panel-wide"><div class="panel-header"><div><h3>Órdenes recientes</h3><p class="muted">Últimas órdenes ingresadas al taller</p></div><a href="#/ordenes" class="text-link">Ver todas</a></div></section>
        <section class="panel"><div class="panel-header"><div><h3>Estado de órdenes</h3><p class="muted">Distribución actual</p></div></div><div class="progress-list"><div><span>En reparación</span><strong>10</strong><i><b style="width:70%"></b></i></div><div><span>En diagnóstico</span><strong>8</strong><i><b style="width:52%"></b></i></div><div><span>Recibidas</span><strong>6</strong><i><b style="width:38%"></b></i></div></div></section>
      </div>
    `;
    const table = renderTable(['Orden', 'Cliente', 'Equipo', 'Estado', 'Ingreso'], orders.slice(0, 3).map((order) => [
      `<strong>${order.id}</strong>`, order.client, order.equipment, renderBadge(order.state, order.state === 'EN_REPARACION' ? 'info' : 'warning').outerHTML, order.date,
    ]));
    content.querySelector('.panel-wide')?.appendChild(table);
    return renderAppLayout(content, 'Dashboard');
  },
};
