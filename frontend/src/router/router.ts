import { LoginPage } from '../pages/auth/LoginPage';
import { ChangePasswordPage } from '../pages/auth/ChangePasswordPage';
import { NotFoundPage } from '../pages/auth/NotFoundPage';
import { ClientesPage } from '../pages/clientes/ClientesPage';
import { DashboardPage } from '../pages/dashboard/DashboardPage';
import { InventarioPage } from '../pages/inventario/InventarioPage';
import { OrdenesPage } from '../pages/ordenes/OrdenesPage';
import { PortalClientePage } from '../pages/portal-cliente/PortalClientePage';
import { UsuariosPage } from '../pages/usuarios/UsuariosPage';
import { CrearUsuarioPage } from '../pages/usuarios/CrearUsuarioPage';
import { EditarUsuarioPage } from '../pages/usuarios/EditarUsuarioPage';
import { DetalleOrdenClientePage } from '../pages/portal-cliente/DetalleOrdenClientePage';
import { MiCuentaPage } from '../pages/portal-cliente/MiCuentaPage';
import { MisEquiposPage } from '../pages/portal-cliente/MisEquiposPage';
import { MisPresupuestosPage } from '../pages/portal-cliente/MisPresupuestosPage';
import { PresupuestosPage } from '../pages/presupuestos/PresupuestosPage';
import { ClienteDetallePage } from '../pages/clientes/ClienteDetallePage';
import { DetalleInventarioPage } from '../pages/inventario/DetalleInventarioPage';
import type { Rol } from '../types/auth';
import { getAccessToken } from '../utils/authStorage';
import { hasAnyRole, hasAuthenticatedSession, requiresPasswordChange } from '../utils/authState';

type RouteHandler = () => HTMLElement;
type Route = {
  render: RouteHandler;
  public?: boolean;
  roles?: readonly Rol[];
};

const routes: Record<string, Route> = {
  '/login': { render: () => LoginPage.render(), public: true },
  '/404': { render: () => NotFoundPage.render(), public: true },
  '/cambiar-clave': { render: () => ChangePasswordPage.render() },
  '/dashboard': { render: () => DashboardPage.render(), roles: ['ADMINISTRADOR', 'RECEPCIONISTA', 'TECNICO'] },
  '/clientes': { render: () => ClientesPage.render(), roles: ['ADMINISTRADOR', 'RECEPCIONISTA'] },
  '/clientes/detalle': { render: () => ClienteDetallePage.render(), roles: ['ADMINISTRADOR', 'RECEPCIONISTA'] },
  '/ordenes': { render: () => OrdenesPage.render(), roles: ['ADMINISTRADOR', 'RECEPCIONISTA', 'TECNICO'] },
  '/inventario': { render: () => InventarioPage.render(), roles: ['ADMINISTRADOR', 'RECEPCIONISTA', 'TECNICO'] },
  '/inventario/detalle': { render: () => DetalleInventarioPage.render(), roles: ['ADMINISTRADOR'] },
  '/usuarios': { render: () => UsuariosPage.render(), roles: ['ADMINISTRADOR'] },
  '/usuarios/nuevo': { render: () => CrearUsuarioPage.render(), roles: ['ADMINISTRADOR', 'RECEPCIONISTA'] },
  '/usuarios/editar': { render: () => EditarUsuarioPage.render(), roles: ['ADMINISTRADOR'] },
  '/mis-ordenes': { render: () => PortalClientePage.render(), roles: ['CLIENTE'] },
  '/mis-ordenes/detalle': { render: () => DetalleOrdenClientePage.render(), roles: ['CLIENTE'] },
  '/mi-cuenta': { render: () => MiCuentaPage.render(), roles: ['CLIENTE'] },
  '/mis-equipos': { render: () => MisEquiposPage.render(), roles: ['CLIENTE'] },
  '/mis-presupuestos': { render: () => MisPresupuestosPage.render(), roles: ['CLIENTE'] },
  '/presupuestos': { render: () => PresupuestosPage.render(), roles: ['ADMINISTRADOR', 'RECEPCIONISTA', 'TECNICO'] },
};

function getCurrentPath(): string {
  const hash = window.location.hash.replace('#', '');
  return hash || '/login'; // Si no hay ruta, va al login por defecto
}

function renderCurrentRoute(): HTMLElement {
  const path = getCurrentPath();
  const route = routes[path.split('?')[0]];

  if (!route) {
    return NotFoundPage.render();
  }

  if (path === '/login' && getAccessToken()) {
    navigate(requiresPasswordChange() ? '/cambiar-clave' : getHomeRoute());
    return document.createElement('div');
  }

  function getHomeRoute(): string {
    return hasAnyRole(['CLIENTE']) && !hasAnyRole(['ADMINISTRADOR', 'RECEPCIONISTA', 'TECNICO'])
      ? '/mi-cuenta'
      : '/dashboard';
  }

  if (!route.public && (!getAccessToken() || !hasAuthenticatedSession())) {
    navigate('/login');
    return document.createElement('div');
  }

  if (path !== '/cambiar-clave' && requiresPasswordChange()) {
    navigate('/cambiar-clave');
    return document.createElement('div');
  }

  if (route.roles && !hasAnyRole(route.roles)) {
    navigate('/404');
    return document.createElement('div');
  }

  return route.render();
}

function navigate(path: string): void {
  window.location.hash = path;
}

export const router = {
  getCurrentPath,
  renderCurrentRoute,
  navigate,
};