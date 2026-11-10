import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { RedireccionInicio } from '../../App.jsx';
import * as HookAutenticacion from '../../hooks/useAutenticacion.js';

describe('Ruta comodín y RedireccionInicio', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('un paciente autenticado que abre una ruta inexistente NO es enviado al login, sino a /introduccion', () => {
    vi.spyOn(HookAutenticacion, 'useAutenticacion').mockReturnValue({
      estaAutenticado: true,
      usuario: { id: 21, nombreCompleto: 'Sebastian Paciente', rol: 'PACIENTE' },
      cargando: false
    });

    render(
      <MemoryRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }} initialEntries={['/ruta-completamente-inexistente']}>
        <Routes>
          <Route path="/iniciar-sesion" element={<div>Pantalla Iniciar Sesión</div>} />
          <Route path="/introduccion" element={<div>Pantalla Introducción Paciente</div>} />
          <Route path="*" element={<RedireccionInicio />} />
        </Routes>
      </MemoryRouter>
    );

    // Debe mostrar la pantalla de introducción y NO la de login
    expect(screen.getByText('Pantalla Introducción Paciente')).toBeInTheDocument();
    expect(screen.queryByText('Pantalla Iniciar Sesión')).not.toBeInTheDocument();
  });

  it('un usuario CLINICO autenticado en ruta inexistente es enviado a /clinico', () => {
    vi.spyOn(HookAutenticacion, 'useAutenticacion').mockReturnValue({
      estaAutenticado: true,
      usuario: { id: 4, nombreCompleto: 'Dr. Alejandro', rol: 'CLINICO' },
      cargando: false
    });

    render(
      <MemoryRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }} initialEntries={['/ruta-invalida']}>
        <Routes>
          <Route path="/iniciar-sesion" element={<div>Pantalla Iniciar Sesión</div>} />
          <Route path="/clinico" element={<div>Portal Clínico</div>} />
          <Route path="*" element={<RedireccionInicio />} />
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByText('Portal Clínico')).toBeInTheDocument();
    expect(screen.queryByText('Pantalla Iniciar Sesión')).not.toBeInTheDocument();
  });

  it('un usuario NO autenticado en ruta inexistente es enviado a /iniciar-sesion', () => {
    vi.spyOn(HookAutenticacion, 'useAutenticacion').mockReturnValue({
      estaAutenticado: false,
      usuario: null,
      cargando: false
    });

    render(
      <MemoryRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }} initialEntries={['/ruta-fantasma']}>
        <Routes>
          <Route path="/iniciar-sesion" element={<div>Pantalla Iniciar Sesión</div>} />
          <Route path="/introduccion" element={<div>Pantalla Introducción Paciente</div>} />
          <Route path="*" element={<RedireccionInicio />} />
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByText('Pantalla Iniciar Sesión')).toBeInTheDocument();
    expect(screen.queryByText('Pantalla Introducción Paciente')).not.toBeInTheDocument();
  });
});
