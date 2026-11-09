import { renderAppLayout } from '../../components/layout/AppLayout';
import { renderBadge } from '../../components/ui/Badge';
import { obtenerMiOrden } from '../../services/ordenesService';
import type { EstadoOrden, OrdenCliente } from '../../types/orden';

export const DetalleOrdenClientePage = {
  render(): HTMLElement {
    const content = document.createElement('div');
    content.innerHTML = `
      <div class="page-heading">
        <div><p class="muted">Espacio del cliente</p><h2>Detalle de reparación</h2></div>
        <a class="btn btn-secondary" href="#/mis-ordenes">Volver a mis reparaciones</a>
      </div>
      <section class="panel customer-order-detail">Cargando reparación...</section>
    `;

    const container = content.querySelector('.customer-order-detail') as HTMLElement;
    const id = Number(new URLSearchParams(window.location.hash.split('?')[1] ?? '').get('id'));
    if (!Number.isInteger(id) || id < 1) {
      showError(container, 'La reparación seleccionada no es válida.');
      return renderAppLayout(content, 'Detalle de reparación');
    }

    void loadOrder(container, id);
    return renderAppLayout(content, 'Detalle de reparación');
  },
};

async function loadOrder(container: HTMLElement, id: number): Promise<void> {
  try {
    renderOrder(container, await obtenerMiOrden(id));
  } catch (error) {
    showError(container, error instanceof Error ? error.message : 'No se pudo cargar la reparación.');
  }
}

function renderOrder(container: HTMLElement, order: OrdenCliente): void {
  const equipmentName = [order.equipo.tipo, order.equipo.marca, order.equipo.modelo]
    .filter(Boolean)
    .join(' ');
  container.innerHTML = `
    <div class="customer-order-detail-header">
      <div><p class="muted">Orden #${order.id}</p><h3>${escapeHtml(equipmentName)}</h3></div>
      ${renderBadge(formatStatus(order.estado), statusTone(order.estado)).outerHTML}
    </div>
    <div class="detail-grid">
      <div><dt>Problema informado</dt><dd>${escapeHtml(order.problemaInicial)}</dd></div>
      <div><dt>Fecha de ingreso</dt><dd>${formatDate(order.fechaIngreso)}</dd></div>
      <div><dt>Número de serie</dt><dd>${escapeHtml(order.equipo.numeroSerie ?? 'No informado')}</dd></div>
      ${order.fechaEntrega ? `<div><dt>Fecha de entrega</dt><dd>${formatDate(order.fechaEntrega)}</dd></div>` : ''}
    </div>
    ${order.motivoCancelacion ? `<p class="order-cancellation">Motivo de cancelación: ${escapeHtml(order.motivoCancelacion)}</p>` : ''}
  `;
}

function showError(container: HTMLElement, message: string): void {
  container.className = 'state-box';
  container.textContent = message;
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
  return value
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll('\'', '&#039;');
}
