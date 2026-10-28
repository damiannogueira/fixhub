import { Card } from '../../components/ui/Card';
import { Input } from '../../components/ui/Input';
import { Button } from '../../components/ui/Button';
import { router } from '../../router/router';

export const LoginPage = {
  render(): HTMLElement {
    const wrapper = document.createElement('div');
    wrapper.className = 'login-wrapper';

    const cardContent = `
      <div class="login-header">
        <img src="/src/assets/img/Logo tecnológico FixHub con soporte central.png" alt="FixHub Logo" class="login-logo" />
        <p class="login-subtitle">Tu Plataforma de Soporte Central</p>
      </div>
      <form id="login-form" class="login-form"></form>
    `;
    
    const card = Card.render(cardContent);
    const form = card.querySelector('#login-form') as HTMLFormElement;

    form.appendChild(Input.render({ 
      label: 'Usuario o Email', 
      type: 'text', 
      id: 'username', 
      placeholder: 'ejemplo@fixhub.com', 
      required: true 
    }));
    
    form.appendChild(Input.render({ 
      label: 'Contraseña', 
      type: 'password', 
      id: 'password', 
      placeholder: '••••••••', 
      required: true 
    }));

    const submitBtn = Button.render({ 
      text: 'Ingresar', 
      type: 'submit', 
      variant: 'primary', 
      className: 'btn-full' 
    });
    form.appendChild(submitBtn);

    form.addEventListener('submit', (e) => {
      e.preventDefault();
      const formData = new FormData(form);
      const username = formData.get('username');
      
      console.log('Login simulado para:', username);
      router.navigate('/dashboard');
    });

    wrapper.appendChild(card);
    return wrapper;
  },
};