import { renderAppLayout } from '../../components/layout/AppLayout';
import { obtenerMiPerfil } from '../../services/clientesService';
import type { Cliente } from '../../types/cliente';

export const MiCuentaPage = {
  render(): HTMLElement {
    const content = document.createElement('div');
    content.innerHTML = `
      <div class="page-heading"><div><p class="muted">Espacio del cliente</p><h2>Mi cuenta</h2></div></div>
      <section class="panel portal-profile"><div class="panel-header"><div><h3>Mis datos</h3><p class="muted">Información de tu cuenta</p></div></div><div class="portal-profile-content">Cargando datos...</div></section>
    `;
    const container = content.querySelector('.portal-profile-content') as HTMLElement;
    void loadProfile(container);
    return renderAppLayout(content, 'Mi cuenta');
  },
};

async function loadProfile(container: HTMLElement): Promise<void> {
  try {
    renderProfile(container, await obtenerMiPerfil());
  } catch (error) {
    container.className = 'state-box';
    container.textContent = error instanceof Error ? error.message : 'No se pudieron cargar tus datos.';
  }
}

function renderProfile(container: HTMLElement, client: Cliente): void {
  container.innerHTML = `
    <dl class="details-list">
      <div><dt>Nombre completo</dt><dd>${escapeHtml(`${client.nombre} ${client.apellido}`)}</dd></div>
      <div><dt>DNI</dt><dd>${escapeHtml(client.dni ?? 'No informado')}</dd></div>
      <div><dt>Email</dt><dd>${escapeHtml(client.email ?? 'No informado')}</dd></div>
      <div><dt>Teléfono</dt><dd>${escapeHtml(client.telefono ?? 'No informado')}</dd></div>
      <div><dt>Dirección</dt><dd>${escapeHtml(client.direccion ?? 'No informada')}</dd></div>
    </dl>
  `;
}

function escapeHtml(value: string): string {
  return value.replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;').replaceAll('\'', '&#039;');
}
