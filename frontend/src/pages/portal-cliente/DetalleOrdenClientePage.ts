import { renderAppLayout } from '../../components/layout/AppLayout';
import { renderBadge } from '../../components/ui/Badge';
import {
  listarTareasCliente,
  obtenerDiagnosticoCliente,
  obtenerMiOrden,
  type DiagnosticoCliente,
  type TareaCliente,
} from '../../services/ordenesService';
import { obtenerPresupuestoPorOrden } from '../../services/presupuestosService';
import type { Presupuesto } from '../../types/presupuesto';
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
    const [order, diagnostico, presupuesto, tareas] = await Promise.all([
      obtenerMiOrden(id),
      optionalRequest(obtenerDiagnosticoCliente(id)),
      optionalRequest(obtenerPresupuestoPorOrden(id)),
      optionalRequest(listarTareasCliente(id)),
    ]);
    renderOrder(container, order, diagnostico, presupuesto, tareas ?? []);
  } catch (error) {
    showError(container, error instanceof Error ? error.message : 'No se pudo cargar la reparación.');
  }
}

function renderOrder(
  container: HTMLElement,
  order: OrdenCliente,
  diagnostico: DiagnosticoCliente | null,
  presupuesto: Presupuesto | null,
  tareas: TareaCliente[],
): void {
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
    <section class="detail-section">
      <h3>Detalle de la reparación</h3>
      ${diagnostico ? `
        <div class="detail-grid">
          <div><dt>Falla detectada</dt><dd>${escapeHtml(diagnostico.descripcionFalla)}</dd></div>
          <div><dt>Tareas requeridas</dt><dd>${escapeHtml(diagnostico.tareasRequeridas)}</dd></div>
          <div><dt>Fecha del diagnóstico</dt><dd>${formatDate(diagnostico.fecha)}</dd></div>
        </div>
      ` : '<p class="muted">El diagnóstico todavía no está disponible.</p>'}
    </section>
    <section class="detail-section">
      <h3>Tareas realizadas</h3>
      ${tareas.length
        ? `<div class="technical-log-list">${tareas.map((tarea) => `<article class="technical-log-item"><strong>${escapeHtml(tarea.tareaRealizada)}</strong><small class="muted">${formatDateTime(tarea.fecha)}</small></article>`).join('')}</div>`
        : `<p class="muted">${tasksEmptyMessage(order.estado)}</p>`}
    </section>
    <section class="detail-section">
      <div class="customer-order-detail-header">
        <h3>Presupuesto y repuestos</h3>
        ${presupuesto?.estado === 'PUBLICADO' ? '<a class="btn btn-primary" href="#/mis-presupuestos">Ir a aprobación de presupuestos</a>' : ''}
      </div>
      ${presupuesto ? `
        <div class="detail-grid">
          <div><dt>Estado</dt><dd>${renderBadge(formatStatus(presupuesto.estado), budgetTone(presupuesto.estado)).outerHTML}</dd></div>
          <div><dt>Mano de obra</dt><dd>${formatMoney(presupuesto.costoManoObra)}</dd></div>
          <div><dt>Total a aprobar</dt><dd><strong>${formatMoney(presupuesto.total)}</strong></dd></div>
        </div>
        ${presupuesto.detalles.length ? `
          <h4>Repuestos presupuestados</h4>
          <ul>${presupuesto.detalles.map((detail) => `<li>${escapeHtml(detail.repuesto)} · ${detail.cantidad} x ${formatMoney(detail.precioUnitario)} = <strong>${formatMoney(detail.cantidad * detail.precioUnitario)}</strong></li>`).join('')}</ul>
        ` : '<p class="muted">No se incluyeron repuestos en este presupuesto.</p>'}
      ` : '<p class="muted">Todavía no hay un presupuesto disponible para esta reparación.</p>'}
    </section>
    ${order.motivoCancelacion ? `<p class="order-cancellation">Motivo de cancelación: ${escapeHtml(order.motivoCancelacion)}</p>` : ''}
  `;
}

async function optionalRequest<T>(request: Promise<T>): Promise<T | null> {
  try {
    return await request;
  } catch {
    return null;
  }
}

function showError(container: HTMLElement, message: string): void {
  container.className = 'state-box';
  container.textContent = message;
}

function formatStatus(status: string): string {
  return status.replaceAll('_', ' ').toLowerCase().replace(/(^| )\w/g, (letter) => letter.toUpperCase());
}

function statusTone(status: EstadoOrden): 'success' | 'warning' | 'danger' | 'info' | 'neutral' {
  if (status === 'CANCELADA') return 'danger';
  if (status === 'LISTA_PARA_RETIRAR') return 'warning';
  if (status === 'ENTREGADA' || status === 'FINALIZADA') return 'success';
  if (status === 'EN_REPARACION' || status === 'APROBADA') return 'info';
  return 'neutral';
}

function budgetTone(status: Presupuesto['estado']): 'success' | 'warning' | 'danger' | 'info' | 'neutral' {
  if (status === 'APROBADO') return 'success';
  if (status === 'RECHAZADO') return 'danger';
  if (status === 'PUBLICADO') return 'info';
  return 'warning';
}

function formatMoney(value: number): string {
  return new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' }).format(value);
}

function tasksEmptyMessage(status: EstadoOrden): string {
  if (status === 'EN_REPARACION') return 'El técnico todavía no registró tareas en esta reparación.';
  if (status === 'FINALIZADA' || status === 'LISTA_PARA_RETIRAR' || status === 'ENTREGADA') {
    return 'No se registraron tareas para esta reparación.';
  }
  return 'Las tareas aparecerán cuando comience la reparación.';
}

function formatDateTime(value: string): string {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString('es-AR');
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
