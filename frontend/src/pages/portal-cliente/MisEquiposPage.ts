import { renderAppLayout } from '../../components/layout/AppLayout';
import { listarMisEquipos } from '../../services/equiposService';
import type { Equipo } from '../../types/equipo';

export const MisEquiposPage = {
  render(): HTMLElement {
    const content = document.createElement('div');
    content.innerHTML = `
      <div class="page-heading"><div><p class="muted">Espacio del cliente</p><h2>Mis equipos</h2></div></div>
      <section class="panel"><div class="portal-equipment-content">Cargando equipos...</div></section>
    `;
    const container = content.querySelector('.portal-equipment-content') as HTMLElement;
    void loadEquipment(container);
    return renderAppLayout(content, 'Mis equipos');
  },
};

async function loadEquipment(container: HTMLElement): Promise<void> {
  try {
    renderEquipment(container, await listarMisEquipos());
  } catch (error) {
    container.className = 'state-box';
    container.textContent = error instanceof Error ? error.message : 'No se pudieron cargar tus equipos.';
  }
}

function renderEquipment(container: HTMLElement, equipment: Equipo[]): void {
  if (equipment.length === 0) {
    container.innerHTML = '<div class="state-box"><h3>No tenés equipos registrados</h3><p>Cuando ingreses un equipo, aparecerá aquí.</p></div>';
    return;
  }
  const list = document.createElement('div');
  list.className = 'equipment-list';
  list.innerHTML = equipment.map((item) => `
    <article class="equipment-card">
      <div><h3>${escapeHtml([item.tipo, item.marca, item.modelo].filter(Boolean).join(' '))}</h3>
      <p class="muted">${item.numeroSerie ? `N° de serie: ${escapeHtml(item.numeroSerie)}` : 'Número de serie no informado'}</p></div>
      ${item.observaciones ? `<p>${escapeHtml(item.observaciones)}</p>` : ''}
    </article>
  `).join('');
  container.replaceChildren(list);
}

function escapeHtml(value: string): string {
  return value.replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;').replaceAll('\'', '&#039;');
}
