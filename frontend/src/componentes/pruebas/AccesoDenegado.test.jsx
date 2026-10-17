import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { AccesoDenegado } from '../../pantallas/AccesoDenegado.jsx';
import * as HookAutenticacion from '../../hooks/useAutenticacion.js';

describe('AccesoDenegado (403)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('muestra mensaje claro, sobrio y no alarmista de acceso no autorizado', () => {
    vi.spyOn(HookAutenticacion, 'useAutenticacion').mockReturnValue({
      estaAutenticado: true,
      usuario: { id: 1, rol: 'CLINICO' }
    });

    render(
      <MemoryRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
        <AccesoDenegado />
      </MemoryRouter>
    );

    expect(screen.getByRole('heading', { level: 1, name: /Acceso No Autorizado/i })).toBeInTheDocument();
    expect(
      screen.getByText(/No cuentas con los permisos requeridos para ingresar a esta sección o tu rol actual no tiene acceso/i)
    ).toBeInTheDocument();
    expect(screen.getByText(/Sesión iniciada como: CLINICO/i)).toBeInTheDocument();
    expect(screen.getByText(/Código 403 · Prohibido/i)).toBeInTheDocument();
  });

  it('redirige a /administracion cuando el usuario tiene rol ADMINISTRADOR', async () => {
    vi.spyOn(HookAutenticacion, 'useAutenticacion').mockReturnValue({
      estaAutenticado: true,
      usuario: { id: 2, rol: 'ADMINISTRADOR' }
    });

    const usuario = userEvent.setup();

    render(
      <MemoryRouter
        future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
        initialEntries={['/acceso-denegado']}
      >
        <Routes>
          <Route path="/acceso-denegado" element={<AccesoDenegado />} />
          <Route path="/administracion" element={<div>Panel de Administración Destino</div>} />
        </Routes>
      </MemoryRouter>
    );

    const boton = screen.getByRole('button', { name: /Ir a mi panel principal/i });
    expect(boton).toBeInTheDocument();

    await usuario.click(boton);
    expect(screen.getByText('Panel de Administración Destino')).toBeInTheDocument();
  });

  it('redirige a /clinico cuando el usuario tiene rol CLINICO', async () => {
    vi.spyOn(HookAutenticacion, 'useAutenticacion').mockReturnValue({
      estaAutenticado: true,
      usuario: { id: 3, rol: 'CLINICO' }
    });

    const usuario = userEvent.setup();

    render(
      <MemoryRouter
        future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
        initialEntries={['/acceso-denegado']}
      >
        <Routes>
          <Route path="/acceso-denegado" element={<AccesoDenegado />} />
          <Route path="/clinico" element={<div>Portal Clínico Destino</div>} />
        </Routes>
      </MemoryRouter>
    );

    const boton = screen.getByRole('button', { name: /Ir a mi panel principal/i });
    expect(boton).toBeInTheDocument();

    await usuario.click(boton);
    expect(screen.getByText('Portal Clínico Destino')).toBeInTheDocument();
  });

  it('redirige a / cuando el usuario tiene rol PACIENTE', async () => {
    vi.spyOn(HookAutenticacion, 'useAutenticacion').mockReturnValue({
      estaAutenticado: true,
      usuario: { id: 4, rol: 'PACIENTE' }
    });

    const usuario = userEvent.setup();

    render(
      <MemoryRouter
        future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
        initialEntries={['/acceso-denegado']}
      >
        <Routes>
          <Route path="/acceso-denegado" element={<AccesoDenegado />} />
          <Route path="/" element={<div>Pantalla Inicio Destino</div>} />
        </Routes>
      </MemoryRouter>
    );

    const boton = screen.getByRole('button', { name: /Ir a mi panel principal/i });
    expect(boton).toBeInTheDocument();

    await usuario.click(boton);
    expect(screen.getByText('Pantalla Inicio Destino')).toBeInTheDocument();
  });

  it('redirige a /iniciar-sesion cuando el usuario no está autenticado', async () => {
    vi.spyOn(HookAutenticacion, 'useAutenticacion').mockReturnValue({
      estaAutenticado: false,
      usuario: null
    });

    const usuario = userEvent.setup();

    render(
      <MemoryRouter
        future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
        initialEntries={['/acceso-denegado']}
      >
        <Routes>
          <Route path="/acceso-denegado" element={<AccesoDenegado />} />
          <Route path="/iniciar-sesion" element={<div>Login Destino</div>} />
        </Routes>
      </MemoryRouter>
    );

    const boton = screen.getByRole('button', { name: /Iniciar sesión/i });
    expect(boton).toBeInTheDocument();

    await usuario.click(boton);
    expect(screen.getByText('Login Destino')).toBeInTheDocument();
  });
});
