import { listarOrdenes } from './ordenesService';
import { listarRepuestos } from './repuestosService';

export async function obtenerDatosDashboard() {
  const [orders, parts] = await Promise.all([
    listarOrdenes(),
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
