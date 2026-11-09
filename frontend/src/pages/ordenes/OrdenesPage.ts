import { renderAppLayout } from '../../components/layout/AppLayout';
import { renderTable } from '../../components/ui/Table';
import { listarClientes } from '../../services/clientesService';
import { listarEquiposDeCliente } from '../../services/equiposService';
import {
  asignarTecnico,
  actualizarDiagnostico,
  cambiarEstadoOrden,
  cancelarOrden,
  crearDiagnostico,
  crearOrden,
  crearRegistroTecnico,
  consumirRepuesto,
  listarConsumos,
  listarMisOrdenesTecnico,
  listarOrdenes,
  listarRegistrosTecnicos,
  obtenerDiagnosticoTecnico,
} from '../../services/ordenesService';
import { listarRepuestos } from '../../services/repuestosService';
import { listarTecnicos } from '../../services/usuariosService';
import type { EstadoOrden, OrdenTrabajo } from '../../types/orden';
import type { Usuario } from '../../types/usuario';
import { hasAnyRole } from '../../utils/authState';

const allOperationalStatuses: EstadoOrden[] = [
  'RECIBIDA',
  'EN_DIAGNOSTICO',
  'PENDIENTE_APROBACION',
  'APROBADA',
  'EN_REPARACION',
  'FINALIZADA',
  'LISTA_PARA_RETIRAR',
  'ENTREGADA',
];

export const OrdenesPage = {
  render(): HTMLElement {
    if (hasAnyRole(['TECNICO']) && !hasAnyRole(['ADMINISTRADOR', 'RECEPCIONISTA'])) {
      return renderTechnicianOrders();
    }
    const content = document.createElement('div');
    content.innerHTML = `
      <div class="page-heading"><div><p class="muted">Seguimiento del taller</p><h2>Órdenes de trabajo</h2></div><button class="btn btn-primary" id="new-order">+ Nueva orden</button></div>
      <section class="panel"><div class="toolbar"><input class="form-input search-input" aria-label="Buscar orden" placeholder="Buscar orden, cliente o equipo..." /></div></section>
      <section class="panel table-panel"><div class="table-state">Cargando órdenes...</div></section>
      <section class="panel order-form-panel" hidden></section>
      <div class="modal-backdrop" id="cancellation-modal" hidden>
        <section class="modal-dialog" role="dialog" aria-modal="true" aria-labelledby="cancellation-title">
          <h3 id="cancellation-title">Cancelar orden</h3>
          <p class="muted">Ingresá el motivo de la cancelación antes de confirmar.</p>
          <form id="cancellation-form" class="user-form">
            <div class="form-group">
              <label class="form-label" for="cancellation-reason">Motivo</label>
              <textarea class="form-input" id="cancellation-reason" name="motivo" rows="4" required></textarea>
            </div>
            <p class="form-alert" id="cancellation-error" hidden></p>
            <div class="form-actions">
              <button class="btn btn-secondary" type="button" id="cancel-cancellation">Volver</button>
              <button class="btn btn-danger" type="submit">Confirmar cancelación</button>
            </div>
          </form>
        </section>
      </div>
    `;
    const tablePanel = content.querySelector('.table-panel') as HTMLElement;
    const formPanel = content.querySelector('.order-form-panel') as HTMLElement;
    const cancellationModal = content.querySelector('#cancellation-modal') as HTMLElement;
    const search = content.querySelector('.search-input') as HTMLInputElement;
    let orders: OrdenTrabajo[] = [];
    let technicians: Usuario[] = [];

    const updateTable = () => {
      const query = search.value.trim().toLowerCase();
      const filtered = orders.filter((order) => {
        const text = `${order.id} ${order.cliente.apellido} ${order.cliente.nombre} ${order.equipo.tipo} ${order.equipo.marca} ${order.equipo.modelo}`.toLowerCase();
        return !query || text.includes(query);
      });
      const table = renderOrdersTable(filtered, technicians);
      table.querySelectorAll<HTMLSelectElement>('[data-order-status]').forEach((select) => {
        select.addEventListener('change', () => {
          const nextStatus = select.value as EstadoOrden;
          if (!nextStatus) return;
          const orderId = Number(select.dataset.orderStatus);
          if (nextStatus === 'CANCELADA') {
            void requestCancellation(cancellationModal, orderId)
              .then(() => loadOrders(tablePanel, (loaded) => { orders = loaded; updateTable(); }))
              .catch((error) => {
                if (error instanceof Error && error.message !== 'CANCELACION_CANCELADA') {
                  showTableError(tablePanel, error.message);
                }
                select.value = '';
              });
            return;
          }
          select.disabled = true;
          void cambiarEstadoOrden(orderId, nextStatus)
            .then(() => loadOrders(tablePanel, (loaded) => { orders = loaded; updateTable(); }))
            .catch((error) => {
              showTableError(tablePanel, error instanceof Error ? error.message : 'No se pudo cambiar el estado.');
              select.disabled = false;
              select.value = '';
            });
        });
      });
      table.querySelectorAll<HTMLSelectElement>('[data-order-technician]').forEach((select) => {
        select.addEventListener('change', () => {
          const technicianId = Number(select.value);
          const orderId = Number(select.dataset.orderTechnician);
          if (!technicianId) return;
          select.disabled = true;
          void asignarTecnico(orderId, technicianId)
            .then(() => loadOrders(tablePanel, (loaded) => { orders = loaded; updateTable(); }))
            .catch((error) => {
              showTableError(tablePanel, error instanceof Error ? error.message : 'No se pudo asignar el técnico.');
              select.disabled = false;
            });
        });
      });
      tablePanel.replaceChildren(table);
    };

    function renderTechnicianOrders(): HTMLElement {
      const content = document.createElement('div');
      content.innerHTML = `
        <div class="page-heading"><div><p class="muted">Trabajo asignado</p><h2>Mis órdenes</h2></div></div>
        <section class="panel"><div class="toolbar"><input class="form-input search-input" aria-label="Buscar orden" placeholder="Buscar por orden, cliente o equipo..." /></div></section>
        <section class="technical-orders"><div class="table-state">Cargando órdenes asignadas...</div></section>
        <div class="modal-backdrop technical-log-modal" id="technical-log-modal" hidden>
          <section class="modal-dialog" role="dialog" aria-modal="true" aria-labelledby="technical-log-title">
            <div class="panel-header"><h3 id="technical-log-title">Registro de tarea</h3><button class="btn btn-secondary" type="button" id="close-technical-log">Cerrar</button></div>
            <div id="technical-log-content"><div class="table-state">Cargando registros...</div></div>
          </section>
        </div>
      `;
      const list = content.querySelector('.technical-orders') as HTMLElement;
      const search = content.querySelector('.search-input') as HTMLInputElement;
      const technicalLogModal = content.querySelector('#technical-log-modal') as HTMLElement;
      technicalLogModal.querySelector('#close-technical-log')?.addEventListener('click', () => { technicalLogModal.hidden = true; });
      let orders: OrdenTrabajo[] = [];

      const render = () => {
        const query = search.value.trim().toLowerCase();
        const filtered = orders.filter((order) => {
          const text = `${order.id} ${order.cliente.nombre} ${order.cliente.apellido} ${order.equipo.tipo} ${order.equipo.marca} ${order.equipo.modelo}`.toLowerCase();
          return !query || text.includes(query);
        });
        list.innerHTML = filtered.length
          ? filtered.map((order) => renderTechnicalOrder(order)).join('')
          : '<section class="panel"><p class="muted">No tenés órdenes asignadas.</p></section>';
        bindTechnicalActions(list, () => load(), technicalLogModal);
      };

      const load = async () => {
        try {
          orders = await listarMisOrdenesTecnico();
          render();
        } catch (error) {
          list.className = 'state-box table-state';
          list.textContent = error instanceof Error ? error.message : 'No se pudieron cargar tus órdenes.';
        }
      };
      search.addEventListener('input', render);
      void load();
      return renderAppLayout(content, 'Mis órdenes');
    }

    function renderTechnicalOrder(order: OrdenTrabajo): string {
      return `
        <article class="panel technical-order-card" data-order-id="${order.id}">
          <div class="panel-header">
            <div><h3>Orden #${order.id}</h3><p class="muted">${escapeHtml(order.cliente.apellido)}, ${escapeHtml(order.cliente.nombre)} · ${escapeHtml([order.equipo.tipo, order.equipo.marca, order.equipo.modelo].filter(Boolean).join(' '))}</p></div>
            <div class="technical-order-meta">
              <span class="status-pill ${statusClass(order.estado)}">${formatStatus(order.estado)}</span>
              ${renderTechnicalMenu(order)}
            </div>
          </div>
          <p><strong>Problema informado:</strong> ${escapeHtml(order.problemaInicial)}</p>
          <div class="technical-detail" hidden></div>
        </article>
      `;
    }

    function renderTechnicalMenu(order: OrdenTrabajo): string {
      const actions: string[] = [];
      if (order.estado === 'RECIBIDA') {
        actions.push('<button type="button" class="action-menu-item" data-action="status" data-status="EN_DIAGNOSTICO">Iniciar diagnóstico</button>');
      }
      if (order.estado === 'EN_DIAGNOSTICO') {
        actions.push('<button type="button" class="action-menu-item" data-action="diagnosis">Registrar diagnóstico</button>');
        actions.push('<a class="action-menu-item" href="#/presupuestos">Preparar presupuesto</a>');
        actions.push('<button type="button" class="action-menu-item" data-action="status" data-status="EN_REPARACION">Iniciar reparación</button>');
      }
      if (order.estado === 'PENDIENTE_APROBACION') {
        actions.push('<span class="action-menu-note">Esperando decisión del cliente</span>');
      }
      if (order.estado === 'APROBADA') {
        actions.push('<button type="button" class="action-menu-item" data-action="status" data-status="EN_REPARACION">Iniciar reparación</button>');
      }
      if (order.estado === 'EN_REPARACION') {
        actions.push('<button type="button" class="action-menu-item" data-action="technical-log">Registrar tarea</button>');
        actions.push('<button type="button" class="action-menu-item" data-action="technical-log">Ver registros técnicos</button>');
        actions.push('<button type="button" class="action-menu-item" data-action="consume-part">Registrar consumo</button>');
        actions.push('<button type="button" class="action-menu-item action-menu-primary" data-action="status" data-status="FINALIZADA">Finalizar reparación</button>');
      }
      if (!actions.length) {
        return '<span class="muted">Sin acciones</span>';
      }
      return `<details class="action-menu"><summary>Acciones</summary><div class="action-menu-content">${actions.join('')}</div></details>`;
    }

    function bindTechnicalActions(container: HTMLElement, reload: () => void, technicalLogModal?: HTMLElement): void {
      container.querySelectorAll<HTMLButtonElement>('[data-action="status"]').forEach((button) => {
        button.addEventListener('click', () => {
          const card = button.closest<HTMLElement>('.technical-order-card');
          const id = Number(card?.dataset.orderId);
          button.disabled = true;
          void cambiarEstadoOrden(id, button.dataset.status as EstadoOrden)
            .then(reload)
            .catch((error) => {
              button.disabled = false;
              showTableError(container, error instanceof Error ? error.message : 'No se pudo actualizar el estado.');
            });
        });
      });
      container.querySelectorAll<HTMLButtonElement>('[data-action="diagnosis"]').forEach((button) => {
        button.addEventListener('click', () => {
          const card = button.closest<HTMLElement>('.technical-order-card');
          const id = Number(card?.dataset.orderId);
          const detail = card?.querySelector<HTMLElement>('.technical-detail');
          if (detail) renderDiagnosisForm(detail, id, reload);
        });
      });
      container.querySelectorAll<HTMLButtonElement>('[data-action="technical-log"]').forEach((button) => {
        button.addEventListener('click', () => {
          const card = button.closest<HTMLElement>('.technical-order-card');
          const id = Number(card?.dataset.orderId);
          const detail = card?.querySelector<HTMLElement>('.technical-detail');
          if (detail && technicalLogModal) renderTechnicalLogForm(detail, id, reload, technicalLogModal);
        });
      });
      container.querySelectorAll<HTMLButtonElement>('[data-action="consume-part"]').forEach((button) => {
        button.addEventListener('click', () => {
          const card = button.closest<HTMLElement>('.technical-order-card');
          const id = Number(card?.dataset.orderId);
          const detail = card?.querySelector<HTMLElement>('.technical-detail');
          if (detail) renderConsumptionForm(detail, id, reload);
        });
      });
    }

    function renderConsumptionForm(container: HTMLElement, orderId: number, reload: () => void): void {
      container.hidden = false;
      container.innerHTML = '<div class="table-state">Cargando repuestos...</div>';
      void Promise.all([listarRepuestos(), listarConsumos(orderId)]).then(([parts, consumptions]) => {
        container.innerHTML = `<h4>Consumos registrados</h4>${consumptions.map((item) => `<p class="muted">${formatDate(item.fecha)} · ${escapeHtml(item.repuesto)} · ${item.cantidad}</p>`).join('') || '<p class="muted">Todavía no hay consumos.</p>'}<form class="user-form"><div class="form-group"><label class="form-label">Repuesto</label><select class="form-input" name="repuestoId" required><option value="">Seleccioná un repuesto</option>${parts.map((part) => `<option value="${part.id}">${escapeHtml(part.nombre)} — stock ${part.stockActual}</option>`).join('')}</select></div><div class="form-group"><label class="form-label">Cantidad</label><input class="form-input" name="cantidad" type="number" min="1" required></div><button class="btn btn-primary" type="submit">Registrar consumo</button></form>`;
        const form = container.querySelector('form') as HTMLFormElement;
        form.addEventListener('submit', (event) => {
          event.preventDefault();
          const values = new FormData(form);
          void consumirRepuesto(orderId, { repuestoId: Number(values.get('repuestoId')), cantidad: Number(values.get('cantidad')) })
            .then(reload)
            .catch((error) => showFormError(form, error instanceof Error ? error.message : 'No se pudo registrar el consumo.'));
        });
      }).catch((error) => { container.textContent = error instanceof Error ? error.message : 'No se pudieron cargar los consumos.'; });
    }

    function renderDiagnosisForm(container: HTMLElement, orderId: number, reload: () => void): void {
      container.hidden = false;
      container.innerHTML = '<div class="table-state">Cargando diagnóstico...</div>';
      void obtenerDiagnosticoTecnico(orderId).catch(() => undefined).then((diagnosis) => {
        container.innerHTML = `<form class="user-form"><div class="form-group"><label class="form-label">Descripción de la falla</label><textarea class="form-input" name="descripcionFalla" required>${escapeHtml(diagnosis?.descripcionFalla ?? '')}</textarea></div><div class="form-group"><label class="form-label">Tareas requeridas</label><textarea class="form-input" name="tareasRequeridas" required>${escapeHtml(diagnosis?.tareasRequeridas ?? '')}</textarea></div><div class="form-actions"><button class="btn btn-primary" type="submit">${diagnosis ? 'Actualizar diagnóstico' : 'Guardar diagnóstico'}</button></div></form>`;
        const form = container.querySelector('form') as HTMLFormElement;
        form.addEventListener('submit', (event) => {
          event.preventDefault();
          const values = new FormData(form);
          const request = { descripcionFalla: String(values.get('descripcionFalla') ?? '').trim(), tareasRequeridas: String(values.get('tareasRequeridas') ?? '').trim() };
          const save = diagnosis ? actualizarDiagnostico(orderId, request) : crearDiagnostico(orderId, request);
          void save.then(reload).catch((error) => showFormError(form, error instanceof Error ? error.message : 'No se pudo guardar el diagnóstico.'));
        });
      });
    }

    function renderTechnicalLogForm(container: HTMLElement, orderId: number, reload: () => void, modal: HTMLElement): void {
      const modalContent = modal.querySelector('#technical-log-content') as HTMLElement;
      modal.hidden = false;
      modalContent.innerHTML = '<div class="table-state">Cargando registros...</div>';
      void listarRegistrosTecnicos(orderId).then((records) => {
        modalContent.innerHTML = `<h4>Historial de tareas</h4><div class="technical-log-list">${records.map((record) => `<article class="technical-log-item"><strong>${escapeHtml(record.tareaRealizada)}</strong><small class="muted">${formatDate(record.fecha)} · ${escapeHtml(record.usuarioNombre)}</small>${record.observacion ? `<p>${escapeHtml(record.observacion)}</p>` : ''}</article>`).join('') || '<p class="muted">Todavía no hay registros.</p>'}</div><form class="user-form"><div class="form-group"><label class="form-label" for="technical-task">Tarea realizada</label><input class="form-input" id="technical-task" name="tareaRealizada" required></div><div class="form-group"><label class="form-label" for="technical-observation">Observación</label><textarea class="form-input" id="technical-observation" name="observacion" rows="3"></textarea></div><div class="form-actions"><button class="btn btn-primary" type="submit">Guardar registro</button></div></form>`;
        const form = modalContent.querySelector('form') as HTMLFormElement;
        form.addEventListener('submit', (event) => {
          event.preventDefault();
          const values = new FormData(form);
          const submit = form.querySelector('button[type="submit"]') as HTMLButtonElement;
          submit.disabled = true;
          void crearRegistroTecnico(orderId, { tareaRealizada: String(values.get('tareaRealizada') ?? '').trim(), observacion: String(values.get('observacion') ?? '').trim() })
            .then(() => { reload(); renderTechnicalLogForm(container, orderId, reload, modal); })
            .catch((error) => { submit.disabled = false; showFormError(form, error instanceof Error ? error.message : 'No se pudo guardar la tarea.'); });
        });
      }).catch((error) => { modalContent.textContent = error instanceof Error ? error.message : 'No se pudieron cargar los registros.'; });
    }

    search.addEventListener('input', updateTable);
    content.querySelector('#new-order')?.addEventListener('click', () => {
      void renderOrderForm(formPanel, () => {
        void Promise.all([
          listarTecnicos(),
          listarOrdenes(),
        ]).then(([loadedTechnicians, loadedOrders]) => {
          technicians = loadedTechnicians.filter((technician) => technician.activo);
          orders = loadedOrders;
          updateTable();
        }).catch((error) => {
          tablePanel.className = 'state-box table-state';
          tablePanel.textContent = error instanceof Error ? error.message : 'No se pudieron cargar las órdenes.';
        });
      });
    });
    void Promise.all([listarTecnicos(), listarOrdenes()])
      .then(([loadedTechnicians, loadedOrders]) => {
        technicians = loadedTechnicians;
        orders = loadedOrders;
        updateTable();
      })
      .catch((error) => {
        tablePanel.className = 'state-box table-state';
        tablePanel.textContent = error instanceof Error ? error.message : 'No se pudieron cargar las órdenes.';
      });
    return renderAppLayout(content, 'Órdenes de trabajo');
  },
};

async function loadOrders(container: HTMLElement, onLoaded: (orders: OrdenTrabajo[]) => void): Promise<void> {
  try {
    onLoaded(await listarOrdenes());
  } catch (error) {
    container.className = 'state-box table-state';
    container.textContent = error instanceof Error ? error.message : 'No se pudieron cargar las órdenes.';
  }
}

function renderOrdersTable(orders: OrdenTrabajo[], technicians: Usuario[]): HTMLElement {
  return renderTable(
    ['Orden', 'Cliente', 'Equipo', 'Estado', 'Ingreso', 'Técnico'],
    orders.map((order) => [
      `<strong>#${order.id}</strong>`,
      `${escapeHtml(order.cliente.apellido)}, ${escapeHtml(order.cliente.nombre)}`,
      escapeHtml([order.equipo.tipo, order.equipo.marca, order.equipo.modelo].filter(Boolean).join(' ')),
      renderStatusAction(order),
      formatDate(order.fechaIngreso),
      renderTechnicianAction(order, technicians),
    ]),
  );
}

function renderTechnicianAction(order: OrdenTrabajo, technicians: Usuario[]): string {
  if (order.estado === 'ENTREGADA' || order.estado === 'CANCELADA') {
    const technician = technicians.find((item) => item.id === order.tecnicoId);
    return technician ? escapeHtml(technicianLabel(technician)) : 'Sin asignar';
  }
  const assignedTechnician = technicians.find((item) => item.id === order.tecnicoId);
  return `<select class="form-input technician-select" data-order-technician="${order.id}" aria-label="Asignar técnico">
    <option value="">${assignedTechnician ? escapeHtml(technicianLabel(assignedTechnician)) : 'Sin asignar'}</option>
    ${technicians.map((technician) => `<option value="${technician.id}" ${technician.id === order.tecnicoId ? 'selected' : ''}>${technicianLabel(technician)}</option>`).join('')}
  </select>`;
}

function technicianLabel(technician: Usuario): string {
  if (technician.email) {
    return technician.email.split('@')[0]
      .split(/[._-]+/)
      .filter(Boolean)
      .map((part) => part.charAt(0).toUpperCase() + part.slice(1).toLowerCase())
      .join(' ');
  }
  return technician.telefono ?? `Técnico #${technician.id}`;
}

function renderStatusAction(order: OrdenTrabajo): string {
  const nextStatuses: EstadoOrden[] = hasAnyRole(['ADMINISTRADOR'])
    ? allOperationalStatuses.filter((status) => status !== order.estado)
    : [];
  if (!hasAnyRole(['ADMINISTRADOR']) && order.estado === 'FINALIZADA') {
    nextStatuses.push('LISTA_PARA_RETIRAR');
  }
  if (!hasAnyRole(['ADMINISTRADOR']) && order.estado === 'LISTA_PARA_RETIRAR') {
    nextStatuses.push('ENTREGADA');
  }
  if (hasAnyRole(['ADMINISTRADOR'])
    && ['RECIBIDA', 'EN_DIAGNOSTICO', 'PENDIENTE_APROBACION', 'APROBADA'].includes(order.estado)) {
    nextStatuses.push('CANCELADA');
  }
  if (!nextStatuses.length) {
    return `<span class="status-pill ${statusClass(order.estado)}">${formatStatus(order.estado)}</span>`;
  }
  return `<select class="form-input table-action-select ${statusClass(order.estado)}" data-order-status="${order.id}" aria-label="Cambiar estado">
    <option value="">${formatStatus(order.estado)}</option>
    ${nextStatuses.map((status) => `<option class="${statusClass(status)}" value="${status}">${formatStatus(status)}</option>`).join('')}
  </select>`;
}

function requestCancellation(modal: HTMLElement, orderId: number): Promise<OrdenTrabajo> {
  const form = modal.querySelector('#cancellation-form') as HTMLFormElement;
  const reason = modal.querySelector('#cancellation-reason') as HTMLTextAreaElement;
  const error = modal.querySelector('#cancellation-error') as HTMLElement;
  reason.value = '';
  error.hidden = true;
  modal.hidden = false;
  reason.focus();

  return new Promise((resolve, reject) => {
    const cancelButton = modal.querySelector('#cancel-cancellation') as HTMLButtonElement;
    const submit = async (event: Event) => {
      event.preventDefault();
      const motivo = reason.value.trim();
      if (!motivo) {
        error.textContent = 'Ingresá un motivo para cancelar la orden.';
        error.hidden = false;
        return;
      }
      const submitButton = form.querySelector('button[type="submit"]') as HTMLButtonElement;
      submitButton.disabled = true;
      try {
        resolve(await cancelarOrden(orderId, motivo));
        closeCancellationModal(modal);
      } catch (requestError) {
        error.textContent = requestError instanceof Error
          ? requestError.message
          : 'No se pudo cancelar la orden.';
        error.hidden = false;
        submitButton.disabled = false;
        reject(requestError);
      }
      form.removeEventListener('submit', submit);
      cancelButton.removeEventListener('click', cancel);
    };
    const cancel = () => {
      form.removeEventListener('submit', submit);
      cancelButton.removeEventListener('click', cancel);
      closeCancellationModal(modal);
      reject(new Error('CANCELACION_CANCELADA'));
    };
    form.addEventListener('submit', submit);
    cancelButton.addEventListener('click', cancel);
  });
}

function closeCancellationModal(modal: HTMLElement): void {
  modal.hidden = true;
  const form = modal.querySelector('#cancellation-form') as HTMLFormElement;
  const submit = form.querySelector('button[type="submit"]') as HTMLButtonElement;
  submit.disabled = false;
}

function showTableError(container: HTMLElement, message: string): void {
  const error = document.createElement('p');
  error.className = 'form-alert';
  error.textContent = message;
  container.prepend(error);
}

async function renderOrderForm(container: HTMLElement, onCreated: () => void): Promise<void> {
  container.hidden = false;
  container.innerHTML = '<div class="table-state">Cargando clientes...</div>';
  try {
    const clients = await listarClientes();
    container.innerHTML = `
      <h3>Nueva orden de trabajo</h3>
      <form class="user-form" id="order-form">
        <div class="form-group"><label class="form-label" for="client">Cliente</label><select class="form-input" id="client" required><option value="">Seleccioná un cliente</option>${clients.map((client) => `<option value="${client.id}">${escapeHtml(`${client.apellido}, ${client.nombre}`)}</option>`).join('')}</select></div>
        <div class="form-group"><label class="form-label" for="equipment">Equipo</label><select class="form-input" id="equipment" required disabled><option value="">Seleccioná primero un cliente</option></select></div>
        <div class="form-group"><label class="form-label" for="problem">Problema informado</label><textarea class="form-input" id="problem" name="problemaInicial" required></textarea></div>
        <div class="form-actions"><button class="btn btn-secondary" type="button" id="cancel-order">Cancelar</button><button class="btn btn-primary" type="submit">Crear orden</button></div>
      </form>
    `;
    const form = container.querySelector('#order-form') as HTMLFormElement;
    const clientSelect = container.querySelector('#client') as HTMLSelectElement;
    const equipmentSelect = container.querySelector('#equipment') as HTMLSelectElement;
    clientSelect.addEventListener('change', async () => {
      equipmentSelect.disabled = true;
      equipmentSelect.innerHTML = '<option value="">Cargando equipos...</option>';
      const equipment = await listarEquiposDeCliente(Number(clientSelect.value));
      equipmentSelect.innerHTML = equipment.length
        ? `<option value="">Seleccioná un equipo</option>${equipment.map((item) => `<option value="${item.id}">${escapeHtml([item.tipo, item.marca, item.modelo].filter(Boolean).join(' '))}</option>`).join('')}`
        : '<option value="">El cliente no tiene equipos</option>';
      equipmentSelect.disabled = equipment.length === 0;
    });
    container.querySelector('#cancel-order')?.addEventListener('click', () => { container.hidden = true; });
    form.addEventListener('submit', async (event) => {
      event.preventDefault();
      const submit = form.querySelector('button[type="submit"]') as HTMLButtonElement;
      submit.disabled = true;
      try {
        const problemaInicial = String(new FormData(form).get('problemaInicial') ?? '').trim();
        if (!problemaInicial) {
          showFormError(form, 'Ingresá el problema informado por el cliente.');
          submit.disabled = false;
          return;
        }
        await crearOrden({
          equipoId: Number(equipmentSelect.value),
          problemaInicial,
        });
        container.hidden = true;
        onCreated();
      } catch (error) {
        showFormError(form, error instanceof Error ? error.message : 'No se pudo crear la orden.');
        submit.disabled = false;
      }
    });
  } catch (error) {
    container.textContent = error instanceof Error ? error.message : 'No se pudo cargar el formulario.';
  }
}

function showFormError(form: HTMLFormElement, message: string): void {
  const alert = document.createElement('p');
  alert.className = 'form-alert';
  alert.textContent = message;
  form.prepend(alert);
}

function formatStatus(status: EstadoOrden): string {
  return status.replaceAll('_', ' ').toLowerCase().replace(/(^| )\w/g, (letter) => letter.toUpperCase());
}

function statusClass(status: EstadoOrden): string {
  return `status-${status.toLowerCase()}`;
}

function formatDate(value: string): string {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleDateString('es-AR');
}

function escapeHtml(value: string): string {
  return value.replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;').replaceAll('\'', '&#039;');
}
