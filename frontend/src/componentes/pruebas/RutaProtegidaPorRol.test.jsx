import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { RutaProtegidaPorRol } from '../comunes/RutaProtegidaPorRol.jsx';
import * as HookAutenticacion from '../../hooks/useAutenticacion.js';

describe('RutaProtegidaPorRol', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('muestra indicador de carga accesible mientras se verifican permisos y rol', () => {
    vi.spyOn(HookAutenticacion, 'useAutenticacion').mockReturnValue({
      estaAutenticado: false,
      usuario: null,
      cargando: true
    });

    render(
      <MemoryRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }} initialEntries={['/clinico']}>
        <Routes>
          <Route element={<RutaProtegidaPorRol rolesPermitidos={['CLINICO']} />}>
            <Route path="/clinico" element={<div>Portal Clínico</div>} />
          </Route>
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByText('Verificando permisos y rol...')).toBeInTheDocument();
    expect(screen.queryByText('Portal Clínico')).not.toBeInTheDocument();
  });

  it('redirige a /iniciar-sesion si el usuario no esta autenticado', () => {
    vi.spyOn(HookAutenticacion, 'useAutenticacion').mockReturnValue({
      estaAutenticado: false,
      usuario: null,
      cargando: false
    });

    render(
      <MemoryRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }} initialEntries={['/clinico']}>
        <Routes>
          <Route path="/iniciar-sesion" element={<div>Pantalla Inicio Sesión</div>} />
          <Route element={<RutaProtegidaPorRol rolesPermitidos={['CLINICO']} />}>
            <Route path="/clinico" element={<div>Portal Clínico Privado</div>} />
          </Route>
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByText('Pantalla Inicio Sesión')).toBeInTheDocument();
    expect(screen.queryByText('Portal Clínico Privado')).not.toBeInTheDocument();
  });

  it('muestra la vista PaginaNoAutorizado si un usuario CLINICO o ADMINISTRADOR intenta acceder a una ruta de PACIENTE', () => {
    // 1. Caso CLINICO intentando entrar a ruta de PACIENTE
    vi.spyOn(HookAutenticacion, 'useAutenticacion').mockReturnValue({
      estaAutenticado: true,
      usuario: { id: 20, nombreCompleto: 'Dra. Ana Clinica', rol: 'CLINICO' },
      cargando: false
    });

    const { unmount } = render(
      <MemoryRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }} initialEntries={['/consentimiento']}>
        <Routes>
          <Route element={<RutaProtegidaPorRol rolesPermitidos={['PACIENTE']} />}>
            <Route path="/consentimiento" element={<div>Pantalla Consentimiento Paciente</div>} />
          </Route>
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByText(/Acceso No Autorizado/i)).toBeInTheDocument();
    expect(screen.getByText(/No cuentas con los permisos requeridos para ingresar a esta sección o tu rol actual no tiene acceso/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Ir a mi panel principal/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Cerrar sesión/i })).toBeInTheDocument();
    expect(screen.queryByText('Pantalla Consentimiento Paciente')).not.toBeInTheDocument();
    unmount();

    // 2. Caso ADMINISTRADOR intentando entrar a ruta de PACIENTE
    vi.spyOn(HookAutenticacion, 'useAutenticacion').mockReturnValue({
      estaAutenticado: true,
      usuario: { id: 30, nombreCompleto: 'Admin General', rol: 'ADMINISTRADOR' },
      cargando: false
    });

    render(
      <MemoryRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }} initialEntries={['/introduccion']}>
        <Routes>
          <Route element={<RutaProtegidaPorRol rolesPermitidos={['PACIENTE']} />}>
            <Route path="/introduccion" element={<div>Pantalla Introducción Paciente</div>} />
          </Route>
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByText(/Acceso No Autorizado/i)).toBeInTheDocument();
    expect(screen.getByText(/No cuentas con los permisos requeridos para ingresar a esta sección o tu rol actual no tiene acceso/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Ir a mi panel principal/i })).toBeInTheDocument();
    expect(screen.queryByText('Pantalla Introducción Paciente')).not.toBeInTheDocument();
  });

  it('muestra la vista PaginaNoAutorizado si un usuario PACIENTE intenta acceder a /administracion', () => {
    vi.spyOn(HookAutenticacion, 'useAutenticacion').mockReturnValue({
      estaAutenticado: true,
      usuario: { id: 10, nombreCompleto: 'Juan Paciente', rol: 'PACIENTE' },
      cargando: false
    });

    render(
      <MemoryRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }} initialEntries={['/administracion']}>
        <Routes>
          <Route element={<RutaProtegidaPorRol rolesPermitidos={['ADMINISTRADOR']} />}>
            <Route path="/administracion" element={<div>Panel Administración Privado</div>} />
          </Route>
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByText(/Acceso No Autorizado/i)).toBeInTheDocument();
    expect(screen.getByText(/No cuentas con los permisos requeridos para ingresar a esta sección o tu rol actual no tiene acceso/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Ir a mi panel principal/i })).toBeInTheDocument();
    expect(screen.queryByText('Panel Administración Privado')).not.toBeInTheDocument();
  });

  it('permite el acceso si el rol del usuario coincide con los roles permitidos', () => {
    vi.spyOn(HookAutenticacion, 'useAutenticacion').mockReturnValue({
      estaAutenticado: true,
      usuario: { id: 30, nombreCompleto: 'Admin Global', rol: 'ADMINISTRADOR' },
      cargando: false
    });

    render(
      <MemoryRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }} initialEntries={['/administracion']}>
        <Routes>
          <Route element={<RutaProtegidaPorRol rolesPermitidos={['ADMINISTRADOR']} />}>
            <Route path="/administracion" element={<div>Panel Administración Permitido</div>} />
          </Route>
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByText('Panel Administración Permitido')).toBeInTheDocument();
  });
});
