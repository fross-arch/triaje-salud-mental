import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ProveedorAutenticacion, ContextoAutenticacion } from '../../contextos/ContextoAutenticacion.jsx';
import { servicioAutenticacion } from '../../servicios/servicioAutenticacion.js';
import { obtenerToken } from '../../servicios/clienteHttp.js';

vi.mock('../../servicios/servicioAutenticacion.js', () => ({
  servicioAutenticacion: {
    renovar: vi.fn(),
    iniciarSesion: vi.fn(),
    registrar: vi.fn(),
    cerrarSesion: vi.fn(),
    obtenerPerfil: vi.fn(),
    eliminarCuenta: vi.fn()
  }
}));

function ComponenteConsumidor() {
  const {
    tokenAcceso,
    usuario,
    estaAutenticado,
    iniciarSesion,
    cerrarSesion,
    eliminarCuenta
  } = React.useContext(ContextoAutenticacion);

  return (
    <div>
      <span data-testid="estado-autenticado">{estaAutenticado ? 'si' : 'no'}</span>
      <span data-testid="valor-token">{tokenAcceso || 'sin-token'}</span>
      <span data-testid="nombre-usuario">{usuario?.nombreCompleto || 'sin-usuario'}</span>

      <button
        onClick={() => iniciarSesion('test@cesde.net', 'clave123')}
        data-testid="boton-login"
      >
        Ingresar
      </button>

      <button onClick={() => cerrarSesion()} data-testid="boton-logout">
        Cerrar
      </button>

      <button onClick={() => eliminarCuenta()} data-testid="boton-eliminar">
        Eliminar
      </button>
    </div>
  );
}

describe('ContextoAutenticacion', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
    sessionStorage.clear();
  });

  it('mantiene el token JWT estrictamente en memoria y no utiliza localStorage ni sessionStorage', async () => {
    servicioAutenticacion.renovar.mockRejectedValueOnce(new Error('Sin cookie'));
    servicioAutenticacion.iniciarSesion.mockResolvedValueOnce({
      tokenAcceso: 'jwt-en-memoria-12345',
      expiraEnSegundos: 900
    });
    servicioAutenticacion.obtenerPerfil.mockResolvedValueOnce({
      id: '1',
      nombreCompleto: 'Estudiante CESDE',
      correo: 'test@cesde.net'
    });

    render(
      <ProveedorAutenticacion>
        <ComponenteConsumidor />
      </ProveedorAutenticacion>
    );

    const usuarioInteractivo = userEvent.setup();
    const botonLogin = screen.getByTestId('boton-login');

    await usuarioInteractivo.click(botonLogin);

    // Verificamos que el token este en memoria
    expect(screen.getByTestId('estado-autenticado')).toHaveTextContent('si');
    expect(screen.getByTestId('valor-token')).toHaveTextContent('jwt-en-memoria-12345');
    expect(screen.getByTestId('nombre-usuario')).toHaveTextContent('Estudiante CESDE');
    expect(obtenerToken()).toBe('jwt-en-memoria-12345');

    // REGLA CRITICA: Verificamos que localStorage y sessionStorage sigan completamente vacios
    expect(localStorage.getItem('token')).toBeNull();
    expect(localStorage.getItem('tokenAcceso')).toBeNull();
    expect(sessionStorage.getItem('token')).toBeNull();
    expect(sessionStorage.getItem('tokenAcceso')).toBeNull();
    expect(localStorage.length).toBe(0);
    expect(sessionStorage.length).toBe(0);
  });

  it('limpia el token de la memoria al cerrar sesion', async () => {
    servicioAutenticacion.renovar.mockResolvedValueOnce({
      tokenAcceso: 'jwt-activo-999',
      expiraEnSegundos: 900
    });
    servicioAutenticacion.obtenerPerfil.mockResolvedValueOnce({
      id: '2',
      nombreCompleto: 'Usuario Activo',
      correo: 'activo@cesde.net'
    });
    servicioAutenticacion.cerrarSesion.mockResolvedValueOnce(null);

    render(
      <ProveedorAutenticacion>
        <ComponenteConsumidor />
      </ProveedorAutenticacion>
    );

    expect(await screen.findByTestId('estado-autenticado')).toHaveTextContent('si');

    const usuarioInteractivo = userEvent.setup();
    const botonLogout = screen.getByTestId('boton-logout');
    await usuarioInteractivo.click(botonLogout);

    expect(screen.getByTestId('estado-autenticado')).toHaveTextContent('no');
    expect(screen.getByTestId('valor-token')).toHaveTextContent('sin-token');
    expect(obtenerToken()).toBeNull();
  });

  it('maneja silenciosamente el 401 en la comprobación inicial de sesión sin llamar a console.error y finalizando la carga', async () => {
    const error401 = new Error('HTTP 401 Unauthorized');
    error401.estado = 401;
    servicioAutenticacion.renovar.mockRejectedValueOnce(error401);

    const spyConsoleError = vi.spyOn(console, 'error').mockImplementation(() => {});

    function ComponenteIndicadorCarga() {
      const { cargando, tokenAcceso, usuario, estaAutenticado } = React.useContext(ContextoAutenticacion);
      return (
        <div>
          <span data-testid="cargando">{cargando ? 'cargando' : 'listo'}</span>
          <span data-testid="token">{tokenAcceso || 'sin-token'}</span>
          <span data-testid="usuario">{usuario ? 'con-usuario' : 'sin-usuario'}</span>
          <span data-testid="autenticado">{estaAutenticado ? 'si' : 'no'}</span>
        </div>
      );
    }

    render(
      <ProveedorAutenticacion>
        <ComponenteIndicadorCarga />
      </ProveedorAutenticacion>
    );

    expect(await screen.findByText('listo')).toBeInTheDocument();
    expect(screen.getByTestId('token')).toHaveTextContent('sin-token');
    expect(screen.getByTestId('usuario')).toHaveTextContent('sin-usuario');
    expect(screen.getByTestId('autenticado')).toHaveTextContent('no');
    expect(spyConsoleError).not.toHaveBeenCalled();

    spyConsoleError.mockRestore();
  });
});
