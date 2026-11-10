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
  Briefcase,
  BarChart3,
  Calendar,
  Activity,
  AlertTriangle,
  Info,
  Download,
  FileText,
  Image as ImageIcon
} from 'lucide-react';

import { servicioAdministracion } from '../servicios/servicioAdministracion.js';
import { servicioTablero } from '../servicios/servicioTablero.js';
import { ModalConfirmacion } from '../componentes/comunes/ModalConfirmacion.jsx';
import { ModalEditarClinico, ESPECIALIDADES_PREDEFINIDAS } from '../componentes/administracion/ModalEditarClinico.jsx';
import { ModalEditarPaciente } from '../componentes/administracion/ModalEditarPaciente.jsx';
import { ImagenAmpliable } from '../componentes/comunes/ImagenAmpliable.jsx';
import { SeccionPerfil } from '../componentes/perfil/SeccionPerfil.jsx';

export function PanelAdministracion() {
  const [pestanaActiva, setPestanaActiva] = useState('clinicos'); // 'clinicos' | 'pacientes' | 'tablero' | 'perfil'

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
  const [clinicoSugeridoDesactivar, setClinicoSugeridoDesactivar] = useState(null);

  // Estado pacientes
  const [pacientes, setPacientes] = useState([]);
  const [cargandoPacientes, setCargandoPacientes] = useState(false);
  const [pacientesCargados, setPacientesCargados] = useState(false);

  // Estado tablero estadístico
  const [diasTablero, setDiasTablero] = useState(30); // 7 | 30 | 90 | 0 (Todo)
  const [datosTablero, setDatosTablero] = useState(null);
  const [cargandoTablero, setCargandoTablero] = useState(false);
  const [tableroCargado, setTableroCargado] = useState(false);
  const [errorTablero, setErrorTablero] = useState('');
  const [descargandoCsv, setDescargandoCsv] = useState(false);
  const [descargandoPdf, setDescargandoPdf] = useState(false);
  const [errorDescarga, setErrorDescarga] = useState('');

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

  const cargarTablero = async (dias = diasTablero) => {
    setCargandoTablero(true);
    setErrorTablero('');
    try {
      const datos = await servicioTablero.obtenerTablero(dias);
      setDatosTablero(datos);
      setTableroCargado(true);
    } catch (err) {
      if (err.status === 503 || (err.message && err.message.toLowerCase().includes('reportes no está disponible'))) {
        setErrorTablero('El servicio de generación de reportes y estadísticas no está disponible en este momento. Por favor, intente más tarde.');
      } else {
        setErrorTablero(err.message || 'No fue posible cargar el tablero estadístico.');
      }
    } finally {
      setCargandoTablero(false);
    }
  };

  const manejarCambiarPeriodo = (nuevoDias) => {
    setDiasTablero(nuevoDias);
    cargarTablero(nuevoDias);
  };

  const manejarDescargarArchivo = async (formato) => {
    setErrorDescarga('');
    if (formato === 'csv') {
      setDescargandoCsv(true);
    } else {
      setDescargandoPdf(true);
    }
    try {
      const blob = await servicioTablero.exportarTablero(diasTablero, formato);
      const url = window.URL.createObjectURL(blob);
      const enlace = document.createElement('a');
      enlace.style.display = 'none';
      enlace.href = url;
      const fechaHoy = new Date().toISOString().split('T')[0];
      const prefijo = datosTablero?.es_demostracion ? 'DEMO_' : '';
      enlace.download = `${prefijo}tablero_${fechaHoy}_${diasTablero}d.${formato}`;
      document.body.appendChild(enlace);
      enlace.click();
      setTimeout(() => {
        if (document.body.contains(enlace)) {
          document.body.removeChild(enlace);
        }
        window.URL.revokeObjectURL(url);
      }, 200);
    } catch (err) {
      setErrorDescarga(err.message || `No fue posible descargar el archivo ${formato.toUpperCase()}.`);
    } finally {
      if (formato === 'csv') {
        setDescargandoCsv(false);
      } else {
        setDescargandoPdf(false);
      }
    }
  };

  const descargarGraficoPng = (base64Str, nombreGrafico) => {
    if (!base64Str) return;
    try {
      const enlace = document.createElement('a');
      enlace.href = `data:image/png;base64,${base64Str}`;
      enlace.download = `${nombreGrafico}.png`;
      document.body.appendChild(enlace);
      enlace.click();
      setTimeout(() => {
        if (document.body.contains(enlace)) {
          document.body.removeChild(enlace);
        }
      }, 200);
    } catch {
      // Ignorar en entorno de test sin DOM completo
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

  useEffect(() => {
    if (pestanaActiva === 'tablero' && !tableroCargado) {
      cargarTablero(diasTablero);
    }
  }, [pestanaActiva, tableroCargado, diasTablero]);

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
    const clinico = clinicoAEliminar;
    const id = clinico.id;
    setAccionEnCurso(id);
    setError('');
    setMensajeEstado('');
    try {
      await servicioAdministracion.eliminarClinico(id);
      setClinicos((prev) => prev.filter((c) => c.id !== id));
      setMensajeEstado(`Profesional ${clinico.nombreCompleto} eliminado exitosamente.`);
      setClinicoAEliminar(null);
      setClinicoSugeridoDesactivar(null);
    } catch (err) {
      setError(err.message || 'Error al eliminar el profesional clínico.');
      if (err.estado === 409 || err.status === 409 || (err.message && err.message.toLowerCase().includes('desactivar'))) {
        setClinicoSugeridoDesactivar(clinico);
      }
      setClinicoAEliminar(null);
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

        {pestanaActiva === 'tablero' && (
          <button
            type="button"
            onClick={() => cargarTablero(diasTablero)}
            disabled={cargandoTablero}
            className="px-3 py-2 border border-slate-300 hover:bg-slate-50 disabled:opacity-50 rounded-xl text-xs font-semibold text-slate-700 flex items-center gap-1.5 transition-colors shadow-xs"
            aria-label="Actualizar estadísticas del tablero"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${cargandoTablero ? 'animate-spin text-indigo-600' : ''}`} aria-hidden="true" />
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
          onClick={() => setPestanaActiva('tablero')}
          className={`pb-3 px-4 text-xs font-bold border-b-2 transition-colors flex items-center gap-2 ${
            pestanaActiva === 'tablero'
              ? 'border-indigo-600 text-indigo-700'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          <BarChart3 className="w-4 h-4" aria-hidden="true" />
          <span>Tablero</span>
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
        <div role="alert" className="mb-4 p-3 bg-rose-50 border border-rose-200 text-rose-800 text-xs rounded-xl flex items-center justify-between gap-3">
          <div className="flex items-center gap-2">
            <AlertCircle className="w-4 h-4 text-rose-600 shrink-0" aria-hidden="true" />
            <span>{error}</span>
          </div>
          {clinicoSugeridoDesactivar && (
            <button
              type="button"
              onClick={async () => {
                const idADesactivar = clinicoSugeridoDesactivar.id;
                setClinicoSugeridoDesactivar(null);
                await manejarDesactivar(idADesactivar);
              }}
              className="px-3 py-1.5 bg-amber-600 hover:bg-amber-700 text-white rounded-lg text-xs font-semibold whitespace-nowrap transition-colors shrink-0 shadow-sm"
            >
              Desactivar profesional
            </button>
          )}
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

      {/* ================= PESTAÑA: TABLERO ESTADÍSTICO ================= */}
      {pestanaActiva === 'tablero' && (
        <section className="space-y-6">
          {/* Barra de controles: Selector de periodo y Descargas */}
          <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4 p-4 bg-white border border-slate-200 rounded-2xl shadow-xs">
            <div className="flex items-center gap-2">
              <Calendar className="w-5 h-5 text-indigo-600" aria-hidden="true" />
              <div>
                <span className="text-xs font-bold text-slate-800">Periodo de análisis:</span>
                <p className="text-[11px] text-slate-500">Filtrar evaluaciones completadas y actividad por antigüedad</p>
              </div>
            </div>

            <div className="flex flex-wrap items-center gap-2">
              <div className="flex items-center gap-1.5 bg-slate-100 p-1 rounded-xl">
                {[
                  { etiqueta: '7 días', valor: 7 },
                  { etiqueta: '30 días', valor: 30 },
                  { etiqueta: '90 días', valor: 90 },
                  { etiqueta: 'Todo el historial', valor: 0 }
                ].map((opcion) => (
                  <button
                    key={opcion.valor}
                    type="button"
                    onClick={() => manejarCambiarPeriodo(opcion.valor)}
                    disabled={cargandoTablero}
                    className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-all ${
                      diasTablero === opcion.valor
                        ? 'bg-white text-indigo-700 shadow-xs'
                        : 'text-slate-600 hover:text-slate-900'
                    }`}
                  >
                    {opcion.etiqueta}
                  </button>
                ))}
              </div>

              {/* Botones de Descarga */}
              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={() => manejarDescargarArchivo('csv')}
                  disabled={cargandoTablero || descargandoCsv || descargandoPdf}
                  className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-emerald-50 hover:bg-emerald-100 text-emerald-800 border border-emerald-200 rounded-xl text-xs font-semibold transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                  title="Descargar datos agregados en formato CSV (sin datos individuales)"
                >
                  {descargandoCsv ? (
                    <RefreshCw className="w-4 h-4 animate-spin text-emerald-700" aria-hidden="true" />
                  ) : (
                    <Download className="w-4 h-4 text-emerald-700" aria-hidden="true" />
                  )}
                  <span>{descargandoCsv ? 'Generando CSV...' : 'Descargar CSV'}</span>
                </button>

                <button
                  type="button"
                  onClick={() => manejarDescargarArchivo('pdf')}
                  disabled={cargandoTablero || descargandoCsv || descargandoPdf}
                  className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-indigo-50 hover:bg-indigo-100 text-indigo-800 border border-indigo-200 rounded-xl text-xs font-semibold transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                  title="Descargar informe consolidado con gráficos en formato PDF"
                >
                  {descargandoPdf ? (
                    <RefreshCw className="w-4 h-4 animate-spin text-indigo-700" aria-hidden="true" />
                  ) : (
                    <FileText className="w-4 h-4 text-indigo-700" aria-hidden="true" />
                  )}
                  <span>{descargandoPdf ? 'Generando PDF...' : 'Informe PDF'}</span>
                </button>
              </div>
            </div>
          </div>

          {/* Banner permanente de modo demostración */}
          {datosTablero?.es_demostracion && (
            <div
              role="status"
              data-testid="banner-demo"
              className="p-4 bg-amber-400 text-slate-950 font-black rounded-2xl shadow-md border-2 border-amber-600 flex items-center justify-center gap-2 text-xs sm:text-sm tracking-wider uppercase text-center"
            >
              <AlertTriangle className="w-5 h-5 text-slate-950 shrink-0" aria-hidden="true" />
              <span>DATOS DE DEMOSTRACIÓN - no corresponden a personas reales</span>
            </div>
          )}

          {/* Aviso de error de descarga */}
          {errorDescarga && (
            <div role="alert" className="p-3 bg-rose-50 border border-rose-200 text-rose-800 text-xs rounded-xl flex items-center justify-between">
              <div className="flex items-center gap-2">
                <AlertTriangle className="w-4 h-4 text-rose-600 shrink-0" aria-hidden="true" />
                <span>{errorDescarga}</span>
              </div>
              <button
                type="button"
                onClick={() => setErrorDescarga('')}
                className="text-rose-600 hover:text-rose-900 font-bold ml-2 text-xs"
              >
                Cerrar
              </button>
            </div>
          )}

          {/* Estado de error de consulta */}
          {errorTablero && (
            <div role="alert" className="p-4 bg-rose-50 border border-rose-200 text-rose-800 text-xs rounded-2xl flex items-start gap-3">
              <AlertTriangle className="w-5 h-5 text-rose-600 shrink-0 mt-0.5" aria-hidden="true" />
              <div>
                <p className="font-bold text-rose-900">No fue posible consultar el tablero</p>
                <p className="mt-1">{errorTablero}</p>
                <button
                  type="button"
                  onClick={() => cargarTablero(diasTablero)}
                  className="mt-3 px-3 py-1.5 bg-rose-100 hover:bg-rose-200 text-rose-900 font-semibold rounded-xl text-xs transition-colors"
                >
                  Reintentar consulta
                </button>
              </div>
            </div>
          )}

          {/* Estado de carga */}
          {cargandoTablero && (
            <div className="py-16 text-center bg-white border border-slate-200 rounded-2xl shadow-xs">
              <RefreshCw className="w-8 h-8 text-indigo-600 animate-spin mx-auto mb-3" aria-hidden="true" />
              <p className="text-sm font-bold text-slate-800">Calculando indicadores y generando gráficos...</p>
              <p className="text-xs text-slate-500 mt-1">El microservicio estadístico está procesando evaluaciones anónimas.</p>
            </div>
          )}

          {/* Estado de datos insuficientes */}
          {!cargandoTablero && !errorTablero && datosTablero?.datos_insuficientes && (
            <div className="p-6 bg-amber-50/70 border border-amber-200 rounded-2xl text-center space-y-3">
              <div className="w-12 h-12 bg-amber-100 text-amber-700 rounded-full flex items-center justify-center mx-auto">
                <Info className="w-6 h-6" aria-hidden="true" />
              </div>
              <h2 className="text-base font-bold text-amber-900">Datos insuficientes para agregación psicométrica</h2>
              <p className="text-xs text-amber-800 max-w-lg mx-auto">
                {datosTablero.mensaje ||
                  `Para proteger la confidencialidad, se requiere un mínimo de ${datosTablero.minimo_requerido || 5} evaluaciones completadas en este periodo.`}
              </p>
              <div className="inline-flex items-center gap-2 px-3 py-1 bg-amber-100/80 rounded-full text-xs font-semibold text-amber-900">
                <span>Evaluaciones registradas: {datosTablero.total_evaluaciones}</span>
                <span>•</span>
                <span>Mínimo requerido: {datosTablero.minimo_requerido}</span>
              </div>
            </div>
          )}

          {/* Vista exitosa con indicadores y los 4 gráficos psicométricos */}
          {!cargandoTablero && !errorTablero && !datosTablero?.datos_insuficientes && datosTablero?.indicadores && (
            <div className="space-y-6">
              {/* Tarjetas de indicadores psicométricos */}
              <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-3">
                <div className="p-4 bg-white border border-slate-200 rounded-2xl shadow-xs">
                  <span className="text-[11px] font-semibold text-slate-500 block">Total Evaluaciones</span>
                  <span className="text-2xl font-extrabold text-slate-900 mt-1 block">
                    {datosTablero.indicadores.total_evaluaciones}
                  </span>
                  <span className="text-[10px] text-slate-400 mt-0.5 block">Completadas</span>
                </div>

                <div className="p-4 bg-white border border-slate-200 rounded-2xl shadow-xs">
                  <span className="text-[11px] font-semibold text-slate-500 block">PHQ-9 (Depresión)</span>
                  <span className="text-2xl font-extrabold text-slate-900 mt-1 block">
                    {datosTablero.indicadores.media_phq9}
                  </span>
                  <span className="text-[10px] text-slate-400 mt-0.5 block">
                    Mediana: {datosTablero.indicadores.mediana_phq9} | ±{datosTablero.indicadores.desviacion_phq9}
                  </span>
                </div>

                <div className="p-4 bg-white border border-slate-200 rounded-2xl shadow-xs">
                  <span className="text-[11px] font-semibold text-slate-500 block">GAD-7 (Ansiedad)</span>
                  <span className="text-2xl font-extrabold text-slate-900 mt-1 block">
                    {datosTablero.indicadores.media_gad7}
                  </span>
                  <span className="text-[10px] text-slate-400 mt-0.5 block">
                    Mediana: {datosTablero.indicadores.mediana_gad7} | ±{datosTablero.indicadores.desviacion_gad7}
                  </span>
                </div>

                <div className="p-4 bg-white border border-slate-200 rounded-2xl shadow-xs">
                  <span className="text-[11px] font-semibold text-slate-500 block">Correlación (r)</span>
                  <span className="text-2xl font-extrabold text-slate-900 mt-1 block">
                    {datosTablero.indicadores.correlacion_phq9_gad7 !== null && datosTablero.indicadores.correlacion_phq9_gad7 !== undefined
                      ? datosTablero.indicadores.correlacion_phq9_gad7
                      : 'N/D'}
                  </span>
                  <span className="text-[10px] text-slate-400 mt-0.5 block">PHQ-9 vs GAD-7</span>
                </div>

                <div className="p-4 bg-white border border-slate-200 rounded-2xl shadow-xs">
                  <span className="text-[11px] font-semibold text-slate-500 block">Riesgo de Crisis</span>
                  <span className={`text-2xl font-extrabold mt-1 block ${
                    datosTablero.indicadores.total_crisis === 0 ? 'text-emerald-600' : 'text-rose-600'
                  }`}>
                    {String(datosTablero.indicadores.total_crisis)}
                  </span>
                  <span className="text-[10px] text-slate-400 mt-0.5 block">
                    {datosTablero.indicadores.porcentaje_crisis !== null && datosTablero.indicadores.porcentaje_crisis !== undefined
                      ? `${datosTablero.indicadores.porcentaje_crisis}% del total`
                      : 'Protegido por umbral'}
                  </span>
                </div>
              </div>

              {/* Los 4 gráficos psicométricos */}
              {datosTablero.graficos && (
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div className="bg-white border border-slate-200 p-4 rounded-2xl shadow-xs space-y-3">
                    <div className="flex items-center justify-between">
                      <span className="text-xs font-bold text-slate-800">Distribución PHQ-9 (Depresión)</span>
                      {datosTablero.graficos.severidad_phq9 && (
                        <button
                          type="button"
                          onClick={() => descargarGraficoPng(datosTablero.graficos.severidad_phq9, 'severidad_phq9')}
                          className="inline-flex items-center gap-1 px-2.5 py-1 text-[11px] font-medium text-slate-600 bg-slate-100 hover:bg-slate-200 rounded-lg transition-colors"
                          title="Descargar imagen en formato PNG"
                          aria-label="Descargar imagen de severidad PHQ-9"
                        >
                          <ImageIcon className="w-3.5 h-3.5" aria-hidden="true" />
                          <span>Descargar imagen</span>
                        </button>
                      )}
                    </div>
                    <ImagenAmpliable
                      src={`data:image/png;base64,${datosTablero.graficos.severidad_phq9}`}
                      alt="Gráfico de distribución por severidad de depresión en el instrumento PHQ-9"
                      tituloModal="Distribución PHQ-9 (Depresión)"
                    />
                  </div>

                  <div className="bg-white border border-slate-200 p-4 rounded-2xl shadow-xs space-y-3">
                    <div className="flex items-center justify-between">
                      <span className="text-xs font-bold text-slate-800">Distribución GAD-7 (Ansiedad)</span>
                      {datosTablero.graficos.severidad_gad7 && (
                        <button
                          type="button"
                          onClick={() => descargarGraficoPng(datosTablero.graficos.severidad_gad7, 'severidad_gad7')}
                          className="inline-flex items-center gap-1 px-2.5 py-1 text-[11px] font-medium text-slate-600 bg-slate-100 hover:bg-slate-200 rounded-lg transition-colors"
                          title="Descargar imagen en formato PNG"
                          aria-label="Descargar imagen de severidad GAD-7"
                        >
                          <ImageIcon className="w-3.5 h-3.5" aria-hidden="true" />
                          <span>Descargar imagen</span>
                        </button>
                      )}
                    </div>
                    <ImagenAmpliable
                      src={`data:image/png;base64,${datosTablero.graficos.severidad_gad7}`}
                      alt="Gráfico de distribución por severidad de ansiedad en el instrumento GAD-7"
                      tituloModal="Distribución GAD-7 (Ansiedad)"
                    />
                  </div>

                  <div className="bg-white border border-slate-200 p-4 rounded-2xl shadow-xs space-y-3">
                    <div className="flex items-center justify-between">
                      <span className="text-xs font-bold text-slate-800">Evaluaciones por Semana</span>
                      {datosTablero.graficos.evaluaciones_semana && (
                        <button
                          type="button"
                          onClick={() => descargarGraficoPng(datosTablero.graficos.evaluaciones_semana, 'evaluaciones_semana')}
                          className="inline-flex items-center gap-1 px-2.5 py-1 text-[11px] font-medium text-slate-600 bg-slate-100 hover:bg-slate-200 rounded-lg transition-colors"
                          title="Descargar imagen en formato PNG"
                          aria-label="Descargar imagen de evaluaciones por semana"
                        >
                          <ImageIcon className="w-3.5 h-3.5" aria-hidden="true" />
                          <span>Descargar imagen</span>
                        </button>
                      )}
                    </div>
                    <ImagenAmpliable
                      src={`data:image/png;base64,${datosTablero.graficos.evaluaciones_semana}`}
                      alt="Gráfico de evolución temporal de evaluaciones completadas por bloques semanales"
                      tituloModal="Evaluaciones por Semana"
                    />
                  </div>

                  <div className="bg-white border border-slate-200 p-4 rounded-2xl shadow-xs space-y-3">
                    <div className="flex items-center justify-between">
                      <span className="text-xs font-bold text-slate-800">Comorbilidad PHQ-9 vs GAD-7</span>
                      {datosTablero.graficos.phq9_vs_gad7 && (
                        <button
                          type="button"
                          onClick={() => descargarGraficoPng(datosTablero.graficos.phq9_vs_gad7, 'phq9_vs_gad7')}
                          className="inline-flex items-center gap-1 px-2.5 py-1 text-[11px] font-medium text-slate-600 bg-slate-100 hover:bg-slate-200 rounded-lg transition-colors"
                          title="Descargar imagen en formato PNG"
                          aria-label="Descargar imagen de comorbilidad PHQ-9 vs GAD-7"
                        >
                          <ImageIcon className="w-3.5 h-3.5" aria-hidden="true" />
                          <span>Descargar imagen</span>
                        </button>
                      )}
                    </div>
                    <ImagenAmpliable
                      src={`data:image/png;base64,${datosTablero.graficos.phq9_vs_gad7}`}
                      alt="Matriz de comorbilidad y frecuencias cruzadas entre PHQ-9 y GAD-7"
                      tituloModal="Comorbilidad PHQ-9 vs GAD-7"
                    />
                  </div>
                </div>
              )}
            </div>
          )}

          {/* ================= SECCIÓN: ACTIVIDAD DEL SISTEMA ================= */}
          {!cargandoTablero && !errorTablero && datosTablero?.actividad?.indicadores && (
            <div className="space-y-6 pt-6 border-t border-slate-200">
              <div>
                <h3 className="text-base font-bold text-slate-900 flex items-center gap-2">
                  <Activity className="w-5 h-5 text-indigo-600" aria-hidden="true" />
                  Actividad del sistema
                </h3>
                <p className="text-xs text-slate-500 mt-1">
                  Métricas agregadas de flujo de usuarios, consultas y gestión del personal.
                </p>
              </div>

              {/* Tarjetas de actividad - Flujo de atención y usuarios */}
              <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-3">
                <div className="p-4 bg-white border border-slate-200 rounded-2xl shadow-xs">
                  <span className="text-[11px] font-semibold text-slate-500 block">Pacientes Registrados</span>
                  <span className="text-2xl font-extrabold text-slate-900 mt-1 block">
                    {String(datosTablero.actividad.indicadores.total_pacientes_registrados)}
                  </span>
                  <span className="text-[10px] text-slate-400 mt-0.5 block">Cuentas únicas creadas</span>
                </div>

                <div className="p-4 bg-white border border-slate-200 rounded-2xl shadow-xs">
                  <span className="text-[11px] font-semibold text-slate-500 block">Pacientes con Evaluación</span>
                  <span className="text-2xl font-extrabold text-slate-900 mt-1 block">
                    {String(datosTablero.actividad.indicadores.total_pacientes_con_evaluacion)}
                  </span>
                  <span className="text-[10px] text-slate-400 mt-0.5 block">Al menos una completada</span>
                </div>

                <div className="p-4 bg-white border border-slate-200 rounded-2xl shadow-xs">
                  <span className="text-[11px] font-semibold text-slate-500 block">Pacientes con Cita Confirmada</span>
                  <span className="text-2xl font-extrabold text-slate-900 mt-1 block">
                    {String(datosTablero.actividad.indicadores.total_pacientes_con_cita_confirmada)}
                  </span>
                  <span className="text-[10px] text-slate-400 mt-0.5 block">Al menos una confirmada</span>
                </div>

                <div className="p-4 bg-white border border-slate-200 rounded-2xl shadow-xs">
                  <span className="text-[11px] font-semibold text-slate-500 block">Citas Propuestas</span>
                  <span className="text-2xl font-extrabold text-slate-900 mt-1 block">
                    {String(datosTablero.actividad.indicadores.citas_propuestas)}
                  </span>
                  <span className="text-[10px] text-slate-400 mt-0.5 block">Por el profesional clínico</span>
                </div>

                <div className="p-4 bg-white border border-slate-200 rounded-2xl shadow-xs">
                  <span className="text-[11px] font-semibold text-slate-500 block">Citas Confirmadas</span>
                  <span className="text-2xl font-extrabold text-emerald-700 mt-1 block">
                    {String(datosTablero.actividad.indicadores.citas_confirmadas)}
                  </span>
                  <span className="text-[10px] text-slate-400 mt-0.5 block">Por el paciente</span>
                </div>

                <div className="p-4 bg-white border border-slate-200 rounded-2xl shadow-xs">
                  <span className="text-[11px] font-semibold text-slate-500 block">Citas Rechazadas</span>
                  <span className="text-2xl font-extrabold text-amber-700 mt-1 block">
                    {String(datosTablero.actividad.indicadores.citas_rechazadas)}
                  </span>
                  <span className="text-[10px] text-slate-400 mt-0.5 block">Por el paciente</span>
                </div>

                <div className="p-4 bg-white border border-slate-200 rounded-2xl shadow-xs">
                  <span className="text-[11px] font-semibold text-slate-500 block">Citas Cumplidas</span>
                  <span className="text-2xl font-extrabold text-indigo-700 mt-1 block">
                    {String(datosTablero.actividad.indicadores.citas_cumplidas)}
                  </span>
                  <span className="text-[10px] text-slate-400 mt-0.5 block">Atenciones efectuadas</span>
                </div>

                <div className="p-4 bg-white border border-slate-200 rounded-2xl shadow-xs">
                  <span className="text-[11px] font-semibold text-slate-500 block">Citas Canceladas / Pospuestas</span>
                  <span className="text-2xl font-extrabold text-slate-800 mt-1 block">
                    {`${datosTablero.actividad.indicadores.citas_canceladas} / ${datosTablero.actividad.indicadores.citas_pospuestas}`}
                  </span>
                  <span className="text-[10px] text-slate-400 mt-0.5 block">En el periodo</span>
                </div>
              </div>

              {/* Subsección: Gestión del personal y derechos de pacientes */}
              <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                <div className="p-3 bg-slate-50 border border-slate-200 rounded-xl">
                  <span className="text-[10px] font-bold text-slate-500 uppercase tracking-wider block">Altas de Clínicos</span>
                  <span className="text-lg font-bold text-slate-800 mt-0.5 block">
                    {datosTablero.actividad.indicadores.clinicos_altas}
                  </span>
                  <span className="text-[10px] text-slate-400">Personal incorporado</span>
                </div>

                <div className="p-3 bg-slate-50 border border-slate-200 rounded-xl">
                  <span className="text-[10px] font-bold text-slate-500 uppercase tracking-wider block">Activaciones Clínicos</span>
                  <span className="text-lg font-bold text-slate-800 mt-0.5 block">
                    {datosTablero.actividad.indicadores.clinicos_activados}
                  </span>
                  <span className="text-[10px] text-slate-400">Reactivaciones</span>
                </div>

                <div className="p-3 bg-slate-50 border border-slate-200 rounded-xl">
                  <span className="text-[10px] font-bold text-slate-500 uppercase tracking-wider block">Desactivaciones</span>
                  <span className="text-lg font-bold text-slate-800 mt-0.5 block">
                    {datosTablero.actividad.indicadores.clinicos_desactivados}
                  </span>
                  <span className="text-[10px] text-slate-400">Pausas de acceso</span>
                </div>

                <div className="p-3 bg-slate-50 border border-slate-200 rounded-xl">
                  <span className="text-[10px] font-bold text-slate-500 uppercase tracking-wider block">Eliminaciones Clínicos</span>
                  <span className="text-lg font-bold text-slate-800 mt-0.5 block">
                    {datosTablero.actividad.indicadores.clinicos_eliminados}
                  </span>
                  <span className="text-[10px] text-slate-400">Bajas de personal</span>
                </div>
              </div>

              {/* Los 4 gráficos de actividad */}
              {datosTablero.actividad.graficos && (
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div className="bg-white border border-slate-200 p-4 rounded-2xl shadow-xs space-y-3">
                    <div className="flex items-center justify-between">
                      <span className="text-xs font-bold text-slate-800">Evolución de Eventos de Pacientes</span>
                      {datosTablero.actividad.graficos.actividad_semanal_pacientes && (
                        <button
                          type="button"
                          onClick={() => descargarGraficoPng(datosTablero.actividad.graficos.actividad_semanal_pacientes, 'actividad_semanal_pacientes')}
                          className="inline-flex items-center gap-1 px-2.5 py-1 text-[11px] font-medium text-slate-600 bg-slate-100 hover:bg-slate-200 rounded-lg transition-colors"
                          title="Descargar imagen en formato PNG"
                          aria-label="Descargar imagen de actividad semanal de pacientes"
                        >
                          <ImageIcon className="w-3.5 h-3.5" aria-hidden="true" />
                          <span>Descargar imagen</span>
                        </button>
                      )}
                    </div>
                    <ImagenAmpliable
                      src={`data:image/png;base64,${datosTablero.actividad.graficos.actividad_semanal_pacientes}`}
                      alt="Evolución temporal semanal de eventos de pacientes (registros, consentimientos, evaluaciones y citas)"
                      tituloModal="Actividad Semanal de Pacientes"
                    />
                  </div>

                  <div className="bg-white border border-slate-200 p-4 rounded-2xl shadow-xs space-y-3">
                    <div className="flex items-center justify-between">
                      <span className="text-xs font-bold text-slate-800">Distribución por Estado de Citas</span>
                      {datosTablero.actividad.graficos.estado_citas && (
                        <button
                          type="button"
                          onClick={() => descargarGraficoPng(datosTablero.actividad.graficos.estado_citas, 'estado_citas')}
                          className="inline-flex items-center gap-1 px-2.5 py-1 text-[11px] font-medium text-slate-600 bg-slate-100 hover:bg-slate-200 rounded-lg transition-colors"
                          title="Descargar imagen en formato PNG"
                          aria-label="Descargar imagen de estado de citas"
                        >
                          <ImageIcon className="w-3.5 h-3.5" aria-hidden="true" />
                          <span>Descargar imagen</span>
                        </button>
                      )}
                    </div>
                    <ImagenAmpliable
                      src={`data:image/png;base64,${datosTablero.actividad.graficos.estado_citas}`}
                      alt="Distribución de citas según su estado actual en el flujo clínico"
                      tituloModal="Distribución por Estado de Citas"
                    />
                  </div>

                  <div className="bg-white border border-slate-200 p-4 rounded-2xl shadow-xs space-y-3">
                    <div className="flex items-center justify-between">
                      <span className="text-xs font-bold text-slate-800">Gestión de Personal Clínico</span>
                      {datosTablero.actividad.graficos.gestion_clinicos && (
                        <button
                          type="button"
                          onClick={() => descargarGraficoPng(datosTablero.actividad.graficos.gestion_clinicos, 'gestion_clinicos')}
                          className="inline-flex items-center gap-1 px-2.5 py-1 text-[11px] font-medium text-slate-600 bg-slate-100 hover:bg-slate-200 rounded-lg transition-colors"
                          title="Descargar imagen en formato PNG"
                          aria-label="Descargar imagen de gestión de clínicos"
                        >
                          <ImageIcon className="w-3.5 h-3.5" aria-hidden="true" />
                          <span>Descargar imagen</span>
                        </button>
                      )}
                    </div>
                    <ImagenAmpliable
                      src={`data:image/png;base64,${datosTablero.actividad.graficos.gestion_clinicos}`}
                      alt="Métricas de gestión de cuentas del personal clínico (altas, activaciones, desactivaciones y eliminaciones)"
                      tituloModal="Gestión de Personal Clínico"
                    />
                  </div>

                  <div className="bg-white border border-slate-200 p-4 rounded-2xl shadow-xs space-y-3">
                    <div className="flex items-center justify-between">
                      <span className="text-xs font-bold text-slate-800">Pacientes en Flujo de Atención</span>
                      {datosTablero.actividad.graficos.comparativa_pacientes && (
                        <button
                          type="button"
                          onClick={() => descargarGraficoPng(datosTablero.actividad.graficos.comparativa_pacientes, 'comparativa_pacientes')}
                          className="inline-flex items-center gap-1 px-2.5 py-1 text-[11px] font-medium text-slate-600 bg-slate-100 hover:bg-slate-200 rounded-lg transition-colors"
                          title="Descargar imagen en formato PNG"
                          aria-label="Descargar imagen de comparativa de pacientes"
                        >
                          <ImageIcon className="w-3.5 h-3.5" aria-hidden="true" />
                          <span>Descargar imagen</span>
                        </button>
                      )}
                    </div>
                    <ImagenAmpliable
                      src={`data:image/png;base64,${datosTablero.actividad.graficos.comparativa_pacientes}`}
                      alt="Comparativa de pacientes únicos en cada etapa del flujo de atención"
                      tituloModal="Pacientes en Flujo de Atención"
                    />
                  </div>
                </div>
              )}
            </div>
          )}

          {/* Pie informativo de privacidad */}
          {!cargandoTablero && !errorTablero && (datosTablero?.indicadores || datosTablero?.actividad) && (
            <div className="p-3 bg-slate-50 border border-slate-200 rounded-xl text-[11px] text-slate-500 flex items-center gap-2">
              <ShieldCheck className="w-4 h-4 text-indigo-600 shrink-0" aria-hidden="true" />
              <span>
                Protección de confidencialidad clínica activa: estadísticas agregadas con umbral mínimo de grupo N={datosTablero?.minimo_requerido || 5} y supresión de celdas pequeñas para evitar deducción por resta. Las descargas no contienen listas ni datos individuales de personas.
              </span>
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
