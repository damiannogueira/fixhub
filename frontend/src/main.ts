import { router } from './router/router';

function initApp(): void {
  // Buscamos el contenedor principal
  const app = document.getElementById('app');
  
  // Si no existe, mostramos error y detenemos la ejecución
  if (!app) {
    console.error('Error crítico: No se encontró el elemento <div id="app"> en el index.html');
    return;
  }

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

// Arrancar la aplicación cuando carga la página
document.addEventListener('DOMContentLoaded', initApp);