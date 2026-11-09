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

export function listarMisOrdenesTecnico(): Promise<OrdenTrabajo[]> {
  return apiRequest<OrdenTrabajo[]>('/ordenes/me');
}

export function obtenerDiagnosticoTecnico(id: number): Promise<Diagnostico> {
  return apiRequest<Diagnostico>(`/ordenes/me/${id}/diagnostico`);
}

export function obtenerDiagnosticoCliente(id: number): Promise<DiagnosticoCliente> {
  return apiRequest<DiagnosticoCliente>(`/clientes/me/ordenes/${id}/diagnostico`);
}

export function crearDiagnostico(id: number, request: CrearDiagnosticoRequest): Promise<Diagnostico> {
  return apiRequest<Diagnostico>(`/ordenes/${id}/diagnostico`, {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

export function actualizarDiagnostico(id: number, request: CrearDiagnosticoRequest): Promise<Diagnostico> {
  return apiRequest<Diagnostico>(`/ordenes/${id}/diagnostico`, {
    method: 'PUT',
    body: JSON.stringify(request),
  });
}

export function listarRegistrosTecnicos(id: number): Promise<RegistroTecnico[]> {
  return apiRequest<RegistroTecnico[]>(`/ordenes/me/${id}/registros-tecnicos`);
}

export function crearRegistroTecnico(id: number, request: CrearRegistroTecnicoRequest): Promise<RegistroTecnico> {
  return apiRequest<RegistroTecnico>(`/ordenes/${id}/registros-tecnicos`, {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

export function consumirRepuesto(id: number, request: { repuestoId: number; cantidad: number }): Promise<ConsumoRepuesto> {
  return apiRequest<ConsumoRepuesto>(`/ordenes/${id}/consumos`, {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

export function listarConsumos(id: number): Promise<ConsumoRepuesto[]> {
  return apiRequest<ConsumoRepuesto[]>(`/ordenes/${id}/consumos`);
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

export type Diagnostico = {
  id: number;
  ordenId: number;
  usuarioId: number;
  descripcionFalla: string;
  tareasRequeridas: string;
  fecha: string;
};

export type DiagnosticoCliente = Omit<Diagnostico, 'usuarioId'>;

export type CrearDiagnosticoRequest = {
  descripcionFalla: string;
  tareasRequeridas: string;
};

export type RegistroTecnico = {
  id: number;
  ordenId: number;
  usuarioId: number;
  usuarioNombre: string;
  tareaRealizada: string;
  observacion?: string;
  fecha: string;
};

export type CrearRegistroTecnicoRequest = {
  tareaRealizada: string;
  observacion?: string;
};

export type ConsumoRepuesto = {
  id: number;
  ordenId: number;
  repuestoId: number;
  repuesto: string;
  cantidad: number;
  precioUnitario: number;
  usuarioId: number;
  usuarioNombre: string;
  fecha: string;
};
