import { renderAppLayout } from '../../components/layout/AppLayout';
import { renderEmptyState } from '../../components/ui/EmptyState';

export const PortalClientePage = {
  render(): HTMLElement {
    const content = document.createElement('div');
    content.innerHTML = `<div class="page-heading"><div><p class="muted">Espacio del cliente</p><h2>Mis reparaciones</h2></div></div><section class="panel portal-placeholder"></section>`;
    content.querySelector('.portal-placeholder')?.appendChild(renderEmptyState('Portal del cliente', 'Aquí podrás consultar tus equipos, órdenes y presupuestos.'));
    return renderAppLayout(content, 'Portal del cliente');
  },
};
