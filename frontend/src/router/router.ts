import { LoginPage } from '../pages/auth/LoginPage';
import { ChangePasswordPage } from '../pages/auth/ChangePasswordPage';
import { ForbiddenPage } from '../pages/auth/ForbiddenPage';
import { ClientesPage } from '../pages/clientes/ClientesPage';
import { DashboardPage } from '../pages/dashboard/DashboardPage';
import { InventarioPage } from '../pages/inventario/InventarioPage';
import { OrdenesPage } from '../pages/ordenes/OrdenesPage';
import { PortalClientePage } from '../pages/portal-cliente/PortalClientePage';
import { UsuariosPage } from '../pages/usuarios/UsuariosPage';
import { CrearUsuarioPage } from '../pages/usuarios/CrearUsuarioPage';
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
  '/cambiar-clave': { render: () => ChangePasswordPage.render() },
  '/dashboard': { render: () => DashboardPage.render() },
  '/clientes': { render: () => ClientesPage.render(), roles: ['ADMINISTRADOR', 'RECEPCIONISTA'] },
  '/ordenes': { render: () => OrdenesPage.render(), roles: ['ADMINISTRADOR', 'RECEPCIONISTA', 'TECNICO'] },
  '/inventario': { render: () => InventarioPage.render(), roles: ['ADMINISTRADOR', 'RECEPCIONISTA', 'TECNICO'] },
  '/usuarios': { render: () => UsuariosPage.render(), roles: ['ADMINISTRADOR'] },
  '/usuarios/nuevo': { render: () => CrearUsuarioPage.render(), roles: ['ADMINISTRADOR'] },
  '/mis-ordenes': { render: () => PortalClientePage.render(), roles: ['CLIENTE'] },
};

function getCurrentPath(): string {
  const hash = window.location.hash.replace('#', '');
  return hash || '/login'; // Si no hay ruta, va al login por defecto
}

function renderCurrentRoute(): HTMLElement {
  const path = getCurrentPath();
  const route = routes[path];

  if (!route) {
    const errorDiv = document.createElement('div');
    errorDiv.textContent = `Error: La ruta "${path}" no existe.`;
    return errorDiv;
  }

  if (path === '/login' && getAccessToken()) {
    navigate(requiresPasswordChange() ? '/cambiar-clave' : '/dashboard');
    return document.createElement('div');
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
    return ForbiddenPage.render();
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