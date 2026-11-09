import type { Repuesto } from '../types/repuesto';
import { apiRequest } from './api';

export function listarRepuestos(nombre?: string): Promise<Repuesto[]> {
  const query = nombre?.trim() ? `?nombre=${encodeURIComponent(nombre.trim())}` : '';
  return apiRequest<Repuesto[]>(`/repuestos${query}`);
}

export function listarRepuestosConStockBajo(): Promise<Repuesto[]> {
  return apiRequest<Repuesto[]>('/repuestos/stock-bajo');
}
