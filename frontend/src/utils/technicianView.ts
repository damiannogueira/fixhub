import { hasAnyRole } from './authState';

/** Usuario que solo es Técnico (sin rol de Administración ni Recepción). */
export function isTechnicianOnly(): boolean {
  return hasAnyRole(['TECNICO']) && !hasAnyRole(['ADMINISTRADOR', 'RECEPCIONISTA']);
}

/** Tiene rol Técnico además de Administración o Recepción. */
export function isHybridTechnician(): boolean {
  return hasAnyRole(['TECNICO']) && hasAnyRole(['ADMINISTRADOR', 'RECEPCIONISTA']);
}

/**
 * Indica si la pantalla de órdenes debe mostrarse en modo Técnico.
 * Quien solo es Técnico siempre lo ve así; quien combina roles elige con `?vista=tecnico`.
 */
export function wantsTechnicianView(): boolean {
  if (!hasAnyRole(['TECNICO'])) return false;
  if (isTechnicianOnly()) return true;
  return new URLSearchParams(window.location.hash.split('?')[1] ?? '').get('vista') === 'tecnico';
}
