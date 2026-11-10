import type { Rol } from '../types/auth';
import type { Usuario } from '../types/usuario';
import { apiRequest } from './api';

export type CrearUsuarioRequest = {
  email?: string;
  telefono?: string;
  claveTemporal: string;
  roles: Rol[];
};

export type CrearUsuarioConClienteRequest = CrearUsuarioRequest & {
  nombre: string;
  apellido: string;
  dni?: string;
  direccion?: string;
};

export type ActualizarUsuarioRequest = {
  email?: string;
  telefono?: string;
  claveTemporal?: string;
  roles: Rol[];
  activo: boolean;
};

export function crearUsuario(request: CrearUsuarioRequest): Promise<Usuario> {
  return apiRequest<Usuario>('/usuarios', {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

export function crearUsuarioConCliente(
  request: CrearUsuarioConClienteRequest,
): Promise<unknown> {
  return apiRequest<unknown>('/clientes/con-roles', {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

export function listarUsuarios(): Promise<Usuario[]> {
  return apiRequest<Usuario[]>('/usuarios');
}

export function listarTecnicos(): Promise<Usuario[]> {
  return apiRequest<Usuario[]>('/usuarios/tecnicos');
}

export function buscarUsuario(id: number): Promise<Usuario> {
  return apiRequest<Usuario>(`/usuarios/${id}`);
}

export function actualizarUsuario(
  id: number,
  request: ActualizarUsuarioRequest,
): Promise<Usuario> {
  return apiRequest<Usuario>(`/usuarios/${id}`, {
    method: 'PUT',
    body: JSON.stringify(request),
  });
}

export function desactivarUsuario(id: number): Promise<void> {
  return apiRequest<void>(`/usuarios/${id}/desactivar`, { method: 'PATCH' });
}
