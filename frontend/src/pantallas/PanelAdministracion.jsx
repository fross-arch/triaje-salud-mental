import React, { useState, useEffect } from 'react';
import {
  ShieldCheck,
  UserPlus,
  CheckCircle,
  XCircle,
  AlertCircle,
  RefreshCw,
  Lock,
  Mail,
  User,
  Users,
  Edit2,
  Trash2,
  Briefcase
} from 'lucide-react';
import { servicioAdministracion } from '../servicios/servicioAdministracion.js';
import { ModalConfirmacion } from '../componentes/comunes/ModalConfirmacion.jsx';
import { ModalEditarClinico, ESPECIALIDADES_PREDEFINIDAS } from '../componentes/administracion/ModalEditarClinico.jsx';
import { ModalEditarPaciente } from '../componentes/administracion/ModalEditarPaciente.jsx';
import { SeccionPerfil } from '../componentes/perfil/SeccionPerfil.jsx';

export function PanelAdministracion() {
  const [pestanaActiva, setPestanaActiva] = useState('clinicos'); // 'clinicos' | 'pacientes'

  // Estado clinicos
  const [clinicos, setClinicos] = useState([]);
  const [cargandoClinicos, setCargandoClinicos] = useState(true);
  const [accionEnCurso, setAccionEnCurso] = useState(null); // id en proceso de activacion/desactivacion/eliminar
  const [formularioAbierto, setFormularioAbierto] = useState(false);
  const [guardando, setGuardando] = useState(false);

  // Formulario creacion clinico
  const [nombreCompleto, setNombreCompleto] = useState('');
  const [correo, setCorreo] = useState('');
  const [contrasenaInicial, setContrasenaInicial] = useState('');
  const [especialidadSeleccionada, setEspecialidadSeleccionada] = useState(ESPECIALIDADES_PREDEFINIDAS[0]);
  const [especialidadOtra, setEspecialidadOtra] = useState('');

  // Edicion clinico
  const [clinicoAEditar, setClinicoAEditar] = useState(null);
  const [guardandoEdit, setGuardandoEdit] = useState(false);
  const [errorEditClinico, setErrorEditClinico] = useState('');

  // Edicion paciente
  const [pacienteAEditar, setPacienteAEditar] = useState(null);
  const [guardandoEditPaciente, setGuardandoEditPaciente] = useState(false);
  const [errorEditPaciente, setErrorEditPaciente] = useState('');

  // Modal eliminar clinico
  const [clinicoAEliminar, setClinicoAEliminar] = useState(null);

  // Estado pacientes
  const [pacientes, setPacientes] = useState([]);
  const [cargandoPacientes, setCargandoPacientes] = useState(false);
  const [pacientesCargados, setPacientesCargados] = useState(false);

  const [mensajeEstado, setMensajeEstado] = useState('');
  const [error, setError] = useState('');

  const cargarClinicos = async () => {
    setCargandoClinicos(true);
    setError('');
    try {
      const datos = await servicioAdministracion.listarClinicos();
      setClinicos(Array.isArray(datos) ? datos : []);
    } catch (err) {
      setError(err.message || 'No fue posible cargar el listado de profesionales clínicos.');
    } finally {
      setCargandoClinicos(false);
    }
  };

  const cargarPacientes = async () => {
    setCargandoPacientes(true);
    setError('');
    try {
      const datos = await servicioAdministracion.listarPacientes();
      setPacientes(Array.isArray(datos) ? datos : []);
      setPacientesCargados(true);
    } catch (err) {
      setError(err.message || 'No fue posible cargar el historial de pacientes.');
    } finally {
      setCargandoPacientes(false);
    }
  };

  useEffect(() => {
    cargarClinicos();
  }, []);

  useEffect(() => {
    if (pestanaActiva === 'pacientes' && !pacientesCargados) {
      cargarPacientes();
    }
  }, [pestanaActiva, pacientesCargados]);

  const resolverEspecialidadEfectiva = (sel, otra) => {
    if (sel === 'OTRA') {
      return otra.trim() || 'Psicología General';
    }
    return sel;
  };

  const manejarRegistrar = async (e) => {
    e.preventDefault();
    setError('');
    setMensajeEstado('');

    if (contrasenaInicial.length < 10) {
      setError('La contraseña inicial debe tener al menos 10 caracteres.');
      return;
    }

    const especialidadFinal = resolverEspecialidadEfectiva(especialidadSeleccionada, especialidadOtra);

    setGuardando(true);
    try {
      const nuevo = await servicioAdministracion.crearClinico({
        nombreCompleto: nombreCompleto.trim(),
        correo: correo.trim().toLowerCase(),
        contrasenaInicial,
        especialidad: especialidadFinal
      });

      setClinicos((prev) => [nuevo, ...prev]);
      setNombreCompleto('');
      setCorreo('');
      setContrasenaInicial('');
      setEspecialidadSeleccionada(ESPECIALIDADES_PREDEFINIDAS[0]);
      setEspecialidadOtra('');
      setFormularioAbierto(false);
      setMensajeEstado(`Cuenta clínica creada exitosamente para ${nuevo.nombreCompleto}.`);
    } catch (err) {
      setError(err.message || 'Error al registrar el profesional clínico.');
    } finally {
      setGuardando(false);
    }
  };

  const abrirEdicion = (clinico) => {
    setClinicoAEditar(clinico);
    setErrorEditClinico('');
  };

  const manejarGuardarEdicionClinico = async (datos) => {
    setError('');
    setErrorEditClinico('');
    setMensajeEstado('');
    setGuardandoEdit(true);
    try {
      const payloadClinico = {
        nombreCompleto: datos.nombreCompleto,
        correo: datos.correo,
        especialidad: datos.especialidad
      };
      if (datos.nuevaContrasena) {
        payloadClinico.nuevaContrasena = datos.nuevaContrasena;
      }
      const actualizado = await servicioAdministracion.actualizarClinico(datos.id, payloadClinico);

      if (actualizado) {
        setClinicos((prev) =>
          prev.map((c) => (c.id === datos.id ? { ...c, ...actualizado } : c))
        );
        setClinicoAEditar(null);
        setMensajeEstado(`Datos del profesional ${actualizado.nombreCompleto || datos.nombreCompleto} actualizados exitosamente.`);
      }
    } catch (err) {
      const msj = err.status === 409 || err.estado === 409
        ? 'El correo electrónico ya se encuentra registrado por otro usuario.'
        : (err.message || 'Error al actualizar el profesional clínico.');
      setErrorEditClinico(msj);
    } finally {
      setGuardandoEdit(false);
    }
  };

  const abrirEdicionPaciente = (paciente) => {
    setPacienteAEditar(paciente);
    setErrorEditPaciente('');
  };

  const manejarGuardarEdicionPaciente = async (datos) => {
    setError('');
    setErrorEditPaciente('');
    setMensajeEstado('');
    setGuardandoEditPaciente(true);
    try {
      const payloadPaciente = {
        nombreCompleto: datos.nombreCompleto,
        correo: datos.correo
      };
      if (datos.nuevaContrasena) {
        payloadPaciente.nuevaContrasena = datos.nuevaContrasena;
      }
      const actualizado = await servicioAdministracion.actualizarPaciente(datos.id, payloadPaciente);

      if (actualizado) {
        const nombre = actualizado.nombre || actualizado.nombreCompleto || datos.nombreCompleto;
        setPacientes((prev) =>
          prev.map((p) => (p.id === datos.id ? {
            ...p,
            ...actualizado,
            nombre,
            nombreCompleto: nombre
          } : p))
        );
        setPacienteAEditar(null);
        setMensajeEstado(`Datos del paciente ${nombre} actualizados exitosamente.`);
      }
    } catch (err) {
      const msj = err.status === 409 || err.estado === 409
        ? 'El correo electrónico ya se encuentra registrado por otro usuario.'
        : (err.message || 'Error al actualizar el paciente.');
      setErrorEditPaciente(msj);
    } finally {
      setGuardandoEditPaciente(false);
    }
  };

  const manejarActivar = async (id) => {
    setAccionEnCurso(id);
    setError('');
    setMensajeEstado('');
    try {
      const actualizado = await servicioAdministracion.activarClinico(id);
      setClinicos((prev) =>
        prev.map((c) => (c.id === id ? { ...c, ...actualizado, activo: true } : c))
      );
      setMensajeEstado(`Cuenta de ${actualizado.nombreCompleto} activada exitosamente.`);
    } catch (err) {
      setError(err.message || 'Error al activar la cuenta.');
    } finally {
      setAccionEnCurso(null);
    }
  };

  const manejarDesactivar = async (id) => {
    setAccionEnCurso(id);
    setError('');
    setMensajeEstado('');
    try {
      const actualizado = await servicioAdministracion.desactivarClinico(id);
      setClinicos((prev) =>
        prev.map((c) => (c.id === id ? { ...c, ...actualizado, activo: false } : c))
      );
      setMensajeEstado(`Cuenta de ${actualizado.nombreCompleto} desactivada exitosamente.`);
    } catch (err) {
      setError(err.message || 'Error al desactivar la cuenta.');
    } finally {
      setAccionEnCurso(null);
    }
  };

  const manejarConfirmarEliminar = async () => {
    if (!clinicoAEliminar) return;
    const id = clinicoAEliminar.id;
    setAccionEnCurso(id);
    setError('');
    setMensajeEstado('');
    try {
      await servicioAdministracion.eliminarClinico(id);
      setClinicos((prev) => prev.filter((c) => c.id !== id));
      setMensajeEstado(`Profesional ${clinicoAEliminar.nombreCompleto} eliminado exitosamente.`);
      setClinicoAEliminar(null);
    } catch (err) {
      setError(err.message || 'Error al eliminar el profesional clínico.');
    } finally {
      setAccionEnCurso(null);
    }
  };

  return (
    <main className="max-w-5xl mx-auto px-4 py-8 sm:py-12">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 flex items-center gap-2">
            <ShieldCheck className="w-6 h-6 text-indigo-600" aria-hidden="true" />
            <span>Panel de Administración</span>
          </h1>
          <p className="text-xs text-slate-500 mt-1">
            Gestión institucional de profesionales autorizados y consulta administrativa de pacientes.
          </p>
        </div>

        {pestanaActiva === 'clinicos' && (
          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={cargarClinicos}
              className="px-3 py-2 border border-slate-300 hover:bg-slate-50 rounded-xl text-xs font-semibold text-slate-700 flex items-center gap-1.5 transition-colors shadow-xs"
              aria-label="Actualizar listado de profesionales"
            >
              <RefreshCw className="w-3.5 h-3.5" aria-hidden="true" />
              <span>Actualizar</span>
            </button>

            <button
              type="button"
              onClick={() => setFormularioAbierto(!formularioAbierto)}
              className="px-3.5 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl text-xs font-semibold shadow-xs transition-colors flex items-center gap-1.5"
              aria-expanded={formularioAbierto}
            >
              <UserPlus className="w-4 h-4" aria-hidden="true" />
              <span>{formularioAbierto ? 'Cerrar formulario' : 'Registrar profesional'}</span>
            </button>
          </div>
        )}

        {pestanaActiva === 'pacientes' && (
          <button
            type="button"
            onClick={cargarPacientes}
            className="px-3 py-2 border border-slate-300 hover:bg-slate-50 rounded-xl text-xs font-semibold text-slate-700 flex items-center gap-1.5 transition-colors shadow-xs"
            aria-label="Actualizar historial de pacientes"
          >
            <RefreshCw className="w-3.5 h-3.5" aria-hidden="true" />
            <span>Actualizar</span>
          </button>
        )}
      </div>

      {/* Pestañas de navegación administrativa */}
      <div className="flex gap-2 border-b border-slate-200 mb-6">
        <button
          type="button"
          onClick={() => setPestanaActiva('clinicos')}
          className={`pb-3 px-4 text-xs font-bold border-b-2 transition-colors flex items-center gap-2 ${
            pestanaActiva === 'clinicos'
              ? 'border-indigo-600 text-indigo-700'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          <ShieldCheck className="w-4 h-4" aria-hidden="true" />
          <span>Gestión de Clínicos</span>
        </button>

        <button
          type="button"
          onClick={() => setPestanaActiva('pacientes')}
          className={`pb-3 px-4 text-xs font-bold border-b-2 transition-colors flex items-center gap-2 ${
            pestanaActiva === 'pacientes'
              ? 'border-indigo-600 text-indigo-700'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          <Users className="w-4 h-4" aria-hidden="true" />
          <span>Historial de Pacientes</span>
        </button>

        <button
          type="button"
          onClick={() => setPestanaActiva('perfil')}
          className={`pb-3 px-4 text-xs font-bold border-b-2 transition-colors flex items-center gap-2 ${
            pestanaActiva === 'perfil'
              ? 'border-indigo-600 text-indigo-700'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          <User className="w-4 h-4" aria-hidden="true" />
          <span>Mi Perfil</span>
        </button>
      </div>

      {mensajeEstado && (
        <div role="status" className="mb-4 p-3 bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs rounded-xl flex items-center gap-2">
          <CheckCircle className="w-4 h-4 text-emerald-600 shrink-0" aria-hidden="true" />
          <span>{mensajeEstado}</span>
        </div>
      )}

      {error && (
        <div role="alert" className="mb-4 p-3 bg-rose-50 border border-rose-200 text-rose-800 text-xs rounded-xl flex items-center gap-2">
          <AlertCircle className="w-4 h-4 text-rose-600 shrink-0" aria-hidden="true" />
          <span>{error}</span>
        </div>
      )}

      {/* ================= PESTAÑA: GESTION DE CLINICOS ================= */}
      {pestanaActiva === 'clinicos' && (
        <>
          {/* Formulario de registro de profesional clinico */}
          {formularioAbierto && (
            <section aria-labelledby="titulo-nuevo-profesional" className="mb-6 bg-white border border-indigo-200 rounded-2xl p-6 shadow-sm">
              <h2 id="titulo-nuevo-profesional" className="text-base font-bold text-slate-900 mb-4 flex items-center gap-2">
                <UserPlus className="w-5 h-5 text-indigo-600" aria-hidden="true" />
                <span>Registrar nuevo profesional de la salud</span>
              </h2>

              <form onSubmit={manejarRegistrar} className="grid sm:grid-cols-2 lg:grid-cols-3 gap-4">
                <div>
                  <label htmlFor="campo-nombre-clinico" className="block text-xs font-bold text-slate-700 mb-1">
                    Nombre completo:
                  </label>
                  <div className="relative">
                    <User className="w-4 h-4 text-slate-400 absolute left-3 top-2.5" aria-hidden="true" />
                    <input
                      type="text"
                      id="campo-nombre-clinico"
                      value={nombreCompleto}
                      onChange={(e) => setNombreCompleto(e.target.value)}
                      placeholder="Dra. María Pérez"
                      className="w-full pl-9 pr-3 py-2 border border-slate-300 rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-indigo-500"
                      required
                    />
                  </div>
                </div>

                <div>
                  <label htmlFor="campo-correo-clinico" className="block text-xs font-bold text-slate-700 mb-1">
                    Correo electrónico institucional:
                  </label>
                  <div className="relative">
                    <Mail className="w-4 h-4 text-slate-400 absolute left-3 top-2.5" aria-hidden="true" />
                    <input
                      type="email"
                      id="campo-correo-clinico"
                      value={correo}
                      onChange={(e) => setCorreo(e.target.value)}
                      placeholder="mperez@cesde.net"
                      className="w-full pl-9 pr-3 py-2 border border-slate-300 rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-indigo-500"
                      required
                    />
                  </div>
                </div>

                <div>
                  <label htmlFor="campo-clave-clinico" className="block text-xs font-bold text-slate-700 mb-1">
                    Contraseña inicial (mín. 10 car.):
                  </label>
                  <div className="relative">
                    <Lock className="w-4 h-4 text-slate-400 absolute left-3 top-2.5" aria-hidden="true" />
                    <input
                      type="password"
                      id="campo-clave-clinico"
                      value={contrasenaInicial}
                      onChange={(e) => setContrasenaInicial(e.target.value)}
                      placeholder="••••••••••••"
                      className="w-full pl-9 pr-3 py-2 border border-slate-300 rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-indigo-500"
                      required
                    />
                  </div>
                </div>

                {/* Campo Especialidad */}
                <div>
                  <label htmlFor="campo-especialidad-clinico" className="block text-xs font-bold text-slate-700 mb-1">
                    Especialidad profesional:
                  </label>
                  <div className="relative">
                    <Briefcase className="w-4 h-4 text-slate-400 absolute left-3 top-2.5" aria-hidden="true" />
                    <select
                      id="campo-especialidad-clinico"
                      value={especialidadSeleccionada}
                      onChange={(e) => setEspecialidadSeleccionada(e.target.value)}
                      className="w-full pl-9 pr-3 py-2 bg-white border border-slate-300 rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-indigo-500"
                    >
                      {ESPECIALIDADES_PREDEFINIDAS.map((esp) => (
                        <option key={esp} value={esp}>
                          {esp}
                        </option>
                      ))}
                      <option value="OTRA">Otra (escribir especialidad)</option>
                    </select>
                  </div>
                </div>

                {especialidadSeleccionada === 'OTRA' && (
                  <div>
                    <label htmlFor="campo-otra-especialidad" className="block text-xs font-bold text-slate-700 mb-1">
                      Escribe la especialidad:
                    </label>
                    <input
                      type="text"
                      id="campo-otra-especialidad"
                      value={especialidadOtra}
                      onChange={(e) => setEspecialidadOtra(e.target.value)}
                      placeholder="Ej. Terapia Familiar Sistémica"
                      className="w-full px-3 py-2 border border-slate-300 rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-indigo-500"
                      required
                    />
                  </div>
                )}

                <div className="sm:col-span-2 lg:col-span-3 flex justify-end gap-2.5 pt-2 border-t border-slate-100">
                  <button
                    type="button"
                    onClick={() => setFormularioAbierto(false)}
                    className="px-4 py-2 border border-slate-300 hover:bg-slate-50 text-slate-700 rounded-xl text-xs font-semibold transition-colors"
                  >
                    Cancelar
                  </button>
                  <button
                    type="submit"
                    disabled={guardando}
                    className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 disabled:bg-slate-300 text-white rounded-xl text-xs font-semibold shadow-xs transition-colors"
                  >
                    {guardando ? 'Guardando...' : 'Crear cuenta clínica'}
                  </button>
                </div>
              </form>
            </section>
          )}

          {/* Listado de cuentas clinicas */}
          <section aria-labelledby="titulo-listado-clinicos" className="bg-white border border-slate-200 rounded-2xl p-6 shadow-sm">
            <h2 id="titulo-listado-clinicos" className="text-base font-bold text-slate-900 mb-4">
              Profesionales registrados ({clinicos.length})
            </h2>

            {cargandoClinicos ? (
              <div className="py-12 text-center" role="status" aria-live="polite">
                <div className="w-8 h-8 border-4 border-indigo-600 border-t-transparent rounded-full animate-spin mx-auto mb-3" aria-hidden="true" />
                <p className="text-xs text-slate-500">Cargando cuentas clínicas...</p>
              </div>
            ) : clinicos.length === 0 ? (
              <div className="py-8 text-center text-slate-500 text-xs">
                No hay profesionales registrados en el sistema.
              </div>
            ) : (
              <div className="divide-y divide-slate-100">
                {clinicos.map((clinico) => {
                  const procesando = accionEnCurso === clinico.id;

                  return (
                    <div
                      key={clinico.id}
                      className="py-4 flex flex-col sm:flex-row sm:items-center justify-between gap-3 text-xs"
                    >
                      <div className="space-y-1">
                        <div className="flex items-center gap-2">
                          <span className="font-bold text-slate-900 text-sm">{clinico.nombreCompleto}</span>
                          <span
                            className={`text-[11px] font-semibold px-2 py-0.5 rounded-full ${
                              clinico.activo
                                ? 'bg-emerald-100 text-emerald-800'
                                : 'bg-slate-100 text-slate-600'
                            }`}
                          >
                            {clinico.activo ? 'Activo' : 'Inactivo'}
                          </span>
                        </div>
                        <p className="text-slate-600">
                          <strong>Especialidad:</strong> {clinico.especialidad || 'No asignada'}
                        </p>
                        <p className="text-slate-500">{clinico.correo} · Rol: {clinico.rol || 'CLINICO'}</p>
                      </div>

                      <div className="flex items-center gap-2 shrink-0 self-end sm:self-center">
                        <button
                          type="button"
                          onClick={() => abrirEdicion(clinico)}
                          className="px-3 py-1.5 border border-slate-300 hover:bg-slate-50 text-slate-700 font-semibold rounded-xl text-xs transition-colors flex items-center gap-1.5"
                          aria-label={`Editar datos de ${clinico.nombreCompleto}`}
                        >
                          <Edit2 className="w-3.5 h-3.5 text-slate-500" aria-hidden="true" />
                          <span>Editar</span>
                        </button>

                        {clinico.activo ? (
                          <button
                            type="button"
                            onClick={() => manejarDesactivar(clinico.id)}
                            disabled={procesando}
                            className="px-3 py-1.5 border border-amber-300 hover:bg-amber-50 text-amber-800 font-semibold rounded-xl text-xs transition-colors flex items-center gap-1.5 disabled:opacity-50"
                            aria-label={`Desactivar cuenta de ${clinico.nombreCompleto}`}
                          >
                            <XCircle className="w-3.5 h-3.5 text-amber-600" aria-hidden="true" />
                            <span>{procesando ? 'Desactivando...' : 'Desactivar'}</span>
                          </button>
                        ) : (
                          <button
                            type="button"
                            onClick={() => manejarActivar(clinico.id)}
                            disabled={procesando}
                            className="px-3 py-1.5 border border-emerald-300 hover:bg-emerald-50 text-emerald-700 font-semibold rounded-xl text-xs transition-colors flex items-center gap-1.5 disabled:opacity-50"
                            aria-label={`Activar cuenta de ${clinico.nombreCompleto}`}
                          >
                            <CheckCircle className="w-3.5 h-3.5 text-emerald-600" aria-hidden="true" />
                            <span>{procesando ? 'Activando...' : 'Activar'}</span>
                          </button>
                        )}

                        <button
                          type="button"
                          onClick={() => setClinicoAEliminar(clinico)}
                          disabled={procesando}
                          className="px-3 py-1.5 border border-rose-300 hover:bg-rose-50 text-rose-700 font-semibold rounded-xl text-xs transition-colors flex items-center gap-1.5 disabled:opacity-50"
                          aria-label={`Eliminar profesional ${clinico.nombreCompleto}`}
                        >
                          <Trash2 className="w-3.5 h-3.5 text-rose-600" aria-hidden="true" />
                          <span>Eliminar</span>
                        </button>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </section>
        </>
      )}

      {/* ================= PESTAÑA: HISTORIAL DE PACIENTES ================= */}
      {pestanaActiva === 'pacientes' && (
        <section aria-labelledby="titulo-historial-pacientes" className="bg-white border border-slate-200 rounded-2xl p-6 shadow-sm">
          <div className="mb-4">
            <h2 id="titulo-historial-pacientes" className="text-base font-bold text-slate-900 flex items-center gap-2">
              <Users className="w-5 h-5 text-indigo-600" aria-hidden="true" />
              <span>Historial de Pacientes Registrados ({pacientes.length})</span>
            </h2>
            <p className="text-xs text-slate-500 mt-1 leading-relaxed">
              Listado con fines exclusivamente administrativos. En estricto apego al secreto profesional y a la Ley 1581 de 2012, no se expone información médica, respuestas de cribado ni puntajes.
            </p>
          </div>

          {cargandoPacientes ? (
            <div className="py-12 text-center" role="status" aria-live="polite">
              <div className="w-8 h-8 border-4 border-indigo-600 border-t-transparent rounded-full animate-spin mx-auto mb-3" aria-hidden="true" />
              <p className="text-xs text-slate-500">Cargando pacientes registrados...</p>
            </div>
          ) : pacientes.length === 0 ? (
            <div className="py-8 text-center text-slate-500 text-xs">
              No hay pacientes registrados en el sistema.
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs border-collapse">
                <thead>
                  <tr className="border-b border-slate-200 text-slate-600 font-bold bg-slate-50">
                    <th scope="col" className="py-3 px-4">Nombre completo</th>
                    <th scope="col" className="py-3 px-4">Correo electrónico</th>
                    <th scope="col" className="py-3 px-4">Fecha de registro</th>
                    <th scope="col" className="py-3 px-4 text-center">Estado</th>
                    <th scope="col" className="py-3 px-4 text-right">Acciones</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {pacientes.map((paciente) => (
                    <tr key={paciente.id || paciente.correo} className="hover:bg-slate-50/50">
                      <td className="py-3 px-4 font-semibold text-slate-900">
                        {paciente.nombre || paciente.nombreCompleto || 'Paciente anónimo'}
                      </td>
                      <td className="py-3 px-4 text-slate-600">{paciente.correo}</td>
                      <td className="py-3 px-4 text-slate-500">
                        {paciente.fechaRegistro
                          ? new Date(paciente.fechaRegistro).toLocaleDateString('es-CO', {
                              year: 'numeric',
                              month: 'long',
                              day: 'numeric'
                            })
                          : 'No registrada'}
                      </td>
                      <td className="py-3 px-4 text-center">
                        <span
                          className={`inline-block text-[11px] font-semibold px-2.5 py-0.5 rounded-full ${
                            paciente.activo !== false
                              ? 'bg-emerald-100 text-emerald-800'
                              : 'bg-slate-100 text-slate-600'
                          }`}
                        >
                          {paciente.activo !== false ? 'Activo' : 'Inactivo'}
                        </span>
                      </td>
                      <td className="py-3 px-4 text-right">
                        <button
                          type="button"
                          onClick={() => abrirEdicionPaciente(paciente)}
                          className="px-3 py-1.5 border border-slate-300 hover:bg-slate-50 text-slate-700 font-semibold rounded-xl text-xs transition-colors inline-flex items-center gap-1.5"
                          aria-label={`Editar datos de ${paciente.nombre || paciente.nombreCompleto || 'paciente'}`}
                        >
                          <Edit2 className="w-3.5 h-3.5 text-slate-500" aria-hidden="true" />
                          <span>Editar</span>
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>
      )}

      {/* ================= PESTAÑA: MI PERFIL ================= */}
      {pestanaActiva === 'perfil' && (
        <SeccionPerfil />
      )}

      {/* Modal de edición de clínico */}
      <ModalEditarClinico
        abierto={Boolean(clinicoAEditar)}
        clinico={clinicoAEditar}
        alGuardar={manejarGuardarEdicionClinico}
        alCerrar={() => setClinicoAEditar(null)}
        guardando={guardandoEdit}
        errorExterno={errorEditClinico}
      />

      {/* Modal de edición de paciente */}
      <ModalEditarPaciente
        abierto={Boolean(pacienteAEditar)}
        paciente={pacienteAEditar}
        alGuardar={manejarGuardarEdicionPaciente}
        alCerrar={() => setPacienteAEditar(null)}
        guardando={guardandoEditPaciente}
        errorExterno={errorEditPaciente}
      />

      {/* Modal de confirmación para eliminar clínico */}
      <ModalConfirmacion
        abierto={Boolean(clinicoAEliminar)}
        titulo="¿Eliminar cuenta de profesional clínico?"
        mensaje={`¿Estás seguro de que deseas eliminar permanentemente la cuenta de ${clinicoAEliminar?.nombreCompleto}? Esta acción no se puede deshacer.`}
        textoConfirmar="Eliminar profesional"
        textoCancelar="Cancelar"
        esPeligroso={true}
        alConfirmar={manejarConfirmarEliminar}
        alCancelar={() => setClinicoAEliminar(null)}
      />
    </main>
  );
}

export default PanelAdministracion;
