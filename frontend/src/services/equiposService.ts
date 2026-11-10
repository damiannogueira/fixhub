import type { Equipo } from '../types/equipo';
import { apiRequest } from './api';

export function listarMisEquipos(): Promise<Equipo[]> {
  return apiRequest<Equipo[]>('/clientes/me/equipos');
}

export type EquipoRequest = Omit<Equipo, 'id' | 'clienteId'>;

export function listarEquiposDeCliente(clienteId: number): Promise<Equipo[]> {
  return apiRequest<Equipo[]>(`/clientes/${clienteId}/equipos`);
}

export function crearEquipo(clienteId: number, request: EquipoRequest): Promise<Equipo> {
  return apiRequest<Equipo>(`/clientes/${clienteId}/equipos`, {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

export function actualizarEquipo(id: number, request: EquipoRequest): Promise<Equipo> {
  return apiRequest<Equipo>(`/equipos/${id}`, {
    method: 'PUT',
    body: JSON.stringify(request),
  });
}
