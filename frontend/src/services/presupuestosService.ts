import type { Presupuesto, EstadoPresupuesto } from '../types/presupuesto';
import { apiRequest } from './api';

export type CrearPresupuestoRequest = {
  costoManoObra: number;
  detalles: { repuestoId: number; cantidad: number }[];
};

export function listarMisPresupuestos(): Promise<Presupuesto[]> {
  return apiRequest<Presupuesto[]>('/clientes/me/presupuestos');
}

export function obtenerPresupuestoPorOrden(ordenId: number): Promise<Presupuesto> {
  return apiRequest<Presupuesto>(`/ordenes/${ordenId}/presupuesto`);
}

export function listarPresupuestos(): Promise<Presupuesto[]> {
  return apiRequest<Presupuesto[]>('/presupuestos');
}

export function listarMisPresupuestosTecnico(): Promise<Presupuesto[]> {
  return apiRequest<Presupuesto[]>('/presupuestos/me');
}

export function decidirPresupuesto(id: number, decision: Extract<EstadoPresupuesto, 'APROBADO' | 'RECHAZADO'>): Promise<Presupuesto> {
  return apiRequest<Presupuesto>(`/presupuestos/${id}/decision`, {
    method: 'POST',
    body: JSON.stringify({ decision }),
  });
}

export function crearPresupuesto(ordenId: number, request: CrearPresupuestoRequest): Promise<Presupuesto> {
  return apiRequest<Presupuesto>(`/ordenes/${ordenId}/presupuesto`, {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

export function publicarPresupuesto(id: number): Promise<Presupuesto> {
  return apiRequest<Presupuesto>(`/presupuestos/${id}/publicar`, { method: 'POST' });
}

export function editarPresupuesto(id: number, request: CrearPresupuestoRequest): Promise<Presupuesto> {
  return apiRequest<Presupuesto>(`/presupuestos/${id}`, {
    method: 'PUT',
    body: JSON.stringify(request),
  });
}
