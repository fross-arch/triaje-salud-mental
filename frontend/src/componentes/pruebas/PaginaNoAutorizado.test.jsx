import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { PaginaNoAutorizado } from '../../paginas/PaginaNoAutorizado.jsx';
import * as HookAutenticacion from '../../hooks/useAutenticacion.js';

describe('PaginaNoAutorizado', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('muestra encabezado, mensaje descriptivo y código 403 con diseño sobrio', () => {
    vi.spyOn(HookAutenticacion, 'useAutenticacion').mockReturnValue({
      estaAutenticado: true,
      usuario: { id: 1, rol: 'CLINICO' },
      cerrarSesion: vi.fn()
    });

    render(
      <MemoryRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
        <PaginaNoAutorizado />
      </MemoryRouter>
    );

    expect(screen.getByRole('heading', { level: 1, name: /Acceso No Autorizado/i })).toBeInTheDocument();
    expect(
      screen.getByText('No cuentas con los permisos requeridos para ingresar a esta sección o tu rol actual no tiene acceso.')
    ).toBeInTheDocument();
    expect(screen.getByText('Sesión iniciada como: CLINICO')).toBeInTheDocument();
    expect(screen.getByText(/Código 403 · Prohibido/i)).toBeInTheDocument();
  });

  it('para rol PACIENTE, el botón "Ir a mi panel principal" redirige a /', async () => {
    vi.spyOn(HookAutenticacion, 'useAutenticacion').mockReturnValue({
      estaAutenticado: true,
      usuario: { id: 2, rol: 'PACIENTE' },
      cerrarSesion: vi.fn()
    });

    const user = userEvent.setup();

    render(
      <MemoryRouter
        future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
        initialEntries={['/no-autorizado']}
      >
        <Routes>
          <Route path="/no-autorizado" element={<PaginaNoAutorizado />} />
          <Route path="/" element={<div>Panel Raiz Paciente</div>} />
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByText('Sesión iniciada como: PACIENTE')).toBeInTheDocument();
    const boton = screen.getByRole('button', { name: /Ir a mi panel principal/i });
    expect(boton).toBeInTheDocument();

    await user.click(boton);
    expect(screen.getByText('Panel Raiz Paciente')).toBeInTheDocument();
  });

  it('para rol CLINICO, el botón "Ir a mi panel principal" redirige a /clinico', async () => {
    vi.spyOn(HookAutenticacion, 'useAutenticacion').mockReturnValue({
      estaAutenticado: true,
      usuario: { id: 3, rol: 'CLINICO' },
      cerrarSesion: vi.fn()
    });

    const user = userEvent.setup();

    render(
      <MemoryRouter
        future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
        initialEntries={['/no-autorizado']}
      >
        <Routes>
          <Route path="/no-autorizado" element={<PaginaNoAutorizado />} />
          <Route path="/clinico" element={<div>Portal Clínico Destino</div>} />
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByText('Sesión iniciada como: CLINICO')).toBeInTheDocument();
    const boton = screen.getByRole('button', { name: /Ir a mi panel principal/i });
    await user.click(boton);
    expect(screen.getByText('Portal Clínico Destino')).toBeInTheDocument();
  });

  it('para rol ADMINISTRADOR, el botón "Ir a mi panel principal" redirige a /administracion', async () => {
    vi.spyOn(HookAutenticacion, 'useAutenticacion').mockReturnValue({
      estaAutenticado: true,
      usuario: { id: 4, rol: 'ADMINISTRADOR' },
      cerrarSesion: vi.fn()
    });

    const user = userEvent.setup();

    render(
      <MemoryRouter
        future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
        initialEntries={['/no-autorizado']}
      >
        <Routes>
          <Route path="/no-autorizado" element={<PaginaNoAutorizado />} />
          <Route path="/administracion" element={<div>Panel Administración Destino</div>} />
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByText('Sesión iniciada como: ADMINISTRADOR')).toBeInTheDocument();
    const boton = screen.getByRole('button', { name: /Ir a mi panel principal/i });
    await user.click(boton);
    expect(screen.getByText('Panel Administración Destino')).toBeInTheDocument();
  });

  it('permite cerrar sesión con el botón secundario cuando el usuario está autenticado', async () => {
    const mockCerrarSesion = vi.fn().mockResolvedValue();
    vi.spyOn(HookAutenticacion, 'useAutenticacion').mockReturnValue({
      estaAutenticado: true,
      usuario: { id: 5, rol: 'PACIENTE' },
      cerrarSesion: mockCerrarSesion
    });

    const user = userEvent.setup();

    render(
      <MemoryRouter
        future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
        initialEntries={['/no-autorizado']}
      >
        <Routes>
          <Route path="/no-autorizado" element={<PaginaNoAutorizado />} />
          <Route path="/iniciar-sesion" element={<div>Pantalla Iniciar Sesión Destino</div>} />
        </Routes>
      </MemoryRouter>
    );

    const botonCerrar = screen.getByRole('button', { name: /Cerrar sesión/i });
    expect(botonCerrar).toBeInTheDocument();

    await user.click(botonCerrar);
    expect(mockCerrarSesion).toHaveBeenCalledTimes(1);
    expect(screen.getByText('Pantalla Iniciar Sesión Destino')).toBeInTheDocument();
  });

  it('cuando no está autenticado, muestra botón "Iniciar sesión" que redirige a /iniciar-sesion', async () => {
    vi.spyOn(HookAutenticacion, 'useAutenticacion').mockReturnValue({
      estaAutenticado: false,
      usuario: null,
      cerrarSesion: vi.fn()
    });

    const user = userEvent.setup();

    render(
      <MemoryRouter
        future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
        initialEntries={['/no-autorizado']}
      >
        <Routes>
          <Route path="/no-autorizado" element={<PaginaNoAutorizado />} />
          <Route path="/iniciar-sesion" element={<div>Formulario Iniciar Sesión</div>} />
        </Routes>
      </MemoryRouter>
    );

    expect(screen.queryByText(/Sesión iniciada como:/i)).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Ir a mi panel principal/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Cerrar sesión/i })).not.toBeInTheDocument();

    const botonLogin = screen.getByRole('button', { name: /Iniciar sesión/i });
    expect(botonLogin).toBeInTheDocument();

    await user.click(botonLogin);
    expect(screen.getByText('Formulario Iniciar Sesión')).toBeInTheDocument();
  });
});
