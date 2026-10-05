export const Card = {
  render(content: string): HTMLElement {
    const card = document.createElement('div');
    card.className = 'card';
    card.innerHTML = content;
    return card;
  },
};