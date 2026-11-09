import { renderBadge } from '../ui/Badge';
import type { OrdenTrabajo } from '../../types/orden';
import { equipmentLabel, escapeHtml, formatDate, formatStatus, orderStatusTone } from '../../utils/format';

/**
 * Lista de reparaciones (actuales y anteriores) para Administración y Recepción.
 * Cada fila enlaza al detalle de la orden.
 */
export function renderRepairList(orders: OrdenTrabajo[], emptyMessage: string, showEquipment = true): string {
  if (!orders.length) {
    return `<p class="muted">${escapeHtml(emptyMessage)}</p>`;
  }
  return `<div class="technical-log-list">${orders.map((order) => `
    <article class="technical-log-item">
      <strong><a class="text-link" href="#/ordenes/detalle?id=${order.id}">Orden #${order.id}</a>${showEquipment ? ` · ${escapeHtml(equipmentLabel(order.equipo))}` : ''}</strong>
      <small class="muted">Ingresó el ${formatDate(order.fechaIngreso)}${order.fechaEntrega ? ` · Entregada el ${formatDate(order.fechaEntrega)}` : ''}</small>
      <p>${escapeHtml(order.problemaInicial)}</p>
      ${renderBadge(formatStatus(order.estado), orderStatusTone(order.estado)).outerHTML}
    </article>`).join('')}</div>`;
}
