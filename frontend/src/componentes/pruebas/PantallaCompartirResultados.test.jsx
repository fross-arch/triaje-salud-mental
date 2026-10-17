import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { PantallaCompartirResultados } from '../../pantallas/PantallaCompartirResultados.jsx';
import { servicioClinicos } from '../../servicios/servicioClinicos.js';
import { servicioTriaje } from '../../servicios/servicioTriaje.js';
import { servicioConsentimiento } from '../../servicios/servicioConsentimiento.js';
import { TEXTOS_AUTORIZACION_COMPARTIR, TEXTO_ADVERTENCIA_CRISIS_COMPARTIR } from '../../constantes/textosLegales.js';

vi.mock('../../servicios/servicioClinicos.js', () => ({
  servicioClinicos: {
    listarActivos: vi.fn()
  }
}));

vi.mock('../../servicios/servicioTriaje.js', () => ({
  servicioTriaje: {
    otorgarAutorizacion: vi.fn()
  }
}));

vi.mock('../../servicios/servicioConsentimiento.js', () => ({
  servicioConsentimiento: {
    consultarVigente: vi.fn().mockResolvedValue({ vigente: true })
  }
}));

describe('PantallaCompartirResultados', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    servicioConsentimiento.consultarVigente.mockResolvedValue({ vigente: true });
  });

  it('valida que el botón de envío permanezca deshabilitado hasta marcar la casilla obligatoria de autorización y que se despliegue el texto de advertencia', async () => {
    servicioClinicos.listarActivos.mockResolvedValueOnce([
      { id: 101, nombreCompleto: 'Dra. Sofía Psicóloga' },
      { id: 102, nombreCompleto: 'Dr. Carlos Psicólogo' }
    ]);

    render(
      <MemoryRouter initialEntries={['/resultados/eval-123/compartir']}>
        <Routes>
          <Route path="/resultados/:id/compartir" element={<PantallaCompartirResultados />} />
        </Routes>
      </MemoryRouter>
    );

    // Esperar a que carguen los clinicos
    expect(await screen.findByText('Dra. Sofía Psicóloga')).toBeInTheDocument();

    // Validar despliegue de advertencias y del texto íntegro
    expect(screen.getByText(TEXTOS_AUTORIZACION_COMPARTIR.titulo)).toBeInTheDocument();
    expect(screen.getByText(new RegExp(TEXTO_ADVERTENCIA_CRISIS_COMPARTIR, 'i'))).toBeInTheDocument();

    const botonEnviar = screen.getByRole('button', { name: /autorizar y compartir/i });
    const casillaAutorizacion = screen.getByRole('checkbox');

    // Boton deshabilitado inicialmente antes de marcar la casilla
    expect(casillaAutorizacion).not.toBeChecked();
    expect(botonEnviar).toBeDisabled();

    // Marcar la casilla obligatoria
    const usuario = userEvent.setup();
    await usuario.click(casillaAutorizacion);

    expect(casillaAutorizacion).toBeChecked();
    expect(botonEnviar).toBeEnabled();

    // Desmarcar vuelve a deshabilitar el boton
    await usuario.click(casillaAutorizacion);
    expect(casillaAutorizacion).not.toBeChecked();
    expect(botonEnviar).toBeDisabled();
  });

  it('envía la autorización al servidor cuando la casilla está marcada y muestra mensaje de confirmación', async () => {
    servicioClinicos.listarActivos.mockResolvedValueOnce([
      { id: 101, nombreCompleto: 'Dra. Sofía Psicóloga' }
    ]);
    servicioTriaje.otorgarAutorizacion.mockResolvedValueOnce({
      id: 'aut-uuid-1',
      evaluacionId: 'eval-123',
      clinicoId: 101,
      vigente: true
    });

    render(
      <MemoryRouter initialEntries={['/resultados/eval-123/compartir']}>
        <Routes>
          <Route path="/resultados/:id/compartir" element={<PantallaCompartirResultados />} />
        </Routes>
      </MemoryRouter>
    );

    expect(await screen.findByText('Dra. Sofía Psicóloga')).toBeInTheDocument();

    const usuario = userEvent.setup();
    const casillaAutorizacion = screen.getByRole('checkbox');
    await usuario.click(casillaAutorizacion);

    const botonEnviar = screen.getByRole('button', { name: /autorizar y compartir/i });
    await usuario.click(botonEnviar);

    await waitFor(() => {
      expect(servicioTriaje.otorgarAutorizacion).toHaveBeenCalledWith('eval-123', 101, true);
      expect(screen.getByText(/¡autorización otorgada!/i)).toBeInTheDocument();
    });
  });

  it('muestra la especialidad junto al nombre del clínico en las opciones del selector', async () => {
    servicioClinicos.listarActivos.mockResolvedValueOnce([
      { id: 101, nombreCompleto: 'Dr. Carlos Ruiz', especialidad: 'Psicología Clínica' },
      { id: 102, nombreCompleto: 'Dra. María Gómez', especialidad: 'Psiquiatría de Enlace' }
    ]);

    render(
      <MemoryRouter initialEntries={['/resultados/eval-123/compartir']}>
        <Routes>
          <Route path="/resultados/:id/compartir" element={<PantallaCompartirResultados />} />
        </Routes>
      </MemoryRouter>
    );

    expect(await screen.findByText('Dr. Carlos Ruiz — Psicología Clínica')).toBeInTheDocument();
    expect(screen.getByText('Dra. María Gómez — Psiquiatría de Enlace')).toBeInTheDocument();
  });

  it('deshabilita el selector, casilla y botón si el consentimiento está revocado, mostrando aviso', async () => {
    servicioClinicos.listarActivos.mockResolvedValueOnce([
      { id: 101, nombreCompleto: 'Dr. Carlos Ruiz', especialidad: 'Psicología Clínica' }
    ]);
    servicioConsentimiento.consultarVigente.mockResolvedValueOnce({ vigente: false });

    render(
      <MemoryRouter initialEntries={['/resultados/eval-123/compartir']}>
        <Routes>
          <Route path="/resultados/:id/compartir" element={<PantallaCompartirResultados />} />
        </Routes>
      </MemoryRouter>
    );

    expect(await screen.findByText('Dr. Carlos Ruiz — Psicología Clínica')).toBeInTheDocument();

    // Verificamos aviso de consentimiento revocado
    expect(screen.getByRole('alert', { name: /aviso de consentimiento revocado/i })).toBeInTheDocument();
    expect(screen.getByText(/Debes renovar tu consentimiento informado/i)).toBeInTheDocument();

    // Enlace para renovar consentimiento
    const enlaceConsentimiento = screen.getByRole('link', { name: /renovar consentimiento/i });
    expect(enlaceConsentimiento).toHaveAttribute('href', '/consentimiento');

    // Controles deshabilitados
    const selectorClinico = screen.getByLabelText(/selecciona el profesional/i);
    const casillaAutorizacion = screen.getByRole('checkbox');
    const botonEnviar = screen.getByRole('button', { name: /autorizar y compartir/i });

    expect(selectorClinico).toBeDisabled();
    expect(casillaAutorizacion).toBeDisabled();
    expect(botonEnviar).toBeDisabled();
  });
});
