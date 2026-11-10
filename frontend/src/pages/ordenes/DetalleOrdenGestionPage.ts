import { ApiError } from '../../services/api';
import { renderAppLayout } from '../../components/layout/AppLayout';
import { renderRepairList } from '../../components/shared/RepairList';
import { renderBadge } from '../../components/ui/Badge';
import {
  listarConsumos,
  listarHistorialOrden,
  listarOrdenesDeEquipo,
  listarRegistrosOrden,
  obtenerDiagnosticoOrden,
  obtenerOrden,
  type ConsumoRepuesto,
  type Diagnostico,
  type EventoHistorial,
  type RegistroTecnico,
} from '../../services/ordenesService';
import { obtenerPresupuestoPorOrden } from '../../services/presupuestosService';
import { listarTecnicos } from '../../services/usuariosService';
import type { OrdenTrabajo } from '../../types/orden';
import type { Presupuesto } from '../../types/presupuesto';
import type { Usuario } from '../../types/usuario';
import {
  equipmentLabel,
  escapeHtml,
  formatDateTime,
  formatMoney,
  formatStatus,
  orderStatusTone,
  technicianLabel,
  type Tone,
} from '../../utils/format';

/**
 * Detalle de una orden para Administración y Recepción: seguimiento completo
 * (diagnóstico, presupuesto, tareas, consumos, historial) en modo consulta.
 */
export const DetalleOrdenGestionPage = {
  render(): HTMLElement {
    const content = document.createElement('div');
    const id = Number(new URLSearchParams(window.location.hash.split('?')[1] ?? '').get('id'));
    content.innerHTML = `
      <div class="page-heading">
        <div><p class="muted">Seguimiento del taller</p><h2>Detalle de la orden</h2></div>
        <a class="btn btn-secondary" href="#/ordenes">Volver a órdenes</a>
      </div>
      <section class="panel technical-order-detail"><div class="table-state">Cargando detalle...</div></section>
    `;
    const panel = content.querySelector('.technical-order-detail') as HTMLElement;
    if (!Number.isInteger(id) || id < 1) {
      showError(panel, 'La orden seleccionada no es válida.');
    } else {
      void loadDetail(panel, id);
    }
    return renderAppLayout(content, 'Detalle de la orden');
  },
};

async function loadDetail(panel: HTMLElement, id: number): Promise<void> {
  try {
    const order = await obtenerOrden(id);
    const [diagnosis, budget, records, consumptions, history, technicians, equipmentOrders] = await Promise.all([
      optional(obtenerDiagnosticoOrden(id)),
      optional(obtenerPresupuestoPorOrden(id)),
      optional(listarRegistrosOrden(id)),
      optional(listarConsumos(id)),
      optional(listarHistorialOrden(id)),
      optional(listarTecnicos()),
      optional(listarOrdenesDeEquipo(order.equipo.id)),
    ]);
    renderDetail(panel, {
      order,
      diagnosis,
      budget,
      records: records ?? [],
      consumptions: consumptions ?? [],
      history: history ?? [],
      technicians: technicians ?? [],
      otherRepairs: (equipmentOrders ?? []).filter((item) => item.id !== order.id),
    });
  } catch (error) {
    showError(panel, error instanceof Error ? error.message : 'No se pudo cargar el detalle.');
  }
}

type DetailData = {
  order: OrdenTrabajo;
  diagnosis: Diagnostico | null;
  budget: Presupuesto | null;
  records: RegistroTecnico[];
  consumptions: ConsumoRepuesto[];
  history: EventoHistorial[];
  technicians: Usuario[];
  otherRepairs: OrdenTrabajo[];
};

function renderDetail(panel: HTMLElement, data: DetailData): void {
  const { order, diagnosis, budget, records, consumptions, history, technicians, otherRepairs } = data;
  const technicianNames = new Map(technicians.map((technician) => [technician.id, technicianLabel(technician)]));
  const technicianName = (id: number | string): string => technicianNames.get(Number(id)) ?? `Técnico #${id}`;

  panel.innerHTML = `
    <div class="technical-detail-header">
      <div>
        <p class="muted">Orden #${order.id}</p>
        <h3>${escapeHtml(equipmentLabel(order.equipo))}</h3>
        <p class="muted">Cliente: <a class="text-link" href="#/clientes/detalle?id=${order.cliente.id}">${escapeHtml(order.cliente.apellido)}, ${escapeHtml(order.cliente.nombre)}</a></p>
      </div>
      ${renderBadge(formatStatus(order.estado), orderStatusTone(order.estado)).outerHTML}
    </div>
    <div class="detail-grid technical-order-summary">
      <div><dt>Problema informado</dt><dd>${escapeHtml(order.problemaInicial)}</dd></div>
      <div><dt>Técnico responsable</dt><dd>${order.tecnicoId ? escapeHtml(technicianName(order.tecnicoId)) : 'Sin asignar'}</dd></div>
      <div><dt>Fecha de ingreso</dt><dd>${formatDateTime(order.fechaIngreso)}</dd></div>
      <div><dt>Número de serie</dt><dd>${escapeHtml(order.equipo.numeroSerie ?? 'No informado')}</dd></div>
      ${order.fechaEntrega ? `<div><dt>Fecha de entrega</dt><dd>${formatDateTime(order.fechaEntrega)}</dd></div>` : ''}
      ${order.motivoCancelacion ? `<div><dt>Motivo de cancelación</dt><dd>${escapeHtml(order.motivoCancelacion)}</dd></div>` : ''}
    </div>
    <section class="technical-detail-section">
      <div class="section-heading"><h3>Diagnóstico</h3></div>
      ${diagnosis
        ? `<div class="diagnosis-summary"><p><strong>Falla detectada</strong><span>${escapeHtml(diagnosis.descripcionFalla)}</span></p><p><strong>Tareas requeridas</strong><span>${escapeHtml(diagnosis.tareasRequeridas)}</span></p><small class="muted">${formatDateTime(diagnosis.fecha)}</small></div>`
        : '<p class="muted">Todavía no hay un diagnóstico registrado.</p>'}
    </section>
    <section class="technical-detail-section">
      <div class="section-heading"><h3>Presupuesto</h3>${budget ? '<a class="btn btn-secondary" href="#/presupuestos">Ir a presupuestos</a>' : ''}</div>
      ${budget ? renderBudget(budget) : '<p class="muted">La orden no tiene presupuesto.</p>'}
    </section>
    <section class="technical-detail-section">
      <div class="section-heading"><h3>Tareas y observaciones técnicas</h3></div>
      ${records.length
        ? `<div class="technical-log-list">${records.map((record) => `<article class="technical-log-item"><strong>${escapeHtml(record.tareaRealizada)}</strong><small class="muted">${formatDateTime(record.fecha)} · ${escapeHtml(record.usuarioNombre)}</small>${record.observacion ? `<p>${escapeHtml(record.observacion)}</p>` : ''}</article>`).join('')}</div>`
        : '<p class="muted">Todavía no hay tareas registradas.</p>'}
    </section>
    <section class="technical-detail-section">
      <div class="section-heading"><h3>Consumos de repuestos</h3></div>
      ${consumptions.length
        ? `<div class="technical-log-list">${consumptions.map((item) => `<article class="technical-log-item"><strong>${escapeHtml(item.nombreRepuesto)} · ${item.cantidad} unidad(es)</strong><small class="muted">${formatDateTime(item.fecha)} · Usuario #${item.usuarioId}</small><p>Precio unitario: ${formatMoney(item.precioUnitario)}</p></article>`).join('')}</div>`
        : '<p class="muted">Todavía no hay consumos registrados.</p>'}
    </section>
    <section class="technical-detail-section">
      <div class="section-heading"><h3>Historial de la orden</h3></div>
      ${history.length
        ? `<div class="technical-log-list">${history.map((event) => renderEvent(event, technicianName)).join('')}</div>`
        : '<p class="muted">No hay cambios registrados.</p>'}
    </section>
    <section class="technical-detail-section">
      <div class="section-heading"><h3>Otras reparaciones de este equipo</h3></div>
      ${renderRepairList(otherRepairs, 'Este equipo no tiene otras reparaciones registradas.', false)}
    </section>
  `;
}

function renderBudget(budget: Presupuesto): string {
  return `
    <div class="detail-grid">
      <div><dt>Estado</dt><dd>${renderBadge(formatStatus(budget.estado), budgetTone(budget.estado)).outerHTML}</dd></div>
      <div><dt>Mano de obra</dt><dd>${formatMoney(budget.costoManoObra)}</dd></div>
      <div><dt>Total</dt><dd><strong>${formatMoney(budget.total)}</strong></dd></div>
    </div>
    ${budget.detalles.length
      ? `<h4>Repuestos presupuestados</h4><ul>${budget.detalles.map((detail) => `<li>${escapeHtml(detail.repuesto)} · ${detail.cantidad} x ${formatMoney(detail.precioUnitario)} = <strong>${formatMoney(detail.cantidad * detail.precioUnitario)}</strong></li>`).join('')}</ul>`
      : '<p class="muted">No se incluyeron repuestos.</p>'}`;
}

function renderEvent(event: EventoHistorial, technicianName: (id: number | string) => string): string {
  return `<article class="technical-log-item"><strong>${escapeHtml(describeEvent(event, technicianName))}</strong><small class="muted">${formatDateTime(event.fecha)} · ${escapeHtml(event.usuarioNombre)}</small></article>`;
}

function describeEvent(event: EventoHistorial, technicianName: (id: number | string) => string): string {
  const comment = event.comentario ?? '';
  switch (event.tipoEvento) {
    case 'CAMBIO_ESTADO':
      return `Estado: ${event.estadoAnterior ? formatStatus(event.estadoAnterior) : '—'} → ${event.estadoNuevo ? formatStatus(event.estadoNuevo) : '—'}`;
    case 'CANCELACION':
      return `Orden cancelada${comment ? ` · Motivo: ${comment}` : ''}`;
    case 'ASIGNACION_TECNICO':
    case 'CAMBIO_TECNICO': {
      // El backend guarda los ids de los técnicos, seguidos del motivo si lo hubo:
      // "Técnico anterior: 3; técnico nuevo: 5 | Motivo: ...". Los ids se reemplazan por nombres.
      const [technicians, ...reason] = comment.split(' | Motivo: ');
      const description = technicians.replace(
        /(asignado|anterior|nuevo): (\d+)/gi,
        (_match, label: string, id: string) => `${label}: ${technicianName(id)}`,
      ) || (event.tipoEvento === 'ASIGNACION_TECNICO' ? 'Técnico asignado' : 'Cambio de técnico');
      return reason.length ? `${description} · Motivo: ${reason.join(' | Motivo: ')}` : description;
    }
  }
}

function budgetTone(status: Presupuesto['estado']): Tone {
  if (status === 'APROBADO') return 'success';
  if (status === 'RECHAZADO') return 'danger';
  if (status === 'PUBLICADO') return 'info';
  return 'warning';
}

async function optional<T>(request: Promise<T>): Promise<T | null> {
  try {
    return await request;
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) return null;
    throw error;
  }
}

function showError(panel: HTMLElement, message: string): void {
  panel.className = 'state-box';
  panel.textContent = message;
}
