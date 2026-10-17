import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ListaAutorizaciones } from '../triaje/ListaAutorizaciones.jsx';
import { servicioTriaje } from '../../servicios/servicioTriaje.js';

vi.mock('../../servicios/servicioTriaje.js', () => ({
  servicioTriaje: {
    listarAutorizaciones: vi.fn(),
    revocarAutorizacion: vi.fn()
  }
}));

describe('ListaAutorizaciones', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('verifica que pulsar "Dejar de compartir" invoque la ruta de revocación correspondiente', async () => {
    const autorizacionesIniciales = [
      {
        id: 'aut-1',
        evaluacionId: 'eval-xyz-123',
        clinicoId: 55,
        nombreClinico: 'Dr. Roberto Mendoza',
        versionTextoAutorizacion: '1.0',
        fechaOtorgamiento: '2026-10-15T10:00:00Z',
        vigente: true,
        estado: 'VIGENTE'
      }
    ];

    servicioTriaje.revocarAutorizacion.mockResolvedValueOnce({
      id: 'aut-1',
      evaluacionId: 'eval-xyz-123',
      clinicoId: 55,
      vigente: false,
      estado: 'REVOCADA'
    });

    render(
      <ListaAutorizaciones
        evaluacionId="eval-xyz-123"
        autorizacionesIniciales={autorizacionesIniciales}
      />
    );

    // Debe mostrar al clínico en la lista de autorizados
    expect(screen.getByText('Dr. Roberto Mendoza')).toBeInTheDocument();

    const botonDejarCompartir = screen.getByRole('button', { name: /dejar de compartir/i });
    expect(botonDejarCompartir).toBeInTheDocument();

    const usuario = userEvent.setup();
    await usuario.click(botonDejarCompartir);

    // Verifica que se invoque la función de revocación con el evaluacionId y clinicoId correctos
    await waitFor(() => {
      expect(servicioTriaje.revocarAutorizacion).toHaveBeenCalledWith('eval-xyz-123', 55);
      expect(screen.getByText(/autorización revocada exitosamente/i)).toBeInTheDocument();
      // Ya no debe figurar como autorización vigente
      expect(screen.queryByText('Dr. Roberto Mendoza')).not.toBeInTheDocument();
    });
  });
});
