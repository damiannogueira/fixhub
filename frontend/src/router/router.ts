import { LoginPage } from '../pages/auth/LoginPage';

// Definimos que cada ruta debe devolver SIEMPRE un HTMLElement
type RouteHandler = () => HTMLElement;

const routes: Record<string, RouteHandler> = {
  '/login': () => LoginPage.render(),
};

function getCurrentPath(): string {
  const hash = window.location.hash.replace('#', '');
  return hash || '/login'; // Si no hay ruta, va al login por defecto
}

function renderCurrentRoute(): HTMLElement {
  const path = getCurrentPath();
  const handler = routes[path];

  // Si la ruta no existe, devolvemos un div vacío para evitar que app.appendChild falle
  if (!handler) {
    const errorDiv = document.createElement('div');
    errorDiv.textContent = `Error: La ruta "${path}" no existe.`;
    return errorDiv;
  }

  // Ejecutamos el handler que nos devuelve el HTMLElement de la página
  return handler(); 
}

function navigate(path: string): void {
  window.location.hash = path;
}

export const router = {
  getCurrentPath,
  renderCurrentRoute,
  navigate,
};