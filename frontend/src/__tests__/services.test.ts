import { makeToken } from './fixtures/auth';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { changePassword, login } from '../services/authService';
import { actualizarCliente, listarClientes, obtenerMiPerfil } from '../services/clientesService';
import { obtenerDatosDashboard } from '../services/dashboardService';
import { crearEquipo, listarMisEquipos } from '../services/equiposService';
import { asignarTecnico, cambiarEstadoOrden, cancelarOrden, listarMisOrdenesTecnico, listarOrdenes } from '../services/ordenesService';
import { decidirPresupuesto, publicarPresupuesto } from '../services/presupuestosService';
import { desactivarRepuesto, listarRepuestos } from '../services/repuestosService';
import { actualizarUsuario, desactivarUsuario, listarTecnicos } from '../services/usuariosService';
import { resetAuthState } from '../utils/authState';

type Call = { url: string; method: string; body: unknown };

let calls: Call[];
let nextBody: unknown;

beforeEach(() => {
  calls = [];
  nextBody = [];
  localStorage.clear();
  resetAuthState();
  vi.stubGlobal('fetch', vi.fn(async (url: string, init: RequestInit = {}) => {
    calls.push({
      url,
      method: init.method ?? 'GET',
      body: typeof init.body === 'string' ? JSON.parse(init.body) : undefined,
    });
    return new Response(JSON.stringify(nextBody), { status: 200 });
  }));
});
afterEach(() => vi.unstubAllGlobals());

function last(): Call {
  return calls[calls.length - 1];
}

describe('servicios: URL, método y cuerpo', () => {
  it('auth', async () => {
    await login({ identificador: 'a@b.com', clave: 'x' } as never);
    expect(last().url.endsWith('/auth/login')).toBe(true);
    expect(last().method).toBe('POST');
    expect(last().body).toEqual({ identificador: 'a@b.com', clave: 'x' });

    await changePassword({ claveActual: 'a', claveNueva: 'b', confirmacionClave: 'b' });
    expect(last().url.endsWith('/auth/cambiar-clave')).toBe(true);
    expect(last().method).toBe('POST');
  });

  it('clientes', async () => {
    await listarClientes();
    expect(last().url.endsWith('/clientes')).toBe(true);
    await obtenerMiPerfil();
    expect(last().url.endsWith('/clientes/me')).toBe(true);
    await actualizarCliente(5, { nombre: 'Ana', apellido: 'Paz' });
    expect(last().url.endsWith('/clientes/5')).toBe(true);
    expect(last().method).toBe('PUT');
  });

  it('equipos', async () => {
    await listarMisEquipos();
    expect(last().url.endsWith('/clientes/me/equipos')).toBe(true);
    await crearEquipo(3, { tipo: 'Notebook' } as never);
    expect(last().url.endsWith('/clientes/3/equipos')).toBe(true);
    expect(last().method).toBe('POST');
  });

  it('órdenes', async () => {
    await listarOrdenes();
    expect(last().url.endsWith('/ordenes')).toBe(true);

    await cambiarEstadoOrden(9, 'EN_REPARACION');
    expect(last().url.endsWith('/ordenes/9/estado')).toBe(true);
    expect(last().method).toBe('PATCH');
    expect(last().body).toEqual({ estado: 'EN_REPARACION' });

    await cancelarOrden(9, 'sin repuesto');
    expect(last().url.endsWith('/ordenes/9/cancelacion')).toBe(true);
    expect(last().body).toEqual({ motivo: 'sin repuesto' });
  });

  it('asignarTecnico envía el motivo solo si existe', async () => {
    await asignarTecnico(1, 4);
    expect(last().body).toEqual({ tecnicoId: 4 });
    await asignarTecnico(1, 4, 'licencia');
    expect(last().body).toEqual({ tecnicoId: 4, motivo: 'licencia' });
  });

  it('presupuestos', async () => {
    await decidirPresupuesto(2, 'APROBADO');
    expect(last().url.endsWith('/presupuestos/2/decision')).toBe(true);
    expect(last().body).toEqual({ decision: 'APROBADO' });
    await publicarPresupuesto(2);
    expect(last().url.endsWith('/presupuestos/2/publicar')).toBe(true);
    expect(last().method).toBe('POST');
  });

  it('repuestos', async () => {
    await listarRepuestos();
    expect(last().url.endsWith('/repuestos')).toBe(true);
    await listarRepuestos('  pantalla LCD ');
    expect(last().url.endsWith('/repuestos?nombre=pantalla%20LCD')).toBe(true);
    await desactivarRepuesto(8);
    expect(last().method).toBe('DELETE');
  });

  it('usuarios', async () => {
    await listarTecnicos();
    expect(last().url.endsWith('/usuarios/tecnicos')).toBe(true);
    await actualizarUsuario(3, { roles: ['TECNICO'], activo: true });
    expect(last().url.endsWith('/usuarios/3')).toBe(true);
    expect(last().method).toBe('PUT');
    await desactivarUsuario(3);
    expect(last().url.endsWith('/usuarios/3/desactivar')).toBe(true);
    expect(last().method).toBe('PATCH');
  });
});

describe('dashboardService', () => {
  function setRoles(roles: string[]): void {
    localStorage.setItem('access_token', makeToken({ roles }));
  }

  it('calcula órdenes abiertas, en diagnóstico, listas y repuestos con stock bajo', async () => {
    const responses: unknown[] = [
      [
        { id: 1, estado: 'EN_DIAGNOSTICO' },
        { id: 2, estado: 'LISTA_PARA_RETIRAR' },
        { id: 3, estado: 'ENTREGADA' },
        { id: 4, estado: 'CANCELADA' },
      ],
      [
        { id: 1, stockActual: 1, stockMinimo: 2 },
        { id: 2, stockActual: 5, stockMinimo: 2 },
        { id: 3, stockActual: 2, stockMinimo: 2 },
      ],
    ];
    vi.stubGlobal('fetch', vi.fn(async (url: string) => {
      calls.push({ url, method: 'GET', body: undefined });
      return new Response(JSON.stringify(url.includes('/repuestos') ? responses[1] : responses[0]), { status: 200 });
    }));

    const data = await obtenerDatosDashboard();
    expect(data.openOrders.map((o) => o.id)).toEqual([1, 2]);
    expect(data.diagnosticOrders.map((o) => o.id)).toEqual([1]);
    expect(data.readyOrders.map((o) => o.id)).toEqual([2]);
    expect(data.lowStockParts.map((p) => p.id)).toEqual([1, 3]);
  });

  it('un técnico puro consulta sus órdenes asignadas (/ordenes/me)', async () => {
    const { setAuthenticated } = await import('../utils/authState');
    setRoles(['TECNICO']);
    setAuthenticated({ token: localStorage.getItem('access_token'), requiereCambioClave: false } as never);
    await obtenerDatosDashboard();
    expect(calls.some((c) => c.url.endsWith('/ordenes/me'))).toBe(true);
    expect(calls.some((c) => c.url.endsWith('/ordenes'))).toBe(false);
    await listarMisOrdenesTecnico();
  });
});
