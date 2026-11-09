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
import { listarMisPresupuestosTecnico } from '../../services/presupuestosService';
import type { EstadoPresupuesto } from '../../types/presupuesto';
import { listarTecnicos } from '../../services/usuariosService';
import type { EstadoOrden, OrdenTrabajo } from '../../types/orden';
import type { Usuario } from '../../types/usuario';
import { hasAnyRole } from '../../utils/authState';
import { router } from '../../router/router';
import { isHybridTechnician, wantsTechnicianView } from '../../utils/technicianView';

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

// Estado del presupuesto de cada orden del técnico: define si puede iniciar la reparación.
let budgetStatusByOrder = new Map<number, EstadoPresupuesto>();

export const OrdenesPage = {
  render(): HTMLElement {
    if (wantsTechnicianView()) {
      return renderTechnicianOrders();
    }
    const content = document.createElement('div');
    content.innerHTML = `
      <div class="page-heading"><div><p class="muted">Seguimiento del taller</p><h2>Órdenes de trabajo</h2></div><div class="form-actions">${isHybridTechnician() ? '<a class="btn btn-secondary" href="#/ordenes?vista=tecnico">Mis órdenes asignadas</a>' : ''}<button class="btn btn-primary" id="new-order">+ Nueva orden</button></div></div>
      <section class="panel"><div class="toolbar"><input class="form-input search-input" aria-label="Buscar orden" placeholder="Buscar orden, cliente o equipo..." /><select class="form-input filter-select" id="status-filter" aria-label="Filtrar por estado"><option value="">Todos los estados</option>${allOperationalStatuses.concat('CANCELADA').map((status) => `<option class="${statusClass(status)}" value="${status}">${formatStatus(status)}</option>`).join('')}</select></div></section>
      <section class="panel table-panel"><div class="table-state">Cargando órdenes...</div></section>
      <section class="panel order-form-panel" hidden></section>
      <div class="modal-backdrop" id="reassignment-modal" hidden>
        <section class="modal-dialog" role="dialog" aria-modal="true" aria-labelledby="reassignment-title">
          <h3 id="reassignment-title">Cambiar técnico responsable</h3>
          <p class="muted">La orden ya tiene un técnico asignado. Indicá el motivo del cambio: quedará registrado en el historial.</p>
          <form id="reassignment-form" class="user-form">
            <div class="form-group">
              <label class="form-label" for="reassignment-reason">Motivo</label>
              <textarea class="form-input" id="reassignment-reason" name="motivo" rows="4" maxlength="500" required></textarea>
            </div>
            <p class="form-alert" id="reassignment-error" hidden></p>
            <div class="form-actions">
              <button class="btn btn-secondary" type="button" id="cancel-reassignment">Volver</button>
              <button class="btn btn-primary" type="submit">Confirmar cambio</button>
            </div>
          </form>
        </section>
      </div>
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
    const reassignmentModal = content.querySelector('#reassignment-modal') as HTMLElement;
    const search = content.querySelector('.search-input') as HTMLInputElement;
    const statusFilter = content.querySelector('#status-filter') as HTMLSelectElement;
    statusFilter.addEventListener('change', () => {
      statusFilter.className = `form-input filter-select${statusFilter.value ? ` ${statusClass(statusFilter.value as EstadoOrden)}` : ''}`;
      updateTable();
    });
    let orders: OrdenTrabajo[] = [];
    let technicians: Usuario[] = [];

    const updateTable = () => {
      const query = search.value.trim().toLowerCase();
      const selectedStatus = statusFilter.value as EstadoOrden | '';
      const filtered = orders.filter((order) => {
        const text = `${order.id} ${order.cliente.apellido} ${order.cliente.nombre} ${order.equipo.tipo} ${order.equipo.marca} ${order.equipo.modelo}`.toLowerCase();
        return (!query || text.includes(query)) && (!selectedStatus || order.estado === selectedStatus);
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
          const currentTechnicianId = Number(select.dataset.currentTechnician);
          if (currentTechnicianId) {
            // Cambiar un técnico ya asignado exige un motivo y solo lo hace el Administrador.
            if (technicianId === currentTechnicianId) return;
            void requestReassignment(reassignmentModal, orderId, technicianId)
              .then(() => loadOrders(tablePanel, (loaded) => { orders = loaded; updateTable(); }))
              .catch(() => updateTable());
            return;
          }
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
        <div class="page-heading"><div><p class="muted">Trabajo asignado</p><h2>Mis órdenes</h2></div>${isHybridTechnician() ? '<a class="btn btn-secondary" href="#/ordenes">Ver todas las órdenes</a>' : ''}</div>
        <section class="panel"><div class="toolbar"><input class="form-input search-input" aria-label="Buscar orden" placeholder="Buscar por orden, cliente o equipo..." /></div></section>
        <section class="technical-orders"><div class="table-state">Cargando órdenes asignadas...</div></section>
        <div class="modal-backdrop" id="technical-task-modal" hidden>
          <section class="modal-dialog technical-action-modal" role="dialog" aria-modal="true" aria-labelledby="technical-task-title">
            <div class="section-heading"><h3 id="technical-task-title">Registrar tarea</h3><button class="btn btn-secondary" type="button" data-close-technical-modal>Cancelar</button></div>
            <div id="technical-task-content"></div>
          </section>
        </div>
        <div class="modal-backdrop" id="technical-consumption-modal" hidden>
          <section class="modal-dialog technical-action-modal" role="dialog" aria-modal="true" aria-labelledby="technical-consumption-title">
            <div class="section-heading"><h3 id="technical-consumption-title">Registrar consumo</h3><button class="btn btn-secondary" type="button" data-close-technical-modal>Cancelar</button></div>
            <div id="technical-consumption-content"></div>
          </section>
        </div>
      `;
      const list = content.querySelector('.technical-orders') as HTMLElement;
      const search = content.querySelector('.search-input') as HTMLInputElement;
      const taskModal = content.querySelector('#technical-task-modal') as HTMLElement;
      const consumptionModal = content.querySelector('#technical-consumption-modal') as HTMLElement;
      content.querySelectorAll<HTMLElement>('[data-close-technical-modal]').forEach((button) => {
        button.addEventListener('click', () => { (button.closest('.modal-backdrop') as HTMLElement).hidden = true; });
      });
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
        bindTechnicalActions(list, () => load(), taskModal, consumptionModal);
        list.querySelectorAll<HTMLElement>('.technical-order-card').forEach((card) => {
          card.addEventListener('click', (event) => {
            const target = event.target as HTMLElement;
            if (target.closest('button, a, details, select, input, textarea')) return;
            router.navigate(`/ordenes/detalle?id=${card.dataset.orderId}&vista=tecnico`);
          });
        });
      };

      const load = async () => {
        try {
          const [assignedOrders, budgets] = await Promise.all([
            listarMisOrdenesTecnico(),
            listarMisPresupuestosTecnico(),
          ]);
          orders = assignedOrders;
          budgetStatusByOrder = new Map(budgets.map((budget) => [budget.ordenId, budget.estado]));
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
        const budgetStatus = budgetStatusByOrder.get(order.id);
        if (!budgetStatus) {
          // Sin presupuesto: se presupuesta o se repara directamente (flujo sin presupuesto).
          actions.push(`<a class="action-menu-item" href="#/presupuestos?ordenId=${order.id}">Preparar presupuesto</a>`);
          actions.push('<button type="button" class="action-menu-item" data-action="status" data-status="EN_REPARACION">Iniciar reparación</button>');
        } else {
          // Con presupuesto: la reparación espera la aprobación del cliente.
          actions.push(`<a class="action-menu-item" href="#/presupuestos?ordenId=${order.id}">Editar presupuesto</a>`);
          actions.push('<span class="action-menu-note">Presupuesto en borrador: debe publicarse y aprobarlo el cliente antes de iniciar la reparación</span>');
        }
      }
      if (order.estado === 'PENDIENTE_APROBACION') {
        actions.push('<span class="action-menu-note">Esperando decisión del cliente</span>');
      }
      if (order.estado === 'APROBADA') {
        actions.push('<button type="button" class="action-menu-item" data-action="status" data-status="EN_REPARACION">Iniciar reparación</button>');
      }
      if (order.estado === 'EN_REPARACION') {
        actions.push('<button type="button" class="action-menu-item" data-action="technical-log">Registrar tarea</button>');
        actions.push('<button type="button" class="action-menu-item" data-action="technical-log-view">Ver registros técnicos</button>');
        actions.push('<button type="button" class="action-menu-item" data-action="consume-part">Registrar consumo</button>');
        actions.push('<button type="button" class="action-menu-item action-menu-primary" data-action="status" data-status="FINALIZADA">Finalizar reparación</button>');
      }
      if (!actions.length) {
        return '<span class="muted">Sin acciones</span>';
      }
      return `<details class="action-menu"><summary>Acciones</summary><div class="action-menu-content">${actions.join('')}</div></details>`;
    }

    function bindTechnicalActions(container: HTMLElement, reload: () => void, taskModal: HTMLElement, consumptionModal: HTMLElement): void {
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
          renderTechnicalTaskModal(taskModal, id, reload);
        });
        container.querySelectorAll<HTMLButtonElement>('[data-action="technical-log-view"]').forEach((button) => {
          button.addEventListener('click', () => {
            const card = button.closest<HTMLElement>('.technical-order-card');
            router.navigate(`/ordenes/detalle?id=${card?.dataset.orderId}&vista=tecnico`);
          });
        });
      });
      container.querySelectorAll<HTMLButtonElement>('[data-action="consume-part"]').forEach((button) => {
        button.addEventListener('click', () => {
          const card = button.closest<HTMLElement>('.technical-order-card');
          const id = Number(card?.dataset.orderId);
          renderTechnicalConsumptionModal(consumptionModal, id, reload);
        });
      });
    }

    function renderTechnicalTaskModal(modal: HTMLElement, orderId: number, reload: () => void): void {
      const content = modal.querySelector('#technical-task-content') as HTMLElement;
      modal.hidden = false;
      content.innerHTML = '<div class="table-state">Cargando registros...</div>';
      void listarRegistrosTecnicos(orderId).then((records) => {
        content.innerHTML = `<div class="technical-log-list">${records.map((record) => `<article class="technical-log-item"><strong>${escapeHtml(record.tareaRealizada)}</strong><small class="muted">${formatDate(record.fecha)} · ${escapeHtml(record.usuarioNombre)}</small>${record.observacion ? `<p>${escapeHtml(record.observacion)}</p>` : ''}</article>`).join('') || '<p class="muted">Todavía no hay tareas registradas.</p>'}</div><form class="user-form"><div class="form-group"><label class="form-label" for="list-task-name">Tarea realizada</label><input class="form-input" id="list-task-name" name="tareaRealizada" required></div><div class="form-group"><label class="form-label" for="list-task-note">Observación</label><textarea class="form-input" id="list-task-note" name="observacion" rows="4"></textarea></div><p class="form-alert" hidden></p><div class="form-actions"><button class="btn btn-primary" type="submit">Guardar tarea</button></div></form>`;
        const form = content.querySelector('form') as HTMLFormElement;
        form.addEventListener('submit', (event) => {
          event.preventDefault();
          const values = new FormData(form);
          const submit = form.querySelector('button[type="submit"]') as HTMLButtonElement;
          submit.disabled = true;
          void crearRegistroTecnico(orderId, { tareaRealizada: String(values.get('tareaRealizada') ?? '').trim(), observacion: String(values.get('observacion') ?? '').trim() })
            .then(() => { modal.hidden = true; reload(); })
            .catch((error) => { submit.disabled = false; showFormError(form, error instanceof Error ? error.message : 'No se pudo registrar la tarea.'); });
        });
      }).catch((error) => { content.textContent = error instanceof Error ? error.message : 'No se pudieron cargar los registros.'; });
    }

    function renderTechnicalConsumptionModal(modal: HTMLElement, orderId: number, reload: () => void): void {
      const content = modal.querySelector('#technical-consumption-content') as HTMLElement;
      modal.hidden = false;
      content.innerHTML = '<div class="table-state">Cargando repuestos...</div>';
      void Promise.all([listarRepuestos(), listarConsumos(orderId)]).then(([parts, consumptions]) => {
        content.innerHTML = `${consumptions.length ? `<div class="technical-log-list">${consumptions.map((item) => `<article class="technical-log-item"><strong>${escapeHtml(item.repuesto)} · ${item.cantidad} unidad(es)</strong><small class="muted">${formatDate(item.fecha)} · ${escapeHtml(item.usuarioNombre)}</small><p>Precio unitario: ${formatMoney(item.precioUnitario)}</p></article>`).join('')}</div>` : '<p class="muted">Todavía no hay consumos registrados.</p>'}<form class="user-form"><div class="form-group"><label class="form-label" for="list-consumption-part">Repuesto</label><select class="form-input" id="list-consumption-part" name="repuestoId" required><option value="">Seleccioná un repuesto</option>${parts.map((part) => `<option value="${part.id}">${escapeHtml(part.nombre)} — stock ${part.stockActual}</option>`).join('')}</select></div><div class="form-group"><label class="form-label" for="list-consumption-quantity">Cantidad</label><input class="form-input" id="list-consumption-quantity" name="cantidad" type="number" min="1" required></div><p class="form-alert" hidden></p><div class="form-actions"><button class="btn btn-primary" type="submit">Guardar consumo</button></div></form>`;
        const form = content.querySelector('form') as HTMLFormElement;
        form.addEventListener('submit', (event) => {
          event.preventDefault();
          const values = new FormData(form);
          const submit = form.querySelector('button[type="submit"]') as HTMLButtonElement;
          submit.disabled = true;
          void consumirRepuesto(orderId, { repuestoId: Number(values.get('repuestoId')), cantidad: Number(values.get('cantidad')) })
            .then(() => { modal.hidden = true; reload(); })
            .catch((error) => { submit.disabled = false; showFormError(form, error instanceof Error ? error.message : 'No se pudo registrar el consumo.'); });
        });
      }).catch((error) => { content.textContent = error instanceof Error ? error.message : 'No se pudieron cargar los consumos.'; });
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
      `<a class="text-link" href="#/ordenes/detalle?id=${order.id}" title="Ver detalle de la orden"><strong>#${order.id}</strong></a>`,
      `${escapeHtml(order.cliente.apellido)}, ${escapeHtml(order.cliente.nombre)}`,
      escapeHtml([order.equipo.tipo, order.equipo.marca, order.equipo.modelo].filter(Boolean).join(' ')),
      renderStatusAction(order),
      formatDate(order.fechaIngreso),
      renderTechnicianAction(order, technicians),
    ]),
  );
}

function renderTechnicianAction(order: OrdenTrabajo, technicians: Usuario[]): string {
  const assignedTechnician = technicians.find((item) => item.id === order.tecnicoId);
  const assignedLabel = assignedTechnician
    ? escapeHtml(technicianLabel(assignedTechnician))
    : order.tecnicoId ? `Técnico #${order.tecnicoId}` : 'Sin asignar';
  // Terminada la reparación el responsable ya no cambia (misma regla que el backend).
  const closed = ['FINALIZADA', 'LISTA_PARA_RETIRAR', 'ENTREGADA', 'CANCELADA'].includes(order.estado);
  if (closed) {
    return assignedLabel;
  }
  // Asignar por primera vez: Administración y Recepción. Cambiar un técnico ya asignado: solo el Administrador.
  if (order.tecnicoId && !hasAnyRole(['ADMINISTRADOR'])) {
    return `<span title="Solo el Administrador puede cambiar el técnico ya asignado">${assignedLabel}</span>`;
  }
  return `<select class="form-input technician-select" data-order-technician="${order.id}" data-current-technician="${order.tecnicoId ?? ''}" aria-label="${order.tecnicoId ? 'Cambiar técnico' : 'Asignar técnico'}">
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
  // Mismas reglas que el backend: Administración y Recepción solo realizan
  // las transiciones de entrega. Las técnicas son del Técnico asignado y las
  // de presupuesto ocurren al publicarlo y cuando el cliente decide.
  const nextStatuses: EstadoOrden[] = [];
  if (order.estado === 'FINALIZADA') {
    nextStatuses.push('LISTA_PARA_RETIRAR');
  }
  if (order.estado === 'LISTA_PARA_RETIRAR') {
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

/**
 * Pide el motivo y cambia el técnico de una orden que ya tenía uno asignado.
 * Se resuelve al guardarse; se rechaza si la persona vuelve atrás.
 * Si el backend rechaza el cambio, el modal queda abierto con el mensaje.
 */
function requestReassignment(modal: HTMLElement, orderId: number, technicianId: number): Promise<OrdenTrabajo> {
  const form = modal.querySelector('#reassignment-form') as HTMLFormElement;
  const reason = modal.querySelector('#reassignment-reason') as HTMLTextAreaElement;
  const error = modal.querySelector('#reassignment-error') as HTMLElement;
  const cancelButton = modal.querySelector('#cancel-reassignment') as HTMLButtonElement;
  const submitButton = form.querySelector('button[type="submit"]') as HTMLButtonElement;
  reason.value = '';
  error.hidden = true;
  submitButton.disabled = false;
  modal.hidden = false;
  reason.focus();

  return new Promise((resolve, reject) => {
    const cleanup = () => {
      form.removeEventListener('submit', onSubmit);
      cancelButton.removeEventListener('click', onCancel);
      modal.hidden = true;
    };
    const onSubmit = async (event: Event) => {
      event.preventDefault();
      const motivo = reason.value.trim();
      if (!motivo) {
        error.textContent = 'Ingresá el motivo del cambio de técnico.';
        error.hidden = false;
        return;
      }
      submitButton.disabled = true;
      error.hidden = true;
      try {
        const updated = await asignarTecnico(orderId, technicianId, motivo);
        cleanup();
        resolve(updated);
      } catch (requestError) {
        error.textContent = requestError instanceof Error ? requestError.message : 'No se pudo cambiar el técnico.';
        error.hidden = false;
        submitButton.disabled = false;
      }
    };
    const onCancel = () => {
      cleanup();
      reject(new Error('REASIGNACION_CANCELADA'));
    };
    form.addEventListener('submit', onSubmit);
    cancelButton.addEventListener('click', onCancel);
  });
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
        <div class="form-group client-autocomplete"><label class="form-label" for="client">Cliente</label><input class="form-input" id="client" autocomplete="off" placeholder="Escribí nombre o apellido" required><div class="client-autocomplete-results" id="client-options" hidden></div><input type="hidden" id="client-id"></div>
        <div class="form-group"><label class="form-label" for="equipment">Equipo</label><select class="form-input" id="equipment" required disabled><option value="">Seleccioná primero un cliente</option></select></div>
        <div class="form-group"><label class="form-label" for="problem">Problema informado</label><textarea class="form-input" id="problem" name="problemaInicial" required></textarea></div>
        <div class="form-actions"><button class="btn btn-secondary" type="button" id="cancel-order">Cancelar</button><button class="btn btn-primary" type="submit">Crear orden</button></div>
      </form>
    `;
    const form = container.querySelector('#order-form') as HTMLFormElement;
    const clientInput = container.querySelector('#client') as HTMLInputElement;
    const clientIdInput = container.querySelector('#client-id') as HTMLInputElement;
    const clientOptions = container.querySelector('#client-options') as HTMLElement;
    const equipmentSelect = container.querySelector('#equipment') as HTMLSelectElement;
    const loadClientEquipment = async (clientId: number): Promise<void> => {
      equipmentSelect.disabled = true;
      equipmentSelect.innerHTML = '<option value="">Cargando equipos...</option>';
      const equipment = await listarEquiposDeCliente(clientId);
      equipmentSelect.innerHTML = equipment.length
        ? `<option value="">Seleccioná un equipo</option>${equipment.map((item) => `<option value="${item.id}">${escapeHtml([item.tipo, item.marca, item.modelo].filter(Boolean).join(' '))}</option>`).join('')}<option value="nuevo">+ Crear nuevo equipo</option>`
        : '<option value="">El cliente no tiene equipos</option><option value="nuevo">+ Crear nuevo equipo</option>';
      equipmentSelect.disabled = false;
    };
    clientInput.addEventListener('input', () => {
      const value = clientInput.value.trim().toLowerCase();
      const matches = clients
        .filter((client) => {
          const label = `${client.apellido}, ${client.nombre}`.toLowerCase();
          return !value || label.includes(value);
        })
      clientOptions.innerHTML = matches.length
        ? matches.map((client) => `<button type="button" class="client-autocomplete-option" data-client-id="${client.id}"><strong>${escapeHtml(`${client.apellido}, ${client.nombre}`)}</strong><small>${escapeHtml(client.email ?? client.telefono ?? 'Sin contacto')}</small></button>`).join('')
        : '<p class="client-autocomplete-empty">No se encontraron clientes.</p>';
      clientOptions.hidden = false;
      const client = clients.find((item) => `${item.apellido}, ${item.nombre}`.toLowerCase() === value);
      clientIdInput.value = client ? String(client.id) : '';
      equipmentSelect.disabled = true;
      equipmentSelect.innerHTML = '<option value="">Seleccioná un cliente válido</option>';
      if (client) void loadClientEquipment(client.id);
    });
    clientInput.addEventListener('focus', () => {
      clientInput.dispatchEvent(new Event('input'));
    });
    clientOptions.addEventListener('click', (event) => {
      const option = (event.target as HTMLElement).closest<HTMLButtonElement>('[data-client-id]');
      if (!option) return;
      const client = clients.find((item) => item.id === Number(option.dataset.clientId));
      if (!client) return;
      clientInput.value = `${client.apellido}, ${client.nombre}`;
      clientIdInput.value = String(client.id);
      clientOptions.hidden = true;
      void loadClientEquipment(client.id);
    });
    document.addEventListener('click', (event) => {
      if (!container.querySelector('.client-autocomplete')?.contains(event.target as Node)) clientOptions.hidden = true;
    });
    equipmentSelect.addEventListener('change', () => {
      if (equipmentSelect.value === 'nuevo' && clientIdInput.value) {
        window.location.hash = `/clientes/detalle?id=${clientIdInput.value}&nuevoEquipo=1`;
      }
    });
    container.querySelector('#cancel-order')?.addEventListener('click', () => { container.hidden = true; });
    form.addEventListener('submit', async (event) => {
      event.preventDefault();
      const submit = form.querySelector('button[type="submit"]') as HTMLButtonElement;
      submit.disabled = true;
      try {
        const problemaInicial = String(new FormData(form).get('problemaInicial') ?? '').trim();
        if (!clientIdInput.value) {
          showFormError(form, 'Seleccioná un cliente existente de la lista.');
          submit.disabled = false;
          return;
        }
        if (!equipmentSelect.value || equipmentSelect.value === 'nuevo') {
          showFormError(form, 'Seleccioná un equipo existente para crear la orden.');
          submit.disabled = false;
          return;
        }
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

function formatMoney(value: number): string {
  return new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' }).format(value);
}

function escapeHtml(value: string): string {
  return value.replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;').replaceAll('\'', '&#039;');
}
