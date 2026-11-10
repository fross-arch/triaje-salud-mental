import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { PantallaInicioSesion } from '../../pantallas/PantallaInicioSesion.jsx';
import * as HookAutenticacion from '../../hooks/useAutenticacion.js';
import { servicioConsentimiento } from '../../servicios/servicioConsentimiento.js';

vi.mock('../../servicios/servicioConsentimiento.js', () => ({
  servicioConsentimiento: {
    consultarVigente: vi.fn()
  }
}));

describe('PantallaInicioSesion - Prevención de llamadas 403 por rol', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('no consulta consentimientos y redirige directo a /administracion cuando el rol es ADMINISTRADOR', async () => {
    const mockIniciarSesion = vi.fn().mockResolvedValue({
      exito: true,
      usuario: { id: 1, rol: 'ADMINISTRADOR', nombreCompleto: 'Admin' }
    });

    vi.spyOn(HookAutenticacion, 'useAutenticacion').mockReturnValue({
      iniciarSesion: mockIniciarSesion
    });

    const usuario = userEvent.setup();

    render(
      <MemoryRouter
        future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
        initialEntries={['/iniciar-sesion']}
      >
        <Routes>
          <Route path="/iniciar-sesion" element={<PantallaInicioSesion />} />
          <Route path="/administracion" element={<div>Panel de Administración Destino</div>} />
        </Routes>
      </MemoryRouter>
    );

    await usuario.type(screen.getByLabelText(/Correo electrónico/i), 'admin@salud.org');
    await usuario.type(screen.getByLabelText(/Contraseña/i), 'ClaveSegura123!');
    await usuario.click(screen.getByRole('button', { name: /^ingresar$/i }));

    await waitFor(() => {
      expect(screen.getByText('Panel de Administración Destino')).toBeInTheDocument();
    });

    // Verificación clave: NO se ejecuta la llamada huérfana de consentimiento
    expect(servicioConsentimiento.consultarVigente).not.toHaveBeenCalled();
  });

  it('no consulta consentimientos y redirige directo a /clinico cuando el rol es CLINICO', async () => {
    const mockIniciarSesion = vi.fn().mockResolvedValue({
      exito: true,
      usuario: { id: 2, rol: 'CLINICO', nombreCompleto: 'Dra. Clinica' }
    });

    vi.spyOn(HookAutenticacion, 'useAutenticacion').mockReturnValue({
      iniciarSesion: mockIniciarSesion
    });

    const usuario = userEvent.setup();

    render(
      <MemoryRouter
        future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
        initialEntries={['/iniciar-sesion']}
      >
        <Routes>
          <Route path="/iniciar-sesion" element={<PantallaInicioSesion />} />
          <Route path="/clinico" element={<div>Portal Clínico Destino</div>} />
        </Routes>
      </MemoryRouter>
    );

    await usuario.type(screen.getByLabelText(/Correo electrónico/i), 'clinico@salud.org');
    await usuario.type(screen.getByLabelText(/Contraseña/i), 'ClaveSegura123!');
    await usuario.click(screen.getByRole('button', { name: /^ingresar$/i }));

    await waitFor(() => {
      expect(screen.getByText('Portal Clínico Destino')).toBeInTheDocument();
    });

    // Verificación clave: NO se ejecuta la llamada huérfana de consentimiento
    expect(servicioConsentimiento.consultarVigente).not.toHaveBeenCalled();
  });

  it('consulta consentimiento y redirige a /introduccion cuando el rol es PACIENTE con consentimiento vigente', async () => {
    const mockIniciarSesion = vi.fn().mockResolvedValue({
      exito: true,
      usuario: { id: 3, rol: 'PACIENTE', nombreCompleto: 'Paciente Juan' }
    });

    vi.spyOn(HookAutenticacion, 'useAutenticacion').mockReturnValue({
      iniciarSesion: mockIniciarSesion
    });

    servicioConsentimiento.consultarVigente.mockResolvedValueOnce({ vigente: true });

    const usuario = userEvent.setup();

    render(
      <MemoryRouter
        future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
        initialEntries={['/iniciar-sesion']}
      >
        <Routes>
          <Route path="/iniciar-sesion" element={<PantallaInicioSesion />} />
          <Route path="/introduccion" element={<div>Introducción Paciente Destino</div>} />
        </Routes>
      </MemoryRouter>
    );

    await usuario.type(screen.getByLabelText(/Correo electrónico/i), 'paciente@ejemplo.com');
    await usuario.type(screen.getByLabelText(/Contraseña/i), 'ClaveSegura123!');
    await usuario.click(screen.getByRole('button', { name: /^ingresar$/i }));

    await waitFor(() => {
      expect(screen.getByText('Introducción Paciente Destino')).toBeInTheDocument();
    });

    // Verificación clave: SÍ se ejecuta la consulta para el rol PACIENTE
    expect(servicioConsentimiento.consultarVigente).toHaveBeenCalledTimes(1);
  });

  it('redirige a /consentimiento cuando el rol es PACIENTE y vigente es false explícito', async () => {
    const mockIniciarSesion = vi.fn().mockResolvedValue({
      exito: true,
      usuario: { id: 4, rol: 'PACIENTE', nombreCompleto: 'Paciente Sin Consentimiento' }
    });

    vi.spyOn(HookAutenticacion, 'useAutenticacion').mockReturnValue({
      iniciarSesion: mockIniciarSesion
    });

    servicioConsentimiento.consultarVigente.mockResolvedValueOnce({ vigente: false });

    const usuario = userEvent.setup();

    render(
      <MemoryRouter
        future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
        initialEntries={['/iniciar-sesion']}
      >
        <Routes>
          <Route path="/iniciar-sesion" element={<PantallaInicioSesion />} />
          <Route path="/consentimiento" element={<div>Consentimiento Destino</div>} />
        </Routes>
      </MemoryRouter>
    );

    await usuario.type(screen.getByLabelText(/Correo electrónico/i), 'sin.consentimiento@ejemplo.com');
    await usuario.type(screen.getByLabelText(/Contraseña/i), 'ClaveSegura123!');
    await usuario.click(screen.getByRole('button', { name: /^ingresar$/i }));

    await waitFor(() => {
      expect(screen.getByText('Consentimiento Destino')).toBeInTheDocument();
    });

    expect(servicioConsentimiento.consultarVigente).toHaveBeenCalledTimes(1);
  });

  it('permanece en la pantalla con mensaje de error y NO redirige a /consentimiento ante fallo de red o servidor al consultar consentimiento', async () => {
    const mockIniciarSesion = vi.fn().mockResolvedValue({
      exito: true,
      usuario: { id: 5, rol: 'PACIENTE', nombreCompleto: 'Paciente Con Error' }
    });

    vi.spyOn(HookAutenticacion, 'useAutenticacion').mockReturnValue({
      iniciarSesion: mockIniciarSesion
    });

    servicioConsentimiento.consultarVigente.mockRejectedValueOnce(new Error('Fallo de conexión con el servidor.'));

    const usuario = userEvent.setup();

    render(
      <MemoryRouter
        future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
        initialEntries={['/iniciar-sesion']}
      >
        <Routes>
          <Route path="/iniciar-sesion" element={<PantallaInicioSesion />} />
          <Route path="/consentimiento" element={<div>Consentimiento Inesperado</div>} />
        </Routes>
      </MemoryRouter>
    );

    await usuario.type(screen.getByLabelText(/Correo electrónico/i), 'error.red@ejemplo.com');
    await usuario.type(screen.getByLabelText(/Contraseña/i), 'ClaveSegura123!');
    await usuario.click(screen.getByRole('button', { name: /^ingresar$/i }));

    // Permanece en la pantalla y muestra el mensaje
    expect(await screen.findByText('Fallo de conexión con el servidor.')).toBeInTheDocument();
    expect(screen.queryByText('Consentimiento Inesperado')).not.toBeInTheDocument();
  });
});
