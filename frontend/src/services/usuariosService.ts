import type { Rol } from '../types/auth';
import type { Usuario } from '../types/usuario';
import { apiRequest } from './api';

export type CrearUsuarioRequest = {
  email?: string;
  telefono?: string;
  claveTemporal: string;
  roles: Rol[];
};

export function crearUsuario(request: CrearUsuarioRequest): Promise<Usuario> {
  return apiRequest<Usuario>('/usuarios', {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

export function listarUsuarios(): Promise<Usuario[]> {
  return apiRequest<Usuario[]>('/usuarios');
}
