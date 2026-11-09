import { renderAppLayout } from '../../components/layout/AppLayout';
import { renderBadge } from '../../components/ui/Badge';
import {
  crearRegistroTecnico,
  listarConsumos,
  listarRegistrosTecnicos,
  listarMisOrdenesTecnico,
  consumirRepuesto,
  obtenerDiagnosticoTecnico,
  crearDiagnostico,
  actualizarDiagnostico,
  type ConsumoRepuesto,
  type RegistroTecnico,
} from '../../services/ordenesService';
import { listarRepuestos } from '../../services/repuestosService';
import type { OrdenTrabajo } from '../../types/orden';
import { isHybridTechnician } from '../../utils/technicianView';

export const DetalleOrdenTecnicoPage = {
  render(): HTMLElement {
    const content = document.createElement('div');
    const id = Number(new URLSearchParams(window.location.hash.split('?')[1] ?? '').get('id'));
    content.innerHTML = `
      <div class="page-heading">
        <div><p class="muted">Trabajo asignado</p><h2>Detalle de la orden</h2></div>
        <a class="btn btn-secondary" href="#/ordenes${isHybridTechnician() ? '?vista=tecnico' : ''}">Volver a mis órdenes</a>
      </div>
      <section class="panel technical-order-detail"><div class="table-state">Cargando detalle...</div></section>
    `;
    const panel = content.querySelector('.technical-order-detail') as HTMLElement;
    if (!Number.isInteger(id) || id < 1) {
      showError(panel, 'La orden seleccionada no es válida.');
      return renderAppLayout(content, 'Detalle de la orden');
    }
    void loadDetail(panel, id);
    return renderAppLayout(content, 'Detalle de la orden');
  },
};

async function loadDetail(panel: HTMLElement, id: number): Promise<void> {
  try {
    const order = (await listarMisOrdenesTecnico()).find((item) => item.id === id);
    if (!order) {
      showError(panel, 'La orden no existe o no está asignada a este técnico.');
      return;
    }
    const [records, consumptions] = await Promise.all([listarRegistrosTecnicos(id), listarConsumos(id)]);
    const diagnosis = await obtenerDiagnosticoTecnico(id).catch(() => null);
    renderDetail(panel, order, records, consumptions, diagnosis);
  } catch (error) {
    showError(panel, error instanceof Error ? error.message : 'No se pudo cargar el detalle.');
  }
}

function renderDetail(
  panel: HTMLElement,
  order: OrdenTrabajo,
  records: RegistroTecnico[],
  consumptions: ConsumoRepuesto[],
  diagnosis: import('../../services/ordenesService').Diagnostico | null,
): void {
  // El backend solo admite crear o editar el diagnóstico en EN_DIAGNOSTICO.
  const canEditDiagnosis = order.estado === 'EN_DIAGNOSTICO';
  // Tareas y consumos solo se registran durante la reparación (misma regla que el backend).
  const canRegisterWork = order.estado === 'EN_REPARACION';
  panel.innerHTML = `
    <div class="technical-detail-header">
      <div><p class="muted">Orden #${order.id}</p><h3>${escapeHtml([order.equipo.tipo, order.equipo.marca, order.equipo.modelo].filter(Boolean).join(' '))}</h3><p class="muted">Cliente: ${escapeHtml(order.cliente.apellido)}, ${escapeHtml(order.cliente.nombre)}</p></div>
      ${renderBadge(formatStatus(order.estado), statusTone(order.estado)).outerHTML}
    </div>
    <div class="detail-grid technical-order-summary">
      <div><dt>Problema informado</dt><dd>${escapeHtml(order.problemaInicial)}</dd></div>
      <div><dt>Fecha de ingreso</dt><dd>${formatDate(order.fechaIngreso)}</dd></div>
      <div><dt>Número de serie</dt><dd>${escapeHtml(order.equipo.numeroSerie ?? 'No informado')}</dd></div>
    </div>
    <section class="technical-detail-section">
      <div class="section-heading"><h3>Diagnóstico técnico</h3>${canEditDiagnosis ? `<button class="btn btn-secondary" type="button" id="edit-diagnosis">${diagnosis ? 'Editar diagnóstico' : 'Registrar diagnóstico'}</button>` : ''}</div>
      <div id="diagnosis-content">${diagnosis ? `<div class="diagnosis-summary"><p><strong>Falla detectada</strong><span>${escapeHtml(diagnosis.descripcionFalla)}</span></p><p><strong>Tareas requeridas</strong><span>${escapeHtml(diagnosis.tareasRequeridas)}</span></p><small class="muted">${formatDate(diagnosis.fecha)}</small></div>` : '<p class="muted">Todavía no hay un diagnóstico registrado.</p>'}</div>
    </section>
    <section class="technical-detail-section">
      <div class="section-heading"><h3>Registro técnico</h3>${canRegisterWork ? '<button class="btn btn-primary" type="button" id="open-task-modal">Registrar tarea</button>' : ''}</div>
      <div id="technical-records">${renderRecords(records)}</div>
    </section>
    <section class="technical-detail-section">
      <div class="section-heading"><h3>Consumos de repuestos</h3>${canRegisterWork ? '<button class="btn btn-primary" type="button" id="open-consumption-modal">Registrar consumo</button>' : ''}</div>
      <div id="technical-consumptions">${renderConsumptions(consumptions)}</div>
    </section>
    <div class="modal-backdrop" id="task-modal" hidden>
      <section class="modal-dialog" role="dialog" aria-modal="true" aria-labelledby="task-modal-title">
        <div class="section-heading"><h3 id="task-modal-title">Registrar tarea</h3><button class="btn btn-secondary" type="button" data-close-modal>Cancelar</button></div>
        <form class="user-form" id="task-form">
          <div class="form-group"><label class="form-label" for="task-name">Tarea realizada</label><input class="form-input" id="task-name" name="tareaRealizada" required></div>
          <div class="form-group"><label class="form-label" for="task-note">Observación</label><textarea class="form-input" id="task-note" name="observacion" rows="4"></textarea></div>
          <p class="form-alert" hidden></p>
          <div class="form-actions"><button class="btn btn-primary" type="submit">Guardar tarea</button></div>
        </form>
      </section>
    </div>
    <div class="modal-backdrop" id="consumption-modal" hidden>
      <section class="modal-dialog" role="dialog" aria-modal="true" aria-labelledby="consumption-modal-title">
        <div class="section-heading"><h3 id="consumption-modal-title">Registrar consumo</h3><button class="btn btn-secondary" type="button" data-close-modal>Cancelar</button></div>
        <form class="user-form" id="consumption-form">
          <div class="form-group"><label class="form-label" for="consumption-part">Repuesto</label><select class="form-input" id="consumption-part" name="repuestoId" required><option value="">Cargando repuestos...</option></select></div>
          <div class="form-group"><label class="form-label" for="consumption-quantity">Cantidad</label><input class="form-input" id="consumption-quantity" name="cantidad" type="number" min="1" required></div>
          <p class="form-alert" hidden></p>
          <div class="form-actions"><button class="btn btn-primary" type="submit">Guardar consumo</button></div>
        </form>
      </section>
    </div>
  `;

  const taskModal = panel.querySelector('#task-modal') as HTMLElement;
  const consumptionModal = panel.querySelector('#consumption-modal') as HTMLElement;
  panel.querySelector('#open-task-modal')?.addEventListener('click', () => { taskModal.hidden = false; });
  panel.querySelector('#open-consumption-modal')?.addEventListener('click', () => {
    consumptionModal.hidden = false;
    void loadParts(consumptionModal);
  });
  panel.querySelector('#edit-diagnosis')?.addEventListener('click', () => renderDiagnosis(panel, order.id, diagnosis));
  panel.querySelectorAll<HTMLElement>('[data-close-modal]').forEach((button) => {
    button.addEventListener('click', () => { (button.closest('.modal-backdrop') as HTMLElement).hidden = true; });
  });

  panel.querySelector('#task-form')?.addEventListener('submit', (event) => {
    event.preventDefault();
    const form = event.currentTarget as HTMLFormElement;
    const values = new FormData(form);
    const submit = form.querySelector('button[type="submit"]') as HTMLButtonElement;
    submit.disabled = true;
    void crearRegistroTecnico(order.id, {
      tareaRealizada: String(values.get('tareaRealizada') ?? '').trim(),
      observacion: String(values.get('observacion') ?? '').trim(),
    }).then(() => loadDetail(panel, order.id)).catch((error) => {
      submit.disabled = false;
      showFormError(form, error);
    });
  });

  panel.querySelector('#consumption-form')?.addEventListener('submit', (event) => {
    event.preventDefault();
    const form = event.currentTarget as HTMLFormElement;
    const values = new FormData(form);
    const submit = form.querySelector('button[type="submit"]') as HTMLButtonElement;
    submit.disabled = true;
    void consumirRepuesto(order.id, {
      repuestoId: Number(values.get('repuestoId')),
      cantidad: Number(values.get('cantidad')),
    }).then(() => loadDetail(panel, order.id)).catch((error) => {
      submit.disabled = false;
      showFormError(form, error);
    });
  });
}

function renderDiagnosis(panel: HTMLElement, orderId: number, diagnosis: import('../../services/ordenesService').Diagnostico | null): void {
  const content = panel.querySelector('#diagnosis-content') as HTMLElement;
  content.innerHTML = `<form class="user-form diagnosis-form"><div class="form-group"><label class="form-label" for="diagnosis-failure">Falla detectada</label><textarea class="form-input" id="diagnosis-failure" name="descripcionFalla" rows="3" required>${escapeHtml(diagnosis?.descripcionFalla ?? '')}</textarea></div><div class="form-group"><label class="form-label" for="diagnosis-tasks">Tareas requeridas</label><textarea class="form-input" id="diagnosis-tasks" name="tareasRequeridas" rows="3" required>${escapeHtml(diagnosis?.tareasRequeridas ?? '')}</textarea></div><div class="form-actions"><button class="btn btn-secondary" type="button" id="cancel-diagnosis">Cancelar</button><button class="btn btn-primary" type="submit">Guardar diagnóstico</button></div></form>`;
  const form = content.querySelector('form') as HTMLFormElement;
  content.querySelector('#cancel-diagnosis')?.addEventListener('click', () => void loadDetail(panel, orderId));
  form.addEventListener('submit', (event) => {
    event.preventDefault();
    const values = new FormData(form);
    const request = { descripcionFalla: String(values.get('descripcionFalla') ?? '').trim(), tareasRequeridas: String(values.get('tareasRequeridas') ?? '').trim() };
    const save = diagnosis ? actualizarDiagnostico(orderId, request) : crearDiagnostico(orderId, request);
    void save.then(() => loadDetail(panel, orderId)).catch((error) => showFormError(form, error));
  });
}

async function loadParts(modal: HTMLElement): Promise<void> {
  const select = modal.querySelector('select') as HTMLSelectElement;
  try {
    const parts = await listarRepuestos();
    select.innerHTML = `<option value="">Seleccioná un repuesto</option>${parts.map((part) => `<option value="${part.id}">${escapeHtml(part.nombre)} — stock ${part.stockActual}</option>`).join('')}`;
  } catch (error) {
    select.innerHTML = '<option value="">No se pudieron cargar los repuestos</option>';
  }
}

function renderRecords(records: RegistroTecnico[]): string {
  return records.length ? `<div class="technical-log-list">${records.map((record) => `<article class="technical-log-item"><strong>${escapeHtml(record.tareaRealizada)}</strong><small class="muted">${formatDate(record.fecha)} · ${escapeHtml(record.usuarioNombre)}</small>${record.observacion ? `<p>${escapeHtml(record.observacion)}</p>` : ''}</article>`).join('')}</div>` : '<p class="muted">Todavía no hay tareas registradas.</p>';
}

function renderConsumptions(consumptions: ConsumoRepuesto[]): string {
  return consumptions.length ? `<div class="technical-log-list">${consumptions.map((item) => `<article class="technical-log-item"><strong>${escapeHtml(item.repuesto)} · ${item.cantidad} unidad(es)</strong><small class="muted">${formatDate(item.fecha)} · ${escapeHtml(item.usuarioNombre)}</small><p>Precio unitario: ${formatMoney(item.precioUnitario)}</p></article>`).join('')}</div>` : '<p class="muted">Todavía no hay consumos registrados.</p>';
}

function showFormError(form: HTMLFormElement, error: unknown): void {
  const element = form.querySelector('.form-alert') as HTMLElement;
  element.textContent = error instanceof Error ? error.message : 'No se pudo completar la operación.';
  element.hidden = false;
}

function showError(panel: HTMLElement, message: string): void {
  panel.className = 'state-box';
  panel.textContent = message;
}

function formatStatus(status: string): string {
  return status.replaceAll('_', ' ').toLowerCase().replace(/(^| )\w/g, (letter) => letter.toUpperCase());
}

function statusTone(status: OrdenTrabajo['estado']): 'success' | 'warning' | 'danger' | 'info' | 'neutral' {
  if (status === 'CANCELADA') return 'danger';
  if (status === 'LISTA_PARA_RETIRAR') return 'warning';
  if (status === 'ENTREGADA' || status === 'FINALIZADA') return 'success';
  if (status === 'EN_REPARACION' || status === 'APROBADA') return 'info';
  return 'neutral';
}

function formatDate(value: string): string {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString('es-AR');
}

function formatMoney(value: number): string {
  return new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' }).format(value);
}

function escapeHtml(value: string): string {
  return value.replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;').replaceAll('"', '&quot;').replaceAll('\'', '&#039;');
}
