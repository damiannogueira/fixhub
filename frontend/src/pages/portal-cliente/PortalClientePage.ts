import { renderAppLayout } from '../../components/layout/AppLayout';
import { renderBadge } from '../../components/ui/Badge';
import { listarMisOrdenes } from '../../services/ordenesService';
import type { EstadoOrden, OrdenCliente } from '../../types/orden';

export const PortalClientePage = {
  render(): HTMLElement {
    const content = document.createElement('div');
    content.innerHTML = `
      <div class="page-heading"><div><p class="muted">Espacio del cliente</p><h2>Mis reparaciones</h2></div></div>
      <section class="panel"><div class="portal-orders-content">Cargando reparaciones...</div></section>
    `;
    const container = content.querySelector('.portal-orders-content') as HTMLElement;
    void loadOrders(container);
    return renderAppLayout(content, 'Mis reparaciones');
  },
};

async function loadOrders(container: HTMLElement): Promise<void> {
  try {
    renderOrders(container, await listarMisOrdenes());
  } catch (error) {
    container.className = 'state-box';
    container.textContent = error instanceof Error ? error.message : 'No se pudieron cargar tus reparaciones.';
  }
}

function renderOrders(container: HTMLElement, orders: OrdenCliente[]): void {
  if (orders.length === 0) {
    container.innerHTML = '<div class="state-box"><h3>No tenés reparaciones registradas</h3><p>Cuando se cree una orden a tu nombre, aparecerá aquí.</p></div>';
    return;
  }

  const list = document.createElement('div');
  list.className = 'customer-orders-list';
  list.innerHTML = orders.map((order) => {
    const equipmentName = [order.equipo.tipo, order.equipo.marca, order.equipo.modelo]
      .filter(Boolean)
      .join(' ');
    return `
      <article class="customer-order-card">
        <div class="customer-order-header">
          <div><h3>Orden #${order.id}</h3><p class="muted">${escapeHtml(equipmentName)}</p></div>
          ${renderBadge(formatStatus(order.estado), statusTone(order.estado)).outerHTML}
        </div>
        <p>${escapeHtml(order.problemaInicial)}</p>
        <small class="muted">Ingresó el ${formatDate(order.fechaIngreso)}</small>
        ${order.motivoCancelacion ? `<p class="order-cancellation">Motivo de cancelación: ${escapeHtml(order.motivoCancelacion)}</p>` : ''}
        <a class="text-link customer-order-link" href="#/mis-ordenes/detalle?id=${order.id}">Ver detalle</a>
      </article>
    `;
  }).join('');
  container.replaceChildren(list);
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
