export const ForbiddenPage = {
  render(): HTMLElement {
    const page = document.createElement('main');
    page.className = 'login-wrapper';
    page.innerHTML = `
      <section class="card state-box">
        <h1>Acceso no autorizado</h1>
        <p>No tenés permisos para acceder a esta sección.</p>
        <a class="text-link" href="#/dashboard">Volver al dashboard</a>
      </section>
    `;
    return page;
  },
};
