import type { Repuesto } from '../types/repuesto';
import { apiRequest } from './api';

export function listarRepuestos(nombre?: string): Promise<Repuesto[]> {
  const query = nombre?.trim() ? `?nombre=${encodeURIComponent(nombre.trim())}` : '';
  return apiRequest<Repuesto[]>(`/repuestos${query}`);
}

/** Solo Administrador: incluye repuestos inactivos. */
export function listarTodosRepuestos(): Promise<Repuesto[]> {
  return apiRequest<Repuesto[]>('/repuestos/todos');
}

export function activarRepuesto(id: number): Promise<Repuesto> {
  return apiRequest<Repuesto>(`/repuestos/${id}/activar`, { method: 'PATCH' });
}

export function listarRepuestosConStockBajo(): Promise<Repuesto[]> {
  return apiRequest<Repuesto[]>('/repuestos/stock-bajo');
}

export type RepuestoRequest = {
  codigo: string;
  nombre: string;
  descripcion?: string;
  stockMinimo: number;
  precioCosto: number;
  precioVenta: number;
};

export type MovimientoStockRequest = {
  cantidad: number;
  motivo: string;
  referencia?: string;
};

export function crearRepuesto(request: RepuestoRequest): Promise<Repuesto> {
  return apiRequest<Repuesto>('/repuestos', { method: 'POST', body: JSON.stringify(request) });
}

export function actualizarRepuesto(id: number, request: RepuestoRequest): Promise<Repuesto> {
  return apiRequest<Repuesto>(`/repuestos/${id}`, { method: 'PUT', body: JSON.stringify(request) });
}

export function desactivarRepuesto(id: number): Promise<void> {
  return apiRequest<void>(`/repuestos/${id}`, { method: 'DELETE' });
}

export function registrarEntrada(id: number, request: MovimientoStockRequest): Promise<MovimientoStock> {
  return apiRequest<MovimientoStock>(`/repuestos/${id}/entradas`, { method: 'POST', body: JSON.stringify(request) });
}

export function registrarAjuste(id: number, request: MovimientoStockRequest): Promise<MovimientoStock> {
  return apiRequest<MovimientoStock>(`/repuestos/${id}/ajustes`, { method: 'POST', body: JSON.stringify(request) });
}

export function listarMovimientos(id: number): Promise<MovimientoStock[]> {
  return apiRequest<MovimientoStock[]>(`/repuestos/${id}/movimientos`);
}

export type MovimientoStock = {
  id: number;
  repuestoId: number;
  tipo: 'ENTRADA' | 'AJUSTE' | 'CONSUMO';
  cantidad: number;
  motivo: string;
  referencia?: string;
  usuarioId: number;
  usuarioNombre: string;
  consumoId?: number;
  fecha: string;
};
