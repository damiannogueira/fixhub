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
