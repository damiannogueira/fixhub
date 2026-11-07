export type Repuesto = {
  id: number;
  codigo: string;
  nombre: string;
  descripcion?: string;
  stockActual: number;
  stockMinimo: number;
  precioCosto: number;
  precioVenta: number;
  activo: boolean;
  fechaCreacion: string;
  fechaModificacion: string;
};
