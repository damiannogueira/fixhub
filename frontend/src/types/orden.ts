import type { Equipo } from './equipo';

export type EstadoOrden =
  | 'RECIBIDA'
  | 'EN_DIAGNOSTICO'
  | 'PENDIENTE_APROBACION'
  | 'APROBADA'
  | 'EN_REPARACION'
  | 'FINALIZADA'
  | 'LISTA_PARA_RETIRAR'
  | 'ENTREGADA'
  | 'CANCELADA';

export type Orden = {
  id: number;
  equipo: Equipo;
  tecnicoId?: number;
  estado: EstadoOrden;
  problemaInicial: string;
  motivoCancelacion?: string;
  fechaIngreso: string;
  fechaEntrega?: string;
};

export type OrdenCliente = {
  id: number;
  equipo: {
    id: number;
    tipo: string;
    marca: string;
    modelo: string;
    numeroSerie?: string;
    observaciones?: string;
  };
  estado: EstadoOrden;
  problemaInicial: string;
  motivoCancelacion?: string;
  fechaIngreso: string;
  fechaEntrega?: string;
};

export type OrdenTrabajo = OrdenCliente & {
  cliente: {
    id: number;
    nombre: string;
    apellido: string;
  };
  tecnicoId?: number;
};

export type CrearOrdenRequest = {
  equipoId: number;
  tecnicoId?: number;
  problemaInicial: string;
};
