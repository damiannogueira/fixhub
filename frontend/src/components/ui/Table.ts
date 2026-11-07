export function renderTable(headers: string[], rows: string[][]): HTMLElement {
  const table = document.createElement('div');
  table.className = 'table-wrapper';
  table.innerHTML = `
    <table>
      <thead><tr>${headers.map((header) => `<th>${header}</th>`).join('')}</tr></thead>
      <tbody>${rows.map((row) => `<tr>${row.map((cell) => `<td>${cell}</td>`).join('')}</tr>`).join('')}</tbody>
    </table>
  `;
  return table;
}
