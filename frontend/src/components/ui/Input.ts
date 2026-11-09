interface InputProps {
  label: string;
  type: string;
  id: string;
  placeholder?: string;
  value?: string;
  required?: boolean;
}

export const Input = {
  render({ label, type, id, placeholder = '', value = '', required = false }: InputProps): HTMLElement {
    const wrapper = document.createElement('div');
    wrapper.className = 'form-group';

    wrapper.innerHTML = `
      <label for="${id}" class="form-label">${label}</label>
      <input type="${type}" id="${id}" name="${id}" value="${value}" placeholder="${placeholder}" ${required ? 'required' : ''} class="form-input" />
    `;
    return wrapper;
  },
};