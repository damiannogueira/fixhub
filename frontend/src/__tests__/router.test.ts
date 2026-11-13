import { beforeEach, describe, expect, it, vi } from 'vitest';

// Las páginas se reemplazan por stubs: acá se prueba el control de acceso del router, no el contenido.
const { stub } = vi.hoisted(() => ({
  stub: (name: string) => {
    const el = document.createElement('div');
    el.dataset.page = name;
    return el;
  },
}));

vi.mock('../pages/landing/LandingPage', () => ({ LandingPage: { render: () => stub('Landing') } }));
vi.mock('../pages/auth/LoginPage', () => ({ LoginPage: { render: () => stub('Login') } }));
vi.mock('../pages/auth/ChangePasswordPage', () => ({ ChangePasswordPage: { render: () => stub('ChangePassword') } }));
vi.mock('../pages/auth/NotFoundPage', () => ({ NotFoundPage: { render: () => stub('NotFound') } }));
vi.mock('../pages/clientes/ClientesPage', () => ({ ClientesPage: { render: () => stub('Clientes') } }));
vi.mock('../pages/clientes/ClienteDetallePage', () => ({ ClienteDetallePage: { render: () => stub('ClienteDetalle') } }));
vi.mock('../pages/dashboard/DashboardPage', () => ({ DashboardPage: { render: () => stub('Dashboard') } }));
vi.mock('../pages/inventario/InventarioPage', () => ({ InventarioPage: { render: () => stub('Inventario') } }));
vi.mock('../pages/inventario/DetalleInventarioPage', () => ({ DetalleInventarioPage: { render: () => stub('DetalleInventario') } }));
vi.mock('../pages/ordenes/OrdenesPage', () => ({ OrdenesPage: { render: () => stub('Ordenes') } }));
vi.mock('../pages/ordenes/DetalleOrdenGestionPage', () => ({ DetalleOrdenGestionPage: { render: () => stub('DetalleGestion') } }));
vi.mock('../pages/ordenes/DetalleOrdenTecnicoPage', () => ({ DetalleOrdenTecnicoPage: { render: () => stub('DetalleTecnico') } }));
vi.mock('../pages/portal-cliente/PortalClientePage', () => ({ PortalClientePage: { render: () => stub('PortalCliente') } }));
vi.mock('../pages/portal-cliente/DetalleOrdenClientePage', () => ({ DetalleOrdenClientePage: { render: () => stub('DetalleCliente') } }));
vi.mock('../pages/portal-cliente/MiCuentaPage', () => ({ MiCuentaPage: { render: () => stub('MiCuenta') } }));
vi.mock('../pages/portal-cliente/MisEquiposPage', () => ({ MisEquiposPage: { render: () => stub('MisEquipos') } }));
vi.mock('../pages/portal-cliente/MisPresupuestosPage', () => ({ MisPresupuestosPage: { render: () => stub('MisPresupuestos') } }));
vi.mock('../pages/presupuestos/PresupuestosPage', () => ({ PresupuestosPage: { render: () => stub('Presupuestos') } }));
vi.mock('../pages/usuarios/UsuariosPage', () => ({ UsuariosPage: { render: () => stub('Usuarios') } }));
vi.mock('../pages/usuarios/CrearUsuarioPage', () => ({ CrearUsuarioPage: { render: () => stub('CrearUsuario') } }));
vi.mock('../pages/usuarios/EditarUsuarioPage', () => ({ EditarUsuarioPage: { render: () => stub('EditarUsuario') } }));

import { router } from '../router/router';
import { resetAuthState, setAuthenticated } from '../utils/authState';

function login(roles: string[], requiereCambioClave = false): void {
  const payload = btoa(JSON.stringify({ roles })).replaceAll('=', '');
  setAuthenticated({ token: `h.${payload}.s`, requiereCambioClave } as never);
}

/** Navega a la ruta y devuelve [página renderizada, ruta final tras redirecciones]. */
function visit(path: string): [string | undefined, string] {
  window.location.hash = path;
  const element = router.renderCurrentRoute();
  return [element.dataset.page, window.location.hash];
}

describe('router: control de acceso', () => {
  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
    resetAuthState();
    window.location.hash = '';
  });

  describe('sin sesión', () => {
    it('permite las rutas públicas', () => {
      expect(visit('/')[0]).toBe('Landing');
      expect(visit('/login')[0]).toBe('Login');
      expect(visit('/404')[0]).toBe('NotFound');
    });

    it('redirige a /login al pedir una ruta privada', () => {
      const [page, hash] = visit('/dashboard');
      expect(page).toBeUndefined();
      expect(hash).toBe('#/login');
    });

    it('muestra NotFound para una ruta inexistente', () => {
      expect(visit('/no-existe')[0]).toBe('NotFound');
    });
  });

  describe('con sesión', () => {
    it('redirige al dashboard si el usuario ya logueado entra a /login o /', () => {
      login(['ADMINISTRADOR']);
      expect(visit('/login')[1]).toBe('#/dashboard');
      expect(visit('/')[1]).toBe('#/dashboard');
    });

    it('un cliente logueado es redirigido a /mi-cuenta', () => {
      login(['CLIENTE']);
      expect(visit('/login')[1]).toBe('#/mi-cuenta');
    });

    it('con cambio de clave pendiente todo redirige a /cambiar-clave', () => {
      login(['RECEPCIONISTA'], true);
      expect(visit('/dashboard')[1]).toBe('#/cambiar-clave');
      expect(visit('/login')[1]).toBe('#/cambiar-clave');
      expect(visit('/cambiar-clave')[0]).toBe('ChangePassword');
    });
  });

  describe('permisos por rol', () => {
    it.each([
      ['ADMINISTRADOR', '/usuarios', 'Usuarios'],
      ['ADMINISTRADOR', '/inventario/detalle', 'DetalleInventario'],
      ['RECEPCIONISTA', '/clientes', 'Clientes'],
      ['RECEPCIONISTA', '/usuarios/nuevo', 'CrearUsuario'],
      ['TECNICO', '/ordenes', 'Ordenes'],
      ['TECNICO', '/dashboard', 'Dashboard'],
      ['CLIENTE', '/mis-ordenes', 'PortalCliente'],
      ['CLIENTE', '/mi-cuenta', 'MiCuenta'],
    ])('%s puede entrar a %s', (role, path, page) => {
      login([role]);
      expect(visit(path)[0]).toBe(page);
    });

    it.each([
      ['TECNICO', '/usuarios'],
      ['TECNICO', '/clientes'],
      ['TECNICO', '/inventario/detalle'],
      ['RECEPCIONISTA', '/usuarios'],
      ['RECEPCIONISTA', '/usuarios/editar'],
      ['RECEPCIONISTA', '/inventario/detalle'],
      ['CLIENTE', '/ordenes'],
      ['CLIENTE', '/dashboard'],
      ['CLIENTE', '/usuarios/nuevo'],
      ['ADMINISTRADOR', '/mis-ordenes'],
      ['ADMINISTRADOR', '/mi-cuenta'],
    ])('%s es rechazado en %s (va a /404)', (role, path) => {
      login([role]);
      const [page, hash] = visit(path);
      expect(page).toBeUndefined();
      expect(hash).toBe('#/404');
    });

    it('un usuario con varios roles accede a lo que permita cualquiera de ellos', () => {
      login(['TECNICO', 'ADMINISTRADOR']);
      expect(visit('/usuarios')[0]).toBe('Usuarios');
    });
  });

  describe('detalle de orden', () => {
    it('un técnico puro ve el detalle de técnico', () => {
      login(['TECNICO']);
      expect(visit('/ordenes/detalle?id=1')[0]).toBe('DetalleTecnico');
    });

    it('un administrador ve el detalle de gestión', () => {
      login(['ADMINISTRADOR']);
      expect(visit('/ordenes/detalle?id=1')[0]).toBe('DetalleGestion');
    });

    it('un híbrido ve el detalle de técnico solo con ?vista=tecnico', () => {
      login(['TECNICO', 'ADMINISTRADOR']);
      expect(visit('/ordenes/detalle?id=1')[0]).toBe('DetalleGestion');
      expect(visit('/ordenes/detalle?id=1&vista=tecnico')[0]).toBe('DetalleTecnico');
    });
  });
});
