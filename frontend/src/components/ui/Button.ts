interface ButtonProps {
  text: string;
  type?: 'button' | 'submit';
  variant?: 'primary' | 'secondary';
  className?: string;
}

export const Button = {
  render({ text, type = 'button', variant = 'primary', className = '' }: ButtonProps): HTMLElement {
    const button = document.createElement('button');
    button.type = type;
    button.className = `btn btn-${variant} ${className}`.trim();
    button.textContent = text;
    return button;
  },
};