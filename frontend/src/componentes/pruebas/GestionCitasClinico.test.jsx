import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { GestionCitasClinico } from '../../pantallas/GestionCitasClinico.jsx';
import { servicioCitas } from '../../servicios/servicioCitas.js';

vi.mock('../../servicios/servicioCitas.js', () => ({
  servicioCitas: {
    listarCitasClinico: vi.fn(),
    posponerCita: vi.fn(),
    cumplirCita: vi.fn(),
    cancelarCitaClinico: vi.fn()
  }
}));

describe('GestionCitasClinico', () => {
  const citasMock = [
    {
      id: 'cita-1',
      pacienteId: 10,
      pacienteNombre: 'Ana Paciente',
      pacienteCorreo: 'ana@ejemplo.com',
      fechaHoraInicio: '2026-11-01T10:00:00Z',
      estado: 'CONFIRMADA',
      modalidad: 'TELECONSULTA',
      observaciones: 'Evaluación inicial'
    },
    {
      id: 'cita-2',
      pacienteId: 20,
      pacienteNombre: 'Pedro Histórico',
      pacienteCorreo: 'pedro@ejemplo.com',
      fechaHoraInicio: '2026-10-01T09:00:00Z',
      estado: 'CUMPLIDA',
      modalidad: 'PRESENCIAL',
      observaciones: 'Se brindaron recomendaciones y plan de seguimiento.'
    }
  ];

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('permite alternar entre la pestaña de Citas Activas y el Historial de Citas Pasadas', async () => {
    servicioCitas.listarCitasClinico.mockResolvedValueOnce(citasMock);

    render(
      <MemoryRouter>
        <GestionCitasClinico />
      </MemoryRouter>
    );

    // En pestaña activa por defecto
    expect(await screen.findByText('Ana Paciente')).toBeInTheDocument();
    expect(screen.queryByText('Pedro Histórico')).not.toBeInTheDocument();

    // Cambiar a Historial de Citas Pasadas
    const usuario = userEvent.setup();
    const pestanaHistorial = screen.getByRole('button', { name: /Historial de Citas Pasadas/i });
    await usuario.click(pestanaHistorial);

    expect(await screen.findByText('Pedro Histórico')).toBeInTheDocument();
    expect(screen.getByText(/Se brindaron recomendaciones y plan de seguimiento/i)).toBeInTheDocument();
    expect(screen.getByText(/Cita finalizada \(sin opción de reabrirse\)/i)).toBeInTheDocument();
    expect(screen.queryByText('Ana Paciente')).not.toBeInTheDocument();
  });

  it('permite dar por cumplida una cita exigiendo observaciones clínicas obligatorias', async () => {
    servicioCitas.listarCitasClinico.mockResolvedValueOnce(citasMock);
    servicioCitas.cumplirCita.mockResolvedValueOnce({
      ...citasMock[0],
      estado: 'CUMPLIDA',
      observaciones: 'Paciente estable. Se acuerda control ambulatorio.'
    });

    render(
      <MemoryRouter>
        <GestionCitasClinico />
      </MemoryRouter>
    );

    expect(await screen.findByText('Ana Paciente')).toBeInTheDocument();

    const usuario = userEvent.setup();
    const botonCumplir = screen.getByRole('button', { name: /Dar por cumplida/i });
    await usuario.click(botonCumplir);

    // Modal de cumplimiento
    expect(screen.getByRole('heading', { name: /Dar cita por cumplida/i })).toBeInTheDocument();

    const campoObservaciones = screen.getByLabelText(/Observaciones de la atención/i);
    const botonConfirmar = screen.getByRole('button', { name: /Confirmar atención cumplida/i });

    // Intento con observaciones vacías
    await usuario.click(botonConfirmar);
    expect(screen.getByText(/Las observaciones de la atención o cierre son obligatorias/i)).toBeInTheDocument();
    expect(servicioCitas.cumplirCita).not.toHaveBeenCalled();

    // Diligenciar observaciones válidas
    await usuario.type(campoObservaciones, 'Paciente estable. Se acuerda control ambulatorio.');
    await usuario.click(botonConfirmar);

    await waitFor(() => {
      expect(servicioCitas.cumplirCita).toHaveBeenCalledWith('cita-1', {
        observaciones: 'Paciente estable. Se acuerda control ambulatorio.'
      });
    });
  });

  it('permite cancelar una cita con motivo obligatorio y carácter definitivo', async () => {
    servicioCitas.listarCitasClinico.mockResolvedValueOnce(citasMock);
    servicioCitas.cancelarCitaClinico.mockResolvedValueOnce({
      ...citasMock[0],
      estado: 'CANCELADA',
      observaciones: 'Paciente canceló por viaje.'
    });

    render(
      <MemoryRouter>
        <GestionCitasClinico />
      </MemoryRouter>
    );

    expect(await screen.findByText('Ana Paciente')).toBeInTheDocument();

    const usuario = userEvent.setup();
    const botonCancelar = screen.getByRole('button', { name: /Cancelar cita/i });
    await usuario.click(botonCancelar);

    expect(screen.getByRole('heading', { name: /Cancelar cita/i })).toBeInTheDocument();

    const campoMotivo = screen.getByLabelText(/Motivo de cancelación/i);
    const botonConfirmarCancelacion = screen.getByRole('button', { name: /Confirmar cancelación/i });

    // Intento vacío
    await usuario.click(botonConfirmarCancelacion);
    expect(screen.getByText(/El motivo de cancelación es obligatorio/i)).toBeInTheDocument();
    expect(servicioCitas.cancelarCitaClinico).not.toHaveBeenCalled();

    // Diligenciar motivo válido
    await usuario.type(campoMotivo, 'Paciente canceló por viaje.');
    await usuario.click(botonConfirmarCancelacion);

    await waitFor(() => {
      expect(servicioCitas.cancelarCitaClinico).toHaveBeenCalledWith('cita-1', expect.objectContaining({
        motivo: 'Paciente canceló por viaje.'
      }));
    });
  });
});
