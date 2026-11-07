export function renderBadge(text: string, tone: 'success' | 'warning' | 'danger' | 'info' | 'neutral' = 'neutral'): HTMLElement {
  const badge = document.createElement('span');
  badge.className = `badge badge-${tone}`;
  badge.textContent = text.replaceAll('_', ' ');
  return badge;
}
