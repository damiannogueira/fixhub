import type { EstadoOrden } from '../types/orden';

export type Tone = 'success' | 'warning' | 'danger' | 'info' | 'neutral';

export function escapeHtml(value: string): string {
  return value
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll('\'', '&#039;');
}

export function formatDate(value: string): string {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleDateString('es-AR');
}

export function formatDateTime(value: string): string {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString('es-AR');
}

export function formatMoney(value: number): string {
  return new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' }).format(value);
}

/** "EN_REPARACION" → "En Reparacion" (mismo criterio que el resto de las pantallas). */
export function formatStatus(status: string): string {
  return status.replaceAll('_', ' ').toLowerCase().replace(/(^| )\w/g, (letter) => letter.toUpperCase());
}

export function orderStatusTone(status: EstadoOrden): Tone {
  if (status === 'CANCELADA') return 'danger';
  if (status === 'LISTA_PARA_RETIRAR') return 'warning';
  if (status === 'ENTREGADA' || status === 'FINALIZADA') return 'success';
  if (status === 'EN_REPARACION' || status === 'APROBADA') return 'info';
  return 'neutral';
}

export function equipmentLabel(equipo: { tipo: string; marca?: string; modelo?: string }): string {
  return [equipo.tipo, equipo.marca, equipo.modelo].filter(Boolean).join(' ');
}

/** Nombre legible de un técnico a partir de su email (o teléfono). */
export function technicianLabel(technician: { id: number; email?: string; telefono?: string }): string {
  if (technician.email) {
    return technician.email.split('@')[0]
      .split(/[._-]+/)
      .filter(Boolean)
      .map((part) => part.charAt(0).toUpperCase() + part.slice(1).toLowerCase())
      .join(' ');
  }
  return technician.telefono ?? `Técnico #${technician.id}`;
}
