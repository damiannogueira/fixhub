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

/** Detalle de una orden para Administración y Recepción. */
export function obtenerOrden(id: number): Promise<OrdenTrabajo> {
  return apiRequest<OrdenTrabajo>(`/ordenes/${id}`);
}

export function obtenerDiagnosticoOrden(id: number): Promise<Diagnostico> {
  return apiRequest<Diagnostico>(`/ordenes/${id}/diagnostico`);
}

export function listarRegistrosOrden(id: number): Promise<RegistroTecnico[]> {
  return apiRequest<RegistroTecnico[]>(`/ordenes/${id}/registros-tecnicos`);
}

export function listarHistorialOrden(id: number): Promise<EventoHistorial[]> {
  return apiRequest<EventoHistorial[]>(`/ordenes/${id}/historial`);
}

/** Reparaciones actuales y anteriores de un cliente. */
export function listarOrdenesDeCliente(clienteId: number): Promise<OrdenTrabajo[]> {
  return apiRequest<OrdenTrabajo[]>(`/clientes/${clienteId}/ordenes`);
}

/** Reparaciones actuales y anteriores de un equipo. */
export function listarOrdenesDeEquipo(equipoId: number): Promise<OrdenTrabajo[]> {
  return apiRequest<OrdenTrabajo[]>(`/equipos/${equipoId}/ordenes`);
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

/** Tareas realizadas en la reparación, tal como las ve el cliente (sin observaciones internas). */
export function listarTareasCliente(id: number): Promise<TareaCliente[]> {
  return apiRequest<TareaCliente[]>(`/clientes/me/ordenes/${id}/registros-tecnicos`);
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

/** El motivo es obligatorio solo al cambiar un técnico ya asignado (y lo puede hacer únicamente el Administrador). */
export function asignarTecnico(id: number, tecnicoId: number, motivo?: string): Promise<OrdenTrabajo> {
  return apiRequest<OrdenTrabajo>(`/ordenes/${id}/tecnico`, {
    method: 'PATCH',
    body: JSON.stringify({ tecnicoId, ...(motivo ? { motivo } : {}) }),
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

export type EventoHistorial = {
  id: number;
  tipoEvento: 'ASIGNACION_TECNICO' | 'CAMBIO_TECNICO' | 'CAMBIO_ESTADO' | 'CANCELACION';
  estadoAnterior?: EstadoOrden;
  estadoNuevo?: EstadoOrden;
  comentario?: string;
  usuarioId: number;
  usuarioNombre: string;
  visibleCliente: boolean;
  fecha: string;
};

export type TareaCliente = {
  id: number;
  ordenId: number;
  tareaRealizada: string;
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
