import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { FormularioProponerCita } from '../clinico/FormularioProponerCita.jsx';
import { servicioCitas } from '../../servicios/servicioCitas.js';
import { TEXTO_ADVERTENCIA_DATOS_CLINICOS_CITA } from '../../constantes/textosLegales.js';

vi.mock('../../servicios/servicioCitas.js', () => ({
  servicioCitas: {
    proponerCita: vi.fn()
  }
}));

describe('FormularioProponerCita', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('verifica la presencia visible de la advertencia de no incluir datos clínicos', () => {
    render(
      <FormularioProponerCita
        evaluacionId="eval-456"
        abierto={true}
        alCerrar={vi.fn()}
      />
    );

    // Debe mostrar visiblemente la advertencia exacta
    expect(screen.getByText(new RegExp(TEXTO_ADVERTENCIA_DATOS_CLINICOS_CITA, 'i'))).toBeInTheDocument();
    expect(TEXTO_ADVERTENCIA_DATOS_CLINICOS_CITA).toBe('No incluyas datos clínicos');
  });

  it('verifica la validación de fecha futura y rechaza fechas pasadas', async () => {
    render(
      <FormularioProponerCita
        evaluacionId="eval-456"
        abierto={true}
        alCerrar={vi.fn()}
        alProponer={vi.fn()}
      />
    );

    const inputFecha = screen.getByLabelText(/fecha y hora de la cita/i);
    const botonEnviar = screen.getByRole('button', { name: /proponer cita/i });

    const usuario = userEvent.setup();

    // Fecha en el pasado (año 2020)
    await usuario.type(inputFecha, '2020-01-01T10:00');
    await usuario.click(botonEnviar);

    // No debe haber llamado al servicio
    expect(servicioCitas.proponerCita).not.toHaveBeenCalled();
    // Muestra mensaje de error de fecha futura
    expect(screen.getByText(/la fecha y hora de la cita debe ser en el futuro/i)).toBeInTheDocument();
  });

  it('permite enviar la propuesta de cita si la fecha es futura y respeta el límite de caracteres', async () => {
    servicioCitas.proponerCita.mockResolvedValueOnce({
      id: 'cita-uuid-1',
      evaluacionId: 'eval-456',
      fechaHora: '2029-11-20T14:30:00.000Z',
      modalidad: 'VIRTUAL',
      mensaje: 'Enlace de teleconsulta en la sala virtual',
      estado: 'PROPUESTA'
    });

    const alProponerMock = vi.fn();
    const alCerrarMock = vi.fn();

    render(
      <FormularioProponerCita
        evaluacionId="eval-456"
        abierto={true}
        alCerrar={alCerrarMock}
        alProponer={alProponerMock}
      />
    );

    const inputFecha = screen.getByLabelText(/fecha y hora de la cita/i);
    const inputMensaje = screen.getByLabelText(/mensaje o indicaciones/i);
    const botonEnviar = screen.getByRole('button', { name: /proponer cita/i });

    const usuario = userEvent.setup();

    // Fecha claramente futura
    await usuario.type(inputFecha, '2029-11-20T14:30');
    await usuario.type(inputMensaje, 'Enlace de teleconsulta en la sala virtual');
    await usuario.click(botonEnviar);

    await waitFor(() => {
      expect(servicioCitas.proponerCita).toHaveBeenCalledTimes(1);
      expect(alProponerMock).toHaveBeenCalled();
      expect(alCerrarMock).toHaveBeenCalled();
    });
  });
});
