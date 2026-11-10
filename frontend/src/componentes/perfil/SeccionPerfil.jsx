import React, { useState, useEffect } from 'react';
import { User, Mail, Briefcase, Lock, CheckCircle, AlertCircle, ChevronDown, ChevronUp } from 'lucide-react';
import { useAutenticacion } from '../../hooks/useAutenticacion.js';
import { servicioAutenticacion } from '../../servicios/servicioAutenticacion.js';
import { ESPECIALIDADES_PREDEFINIDAS } from '../administracion/ModalEditarClinico.jsx';

export function SeccionPerfil({ usuario: usuarioProp, onPerfilActualizado }) {
  const { usuario: usuarioContexto, actualizarUsuarioEnEstado, cargarPerfilUsuario } = useAutenticacion();
  const usuarioActual = usuarioProp || usuarioContexto;

  const [nombreCompleto, setNombreCompleto] = useState('');
  const [correo, setCorreo] = useState('');
  const [especialidadSeleccionada, setEspecialidadSeleccionada] = useState(ESPECIALIDADES_PREDEFINIDAS[0]);
  const [especialidadOtra, setEspecialidadOtra] = useState('');

  // Acordeón / Sección de cambio de contraseña
  const [mostrarCambioClave, setMostrarCambioClave] = useState(false);
  const [contrasenaActual, setContrasenaActual] = useState('');
  const [nuevaContrasena, setNuevaContrasena] = useState('');
  const [confirmarNuevaContrasena, setConfirmarNuevaContrasena] = useState('');

  // Estados de proceso y feedback
  const [guardando, setGuardando] = useState(false);
  const [mensajeExito, setMensajeExito] = useState('');
  const [error, setError] = useState('');

  const esClinico = usuarioActual?.rol === 'CLINICO';

  // Precargar datos desde el usuario actual o consultando GET /api/usuarios/yo
  useEffect(() => {
    let montado = true;

    async function cargarDatos() {
      try {
        const perfil = await servicioAutenticacion.obtenerPerfil();
        if (montado && perfil) {
          setNombreCompleto(perfil.nombreCompleto || '');
          setCorreo(perfil.correo || '');

          const esp = perfil.especialidad ? perfil.especialidad.trim() : '';
          if (esp && ESPECIALIDADES_PREDEFINIDAS.includes(esp)) {
            setEspecialidadSeleccionada(esp);
            setEspecialidadOtra('');
          } else if (esp) {
            setEspecialidadSeleccionada('OTRA');
            setEspecialidadOtra(esp);
          } else {
            setEspecialidadSeleccionada(ESPECIALIDADES_PREDEFINIDAS[0]);
            setEspecialidadOtra('');
          }
        }
      } catch {
        // Si falla la consulta directa, precargar desde el prop o contexto
        if (montado && usuarioActual) {
          setNombreCompleto(usuarioActual.nombreCompleto || '');
          setCorreo(usuarioActual.correo || '');
          const esp = usuarioActual.especialidad ? usuarioActual.especialidad.trim() : '';
          if (esp && ESPECIALIDADES_PREDEFINIDAS.includes(esp)) {
            setEspecialidadSeleccionada(esp);
            setEspecialidadOtra('');
          } else if (esp) {
            setEspecialidadSeleccionada('OTRA');
            setEspecialidadOtra(esp);
          }
        }
      }
    }

    cargarDatos();

    return () => {
      montado = false;
    };
  }, [usuarioActual]);

  const resolverEspecialidadEfectiva = () => {
    if (especialidadSeleccionada === 'OTRA') {
      return especialidadOtra.trim() || 'Psicología General';
    }
    return especialidadSeleccionada;
  };

  const manejarEnvio = async (e) => {
    e.preventDefault();
    setError('');
    setMensajeExito('');

    if (!nombreCompleto.trim()) {
      setError('El nombre completo es obligatorio.');
      return;
    }

    if (!correo.trim()) {
      setError('El correo electrónico es obligatorio.');
      return;
    }

    // Validaciones si se desplegó el acordeón y se desea cambiar la contraseña
    if (mostrarCambioClave) {
      if (!contrasenaActual) {
        setError('Debes ingresar tu contraseña actual para realizar el cambio.');
        return;
      }
      if (!nuevaContrasena) {
        setError('Debes ingresar la nueva contraseña.');
        return;
      }
      if (nuevaContrasena.length < 10) {
        setError('La contraseña debe tener al menos 10 caracteres.');
        return;
      }
      if (nuevaContrasena !== confirmarNuevaContrasena) {
        setError('Las contraseñas no coinciden.');
        return;
      }
    }

    setGuardando(true);

    try {
      const cuerpo = {
        nombreCompleto: nombreCompleto.trim(),
        correo: correo.trim().toLowerCase()
      };

      if (esClinico) {
        cuerpo.especialidad = resolverEspecialidadEfectiva();
      }

      if (mostrarCambioClave && nuevaContrasena) {
        cuerpo.contrasenaActual = contrasenaActual;
        cuerpo.nuevaContrasena = nuevaContrasena;
      }

      const respuesta = await servicioAutenticacion.actualizarPerfil(cuerpo);

      setMensajeExito('Datos actualizados correctamente');
      setContrasenaActual('');
      setNuevaContrasena('');
      setConfirmarNuevaContrasena('');
      setMostrarCambioClave(false);

      if (actualizarUsuarioEnEstado && respuesta) {
        actualizarUsuarioEnEstado(respuesta);
      }
      if (cargarPerfilUsuario) {
        await cargarPerfilUsuario();
      }

      if (onPerfilActualizado) {
        onPerfilActualizado(respuesta);
      }
    } catch (err) {
      const estado = err.estado || err.status;
      if (estado === 409) {
        setError('El correo electrónico ya se encuentra registrado por otro usuario.');
      } else {
        setError(err.mensaje || err.message || 'Error al actualizar el perfil.');
      }
    } finally {
      setGuardando(false);
    }
  };

  return (
    <section className="bg-white border border-slate-200 rounded-2xl p-6 sm:p-8 shadow-sm">
      <div className="flex items-center gap-3 mb-6 pb-4 border-b border-slate-100">
        <div className="w-10 h-10 rounded-xl bg-teal-50 text-teal-700 flex items-center justify-center font-bold">
          <User className="w-5 h-5" aria-hidden="true" />
        </div>
        <div>
          <h2 className="text-lg font-bold text-slate-900">Datos Personales y de Cuenta</h2>
          <p className="text-xs text-slate-500">
            Consulta y actualiza tu información de perfil y tus credenciales de acceso.
          </p>
        </div>
      </div>

      {mensajeExito && (
        <div role="status" className="mb-6 p-4 bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs rounded-xl flex items-center gap-2">
          <CheckCircle className="w-4 h-4 text-emerald-600 shrink-0" aria-hidden="true" />
          <span>{mensajeExito}</span>
        </div>
      )}

      {error && (
        <div role="alert" className="mb-6 p-4 bg-rose-50 border border-rose-200 text-rose-800 text-xs rounded-xl flex items-center gap-2">
          <AlertCircle className="w-4 h-4 text-rose-600 shrink-0" aria-hidden="true" />
          <span>{error}</span>
        </div>
      )}

      <form onSubmit={manejarEnvio} className="space-y-6">
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <label htmlFor="perfil-nombre" className="block text-xs font-semibold text-slate-700 mb-1">
              Nombre completo <span className="text-rose-500">*</span>
            </label>
            <div className="relative">
              <span className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-400">
                <User className="w-4 h-4" aria-hidden="true" />
              </span>
              <input
                id="perfil-nombre"
                name="nombreCompleto"
                type="text"
                autoComplete="name"
                required
                maxLength={120}
                value={nombreCompleto}
                onChange={(e) => setNombreCompleto(e.target.value)}
                className="w-full pl-9 pr-3 py-2 border border-slate-300 rounded-xl text-xs text-slate-800 focus:outline-none focus:ring-2 focus:ring-teal-500 focus:border-teal-500"
                placeholder="Tu nombre completo"
              />
            </div>
          </div>

          <div>
            <label htmlFor="perfil-correo" className="block text-xs font-semibold text-slate-700 mb-1">
              Correo electrónico <span className="text-rose-500">*</span>
            </label>
            <div className="relative">
              <span className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-400">
                <Mail className="w-4 h-4" aria-hidden="true" />
              </span>
              <input
                id="perfil-correo"
                name="correo"
                type="email"
                autoComplete="username"
                required
                maxLength={120}
                value={correo}
                onChange={(e) => setCorreo(e.target.value)}
                className="w-full pl-9 pr-3 py-2 border border-slate-300 rounded-xl text-xs text-slate-800 focus:outline-none focus:ring-2 focus:ring-teal-500 focus:border-teal-500"
                placeholder="correo@ejemplo.com"
              />
            </div>
          </div>
        </div>

        {esClinico && (
          <div className="p-4 bg-teal-50/50 border border-teal-100 rounded-xl space-y-3">
            <label htmlFor="perfil-especialidad" className="block text-xs font-semibold text-teal-900">
              Especialidad Clínica
            </label>
            <div className="relative">
              <span className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-teal-600">
                <Briefcase className="w-4 h-4" aria-hidden="true" />
              </span>
              <select
                id="perfil-especialidad"
                value={especialidadSeleccionada}
                onChange={(e) => setEspecialidadSeleccionada(e.target.value)}
                className="w-full pl-9 pr-3 py-2 border border-teal-200 bg-white rounded-xl text-xs text-slate-800 focus:outline-none focus:ring-2 focus:ring-teal-500 focus:border-teal-500"
              >
                {ESPECIALIDADES_PREDEFINIDAS.map((esp) => (
                  <option key={esp} value={esp}>
                    {esp}
                  </option>
                ))}
                <option value="OTRA">Otra (especificar manualmente)</option>
              </select>
            </div>

            {especialidadSeleccionada === 'OTRA' && (
              <div>
                <label htmlFor="perfil-especialidad-otra" className="block text-[11px] font-medium text-slate-600 mb-1">
                  Especifica tu especialidad
                </label>
                <input
                  id="perfil-especialidad-otra"
                  type="text"
                  maxLength={100}
                  value={especialidadOtra}
                  onChange={(e) => setEspecialidadOtra(e.target.value)}
                  placeholder="Ej: Psicología Forense, Terapia Familiar..."
                  className="w-full px-3 py-2 border border-slate-300 rounded-xl text-xs text-slate-800 focus:outline-none focus:ring-2 focus:ring-teal-500 focus:border-teal-500"
                />
              </div>
            )}
          </div>
        )}

        {/* Acordeón para Cambiar Contraseña */}
        <div className="border border-slate-200 rounded-xl overflow-hidden">
          <button
            type="button"
            onClick={() => {
              setMostrarCambioClave(!mostrarCambioClave);
              setError('');
            }}
            className="w-full px-4 py-3 bg-slate-50 hover:bg-slate-100/80 flex items-center justify-between text-left transition-colors"
            aria-expanded={mostrarCambioClave}
          >
            <div className="flex items-center gap-2">
              <Lock className="w-4 h-4 text-slate-600" aria-hidden="true" />
              <span className="text-xs font-semibold text-slate-800">
                {mostrarCambioClave ? 'Modificando contraseña de acceso' : '¿Deseas cambiar tu contraseña?'}
              </span>
            </div>
            {mostrarCambioClave ? (
              <ChevronUp className="w-4 h-4 text-slate-500" aria-hidden="true" />
            ) : (
              <ChevronDown className="w-4 h-4 text-slate-500" aria-hidden="true" />
            )}
          </button>

          {mostrarCambioClave && (
            <div className="p-4 sm:p-5 bg-white space-y-4 border-t border-slate-200">
              <p className="text-[11px] text-slate-500 leading-relaxed">
                Para actualizar tu contraseña, debes confirmar tu contraseña actual y digitar la nueva contraseña con al menos 10 caracteres.
              </p>

              <div>
                <label htmlFor="perfil-clave-actual" className="block text-xs font-semibold text-slate-700 mb-1">
                  Contraseña actual <span className="text-rose-500">*</span>
                </label>
                <input
                  id="perfil-clave-actual"
                  type="password"
                  autoComplete="current-password"
                  value={contrasenaActual}
                  onChange={(e) => setContrasenaActual(e.target.value)}
                  className="w-full px-3 py-2 border border-slate-300 rounded-xl text-xs text-slate-800 focus:outline-none focus:ring-2 focus:ring-teal-500 focus:border-teal-500"
                  placeholder="Tu contraseña actual"
                />
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label htmlFor="perfil-clave-nueva" className="block text-xs font-semibold text-slate-700 mb-1">
                    Nueva contraseña <span className="text-rose-500">*</span>
                  </label>
                  <input
                    id="perfil-clave-nueva"
                    type="password"
                    autoComplete="new-password"
                    value={nuevaContrasena}
                    onChange={(e) => setNuevaContrasena(e.target.value)}
                    className="w-full px-3 py-2 border border-slate-300 rounded-xl text-xs text-slate-800 focus:outline-none focus:ring-2 focus:ring-teal-500 focus:border-teal-500"
                    placeholder="Al menos 10 caracteres"
                  />
                  <p className="text-[11px] text-slate-500 mt-1">Mínimo 10 caracteres</p>
                </div>

                <div>
                  <label htmlFor="perfil-clave-confirmar" className="block text-xs font-semibold text-slate-700 mb-1">
                    Confirmar nueva contraseña <span className="text-rose-500">*</span>
                  </label>
                  <input
                    id="perfil-clave-confirmar"
                    type="password"
                    autoComplete="new-password"
                    value={confirmarNuevaContrasena}
                    onChange={(e) => setConfirmarNuevaContrasena(e.target.value)}
                    className="w-full px-3 py-2 border border-slate-300 rounded-xl text-xs text-slate-800 focus:outline-none focus:ring-2 focus:ring-teal-500 focus:border-teal-500"
                    placeholder="Repite la nueva contraseña"
                  />
                </div>
              </div>
            </div>
          )}
        </div>

        <div className="flex justify-end pt-2">
          <button
            type="submit"
            disabled={guardando}
            className="px-5 py-2.5 bg-teal-600 hover:bg-teal-700 disabled:opacity-50 text-white rounded-xl text-xs font-semibold shadow-xs transition-colors flex items-center gap-2"
          >
            {guardando ? (
              <>
                <div className="w-3.5 h-3.5 border-2 border-white border-t-transparent rounded-full animate-spin" aria-hidden="true" />
                <span>Guardando cambios...</span>
              </>
            ) : (
              <span>Guardar cambios</span>
            )}
          </button>
        </div>
      </form>
    </section>
  );
}
