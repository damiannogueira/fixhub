import { listarMisOrdenesTecnico, listarOrdenes } from './ordenesService';
import { listarRepuestos } from './repuestosService';
import { getAuthState } from '../utils/authState';

export async function obtenerDatosDashboard() {
  const [orders, parts] = await Promise.all([
    getAuthState().roles.includes('TECNICO')
      && !getAuthState().roles.some((role) => ['ADMINISTRADOR', 'RECEPCIONISTA'].includes(role))
      ? listarMisOrdenesTecnico()
      : listarOrdenes(),
    listarRepuestos(),
  ]);

  return {
    orders,
    openOrders: orders.filter((order) => !['ENTREGADA', 'CANCELADA'].includes(order.estado)),
    diagnosticOrders: orders.filter((order) => order.estado === 'EN_DIAGNOSTICO'),
    readyOrders: orders.filter((order) => order.estado === 'LISTA_PARA_RETIRAR'),
    lowStockParts: parts.filter((part) => part.stockActual <= part.stockMinimo),
  };
}
