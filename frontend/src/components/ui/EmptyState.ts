export function renderEmptyState(title: string, message: string): HTMLElement {
  const state = document.createElement('div');
  state.className = 'state-box';
  state.innerHTML = `<span class="empty-icon">○</span><h3>${title}</h3><p>${message}</p>`;
  return state;
}
