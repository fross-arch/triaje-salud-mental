import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { PantallaPerfil } from '../../pantallas/PantallaPerfil.jsx';
import * as useAutenticacionModulo from '../../hooks/useAutenticacion.js';
import { servicioTriaje } from '../../servicios/servicioTriaje.js';
import { servicioConsentimiento } from '../../servicios/servicioConsentimiento.js';

vi.mock('../../servicios/servicioTriaje.js', () => ({
  servicioTriaje: {
    listarEvaluaciones: vi.fn()
  }
}));

vi.mock('../../servicios/servicioConsentimiento.js', () => ({
  servicioConsentimiento: {
    consultarVigente: vi.fn(),
    revocar: vi.fn()
  }
}));

describe('PantallaPerfil', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('oculta por completo las opciones de consentimiento y eliminación de cuenta para el rol ADMINISTRADOR', async () => {
    vi.spyOn(useAutenticacionModulo, 'useAutenticacion').mockReturnValue({
      usuario: {
        id: 1,
        nombreCompleto: 'Admin Principal',
        correo: 'admin@salud.org',
        rol: 'ADMINISTRADOR'
      },
      eliminarCuenta: vi.fn(),
      estaAutenticado: true
    });
    render(
      <MemoryRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
        <PantallaPerfil />
      </MemoryRouter>
    );

    // Muestra datos del administrador
    expect(await screen.findByText('Admin Principal')).toBeInTheDocument();
    expect(screen.getByText('admin@salud.org')).toBeInTheDocument();

    // NO debe llamar a endpoints de evaluaciones ni de consentimientos
    expect(servicioTriaje.listarEvaluaciones).not.toHaveBeenCalled();
    expect(servicioConsentimiento.consultarVigente).not.toHaveBeenCalled();

    // NO debe existir la sección ni botones de consentimiento o eliminación
    expect(screen.queryByText(/Gestión de privacidad y ejercicio de derechos/i)).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Revocar consentimiento/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Otorgar consentimiento/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Eliminar mi cuenta y datos/i })).not.toBeInTheDocument();
  });

  it('no consulta consentimientos ni evaluaciones de triaje para el rol CLINICO', async () => {
    vi.spyOn(useAutenticacionModulo, 'useAutenticacion').mockReturnValue({
      usuario: {
        id: 2,
        nombreCompleto: 'Dra. Clinica',
        correo: 'clinico@salud.org',
        rol: 'CLINICO'
      },
      eliminarCuenta: vi.fn(),
      estaAutenticado: true
    });

    render(
      <MemoryRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
        <PantallaPerfil />
      </MemoryRouter>
    );

    expect(await screen.findByText('Dra. Clinica')).toBeInTheDocument();
    expect(screen.getByText('clinico@salud.org')).toBeInTheDocument();

    // Verificación estricta: NO se ejecutan llamadas a cribado ni consentimiento
    expect(servicioTriaje.listarEvaluaciones).not.toHaveBeenCalled();
    expect(servicioConsentimiento.consultarVigente).not.toHaveBeenCalled();

    // Tampoco debe mostrar sección de historial ni de revocación de cribados
    expect(screen.queryByText(/Historial de cribados realizados/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/Gestión de privacidad y ejercicio de derechos/i)).not.toBeInTheDocument();
  });

  it('muestra modal no destructivo si un paciente intenta revocar consentimiento con una evaluación EN_CURSO', async () => {
    vi.spyOn(useAutenticacionModulo, 'useAutenticacion').mockReturnValue({
      usuario: {
        id: 10,
        nombreCompleto: 'Paciente Con Evaluacion En Curso',
        correo: 'paciente@ejemplo.com',
        rol: 'PACIENTE'
      },
      eliminarCuenta: vi.fn(),
      estaAutenticado: true
    });

    servicioTriaje.listarEvaluaciones.mockResolvedValueOnce([
      {
        id: 'eval-en-curso-1',
        estado: 'EN_CURSO',
        fechaCreacion: '2026-10-16T12:00:00Z'
      }
    ]);
    servicioConsentimiento.consultarVigente.mockResolvedValueOnce({ vigente: true });

    render(
      <MemoryRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
        <PantallaPerfil />
      </MemoryRouter>
    );

    expect(await screen.findByText('Paciente Con Evaluacion En Curso')).toBeInTheDocument();

    // Verificación: Para PACIENTE sí se consultan los endpoints
    expect(servicioTriaje.listarEvaluaciones).toHaveBeenCalledTimes(1);
    expect(servicioConsentimiento.consultarVigente).toHaveBeenCalledTimes(1);

    const usuario = userEvent.setup();
    const botonRevocar = screen.getByRole('button', { name: /Revocar consentimiento/i });
    await usuario.click(botonRevocar);

    // Debe mostrar la alerta modal no destructiva informativa
    expect(
      screen.getByText(/Hay una evaluación en curso; debes finalizarla o cancelarla antes de gestionar tus consentimientos/i)
    ).toBeInTheDocument();

    // No debe haber llamado a revocar en el backend
    expect(servicioConsentimiento.revocar).not.toHaveBeenCalled();
  });
});
