import { renderAppLayout } from '../../components/layout/AppLayout';
import { listarMovimientos } from '../../services/repuestosService';
import { listarRepuestos } from '../../services/repuestosService';

export const DetalleInventarioPage = {
  render(): HTMLElement {
    const content = document.createElement('div');
    const id = Number(new URLSearchParams(window.location.hash.split('?')[1] ?? '').get('id'));
    content.innerHTML = `
      <div class="page-heading"><div><p class="muted">Inventario</p><h2>Detalle del repuesto</h2></div><a class="btn btn-secondary" href="#/inventario">Volver al inventario</a></div>
      <section class="panel inventory-detail-panel"><div class="table-state">Cargando detalle...</div></section>
    `;
    const panel = content.querySelector('.inventory-detail-panel') as HTMLElement;
    if (!Number.isInteger(id) || id <= 0) {
      panel.textContent = 'El repuesto solicitado no es válido.';
      return renderAppLayout(content, 'Detalle del repuesto');
    }
    void Promise.all([listarRepuestos(), listarMovimientos(id)]).then(([parts, movements]) => {
      const part = parts.find((item) => item.id === id);
      if (!part) {
        panel.textContent = 'No se encontró el repuesto.';
        return;
      }
      panel.innerHTML = `
        <div class="panel-header"><div><p class="muted">${escapeHtml(part.codigo)}</p><h3>${escapeHtml(part.nombre)}</h3></div><span class="status-pill ${part.stockActual <= part.stockMinimo ? 'status-cancelada' : 'status-entregada'}">${part.stockActual} unidades</span></div>
        <p>${escapeHtml(part.descripcion ?? 'Sin descripción.')}</p>
        <h3 class="inventory-detail-title">Movimientos de stock</h3>
        ${movements.length ? `<div class="table-wrapper"><table><thead><tr><th>Fecha</th><th>Tipo</th><th>Cantidad</th><th>Motivo</th><th>Referencia</th><th>Usuario</th></tr></thead><tbody>${movements.map((movement) => `<tr><td>${formatDate(movement.fecha)}</td><td>${movement.tipo}</td><td>${movement.cantidad}</td><td>${escapeHtml(movement.motivo)}</td><td>${escapeHtml(movement.referencia ?? '—')}</td><td>${escapeHtml(movement.usuarioNombre)}</td></tr>`).join('')}</tbody></table></div>` : '<p class="muted">No hay movimientos registrados para este repuesto.</p>'}
      `;
    }).catch((error) => {
      panel.textContent = error instanceof Error ? error.message : 'No se pudo cargar el detalle.';
    });
    return renderAppLayout(content, 'Detalle del repuesto');
  },
};

function formatDate(value: string): string {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString('es-AR');
}

function escapeHtml(value: string): string {
  return value.replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;').replaceAll('\'', '&#039;');
}
