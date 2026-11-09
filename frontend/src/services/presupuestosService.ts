import type { Presupuesto, EstadoPresupuesto } from '../types/presupuesto';
import { apiRequest } from './api';

export type CrearPresupuestoRequest = {
  costoManoObra: number;
  detalles: { repuestoId: number; cantidad: number }[];
};

export function listarMisPresupuestos(): Promise<Presupuesto[]> {
  return apiRequest<Presupuesto[]>('/clientes/me/presupuestos');
}

export function listarPresupuestos(): Promise<Presupuesto[]> {
  return apiRequest<Presupuesto[]>('/presupuestos');
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
