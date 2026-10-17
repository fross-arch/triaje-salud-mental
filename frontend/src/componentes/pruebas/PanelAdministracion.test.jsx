import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { PanelAdministracion } from '../../pantallas/PanelAdministracion.jsx';
import { servicioAdministracion } from '../../servicios/servicioAdministracion.js';

vi.mock('../../servicios/servicioAdministracion.js', () => ({
  servicioAdministracion: {
    listarClinicos: vi.fn(),
    crearClinico: vi.fn(),
    actualizarClinico: vi.fn(),
    eliminarClinico: vi.fn(),
    cambiarEstadoClinico: vi.fn(),
    listarPacientes: vi.fn(),
    actualizarPaciente: vi.fn()
  }
}));

describe('PanelAdministracion', () => {
  const clinicosMock = [
    {
      id: 1,
      nombreCompleto: 'Dr. Alejandro Psicólogo',
      correo: 'alejandro@salud.org',
      especialidad: 'Psicología Clínica',
      activo: true,
      creadoEn: '2026-10-01T10:00:00Z'
    }
  ];

  const pacientesMock = [
    {
      id: 10,
      nombreCompleto: 'Paciente María',
      correo: 'maria@ejemplo.com',
      activo: true,
      creadoEn: '2026-10-05T08:30:00Z'
    }
  ];

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('permite consultar la pestaña de Historial de Pacientes Registrados garantizando estricta privacidad (cero datos clínicos)', async () => {
    servicioAdministracion.listarClinicos.mockResolvedValueOnce(clinicosMock);
    servicioAdministracion.listarPacientes.mockResolvedValueOnce(pacientesMock);

    render(<PanelAdministracion />);

    // Pestaña inicial de clínicos
    expect(await screen.findByText('Dr. Alejandro Psicólogo')).toBeInTheDocument();

    // Cambiar a la pestaña de pacientes registrados
    const usuario = userEvent.setup();
    const pestanaPacientes = screen.getByRole('button', { name: /Historial de Pacientes/i });
    await usuario.click(pestanaPacientes);

    // Verificamos que se llame al servicio de administración de pacientes
    expect(servicioAdministracion.listarPacientes).toHaveBeenCalledTimes(1);

    // Datos administrativos desplegados
    expect(await screen.findByText('Paciente María')).toBeInTheDocument();
    expect(screen.getByText('maria@ejemplo.com')).toBeInTheDocument();
    expect(screen.getByText(/En estricto apego al secreto profesional y a la Ley 1581 de 2012/i)).toBeInTheDocument();

    // Verificamos que no existan encabezados ni datos clínicos en la tabla
    expect(screen.queryByRole('columnheader', { name: /Diagnóstico|Puntaje|Severidad|Historia/i })).not.toBeInTheDocument();
  });

  it('permite seleccionar una especialidad predefinida o escribir una personalizada al crear un clínico', async () => {
    servicioAdministracion.listarClinicos.mockResolvedValueOnce(clinicosMock);
    servicioAdministracion.crearClinico.mockResolvedValueOnce({
      id: 2,
      nombreCompleto: 'Dra. Laura Nueva',
      correo: 'laura@salud.org',
      especialidad: 'Terapia Familiar Sistémica',
      activo: true
    });

    render(<PanelAdministracion />);

    expect(await screen.findByText('Dr. Alejandro Psicólogo')).toBeInTheDocument();

    const usuario = userEvent.setup();
    const botonNuevo = screen.getByRole('button', { name: /Registrar profesional/i });
    await usuario.click(botonNuevo);

    // Formulario abierto
    expect(screen.getByText(/Registrar nuevo profesional de la salud/i)).toBeInTheDocument();

    // Selector de especialidad
    const selectorEsp = screen.getByLabelText(/Especialidad profesional/i);
    expect(selectorEsp).toBeInTheDocument();

    // Elegir "Otra (escribir especialidad)" con value "OTRA"
    await usuario.selectOptions(selectorEsp, 'OTRA');

    // Debe aparecer el input para especificar la especialidad personalizada
    const inputOtra = screen.getByLabelText(/Escribe la especialidad/i);
    expect(inputOtra).toBeInTheDocument();

    // Llenar formulario
    await usuario.type(screen.getByLabelText(/Nombre completo/i), 'Dra. Laura Nueva');
    await usuario.type(screen.getByLabelText(/Correo electrónico institucional/i), 'laura@salud.org');
    await usuario.type(screen.getByLabelText(/Contraseña inicial/i), 'ClaveAdmin2026!');
    await usuario.type(inputOtra, 'Terapia Familiar Sistémica');

    const botonGuardar = screen.getByRole('button', { name: /Crear cuenta clínica/i });
    await usuario.click(botonGuardar);

    await waitFor(() => {
      expect(servicioAdministracion.crearClinico).toHaveBeenCalledWith({
        nombreCompleto: 'Dra. Laura Nueva',
        correo: 'laura@salud.org',
        contrasenaInicial: 'ClaveAdmin2026!',
        especialidad: 'Terapia Familiar Sistémica'
      });
    });
  });

  it('al abrir la edición de un clínico con especialidad predefinida, la selecciona en el select y no muestra campo de texto libre', async () => {
    servicioAdministracion.listarClinicos.mockResolvedValueOnce(clinicosMock);
    servicioAdministracion.actualizarClinico.mockResolvedValueOnce({
      id: 1,
      nombreCompleto: 'Dr. Alejandro Actualizado',
      correo: 'alejandro.mod@salud.org',
      especialidad: 'Psicología Clínica',
      activo: true
    });

    render(<PanelAdministracion />);

    expect(await screen.findByText('Dr. Alejandro Psicólogo')).toBeInTheDocument();

    const usuario = userEvent.setup();
    const botonEditar = screen.getByRole('button', { name: /Editar datos de Dr. Alejandro Psicólogo/i });
    await usuario.click(botonEditar);

    // Modal de edición visible
    expect(screen.getByText(/Editar profesional:/i)).toBeInTheDocument();

    // El select debe tener seleccionada la especialidad predefinida
    const selectEsp = screen.getByLabelText(/Especialidad profesional/i);
    expect(selectEsp.value).toBe('Psicología Clínica');

    // No debe mostrar el campo libre de otra especialidad
    expect(screen.queryByLabelText(/Escribe la especialidad/i)).not.toBeInTheDocument();

    // Guardar cambios
    const botonGuardar = screen.getByRole('button', { name: /Guardar cambios/i });
    await usuario.click(botonGuardar);

    await waitFor(() => {
      expect(servicioAdministracion.actualizarClinico).toHaveBeenCalledWith(1, {
        nombreCompleto: 'Dr. Alejandro Psicólogo',
        correo: 'alejandro@salud.org',
        especialidad: 'Psicología Clínica'
      });
    });
  });

  it('al abrir la edición de un clínico con especialidad personalizada, selecciona OTRA y carga el texto en el campo libre', async () => {
    const clinicoPersonalizado = [
      {
        id: 5,
        nombreCompleto: 'Dra. Sofía Holística',
        correo: 'sofia@salud.org',
        especialidad: 'Terapia Gestalt Integrativa',
        activo: true,
        creadoEn: '2026-10-02T10:00:00Z'
      }
    ];
    servicioAdministracion.listarClinicos.mockResolvedValueOnce(clinicoPersonalizado);
    servicioAdministracion.actualizarClinico.mockResolvedValueOnce({
      id: 5,
      nombreCompleto: 'Dra. Sofía Holística',
      correo: 'sofia@salud.org',
      especialidad: 'Terapia Gestalt Avanzada',
      activo: true
    });

    render(<PanelAdministracion />);

    expect(await screen.findByText('Dra. Sofía Holística')).toBeInTheDocument();

    const usuario = userEvent.setup();
    const botonEditar = screen.getByRole('button', { name: /Editar datos de Dra. Sofía Holística/i });
    await usuario.click(botonEditar);

    // Modal de edición visible
    expect(screen.getByText(/Editar profesional:/i)).toBeInTheDocument();

    // El select debe estar en "OTRA"
    const selectEsp = screen.getByLabelText(/Especialidad profesional/i);
    expect(selectEsp.value).toBe('OTRA');

    // El campo de texto libre debe estar presente y con el valor original
    const inputOtra = screen.getByLabelText(/Escribe la especialidad/i);
    expect(inputOtra).toBeInTheDocument();
    expect(inputOtra.value).toBe('Terapia Gestalt Integrativa');

    // Modificar especialidad libre
    await usuario.clear(inputOtra);
    await usuario.type(inputOtra, 'Terapia Gestalt Avanzada');

    // Guardar
    const botonGuardar = screen.getByRole('button', { name: /Guardar cambios/i });
    await usuario.click(botonGuardar);

    await waitFor(() => {
      expect(servicioAdministracion.actualizarClinico).toHaveBeenCalledWith(5, {
        nombreCompleto: 'Dra. Sofía Holística',
        correo: 'sofia@salud.org',
        especialidad: 'Terapia Gestalt Avanzada'
      });
    });
  });

  it('al abrir la edición de un clínico sin especialidad (null), no produce error y selecciona la opción por defecto', async () => {
    const clinicoSinEsp = [
      {
        id: 8,
        nombreCompleto: 'Dr. Sin Especialidad',
        correo: 'sin.esp@salud.org',
        especialidad: null,
        activo: true,
        creadoEn: '2026-10-03T10:00:00Z'
      }
    ];
    servicioAdministracion.listarClinicos.mockResolvedValueOnce(clinicoSinEsp);

    render(<PanelAdministracion />);

    expect(await screen.findByText('Dr. Sin Especialidad')).toBeInTheDocument();

    const usuario = userEvent.setup();
    const botonEditar = screen.getByRole('button', { name: /Editar datos de Dr. Sin Especialidad/i });
    // Esto fallaba anteriormente con ReferenceError: setEspecialEditOtra is not defined
    await usuario.click(botonEditar);

    expect(screen.getByText(/Editar profesional:/i)).toBeInTheDocument();
    const selectEsp = screen.getByLabelText(/Especialidad profesional/i);
    expect(selectEsp.value).toBe('Psicología Clínica');
    expect(screen.queryByLabelText(/Escribe la especialidad/i)).not.toBeInTheDocument();
  });

  it('permite abrir el modal ModalEditarPaciente al pulsar Editar en la lista de pacientes con los datos precargados', async () => {
    servicioAdministracion.listarClinicos.mockResolvedValueOnce(clinicosMock);
    servicioAdministracion.listarPacientes.mockResolvedValueOnce(pacientesMock);

    render(<PanelAdministracion />);

    const usuario = userEvent.setup();
    const pestanaPacientes = await screen.findByRole('button', { name: /Historial de Pacientes/i });
    await usuario.click(pestanaPacientes);

    const botonEditarPaciente = await screen.findByRole('button', { name: /Editar datos de Paciente María/i });
    await usuario.click(botonEditarPaciente);

    expect(screen.getByRole('dialog')).toBeInTheDocument();
    expect(screen.getByText(/Editar paciente: Paciente María/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Nombre completo/i).value).toBe('Paciente María');
    expect(screen.getByLabelText(/Correo electrónico/i).value).toBe('maria@ejemplo.com');
  });

  it('valida en el cliente que si se escribe una nueva contraseña en ModalEditarPaciente, tenga al menos 10 caracteres', async () => {
    servicioAdministracion.listarClinicos.mockResolvedValueOnce(clinicosMock);
    servicioAdministracion.listarPacientes.mockResolvedValueOnce(pacientesMock);

    render(<PanelAdministracion />);

    const usuario = userEvent.setup();
    const pestanaPacientes = await screen.findByRole('button', { name: /Historial de Pacientes/i });
    await usuario.click(pestanaPacientes);

    const botonEditarPaciente = await screen.findByRole('button', { name: /Editar datos de Paciente María/i });
    await usuario.click(botonEditarPaciente);

    const inputClave = screen.getByLabelText(/Nueva contraseña/i);
    await usuario.type(inputClave, 'corta');

    const botonGuardar = screen.getByRole('button', { name: /Guardar cambios/i });
    await usuario.click(botonGuardar);

    expect(await screen.findByText('La contraseña debe tener al menos 10 caracteres.')).toBeInTheDocument();
    expect(servicioAdministracion.actualizarPaciente).not.toHaveBeenCalled();
  });

  it('permite actualizar los datos del paciente y restablecer la contraseña si cumple los 10 caracteres', async () => {
    servicioAdministracion.listarClinicos.mockResolvedValueOnce(clinicosMock);
    servicioAdministracion.listarPacientes.mockResolvedValueOnce(pacientesMock);
    servicioAdministracion.actualizarPaciente.mockResolvedValueOnce({
      id: 10,
      nombre: 'Paciente María Actualizada',
      correo: 'maria.actualizada@ejemplo.com',
      activo: true
    });

    render(<PanelAdministracion />);

    const usuario = userEvent.setup();
    const pestanaPacientes = await screen.findByRole('button', { name: /Historial de Pacientes/i });
    await usuario.click(pestanaPacientes);

    const botonEditarPaciente = await screen.findByRole('button', { name: /Editar datos de Paciente María/i });
    await usuario.click(botonEditarPaciente);

    const inputNombre = screen.getByLabelText(/Nombre completo/i);
    const inputCorreo = screen.getByLabelText(/Correo electrónico/i);
    const inputClave = screen.getByLabelText(/Nueva contraseña/i);

    await usuario.clear(inputNombre);
    await usuario.type(inputNombre, 'Paciente María Actualizada');
    await usuario.clear(inputCorreo);
    await usuario.type(inputCorreo, 'maria.actualizada@ejemplo.com');
    await usuario.type(inputClave, 'NuevaClaveValida123!');

    const botonGuardar = screen.getByRole('button', { name: /Guardar cambios/i });
    await usuario.click(botonGuardar);

    await waitFor(() => {
      expect(servicioAdministracion.actualizarPaciente).toHaveBeenCalledWith(10, {
        nombreCompleto: 'Paciente María Actualizada',
        correo: 'maria.actualizada@ejemplo.com',
        nuevaContrasena: 'NuevaClaveValida123!'
      });
    });

    expect(await screen.findByText(/Datos del paciente Paciente María Actualizada actualizados exitosamente/i)).toBeInTheDocument();
  });

  it('valida en el cliente que si se escribe una nueva contraseña en ModalEditarClinico, tenga al menos 10 caracteres', async () => {
    servicioAdministracion.listarClinicos.mockResolvedValueOnce(clinicosMock);

    render(<PanelAdministracion />);

    const usuario = userEvent.setup();
    const botonEditar = await screen.findByRole('button', { name: /Editar datos de Dr. Alejandro Psicólogo/i });
    await usuario.click(botonEditar);

    const inputClave = screen.getByLabelText(/Nueva contraseña/i);
    await usuario.type(inputClave, 'clave123');

    const botonGuardar = screen.getByRole('button', { name: /Guardar cambios/i });
    await usuario.click(botonGuardar);

    expect(await screen.findByText('La contraseña debe tener al menos 10 caracteres.')).toBeInTheDocument();
    expect(servicioAdministracion.actualizarClinico).not.toHaveBeenCalled();
  });

  it('permite cerrar el modal con la tecla Escape o pulsando Cancelar', async () => {
    servicioAdministracion.listarClinicos.mockResolvedValueOnce(clinicosMock);
    servicioAdministracion.listarPacientes.mockResolvedValueOnce(pacientesMock);

    render(<PanelAdministracion />);

    const usuario = userEvent.setup();
    const pestanaPacientes = await screen.findByRole('button', { name: /Historial de Pacientes/i });
    await usuario.click(pestanaPacientes);

    const botonEditar = await screen.findByRole('button', { name: /Editar datos de Paciente María/i });
    await usuario.click(botonEditar);

    expect(screen.getByRole('dialog')).toBeInTheDocument();

    // Presionar Escape para cerrar
    await usuario.keyboard('{Escape}');
    await waitFor(() => {
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });
  });

  it('despliega mensaje amigable cuando el backend responde error 409 indicando que el correo ya está en uso', async () => {
    servicioAdministracion.listarClinicos.mockResolvedValueOnce(clinicosMock);
    servicioAdministracion.listarPacientes.mockResolvedValueOnce(pacientesMock);

    const error409 = new Error('El correo ya está registrado.');
    error409.status = 409;
    servicioAdministracion.actualizarPaciente.mockRejectedValueOnce(error409);

    render(<PanelAdministracion />);

    const usuario = userEvent.setup();
    const pestanaPacientes = await screen.findByRole('button', { name: /Historial de Pacientes/i });
    await usuario.click(pestanaPacientes);

    const botonEditar = await screen.findByRole('button', { name: /Editar datos de Paciente María/i });
    await usuario.click(botonEditar);

    const botonGuardar = screen.getByRole('button', { name: /Guardar cambios/i });
    await usuario.click(botonGuardar);

    expect(await screen.findByText('El correo electrónico ya se encuentra registrado por otro usuario.')).toBeInTheDocument();
  });
});

