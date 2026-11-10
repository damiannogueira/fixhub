import type { Rol } from './auth';

export type Usuario = {
  id: number;
  email?: string;
  telefono?: string;
  activo: boolean;
  requiereCambioClave?: boolean;
  fechaCreacion?: string;
  roles: Rol[];
};
