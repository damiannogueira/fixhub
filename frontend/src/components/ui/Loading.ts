export function renderLoading(message = 'Cargando información...'): HTMLElement {
  const loading = document.createElement('div');
  loading.className = 'state-box';
  loading.innerHTML = `<span class="spinner"></span><p>${message}</p>`;
  return loading;
}
