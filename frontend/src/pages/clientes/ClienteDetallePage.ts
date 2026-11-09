import { renderAppLayout } from '../../components/layout/AppLayout';
import { obtenerCliente } from '../../services/clientesService';
import {
  actualizarEquipo,
  crearEquipo,
  listarEquiposDeCliente,
  type EquipoRequest,
} from '../../services/equiposService';
import type { Cliente } from '../../types/cliente';
import type { Equipo } from '../../types/equipo';

export const ClienteDetallePage = {
  render(): HTMLElement {
    const content = document.createElement('div');
    content.innerHTML = `
      <div class="page-heading">
        <div><p class="muted">Gestión de personas</p><h2>Detalle del cliente</h2></div>
        <a class="btn btn-secondary" href="#/clientes">Volver a clientes</a>
      </div>
      <section class="panel client-detail-panel">
        <div class="client-detail-content">Cargando cliente...</div>
      </section>
    `;
    const container = content.querySelector('.client-detail-content') as HTMLElement;
    const id = Number(new URLSearchParams(window.location.hash.split('?')[1] ?? '').get('id'));
    if (!Number.isInteger(id) || id < 1) {
      showError(container, 'El cliente seleccionado no es válido.');
      return renderAppLayout(content, 'Detalle del cliente');
    }

    void loadClient(container, id);
    return renderAppLayout(content, 'Detalle del cliente');
  },
};

async function loadClient(container: HTMLElement, clientId: number): Promise<void> {
  try {
    const [client, equipment] = await Promise.all([
      obtenerCliente(clientId),
      listarEquiposDeCliente(clientId),
    ]);
    renderClient(container, client, equipment);
  } catch (error) {
    showError(container, error instanceof Error ? error.message : 'No se pudo cargar el cliente.');
  }
}

function renderClient(container: HTMLElement, client: Cliente, equipment: Equipo[]): void {
  container.innerHTML = `
    <div class="client-detail-header">
      <div><p class="muted">Cliente #${client.id}</p><h3>${escapeHtml(`${client.apellido}, ${client.nombre}`)}</h3></div>
      <span class="badge badge-${client.usuarioId ? 'success' : 'neutral'}">${client.usuarioId ? 'Cuenta vinculada' : 'Sin cuenta'}</span>
    </div>
    <dl class="details-list client-details-list">
      <div><dt>Email</dt><dd>${escapeHtml(client.email ?? 'No informado')}</dd></div>
      <div><dt>Teléfono</dt><dd>${escapeHtml(client.telefono ?? 'No informado')}</dd></div>
      <div><dt>DNI</dt><dd>${escapeHtml(client.dni ?? 'No informado')}</dd></div>
      <div><dt>Dirección</dt><dd>${escapeHtml(client.direccion ?? 'No informada')}</dd></div>
    </dl>
    <div class="client-equipment-header"><h3>Equipos</h3><button class="btn btn-primary" type="button" id="add-equipment">+ Agregar equipo</button></div>
    <div class="client-equipment-list"></div>
    <form class="equipment-form form-panel" hidden></form>
  `;
  const list = container.querySelector('.client-equipment-list') as HTMLElement;
  const form = container.querySelector('.equipment-form') as HTMLFormElement;
  renderEquipmentList(list, equipment, form, client.id);
  container.querySelector('#add-equipment')?.addEventListener('click', () => {
    renderEquipmentForm(form, undefined, client.id, list, equipment);
  });
}

function renderEquipmentList(
  container: HTMLElement,
  equipment: Equipo[],
  form: HTMLFormElement,
  clientId: number,
): void {
  if (equipment.length === 0) {
    container.innerHTML = '<div class="state-box"><h3>Sin equipos registrados</h3><p>Agregá el equipo recibido para este cliente.</p></div>';
    return;
  }
  container.innerHTML = equipment.map((item) => `
    <article class="equipment-card">
      <div><h3>${escapeHtml([item.tipo, item.marca, item.modelo].filter(Boolean).join(' '))}</h3>
      <p class="muted">${item.numeroSerie ? `N° de serie: ${escapeHtml(item.numeroSerie)}` : 'Número de serie no informado'}</p></div>
      ${item.observaciones ? `<p>${escapeHtml(item.observaciones)}</p>` : ''}
      <button class="link-button" type="button" data-edit-equipment="${item.id}">Editar</button>
    </article>
  `).join('');
  container.querySelectorAll<HTMLButtonElement>('[data-edit-equipment]').forEach((button) => {
    const item = equipment.find((candidate) => candidate.id === Number(button.dataset.editEquipment));
    if (item) button.addEventListener('click', () => renderEquipmentForm(form, item, clientId, container, equipment));
  });
}

function renderEquipmentForm(
  form: HTMLFormElement,
  equipment: Equipo | undefined,
  clientId: number,
  list: HTMLElement,
  equipmentList: Equipo[],
): void {
  form.hidden = false;
  form.innerHTML = `
    <h3>${equipment ? 'Editar equipo' : 'Agregar equipo'}</h3>
    ${equipmentInput('tipo', 'Tipo', equipment?.tipo ?? '', true)}
    ${equipmentInput('marca', 'Marca', equipment?.marca ?? '')}
    ${equipmentInput('modelo', 'Modelo', equipment?.modelo ?? '')}
    ${equipmentInput('numeroSerie', 'Número de serie', equipment?.numeroSerie ?? '')}
    <div class="form-group"><label class="form-label" for="observaciones">Observaciones</label><textarea class="form-input" id="observaciones" name="observaciones">${escapeHtml(equipment?.observaciones ?? '')}</textarea></div>
    <div class="form-actions"><button class="btn btn-secondary" type="button" id="cancel-equipment">Cancelar</button><button class="btn btn-primary" type="submit">Guardar</button></div>
  `;
  form.querySelector('#cancel-equipment')?.addEventListener('click', () => { form.hidden = true; });
  form.onsubmit = async (event) => {
    event.preventDefault();
    const values = new FormData(form);
    const request: EquipoRequest = {
      tipo: String(values.get('tipo') ?? '').trim(),
      marca: String(values.get('marca') ?? '').trim(),
      modelo: String(values.get('modelo') ?? '').trim(),
      numeroSerie: String(values.get('numeroSerie') ?? '').trim(),
      observaciones: String(values.get('observaciones') ?? '').trim(),
    };
    if (!request.tipo) return;
    const submit = form.querySelector('button[type="submit"]') as HTMLButtonElement;
    submit.disabled = true;
    try {
      const saved = equipment
        ? await actualizarEquipo(equipment.id, request)
        : await crearEquipo(clientId, request);
      const next = equipmentList.some((item) => item.id === saved.id)
        ? equipmentList.map((item) => item.id === saved.id ? saved : item)
        : [...equipmentList, saved];
      equipmentList.splice(0, equipmentList.length, ...next);
      renderEquipmentList(list, equipmentList, form, clientId);
      form.hidden = true;
    } catch (error) {
      showError(form, error instanceof Error ? error.message : 'No se pudo guardar el equipo.');
      submit.disabled = false;
    }
  };
}

function equipmentInput(id: string, label: string, value: string, required = false): string {
  return `<div class="form-group"><label class="form-label" for="${id}">${label}</label><input class="form-input" id="${id}" name="${id}" value="${escapeHtml(value)}" ${required ? 'required' : ''}></div>`;
}

function showError(container: HTMLElement, message: string): void {
  container.className = 'state-box';
  container.textContent = message;
}

function escapeHtml(value: string): string {
  return value.replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;').replaceAll('\'', '&#039;');
}
