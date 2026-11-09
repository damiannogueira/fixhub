import { router } from '../../router/router';
import { clearSession } from '../../utils/authStorage';
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

const clientNavItems: NavItem[] = [
  { label: 'Mi cuenta', path: '/mi-cuenta', icon: '◉' },
  { label: 'Mis equipos', path: '/mis-equipos', icon: '▣' },
  { label: 'Mis reparaciones', path: '/mis-ordenes', icon: '▤' },
  { label: 'Mis presupuestos', path: '/mis-presupuestos', icon: '$' },
];

export function renderAppLayout(content: HTMLElement, title: string): HTMLElement {
  const layout = document.createElement('div');
  layout.className = 'app-layout';
  const currentPath = router.getCurrentPath();
  const roles = getAuthState().roles;
  const isClient = roles.includes('CLIENTE')
    && !getAuthState().roles.some((role) => ['ADMINISTRADOR', 'RECEPCIONISTA', 'TECNICO'].includes(role));
  const isReceptionist = roles.includes('RECEPCIONISTA') && !roles.includes('ADMINISTRADOR');
  const navItems = isClient ? clientNavItems : isReceptionist ? receptionistNavItems : managementNavItems;
  const userLabel = isClient ? 'Cliente' : isReceptionist ? 'Recepcionista' : 'Usuario';

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
          <p class="eyebrow">Panel de gestión</p>
          <h1 class="page-title">${title}</h1>
        </div>
        <div class="topbar-user">
          <div class="avatar">${isClient ? 'CL' : 'US'}</div>
          <div><strong>${userLabel}</strong><small>${userLabel}</small></div>
        </div>
      </header>
      <section class="page-content"></section>
    </main>
  `;

  layout.querySelector('.page-content')?.appendChild(content);
  layout.querySelector('.logout-button')?.addEventListener('click', () => {
    clearSession();
    resetAuthState();
    router.navigate('/login');
  });
  layout.querySelector('.menu-toggle')?.addEventListener('click', () => layout.classList.toggle('sidebar-open'));
  return layout;
}
