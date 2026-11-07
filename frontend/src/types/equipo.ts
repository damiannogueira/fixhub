export type Equipo = {
  id: number;
  clienteId: number;
  tipo: string;
  marca: string;
  modelo: string;
  numeroSerie?: string;
  observaciones?: string;
};
