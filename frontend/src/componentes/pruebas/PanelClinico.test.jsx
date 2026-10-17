import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { PanelClinico } from '../../pantallas/PanelClinico.jsx';
import { servicioClinicoEvaluaciones } from '../../servicios/servicioClinicoEvaluaciones.js';

vi.mock('../../servicios/servicioClinicoEvaluaciones.js', () => ({
  servicioClinicoEvaluaciones: {
    listarEvaluaciones: vi.fn()
  }
}));

describe('PanelClinico', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('verifica que las evaluaciones con riesgoCrisis=true se prioricen al tope de la lista', async () => {
    // Lista no ordenada que entrega el servicio (primero sin crisis, luego con crisis)
    servicioClinicoEvaluaciones.listarEvaluaciones.mockResolvedValueOnce([
      {
        evaluacionId: 'eval-sin-crisis-1',
        nombrePaciente: 'Paciente Sin Riesgo',
        fechaFinalizacion: '2026-10-15T12:00:00Z',
        severidadDepresion: 'LEVE',
        severidadAnsiedad: 'MINIMA',
        riesgoCrisis: false,
        resultadoMinimo: false
      },
      {
        evaluacionId: 'eval-con-crisis-2',
        nombrePaciente: 'Paciente En Riesgo',
        fechaFinalizacion: '2026-10-15T11:00:00Z',
        severidadDepresion: 'GRAVE',
        severidadAnsiedad: 'MODERADA',
        riesgoCrisis: true,
        resultadoMinimo: false
      },
      {
        evaluacionId: 'eval-sin-crisis-3',
        nombrePaciente: 'Paciente Leve',
        fechaFinalizacion: '2026-10-15T14:00:00Z',
        severidadDepresion: 'MINIMA',
        severidadAnsiedad: 'MINIMA',
        riesgoCrisis: false,
        resultadoMinimo: true
      }
    ]);

    render(
      <MemoryRouter>
        <PanelClinico />
      </MemoryRouter>
    );

    // Esperar a que carguen las evaluaciones
    expect(await screen.findByText('Paciente En Riesgo')).toBeInTheDocument();

    const contenedorLista = screen.getByTestId('lista-evaluaciones-clinicas');
    const tarjetas = within(contenedorLista).getAllByRole('article');

    expect(tarjetas).toHaveLength(3);

    // La PRIMERA tarjeta debe ser la evaluación con riesgoCrisis=true
    expect(tarjetas[0]).toHaveTextContent('Paciente En Riesgo');
    expect(tarjetas[0]).toHaveTextContent('Prioridad: Indicador de crisis');
    expect(tarjetas[0]).toHaveAttribute('data-testid', 'evaluacion-card-eval-con-crisis-2');

    // Las siguientes son las que no tienen crisis
    expect(tarjetas[1]).toHaveTextContent('Paciente Leve'); // por fecha más reciente entre las sin crisis
    expect(tarjetas[2]).toHaveTextContent('Paciente Sin Riesgo');
  });
});
