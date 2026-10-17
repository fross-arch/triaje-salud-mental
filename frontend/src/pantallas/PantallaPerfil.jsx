import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAutenticacion } from '../hooks/useAutenticacion.js';
import { servicioTriaje } from '../servicios/servicioTriaje.js';
import { servicioConsentimiento } from '../servicios/servicioConsentimiento.js';
import { ModalConfirmacion } from '../componentes/comunes/ModalConfirmacion.jsx';
import { SeccionPerfil } from '../componentes/perfil/SeccionPerfil.jsx';
import {
  User,
  ShieldAlert,
  Trash2,
  PlusCircle,
  FileText,
  Clock,
  CheckCircle,
  AlertTriangle,
  ExternalLink
} from 'lucide-react';

export function PantallaPerfil() {
  const { usuario, eliminarCuenta } = useAutenticacion();
  const navegar = useNavigate();

  const [evaluaciones, setEvaluaciones] = useState([]);
  const [consentimientoVigente, setConsentimientoVigente] = useState(null);
  const [cargando, setCargando] = useState(true);
  const [mensajeEstado, setMensajeEstado] = useState('');
  const [error, setError] = useState('');

  const [modalRevocarAbierto, setModalRevocarAbierto] = useState(false);
  const [modalEliminarAbierto, setModalEliminarAbierto] = useState(false);
  const [modalAlertaEvaluacionEnCurso, setModalAlertaEvaluacionEnCurso] = useState(false);
  const [procesandoAccion, setProcesandoAccion] = useState(false);

  const esAdmin = usuario?.rol === 'ADMINISTRADOR';
  const esClinico = usuario?.rol === 'CLINICO';
  const esPaciente = usuario && (usuario.rol === 'PACIENTE' || usuario.rol === 'USUARIO');

  useEffect(() => {
    let montado = true;

    async function cargarDatosPerfil() {
      // Condicionar estrictamente: SOLO debe ejecutarse si el usuario está autenticado Y su rol es PACIENTE
      if (!usuario || (usuario.rol !== 'PACIENTE' && usuario.rol !== 'USUARIO')) {
        if (montado) {
          setCargando(false);
        }
        return;
      }

      try {
        const [listaEvaluaciones, datosConsentimiento] = await Promise.all([
          servicioTriaje.listarEvaluaciones().catch((err) => {
            if (err?.estado === 403) return [];
            return [];
          }),
          servicioConsentimiento.consultarVigente().catch((err) => {
            if (err?.estado === 403) return { vigente: false };
            return { vigente: false };
          })
        ]);

        if (montado) {
          setEvaluaciones(Array.isArray(listaEvaluaciones) ? listaEvaluaciones : []);
          setConsentimientoVigente(datosConsentimiento?.vigente || false);
        }
      } catch (err) {
        if (montado) {
          setError('No fue posible cargar toda la información del perfil.');
        }
      } finally {
        if (montado) {
          setCargando(false);
        }
      }
    }

    cargarDatosPerfil();

    return () => {
      montado = false;
    };
  }, [usuario]);

  const manejarIniciarRevocacion = () => {
    const tieneEvaluacionEnCurso = evaluaciones.some((e) => e.estado === 'EN_CURSO');
    if (tieneEvaluacionEnCurso) {
      setModalAlertaEvaluacionEnCurso(true);
      return;
    }
    setModalRevocarAbierto(true);
  };

  const manejarRevocarConsentimiento = async () => {
    setProcesandoAccion(true);
    setError('');
    try {
      await servicioConsentimiento.revocar();
      setConsentimientoVigente(false);
      setMensajeEstado('Has revocado tu consentimiento. No podrás iniciar nuevos cribados hasta otorgarlo nuevamente.');
      setModalRevocarAbierto(false);
    } catch (err) {
      const msg = err.message || '';
      if (msg.toLowerCase().includes('en curso')) {
        setModalRevocarAbierto(false);
        setModalAlertaEvaluacionEnCurso(true);
      } else {
        setError(msg || 'Error al revocar el consentimiento.');
      }
    } finally {
      setProcesandoAccion(false);
    }
  };

  const manejarEliminarCuenta = async () => {
    setProcesandoAccion(true);
    setError('');
    try {
      await eliminarCuenta();
      setModalEliminarAbierto(false);
      navegar('/iniciar-sesion');
    } catch (err) {
      setError(err.message || 'Error al eliminar la cuenta y los datos.');
      setProcesandoAccion(false);
    }
  };

  return (
    <main className="max-w-4xl mx-auto px-4 py-8 sm:py-12">
      {/* Encabezado del perfil */}
      <div className="bg-white border border-slate-200 rounded-2xl p-6 sm:p-8 shadow-sm mb-8">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div className="flex items-center gap-4">
            <div className="w-14 h-14 rounded-full bg-teal-100 text-teal-800 flex items-center justify-center font-bold text-xl shrink-0">
              {usuario?.nombreCompleto ? usuario.nombreCompleto.charAt(0).toUpperCase() : <User />}
            </div>
            <div>
              <h1 className="text-xl font-bold text-slate-900">{usuario?.nombreCompleto || 'Usuario'}</h1>
              <p className="text-sm text-slate-500">{usuario?.correo}</p>
              {esPaciente && (
                <div className="mt-1 flex items-center gap-2">
                  <span
                    className={`inline-flex items-center gap-1 text-xs px-2.5 py-0.5 rounded-full font-medium ${
                      consentimientoVigente
                        ? 'bg-emerald-100 text-emerald-800'
                        : 'bg-amber-100 text-amber-800'
                    }`}
                  >
                    {consentimientoVigente ? (
                      <>
                        <CheckCircle className="w-3 h-3" aria-hidden="true" />
                        <span>Consentimiento vigente</span>
                      </>
                    ) : (
                      <>
                        <AlertTriangle className="w-3 h-3" aria-hidden="true" />
                        <span>Consentimiento inactivo</span>
                      </>
                    )}
                  </span>
                </div>
              )}
              {esClinico && (
                <div className="mt-1 flex items-center gap-2">
                  <span className="inline-flex items-center gap-1 text-xs px-2.5 py-0.5 rounded-full font-medium bg-teal-100 text-teal-800">
                    Profesional Clínico
                  </span>
                </div>
              )}
              {esAdmin && (
                <div className="mt-1 flex items-center gap-2">
                  <span className="inline-flex items-center gap-1 text-xs px-2.5 py-0.5 rounded-full font-medium bg-indigo-100 text-indigo-800">
                    Administrador del Sistema
                  </span>
                </div>
              )}
            </div>
          </div>

          {esPaciente && (
            <div>
              {consentimientoVigente ? (
                <Link
                  to="/introduccion"
                  className="px-4 py-2 bg-teal-600 hover:bg-teal-700 text-white rounded-xl text-sm font-semibold flex items-center gap-1.5 shadow-sm transition-colors"
                >
                  <PlusCircle className="w-4 h-4" aria-hidden="true" />
                  <span>Nueva evaluación</span>
                </Link>
              ) : (
                <Link
                  to="/consentimiento"
                  className="px-4 py-2 bg-amber-600 hover:bg-amber-700 text-white rounded-xl text-sm font-semibold flex items-center gap-1.5 shadow-sm transition-colors"
                >
                  <ShieldAlert className="w-4 h-4" aria-hidden="true" />
                  <span>Otorgar consentimiento</span>
                </Link>
              )}
            </div>
          )}
        </div>

        {mensajeEstado && (
          <div role="status" className="mt-4 p-3 bg-teal-50 border border-teal-200 text-teal-900 text-xs rounded-xl">
            {mensajeEstado}
          </div>
        )}

        {error && (
          <div role="alert" className="mt-4 p-3 bg-rose-50 border border-rose-200 text-rose-800 text-xs rounded-xl">
            {error}
          </div>
        )}
      </div>

      {/* Sección editable de perfil (nombre, correo, especialidad y contraseña) */}
      <div className="mb-8">
        <SeccionPerfil />
      </div>

      {/* Historial de evaluaciones exclusivo para PACIENTE */}
      {esPaciente && (
        <section className="bg-white border border-slate-200 rounded-2xl p-6 sm:p-8 shadow-sm mb-8">
          <h2 className="text-lg font-bold text-slate-900 mb-4 flex items-center gap-2">
            <Clock className="w-5 h-5 text-teal-600" aria-hidden="true" />
            <span>Historial de cribados realizados</span>
          </h2>

          {cargando ? (
            <p className="text-xs text-slate-500 py-4">Cargando historial...</p>
          ) : evaluaciones.length === 0 ? (
            <div className="text-center py-8 text-slate-500 text-sm">
              <p>Aún no has completado ninguna evaluación de cribado.</p>
              {consentimientoVigente && (
                <Link to="/introduccion" className="text-teal-700 font-semibold text-xs hover:underline mt-2 inline-block">
                  Iniciar mi primera evaluación
                </Link>
              )}
            </div>
          ) : (
            <div className="divide-y divide-slate-100">
              {evaluaciones.map((item) => (
                <article key={item.id} className="py-4 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                  <div>
                    <div className="flex items-center gap-2">
                      <span className="font-semibold text-slate-800 text-sm">
                        Evaluación del {new Date(item.fechaCreacion).toLocaleDateString('es-CO')}
                      </span>
                      <span className="text-[11px] px-2 py-0.5 rounded-full font-medium bg-slate-100 text-slate-700">
                        {item.estado}
                      </span>
                    </div>
                    <p className="text-xs text-slate-500 mt-1">
                      PHQ-9: {item.puntajePhq9 ?? '-'} puntos ({item.severidadDepresion || 'N/A'}) · GAD-7: {item.puntajeGad7 ?? '-'} puntos ({item.severidadAnsiedad || 'N/A'})
                    </p>
                  </div>

                  {item.estado === 'COMPLETADA' ? (
                    <Link
                      to={`/resultados/${item.id}`}
                      className="text-xs font-semibold text-teal-700 hover:text-teal-900 flex items-center gap-1"
                    >
                      <span>Ver resultados</span>
                      <ExternalLink className="w-3.5 h-3.5" aria-hidden="true" />
                    </Link>
                  ) : (
                    <Link
                      to={`/asistente/${item.id}`}
                      className="text-xs font-semibold text-amber-700 hover:text-amber-900 flex items-center gap-1"
                    >
                      <span>Continuar respondiendo</span>
                    </Link>
                  )}
                </article>
              ))}
            </div>
          )}
        </section>
      )}

      {/* Ejercicio de derechos del usuario (Ley 1581 de 2012) - Exclusivo para PACIENTE */}
      {esPaciente && (
        <section className="bg-white border border-rose-200 rounded-2xl p-6 sm:p-8 shadow-sm">
          <h2 className="text-lg font-bold text-slate-900 mb-2">
            Gestión de privacidad y ejercicio de derechos
          </h2>
          <p className="text-xs text-slate-600 mb-6 leading-relaxed">
            Conforme al régimen de protección de datos personales (Ley 1581 de 2012 de Colombia), puedes revocar en cualquier momento tu autorización de tratamiento de datos o solicitar la supresión total de tu cuenta e historial.
          </p>

          <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-3">
            {consentimientoVigente && (
              <button
                type="button"
                onClick={manejarIniciarRevocacion}
                className="px-4 py-2.5 border border-amber-300 text-amber-800 hover:bg-amber-50 rounded-xl text-xs font-semibold transition-colors flex items-center justify-center gap-2"
              >
                <ShieldAlert className="w-4 h-4 text-amber-600" aria-hidden="true" />
                <span>Revocar consentimiento</span>
              </button>
            )}

            <button
              type="button"
              onClick={() => setModalEliminarAbierto(true)}
              className="px-4 py-2.5 border border-rose-300 text-rose-800 hover:bg-rose-50 rounded-xl text-xs font-semibold transition-colors flex items-center justify-center gap-2"
            >
              <Trash2 className="w-4 h-4 text-rose-600" aria-hidden="true" />
              <span>Eliminar mi cuenta y datos</span>
            </button>
          </div>
        </section>
      )}

      {/* Alerta modal no destructiva cuando hay evaluacion en curso */}
      <ModalConfirmacion
        abierto={modalAlertaEvaluacionEnCurso}
        titulo="Evaluación en curso"
        mensaje="Hay una evaluación en curso; debes finalizarla o cancelarla antes de gestionar tus consentimientos."
        textoConfirmar="Entendido"
        textoCancelar={null}
        esPeligroso={false}
        alConfirmar={() => setModalAlertaEvaluacionEnCurso(false)}
        alCancelar={() => setModalAlertaEvaluacionEnCurso(false)}
      />

      {/* Modales de confirmacion */}
      <ModalConfirmacion
        abierto={modalRevocarAbierto}
        titulo="¿Revocar el consentimiento informado?"
        mensaje="Al revocar el consentimiento, se inhabilitará la realización de nuevos cribados hasta que decidas otorgarlo de nuevo. Las evaluaciones previas continuarán disponibles en tu perfil salvo que elimines la cuenta."
        textoConfirmar="Revocar autorización"
        textoCancelar="Mantener vigente"
        alConfirmar={manejarRevocarConsentimiento}
        alCancelar={() => setModalRevocarAbierto(false)}
      />

      <ModalConfirmacion
        abierto={modalEliminarAbierto}
        titulo="¿Eliminar tu cuenta y todos tus datos?"
        mensaje="Esta acción es irreversible conforme a la Ley 1581 de 2012. Se borrarán permanentemente tu cuenta de usuario, tus consentimientos y todas las respuestas e informes de triaje almacenados en el sistema."
        textoConfirmar="Eliminar definitivamente"
        textoCancelar="Cancelar"
        esPeligroso={true}
        alConfirmar={manejarEliminarCuenta}
        alCancelar={() => setModalEliminarAbierto(false)}
      />
    </main>
  );
}
