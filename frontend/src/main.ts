import { router } from './router/router';
import { hasAuthenticatedSession, requiresPasswordChange, resetAuthState } from './utils/authState';
import { getAccessToken } from './utils/authStorage';

let lastRender: string | null = null;

function initApp(): void {
  // Buscamos el contenedor principal
  const app = document.getElementById('app');
  
  // Si no existe, mostramos error y detenemos la ejecución
  if (!app) {
    console.error('Error crítico: No se encontró el elemento <div id="app"> en el index.html');
    return;
  }

  hasAuthenticatedSession();
  const signature = JSON.stringify([window.location.hash, getAccessToken(), requiresPasswordChange()]);
  if (signature === lastRender && app.childElementCount > 0) return;
  lastRender = signature;

  // 1. Le pedimos al router que nos dé el elemento HTML de la página actual
  const page = router.renderCurrentRoute();
  
  // 2. Limpiamos el contenedor y agregamos la página de forma segura
  app.innerHTML = '';
  app.appendChild(page);
}

// Escuchar cambios de URL (cuando el usuario navega con el hash #)
window.addEventListener('hashchange', () => {
  initApp();
});

window.addEventListener('fixhub:session-expired', () => {
  resetAuthState();
  router.navigate('/login');
});

// Arrancar la aplicación cuando carga la página
document.addEventListener('DOMContentLoaded', initApp);
window.addEventListener('storage', (event) => {
  if (event.storageArea === localStorage
    && (event.key === null || event.key === 'access_token' || event.key === 'fixhub.requires_password_change')) {
    initApp();
  }
});
