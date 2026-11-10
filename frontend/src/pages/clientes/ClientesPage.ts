import { renderAppLayout } from '../../components/layout/AppLayout';
import { renderBadge } from '../../components/ui/Badge';
import { renderTable } from '../../components/ui/Table';
import { router } from '../../router/router';
import { listarClientes } from '../../services/clientesService';
import type { Cliente } from '../../types/cliente';
import { escapeHtml } from '../../utils/format';

export const ClientesPage = {
  render(): HTMLElement {
    const content = document.createElement('div');
    content.innerHTML = `
      <div class="page-heading">
        <div><p class="muted">Gestión de personas</p><h2>Clientes</h2></div>
        <button class="btn btn-primary" id="new-client" type="button">+ Nuevo cliente</button>
      </div>
      <section class="panel">
        <div class="toolbar">
          <input class="form-input search-input" aria-label="Buscar cliente" placeholder="Buscar por nombre o contacto..." />
        </div>
      </section>
      <section class="panel table-panel"></section>
    `;
    content.querySelector('#new-client')?.addEventListener(
      'click',
      () => router.navigate('/usuarios/nuevo?rol=CLIENTE&origen=clientes'),
    );

    const tablePanel = content.querySelector('.table-panel') as HTMLElement;
    const searchInput = content.querySelector('.search-input') as HTMLInputElement;
    const state = document.createElement('div');
    state.className = 'table-state';
    state.textContent = 'Cargando clientes...';
    tablePanel.appendChild(state);

    let clients: Cliente[] = [];
    const updateTable = () => {
      const query = searchInput.value.trim().toLowerCase();
      const filteredClients = clients.filter((client) => {
        const searchableText = [
          client.nombre,
          client.apellido,
          client.email ?? '',
          client.telefono ?? '',
        ].join(' ').toLowerCase();

        return !query || searchableText.includes(query);
      });

      tablePanel.replaceChildren(renderClientsTable(filteredClients));
    };

    searchInput.addEventListener('input', updateTable);
    void loadClients(tablePanel, (loadedClients) => {
      clients = loadedClients;
      updateTable();
    });

    return renderAppLayout(content, 'Clientes');
  },
};

async function loadClients(
  container: HTMLElement,
  onLoaded: (clients: Cliente[]) => void,
): Promise<void> {
  try {
    onLoaded(await listarClientes());
  } catch (error) {
    container.replaceChildren();
    container.className = 'state-box table-state';
    container.textContent = error instanceof Error
      ? error.message
      : 'No se pudieron cargar los clientes.';
  }
}

function renderClientsTable(clients: Cliente[]): HTMLElement {
  return renderTable(
    ['Cliente', 'Contacto', 'Cuenta'],
    clients.map((client) => [
      `<a class="text-link" href="#/clientes/detalle?id=${client.id}"><strong>${escapeHtml(client.apellido)}, ${escapeHtml(client.nombre)}</strong></a>`,
      escapeHtml(client.email ?? client.telefono ?? 'Sin contacto'),
      renderBadge(
        client.usuarioId ? 'Vinculada' : 'Sin cuenta',
        client.usuarioId ? 'success' : 'neutral',
      ).outerHTML,
    ]),
  );
}
