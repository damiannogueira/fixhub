import type { Cliente } from '../types/cliente';
import { apiRequest } from './api';

export function listarClientes(): Promise<Cliente[]> {
  return apiRequest<Cliente[]>('/clientes');
}

export function obtenerMiPerfil(): Promise<Cliente> {
  return apiRequest<Cliente>('/clientes/me');
}

export function obtenerCliente(id: number): Promise<Cliente> {
  return apiRequest<Cliente>(`/clientes/${id}`);
}
