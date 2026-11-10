import { router } from '../../router/router';
import { obtenerMiPerfil } from '../../services/clientesService';
import type { Cliente } from '../../types/cliente';
import { clearSession, getAccessToken } from '../../utils/authStorage';
import { resetAuthState } from '../../utils/authState';
import { getAuthState } from '../../utils/authState';

type NavItem = {
  label: string;
  path: string;
  icon: string;
};

const managementNavItems: NavItem[] = [
  { label: 'Dashboard', path: '/dashboard', icon: '▦' },
  { label: 'Clientes', path: '/clientes', icon: '◉' },
  { label: 'Órdenes', path: '/ordenes', icon: '▤' },
  { label: 'Presupuestos', path: '/presupuestos', icon: '$' },
  { label: 'Inventario', path: '/inventario', icon: '◫' },
  { label: 'Usuarios', path: '/usuarios', icon: '♙' },
];

const receptionistNavItems = managementNavItems.filter((item) => item.path !== '/usuarios');

const technicianNavItems: NavItem[] = [
  { label: 'Dashboard', path: '/dashboard', icon: '▦' },
  { label: 'Mis órdenes', path: '/ordenes', icon: '▤' },
  { label: 'Presupuestos', path: '/presupuestos', icon: '$' },
  { label: 'Repuestos', path: '/inventario', icon: '◫' },
];

const clientNavItems: NavItem[] = [
  { label: 'Mi cuenta', path: '/mi-cuenta', icon: '◉' },
  { label: 'Mis equipos', path: '/mis-equipos', icon: '▣' },
  { label: 'Mis reparaciones', path: '/mis-ordenes', icon: '▤' },
  { label: 'Mis presupuestos', path: '/mis-presupuestos', icon: '$' },
];

// El perfil del cliente se consulta una vez por sesión (se invalida si cambia el token).
let clientProfileCache: { token: string | null; profile: Promise<Cliente> } | null = null;

function loadClientProfile(): Promise<Cliente> {
  const token = getAccessToken();
  if (!clientProfileCache || clientProfileCache.token !== token) {
    const profile = obtenerMiPerfil();
    // Si falla, no se cachea el error: se reintenta en la próxima pantalla.
    profile.catch(() => {
      if (clientProfileCache?.profile === profile) clientProfileCache = null;
    });
    clientProfileCache = { token, profile };
  }
  return clientProfileCache.profile;
}

function showClientInTopbar(layout: HTMLElement): void {
  const avatar = layout.querySelector('.topbar-user .avatar');
  const name = layout.querySelector('.topbar-user strong');
  const detail = layout.querySelector('.topbar-user small');
  if (!avatar || !name || !detail) return;
  loadClientProfile().then((client) => {
    const fullName = `${client.nombre} ${client.apellido}`.trim();
    name.textContent = fullName;
    detail.textContent = client.email ?? client.telefono ?? 'Cliente';
    avatar.textContent = `${client.nombre.charAt(0)}${client.apellido.charAt(0)}`.toUpperCase() || 'CL';
    (name.parentElement as HTMLElement).title = fullName;
  }).catch(() => {
    // Si no se puede obtener el perfil, queda la etiqueta genérica "Cliente".
  });
}

export function renderAppLayout(content: HTMLElement, title: string): HTMLElement {
  const layout = document.createElement('div');
  layout.className = 'app-layout';
  // Sin query string, para que "#/presupuestos?ordenId=5" marque igual la pestaña activa.
  const currentPath = router.getCurrentPath().split('?')[0];
  const roles = getAuthState().roles;
  const isClient = roles.includes('CLIENTE')
    && !getAuthState().roles.some((role) => ['ADMINISTRADOR', 'RECEPCIONISTA', 'TECNICO'].includes(role));
  const isReceptionist = roles.includes('RECEPCIONISTA') && !roles.includes('ADMINISTRADOR');
  const isTechnician = roles.includes('TECNICO')
    && !roles.includes('ADMINISTRADOR')
    && !roles.includes('RECEPCIONISTA');
  const isAdministrator = roles.includes('ADMINISTRADOR');
  const navItems = isClient
    ? clientNavItems
    : isTechnician
      ? technicianNavItems
      : isReceptionist
        ? receptionistNavItems
        : managementNavItems;
  const userLabel = isClient
    ? 'Cliente'
    : isAdministrator
      ? 'Administrador'
      : isTechnician
        ? 'Técnico'
        : isReceptionist
          ? 'Recepcionista'
          : 'Usuario';

  layout.innerHTML = `
    <aside class="sidebar">
      <div class="sidebar-brand">
        <span class="brand-mark">F</span>
        <span>Fix<span class="brand-accent">Hub</span></span>
      </div>
      <nav class="sidebar-nav" aria-label="Navegación principal">
        ${navItems.map((item) => `
          <a href="#${item.path}" class="nav-link ${currentPath === item.path ? 'active' : ''}">
            <span class="nav-icon">${item.icon}</span><span>${item.label}</span>
          </a>
        `).join('')}
      </nav>
      <div class="sidebar-footer">
        <button class="nav-link logout-button" type="button"><span class="nav-icon">↪</span><span>Cerrar sesión</span></button>
      </div>
    </aside>
    <main class="app-main">
      <header class="topbar">
        <button class="menu-toggle" type="button" aria-label="Abrir menú">☰</button>
        <div>
          <p class="eyebrow">${isClient ? 'Espacio del cliente' : isTechnician ? 'Panel técnico' : 'Panel de gestión'}</p>
          <h1 class="page-title">${title}</h1>
        </div>
        <div class="topbar-user">
          <div class="avatar">${isClient ? 'CL' : isAdministrator ? 'AD' : isTechnician ? 'TE' : isReceptionist ? 'RE' : 'US'}</div>
          <div><strong>${userLabel}</strong><small>${userLabel}</small></div>
        </div>
      </header>
      <section class="page-content"></section>
    </main>
  `;

  if (isClient) showClientInTopbar(layout);
  layout.querySelector('.page-content')?.appendChild(content);
  layout.querySelector('.logout-button')?.addEventListener('click', () => {
    clearSession();
    resetAuthState();
    router.navigate('/login');
  });
  layout.querySelector('.menu-toggle')?.addEventListener('click', () => layout.classList.toggle('sidebar-open'));
  return layout;
}
