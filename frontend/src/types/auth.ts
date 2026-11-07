export type Rol =
  | 'ADMINISTRADOR'
  | 'RECEPCIONISTA'
  | 'TECNICO'
  | 'CLIENTE';

export type LoginRequest = {
  identificador: string;
  clave: string;
};

export type LoginResponse = {
  usuarioId: number;
  identificador: string;
  token: string;
  tipo: string;
  expiraEnSegundos: number;
  requiereCambioClave: boolean;
  roles: Rol[];
};
