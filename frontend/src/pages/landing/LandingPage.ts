import { router } from '../../router/router';
import logoUrl from '../../assets/img/Logo tecnológico FixHub con soporte central.png';
import heroUrl from '../../assets/img/ricardo-iv-tamayo-nApaD08bFAE-unsplash.jpg';
import boardUrl from '../../assets/img/hugo-clement-puCEUxEr3xk-unsplash.jpg';
import toolsUrl from '../../assets/img/markus-spiske-eSuHtTPcLlo-unsplash.jpg';

const benefits = [
  {
    icon: '▤',
    title: 'Órdenes bajo control',
    text: 'Registrá cada reparación y seguí su estado desde el ingreso del equipo hasta la entrega.',
  },
  {
    icon: '$',
    title: 'Presupuestos claros',
    text: 'Armá presupuestos detallados y dejá que el cliente los consulte y los apruebe en línea.',
  },
  {
    icon: '◫',
    title: 'Inventario al día',
    text: 'Controlá repuestos y stock para saber qué tenés disponible antes de prometer un plazo.',
  },
  {
    icon: '◉',
    title: 'Portal para clientes',
    text: 'Cada cliente ve sus equipos, reparaciones y presupuestos sin tener que llamar al taller.',
  },
  {
    icon: '♙',
    title: 'Roles y permisos',
    text: 'Administración, recepción y técnicos acceden solo a lo que necesitan para su trabajo.',
  },
  {
    icon: '▦',
    title: 'Panel con métricas',
    text: 'Un tablero con lo importante del día para decidir rápido y sin planillas sueltas.',
  },
];

const steps = [
  { title: 'Recepción', text: 'Se registra el cliente y el equipo con el problema reportado.' },
  { title: 'Diagnóstico', text: 'El técnico evalúa, carga repuestos y genera el presupuesto.' },
  { title: 'Reparación', text: 'El cliente aprueba y el estado se actualiza en cada avance.' },
  { title: 'Entrega', text: 'El equipo vuelve al cliente con todo el historial registrado.' },
];

export const LandingPage = {
  render(): HTMLElement {
    const page = document.createElement('div');
    page.className = 'landing';

    page.innerHTML = `
      <header class="landing-nav">
        <a class="landing-brand" href="#/" aria-label="FixHub inicio">
          <img src="${logoUrl}" alt="FixHub" />
        </a>
        <nav class="landing-nav-links" aria-label="Secciones">
          <button type="button" class="landing-link" data-scroll="beneficios">Beneficios</button>
          <button type="button" class="landing-link" data-scroll="proceso">Cómo funciona</button>
          <button type="button" class="landing-btn landing-btn-accent" data-go="/login">Ingresar</button>
        </nav>
      </header>

      <section class="landing-hero">
        <div class="landing-hero-inner">
          <span class="landing-pill">Gestión de servicio técnico</span>
          <h1>Todo tu taller de reparaciones, en un solo lugar</h1>
          <p>
            FixHub ordena la recepción de equipos, los presupuestos, los repuestos y la
            comunicación con tus clientes, para que te dediques a reparar.
          </p>
          <div class="landing-hero-actions">
            <button type="button" class="landing-btn landing-btn-accent landing-btn-lg" data-go="/login">
              Ingresar al sistema
            </button>
            <button type="button" class="landing-btn landing-btn-ghost landing-btn-lg" data-scroll="beneficios">
              Conocer más
            </button>
          </div>
          <ul class="landing-hero-stats">
            <li><strong>4</strong><span>roles de usuario</span></li>
            <li><strong>100%</strong><span>trazabilidad de órdenes</span></li>
            <li><strong>24/7</strong><span>acceso para clientes</span></li>
          </ul>
        </div>
      </section>

      <section class="landing-section" id="beneficios">
        <div class="landing-container">
          <div class="landing-heading">
            <span class="landing-eyebrow">Beneficios</span>
            <h2>Menos desorden, más reparaciones resueltas</h2>
            <p>Herramientas pensadas para el día a día de un servicio técnico.</p>
          </div>
          <div class="landing-grid">
            ${benefits
              .map(
                (b) => `
              <article class="landing-card">
                <span class="landing-card-icon" aria-hidden="true">${b.icon}</span>
                <h3>${b.title}</h3>
                <p>${b.text}</p>
              </article>`,
              )
              .join('')}
          </div>
        </div>
      </section>

      <section class="landing-section">
        <div class="landing-container landing-gallery">
          <div class="landing-gallery-text">
            <span class="landing-eyebrow">Pensado para talleres reales</span>
            <h2>Cada equipo, cada repuesto y cada herramienta, en orden</h2>
            <p>
              Desde un televisor abierto sobre la mesa hasta el cajón de repuestos: FixHub deja
              registrado qué se hizo, con qué materiales y en qué estado está cada trabajo.
            </p>
            <ul class="landing-checks">
              <li>Historial completo de cada equipo</li>
              <li>Repuestos asociados a cada reparación</li>
              <li>Todo el equipo trabajando sobre la misma información</li>
            </ul>
          </div>
          <div class="landing-collage">
            <img class="landing-collage-main" src="${boardUrl}" alt="Placa electrónica de un televisor en reparación" loading="lazy" />
            <img class="landing-collage-side" src="${toolsUrl}" alt="Herramientas y repuestos de un taller" loading="lazy" />
          </div>
        </div>
      </section>

      <section class="landing-section landing-section-alt" id="proceso">
        <div class="landing-container">
          <div class="landing-heading">
            <span class="landing-eyebrow">Cómo funciona</span>
            <h2>De la recepción a la entrega, paso a paso</h2>
          </div>
          <ol class="landing-steps">
            ${steps
              .map(
                (s, i) => `
              <li class="landing-step">
                <span class="landing-step-number">${i + 1}</span>
                <h3>${s.title}</h3>
                <p>${s.text}</p>
              </li>`,
              )
              .join('')}
          </ol>
        </div>
      </section>

      <section class="landing-cta">
        <div class="landing-container">
          <h2>¿Listo para ordenar tu taller?</h2>
          <p>Ingresá con tu usuario y empezá a gestionar tus reparaciones.</p>
          <button type="button" class="landing-btn landing-btn-accent landing-btn-lg" data-go="/login">
            Ingresar
          </button>
        </div>
      </section>

      <footer class="landing-footer">
        <span>© ${new Date().getFullYear()} FixHub · Plataforma de soporte central</span>
      </footer>
    `;

    page.querySelector<HTMLElement>('.landing-hero')?.style.setProperty(
      '--landing-hero-image',
      `url("${heroUrl}")`,
    );

    page.querySelectorAll<HTMLElement>('[data-go]').forEach((el) => {
      el.addEventListener('click', () => router.navigate(el.dataset.go ?? '/login'));
    });

    page.querySelectorAll<HTMLElement>('[data-scroll]').forEach((el) => {
      el.addEventListener('click', () => {
        page.querySelector(`#${el.dataset.scroll}`)?.scrollIntoView({ behavior: 'smooth' });
      });
    });

    return page;
  },
};
