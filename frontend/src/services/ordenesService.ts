import type { OrdenCliente } from '../types/orden';
import type { EstadoOrden, OrdenTrabajo, CrearOrdenRequest } from '../types/orden';
import { apiRequest } from './api';

export function listarMisOrdenes(): Promise<OrdenCliente[]> {
  return apiRequest<OrdenCliente[]>('/clientes/me/ordenes');
}

export function obtenerMiOrden(id: number): Promise<OrdenCliente> {
  return apiRequest<OrdenCliente>(`/clientes/me/ordenes/${id}`);
}

export function listarOrdenes(): Promise<OrdenTrabajo[]> {
  return apiRequest<OrdenTrabajo[]>('/ordenes');
}

export function crearOrden(request: CrearOrdenRequest): Promise<OrdenTrabajo> {
  return apiRequest<OrdenTrabajo>('/ordenes', {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

export function cambiarEstadoOrden(id: number, estado: EstadoOrden): Promise<OrdenTrabajo> {
  return apiRequest<OrdenTrabajo>(`/ordenes/${id}/estado`, {
    method: 'PATCH',
    body: JSON.stringify({ estado }),
  });
}

export function asignarTecnico(id: number, tecnicoId: number): Promise<OrdenTrabajo> {
  return apiRequest<OrdenTrabajo>(`/ordenes/${id}/tecnico`, {
    method: 'PATCH',
    body: JSON.stringify({ tecnicoId }),
  });
}

export function cancelarOrden(id: number, motivo: string): Promise<OrdenTrabajo> {
  return apiRequest<OrdenTrabajo>(`/ordenes/${id}/cancelacion`, {
    method: 'POST',
    body: JSON.stringify({ motivo }),
  });
}
