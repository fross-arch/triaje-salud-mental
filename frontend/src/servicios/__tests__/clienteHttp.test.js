import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { peticionHttp, clienteHttp, redirigirNoAutorizado, establecerToken } from '../clienteHttp.js';

describe('clienteHttp', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    establecerToken(null);
    global.fetch = vi.fn();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('incluye credentials: "include" en todas las peticiones', async () => {
    global.fetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => ({ datos: 'ok' })
    });

    await peticionHttp('/api/prueba');

    expect(global.fetch).toHaveBeenCalledTimes(1);
    const [, config] = global.fetch.mock.calls[0];
    expect(config.credentials).toBe('include');
  });

  it('captura el error 403 de forma controlada y redirige hacia /no-autorizado', async () => {
    global.fetch.mockResolvedValueOnce({
      ok: false,
      status: 403,
      json: async () => ({ mensaje: 'Acceso no permitido' })
    });

    // Mockeamos window.location
    const originalLocation = window.location;
    delete window.location;
    window.location = {
      pathname: '/clinico',
      assign: vi.fn(),
      href: 'http://localhost/clinico'
    };

    let errorCapturado = null;
    try {
      await peticionHttp('/api/clinico/evaluaciones');
    } catch (err) {
      errorCapturado = err;
    }

    expect(errorCapturado).not.toBeNull();
    expect(errorCapturado.estado).toBe(403);
    expect(errorCapturado.message).toBe('Acceso no permitido');
    expect(errorCapturado.mensaje).toBe('Acceso no permitido');
    expect(window.location.assign).toHaveBeenCalledWith('/no-autorizado');

    window.location = originalLocation;
  });

  it('no redirige a /no-autorizado ante un 403 si la petición es una comprobación silenciosa', async () => {
    global.fetch.mockResolvedValueOnce({
      ok: false,
      status: 403,
      json: async () => ({ mensaje: 'Forbidden silencioso' })
    });

    const originalLocation = window.location;
    delete window.location;
    window.location = {
      pathname: '/login',
      assign: vi.fn(),
      href: 'http://localhost/login'
    };

    let errorCapturado = null;
    try {
      await peticionHttp('/api/verificacion-pasiva', { silencioso: true });
    } catch (err) {
      errorCapturado = err;
    }

    expect(errorCapturado).not.toBeNull();
    expect(errorCapturado.estado).toBe(403);
    expect(window.location.assign).not.toHaveBeenCalled();

    window.location = originalLocation;
  });

  it('en clienteHttp.enviar a /api/autenticacion/renovar incluye credentials: "include"', async () => {
    global.fetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => ({ tokenAcceso: 'token-refrescado-123' })
    });

    const resultado = await clienteHttp.enviar('/api/autenticacion/renovar', {}, { silencioso: true });
    expect(resultado.tokenAcceso).toBe('token-refrescado-123');

    const [, config] = global.fetch.mock.calls[0];
    expect(config.credentials).toBe('include');
  });
});
