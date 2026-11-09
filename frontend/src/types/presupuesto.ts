export type EstadoPresupuesto = 'BORRADOR' | 'PUBLICADO' | 'APROBADO' | 'RECHAZADO';

export type DetallePresupuesto = {
  repuestoId: number;
  repuesto: string;
  cantidad: number;
  precioUnitario: number;
};

export type Presupuesto = {
  id: number;
  ordenId: number;
  estado: EstadoPresupuesto;
  costoManoObra: number;
  total: number;
  fechaCreacion: string;
  fechaPublicacion?: string;
  fechaDecision?: string;
  detalles: DetallePresupuesto[];
};
