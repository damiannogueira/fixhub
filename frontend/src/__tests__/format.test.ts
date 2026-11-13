import { describe, expect, it } from 'vitest';
import {
  equipmentLabel,
  escapeHtml,
  formatDate,
  formatStatus,
  orderStatusTone,
  technicianLabel,
} from '../utils/format';

describe('format', () => {
  it('escapeHtml escapa caracteres peligrosos', () => {
    expect(escapeHtml(`<a href="x">&'</a>`)).toBe('&lt;a href=&quot;x&quot;&gt;&amp;&#039;&lt;/a&gt;');
  });

  it('formatStatus convierte el enum a texto legible', () => {
    expect(formatStatus('EN_REPARACION')).toBe('En Reparacion');
    expect(formatStatus('LISTA_PARA_RETIRAR')).toBe('Lista Para Retirar');
  });

  it('formatDate devuelve el valor original si la fecha es inválida', () => {
    expect(formatDate('no-es-fecha')).toBe('no-es-fecha');
  });

  it('orderStatusTone asigna el tono según el estado', () => {
    expect(orderStatusTone('CANCELADA')).toBe('danger');
    expect(orderStatusTone('LISTA_PARA_RETIRAR')).toBe('warning');
    expect(orderStatusTone('ENTREGADA')).toBe('success');
    expect(orderStatusTone('EN_REPARACION')).toBe('info');
    expect(orderStatusTone('RECIBIDA' as never)).toBe('neutral');
  });

  it('equipmentLabel omite campos vacíos', () => {
    expect(equipmentLabel({ tipo: 'Notebook', marca: 'Lenovo' })).toBe('Notebook Lenovo');
  });

  it('technicianLabel arma el nombre desde el email o usa fallbacks', () => {
    expect(technicianLabel({ id: 1, email: 'juan.perez@fixhub.com' })).toBe('Juan Perez');
    expect(technicianLabel({ id: 2, telefono: '1122' })).toBe('1122');
    expect(technicianLabel({ id: 3 })).toBe('Técnico #3');
  });
});
