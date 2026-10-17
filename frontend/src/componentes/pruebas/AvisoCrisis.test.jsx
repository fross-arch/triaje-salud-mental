import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { AvisoCrisis } from '../triaje/AvisoCrisis.jsx';
import { servicioRecursosAyuda } from '../../servicios/servicioRecursosAyuda.js';

vi.mock('../../servicios/servicioRecursosAyuda.js', () => ({
  servicioRecursosAyuda: {
    obtenerRecursos: vi.fn()
  }
}));

describe('AvisoCrisis', () => {
  const recursosFalsos = [
    {
      nombre: 'Línea 106 Amiga Saludable',
      telefono: '106',
      horario: '24 horas',
      descripcion: 'Atención psicológica y acompañamiento en crisis',
      ambito: 'Medellín'
    },
    {
      nombre: 'Línea Nacional 192',
      telefono: '192',
      horario: '24 horas',
      descripcion: 'Apoyo en salud mental a nivel nacional',
      ambito: 'Nacional'
    }
  ];

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('aparece de forma prominente con rol de alerta e incluye las lineas de ayuda cargadas', async () => {
    servicioRecursosAyuda.obtenerRecursos.mockResolvedValueOnce(recursosFalsos);

    render(
      <AvisoCrisis
        alContinuar={vi.fn()}
        alSalir={vi.fn()}
        mostrarBotonContinuar={true}
      />
    );

    const seccionAlerta = screen.getByRole('alert');
    expect(seccionAlerta).toBeInTheDocument();
    expect(seccionAlerta).toHaveAttribute('aria-live', 'assertive');

    expect(
      await screen.findByText(/Línea 106 Amiga Saludable/i)
    ).toBeInTheDocument();

    expect(
      screen.getByText(/Línea Nacional 192/i)
    ).toBeInTheDocument();
  });

  it('no bloquea la evaluacion y permite continuar mediante el boton de accion', async () => {
    const mockAlContinuar = vi.fn();
    const mockAlSalir = vi.fn();

    render(
      <AvisoCrisis
        recursosPersonalizados={recursosFalsos}
        alContinuar={mockAlContinuar}
        alSalir={mockAlSalir}
        mostrarBotonContinuar={true}
      />
    );

    const botonContinuar = screen.getByRole('button', { name: /continuar con la evaluación/i });
    expect(botonContinuar).toBeInTheDocument();
    expect(botonContinuar).toBeEnabled();

    const usuarioInteractivo = userEvent.setup();
    await usuarioInteractivo.click(botonContinuar);

    expect(mockAlContinuar).toHaveBeenCalledTimes(1);
  });

  it('permite al usuario salir y buscar ayuda', async () => {
    const mockAlContinuar = vi.fn();
    const mockAlSalir = vi.fn();

    render(
      <AvisoCrisis
        recursosPersonalizados={recursosFalsos}
        alContinuar={mockAlContinuar}
        alSalir={mockAlSalir}
        mostrarBotonContinuar={true}
      />
    );

    const botonSalir = screen.getByRole('button', { name: /salir y buscar ayuda/i });
    expect(botonSalir).toBeInTheDocument();
    expect(botonSalir).toBeEnabled();

    const usuarioInteractivo = userEvent.setup();
    await usuarioInteractivo.click(botonSalir);

    expect(mockAlSalir).toHaveBeenCalledTimes(1);
  });
});
