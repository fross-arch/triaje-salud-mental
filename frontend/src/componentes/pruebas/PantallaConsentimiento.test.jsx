import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { PantallaConsentimiento } from '../../pantallas/PantallaConsentimiento.jsx';
import { servicioConsentimiento } from '../../servicios/servicioConsentimiento.js';
import { ContextoAutenticacion } from '../../contextos/ContextoAutenticacion.jsx';

vi.mock('../../servicios/servicioConsentimiento.js', () => ({
  servicioConsentimiento: {
    registrarConsentimiento: vi.fn(),
    consultarVigente: vi.fn(),
    revocar: vi.fn()
  }
}));

const mockNavegacion = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavegacion
  };
});

describe('PantallaConsentimiento', () => {
  const mockCerrarSesion = vi.fn();

  const renderizarComponente = () => {
    return render(
      <ContextoAutenticacion.Provider
        value={{
          usuario: { id: '1', nombreCompleto: 'Usuario Prueba', correo: 'prueba@test.com' },
          cerrarSesion: mockCerrarSesion,
          estaAutenticado: true
        }}
      >
        <MemoryRouter>
          <PantallaConsentimiento />
        </MemoryRouter>
      </ContextoAutenticacion.Provider>
    );
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('bloquea el avance y mantiene deshabilitado el boton si alguna o ambas casillas no estan marcadas', async () => {
    renderizarComponente();

    const botonAceptar = screen.getByRole('button', { name: /aceptar y continuar/i });
    const casillaTratamiento = screen.getByLabelText(/autorizo de forma previa/i);
    const casillaMayoria = screen.getByLabelText(/declaro bajo gravedad de juramento/i);

    // Ambas desmarcadas por defecto
    expect(casillaTratamiento).not.toBeChecked();
    expect(casillaMayoria).not.toBeChecked();
    expect(botonAceptar).toBeDisabled();

    const usuarioInteractivo = userEvent.setup();

    // Marcar solo casilla 1
    await usuarioInteractivo.click(casillaTratamiento);
    expect(casillaTratamiento).toBeChecked();
    expect(casillaMayoria).not.toBeChecked();
    expect(botonAceptar).toBeDisabled();

    // Desmarcar casilla 1 y marcar solo casilla 2
    await usuarioInteractivo.click(casillaTratamiento);
    await usuarioInteractivo.click(casillaMayoria);
    expect(casillaTratamiento).not.toBeChecked();
    expect(casillaMayoria).toBeChecked();
    expect(botonAceptar).toBeDisabled();
  });

  it('habilita el avance y registra el consentimiento exitosamente al marcar ambas casillas obligatorias', async () => {
    servicioConsentimiento.registrarConsentimiento.mockResolvedValueOnce({ id: 10 });
    renderizarComponente();

    const botonAceptar = screen.getByRole('button', { name: /aceptar y continuar/i });
    const casillaTratamiento = screen.getByLabelText(/autorizo de forma previa/i);
    const casillaMayoria = screen.getByLabelText(/declaro bajo gravedad de juramento/i);

    const usuarioInteractivo = userEvent.setup();

    await usuarioInteractivo.click(casillaTratamiento);
    await usuarioInteractivo.click(casillaMayoria);

    expect(casillaTratamiento).toBeChecked();
    expect(casillaMayoria).toBeChecked();
    expect(botonAceptar).toBeEnabled();

    await usuarioInteractivo.click(botonAceptar);

    expect(servicioConsentimiento.registrarConsentimiento).toHaveBeenCalledTimes(1);
    expect(servicioConsentimiento.registrarConsentimiento).toHaveBeenCalledWith(true);
  });

  it('permite rechazar y salir mediante el boton de no aceptacion', async () => {
    renderizarComponente();

    const botonRechazar = screen.getByRole('button', { name: /no acepto y salir/i });
    const usuarioInteractivo = userEvent.setup();

    await usuarioInteractivo.click(botonRechazar);

    expect(mockCerrarSesion).toHaveBeenCalledTimes(1);
    expect(mockNavegacion).toHaveBeenCalledWith('/iniciar-sesion');
  });

  it('redirige a /introduccion si el consentimiento ya se encuentra vigente', async () => {
    servicioConsentimiento.consultarVigente.mockResolvedValueOnce({ vigente: true });
    renderizarComponente();

    await vi.waitFor(() => {
      expect(mockNavegacion).toHaveBeenCalledWith('/introduccion', { replace: true });
    });
  });
});

