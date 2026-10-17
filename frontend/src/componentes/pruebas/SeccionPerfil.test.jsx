import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { SeccionPerfil } from '../perfil/SeccionPerfil.jsx';
import * as useAutenticacionModulo from '../../hooks/useAutenticacion.js';
import { servicioAutenticacion } from '../../servicios/servicioAutenticacion.js';

vi.mock('../../servicios/servicioAutenticacion.js', () => ({
  servicioAutenticacion: {
    obtenerPerfil: vi.fn(),
    actualizarPerfil: vi.fn()
  }
}));

describe('SeccionPerfil', () => {
  const usuarioMock = {
    id: 42,
    nombreCompleto: 'Usuario De Prueba',
    correo: 'prueba@ejemplo.com',
    rol: 'PACIENTE'
  };

  const actualizarUsuarioEnEstadoMock = vi.fn();
  const cargarPerfilUsuarioMock = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
    vi.spyOn(useAutenticacionModulo, 'useAutenticacion').mockReturnValue({
      usuario: usuarioMock,
      actualizarUsuarioEnEstado: actualizarUsuarioEnEstadoMock,
      cargarPerfilUsuario: cargarPerfilUsuarioMock
    });
  });

  it('precarga los datos del usuario obtenidos de GET /api/usuarios/yo', async () => {
    servicioAutenticacion.obtenerPerfil.mockResolvedValueOnce({
      id: 42,
      nombreCompleto: 'Carlos Perez',
      correo: 'carlos@ejemplo.com',
      rol: 'PACIENTE'
    });

    render(<SeccionPerfil />);

    expect(await screen.findByDisplayValue('Carlos Perez')).toBeInTheDocument();
    expect(screen.getByDisplayValue('carlos@ejemplo.com')).toBeInTheDocument();
    expect(servicioAutenticacion.obtenerPerfil).toHaveBeenCalledTimes(1);

    // No debe mostrar campo de especialidad para PACIENTE
    expect(screen.queryByLabelText(/Especialidad Clínica/i)).not.toBeInTheDocument();
  });

  it('muestra el selector de especialidad y permite especificar otra cuando el rol es CLINICO', async () => {
    vi.spyOn(useAutenticacionModulo, 'useAutenticacion').mockReturnValue({
      usuario: {
        id: 99,
        nombreCompleto: 'Dra. Clinica Experta',
        correo: 'dra@salud.org',
        rol: 'CLINICO',
        especialidad: 'Psicología Forense'
      },
      actualizarUsuarioEnEstado: actualizarUsuarioEnEstadoMock,
      cargarPerfilUsuario: cargarPerfilUsuarioMock
    });

    servicioAutenticacion.obtenerPerfil.mockResolvedValueOnce({
      id: 99,
      nombreCompleto: 'Dra. Clinica Experta',
      correo: 'dra@salud.org',
      rol: 'CLINICO',
      especialidad: 'Psicología Forense'
    });

    render(<SeccionPerfil />);

    expect(await screen.findByLabelText(/Especialidad Clínica/i)).toBeInTheDocument();
    // Como 'Psicología Forense' no está en las predefinidas, debe seleccionar 'OTRA' y mostrar el input
    expect(await screen.findByDisplayValue('Psicología Forense')).toBeInTheDocument();
  });

  it('valida en el cliente la confirmación de la contraseña y la longitud mínima de 10 caracteres', async () => {
    servicioAutenticacion.obtenerPerfil.mockResolvedValueOnce({
      id: 42,
      nombreCompleto: 'Carlos Perez',
      correo: 'carlos@ejemplo.com',
      rol: 'PACIENTE'
    });

    render(<SeccionPerfil />);
    const usuario = userEvent.setup();

    // Esperar a que precargue
    await screen.findByDisplayValue('Carlos Perez');

    // Abrir el acordeón de cambiar contraseña
    const botonAcordeon = screen.getByRole('button', { name: /¿Deseas cambiar tu contraseña\?/i });
    await usuario.click(botonAcordeon);

    // Texto de ayuda explícito
    expect(screen.getByText(/Mínimo 10 caracteres/i)).toBeInTheDocument();

    const inputActual = screen.getByLabelText(/Contraseña actual/i);
    const inputNueva = screen.getByLabelText(/^Nueva contraseña/i);
    const inputConfirmar = screen.getByLabelText(/Confirmar nueva contraseña/i);
    const botonGuardar = screen.getByRole('button', { name: /Guardar cambios/i });

    // Caso 1: No digita clave actual
    await usuario.type(inputNueva, 'ClaveLarga123');
    await usuario.type(inputConfirmar, 'ClaveLarga123');
    await usuario.click(botonGuardar);
    expect(await screen.findByText(/Debes ingresar tu contraseña actual/i)).toBeInTheDocument();

    // Caso 2: Clave con menos de 10 caracteres
    await usuario.type(inputActual, 'Actual1234');
    await usuario.clear(inputNueva);
    await usuario.clear(inputConfirmar);
    await usuario.type(inputNueva, 'Corta123');
    await usuario.type(inputConfirmar, 'Corta123');
    await usuario.click(botonGuardar);
    expect(await screen.findByText(/La contraseña debe tener al menos 10 caracteres/i)).toBeInTheDocument();

    // Caso 3: Claves no coinciden
    await usuario.clear(inputNueva);
    await usuario.clear(inputConfirmar);
    await usuario.type(inputNueva, 'ClaveValida123');
    await usuario.type(inputConfirmar, 'NoCoincide123');
    await usuario.click(botonGuardar);
    expect(await screen.findByText(/Las contraseñas no coinciden/i)).toBeInTheDocument();

    expect(servicioAutenticacion.actualizarPerfil).not.toHaveBeenCalled();
  });

  it('guarda los cambios correctamente y actualiza el usuario en el contexto sin romper la sesión', async () => {
    servicioAutenticacion.obtenerPerfil.mockResolvedValueOnce({
      id: 42,
      nombreCompleto: 'Carlos Perez',
      correo: 'carlos@ejemplo.com',
      rol: 'PACIENTE'
    });

    const respuestaActualizada = {
      id: 42,
      nombreCompleto: 'Carlos Perez Modificado',
      correo: 'carlos.nuevo@ejemplo.com',
      rol: 'PACIENTE'
    };
    servicioAutenticacion.actualizarPerfil.mockResolvedValueOnce(respuestaActualizada);

    render(<SeccionPerfil />);
    const usuario = userEvent.setup();

    const inputNombre = await screen.findByDisplayValue('Carlos Perez');
    const inputCorreo = screen.getByDisplayValue('carlos@ejemplo.com');

    await usuario.clear(inputNombre);
    await usuario.type(inputNombre, 'Carlos Perez Modificado');
    await usuario.clear(inputCorreo);
    await usuario.type(inputCorreo, 'carlos.nuevo@ejemplo.com');

    // Cambiar clave con datos válidos
    const botonAcordeon = screen.getByRole('button', { name: /¿Deseas cambiar tu contraseña\?/i });
    await usuario.click(botonAcordeon);

    await usuario.type(screen.getByLabelText(/Contraseña actual/i), 'ClaveAntigua123');
    await usuario.type(screen.getByLabelText(/^Nueva contraseña/i), 'NuevaClaveValida2026!');
    await usuario.type(screen.getByLabelText(/Confirmar nueva contraseña/i), 'NuevaClaveValida2026!');

    const botonGuardar = screen.getByRole('button', { name: /Guardar cambios/i });
    await usuario.click(botonGuardar);

    expect(servicioAutenticacion.actualizarPerfil).toHaveBeenCalledWith({
      nombreCompleto: 'Carlos Perez Modificado',
      correo: 'carlos.nuevo@ejemplo.com',
      contrasenaActual: 'ClaveAntigua123',
      nuevaContrasena: 'NuevaClaveValida2026!'
    });

    // Mensaje de éxito sobrio
    expect(await screen.findByText('Datos actualizados correctamente')).toBeInTheDocument();

    // Actualiza el estado global sin romper sesión
    expect(actualizarUsuarioEnEstadoMock).toHaveBeenCalledWith(respuestaActualizada);
    expect(cargarPerfilUsuarioMock).toHaveBeenCalled();
  });

  it('muestra mensaje amigable cuando el backend responde error 409 de correo duplicado', async () => {
    servicioAutenticacion.obtenerPerfil.mockResolvedValueOnce({
      id: 42,
      nombreCompleto: 'Carlos Perez',
      correo: 'carlos@ejemplo.com',
      rol: 'PACIENTE'
    });

    const error409 = new Error('Conflicto');
    error409.estado = 409;
    servicioAutenticacion.actualizarPerfil.mockRejectedValueOnce(error409);

    render(<SeccionPerfil />);
    const usuario = userEvent.setup();

    await screen.findByDisplayValue('Carlos Perez');
    const botonGuardar = screen.getByRole('button', { name: /Guardar cambios/i });
    await usuario.click(botonGuardar);

    expect(
      await screen.findByText('El correo electrónico ya se encuentra registrado por otro usuario.')
    ).toBeInTheDocument();
  });
});
