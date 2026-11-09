import { renderAppLayout } from '../../components/layout/AppLayout';
import { renderTable } from '../../components/ui/Table';
import { listarClientes } from '../../services/clientesService';
import { listarEquiposDeCliente } from '../../services/equiposService';
import { asignarTecnico, cambiarEstadoOrden, cancelarOrden, crearOrden, listarOrdenes } from '../../services/ordenesService';
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
    return order.tecnicoId ? `#${order.tecnicoId}` : 'Sin asignar';
  }
  return `<select class="form-input technician-select" data-order-technician="${order.id}" aria-label="Asignar técnico">
    <option value="">${order.tecnicoId ? `Técnico #${order.tecnicoId}` : 'Sin asignar'}</option>
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
