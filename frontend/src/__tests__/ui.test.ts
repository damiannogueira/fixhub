import { describe, expect, it } from 'vitest';
import { renderBadge } from '../components/ui/Badge';
import { Button } from '../components/ui/Button';
import { Card } from '../components/ui/Card';
import { renderEmptyState } from '../components/ui/EmptyState';
import { Input } from '../components/ui/Input';
import { renderLoading } from '../components/ui/Loading';
import { renderTable } from '../components/ui/Table';
import { renderRepairList } from '../components/shared/RepairList';
import type { OrdenTrabajo } from '../types/orden';

describe('componentes UI', () => {
  it('Badge reemplaza guiones bajos y aplica el tono (neutral por defecto)', () => {
    const badge = renderBadge('EN_REPARACION', 'info');
    expect(badge.textContent).toBe('EN REPARACION');
    expect(badge.className).toBe('badge badge-info');
    expect(renderBadge('x').className).toBe('badge badge-neutral');
  });

  it('Button usa type=button y variante primary por defecto', () => {
    const button = Button.render({ text: 'Guardar' }) as HTMLButtonElement;
    expect(button.type).toBe('button');
    expect(button.className).toBe('btn btn-primary');
    expect(button.textContent).toBe('Guardar');
  });

  it('Button acepta submit, variante y clase extra', () => {
    const button = Button.render({ text: 'Enviar', type: 'submit', variant: 'secondary', className: 'extra' }) as HTMLButtonElement;
    expect(button.type).toBe('submit');
    expect(button.className).toBe('btn btn-secondary extra');
  });

  it('Card inserta el contenido', () => {
    const card = Card.render('<p>hola</p>');
    expect(card.className).toBe('card');
    expect(card.querySelector('p')?.textContent).toBe('hola');
  });

  it('EmptyState muestra título y mensaje', () => {
    const state = renderEmptyState('Sin datos', 'No hay órdenes');
    expect(state.querySelector('h3')?.textContent).toBe('Sin datos');
    expect(state.querySelector('p')?.textContent).toBe('No hay órdenes');
  });

  it('Loading muestra el mensaje por defecto o uno propio', () => {
    expect(renderLoading().querySelector('p')?.textContent).toBe('Cargando información...');
    expect(renderLoading('Esperá').querySelector('p')?.textContent).toBe('Esperá');
    expect(renderLoading().querySelector('.spinner')).not.toBeNull();
  });

  it('Input vincula label e input y respeta value y required', () => {
    const wrapper = Input.render({ label: 'Email', type: 'email', id: 'email', value: 'a@b.com', required: true });
    const input = wrapper.querySelector('input') as HTMLInputElement;
    expect(wrapper.querySelector('label')?.getAttribute('for')).toBe('email');
    expect(input.id).toBe('email');
    expect(input.type).toBe('email');
    expect(input.value).toBe('a@b.com');
    expect(input.required).toBe(true);
  });

  it('Table genera la cantidad correcta de columnas y filas', () => {
    const table = renderTable(['A', 'B'], [['1', '2'], ['3', '4'], ['5', '6']]);
    expect(table.querySelectorAll('th')).toHaveLength(2);
    expect(table.querySelectorAll('tbody tr')).toHaveLength(3);
  });

  it('Table sin filas no rompe', () => {
    const table = renderTable(['A'], []);
    expect(table.querySelectorAll('tbody tr')).toHaveLength(0);
  });

  // Contrato: Table y EmptyState insertan HTML tal cual; quien los llama debe escapar con escapeHtml.
  it('Table inserta el HTML recibido sin escapar (el llamador debe escapar)', () => {
    const table = renderTable(['A'], [['<b>x</b>']]);
    expect(table.querySelector('td b')).not.toBeNull();
  });
});

describe('RepairList', () => {
  const order = {
    id: 7,
    estado: 'ENTREGADA',
    problemaInicial: '<script>alert(1)</script>',
    fechaIngreso: '2026-01-10T10:00:00',
    equipo: { tipo: 'Notebook', marca: 'Lenovo' },
  } as unknown as OrdenTrabajo;

  it('muestra mensaje vacío escapado cuando no hay órdenes', () => {
    expect(renderRepairList([], '<i>nada</i>')).toBe('<p class="muted">&lt;i&gt;nada&lt;/i&gt;</p>');
  });

  it('enlaza al detalle y escapa el texto ingresado por el usuario', () => {
    const container = document.createElement('div');
    container.innerHTML = renderRepairList([order], 'vacío');
    expect(container.querySelector('a')?.getAttribute('href')).toBe('#/ordenes/detalle?id=7');
    expect(container.querySelector('script')).toBeNull();
    expect(container.textContent).toContain('<script>alert(1)</script>');
    expect(container.textContent).toContain('Notebook Lenovo');
  });

  it('oculta el equipo si showEquipment es false', () => {
    const container = document.createElement('div');
    container.innerHTML = renderRepairList([order], 'vacío', false);
    expect(container.textContent).not.toContain('Notebook');
  });
});
