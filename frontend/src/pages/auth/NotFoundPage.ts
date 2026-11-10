export const NotFoundPage = {
  render(): HTMLElement {
    const page = document.createElement('main');
    page.className = 'login-wrapper';
    page.innerHTML = `
      <section class="card state-box">
        <p class="eyebrow">Error 404</p>
        <h1>Página no encontrada</h1>
        <p>La página que buscás no existe o no está disponible.</p>
        <a class="text-link" href="#/login">Volver al inicio</a>
      </section>
    `;
    return page;
  },
};
