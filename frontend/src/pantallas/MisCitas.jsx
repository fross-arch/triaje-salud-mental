import React, { useState, useEffect } from 'react';
import { Calendar, Clock, Video, MapPin, CheckCircle, XCircle, AlertCircle, RefreshCw, User, History } from 'lucide-react';
import { servicioCitas } from '../servicios/servicioCitas.js';

export function MisCitas() {
  const [citas, setCitas] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [accionEnCurso, setAccionEnCurso] = useState(null); // id de la cita en proceso
  const [pestanaActiva, setPestanaActiva] = useState('activas'); // 'activas' | 'historial'
  const [mensajeEstado, setMensajeEstado] = useState('');
  const [error, setError] = useState('');

  const cargarCitas = async () => {
    setCargando(true);
    setError('');
    try {
      const respuesta = await servicioCitas.listarCitasPaciente();
      setCitas(Array.isArray(respuesta) ? respuesta : []);
    } catch (err) {
      setError(err.message || 'No fue posible cargar tus citas.');
    } finally {
      setCargando(false);
    }
  };

  useEffect(() => {
    cargarCitas();
  }, []);

  const manejarConfirmar = async (id) => {
    setAccionEnCurso(id);
    setError('');
    setMensajeEstado('');
    try {
      const citaActualizada = await servicioCitas.confirmarCita(id);
      setCitas((prev) =>
        prev.map((c) => (c.id === id ? { ...c, ...citaActualizada, estado: 'CONFIRMADA' } : c))
      );
      setMensajeEstado('Cita confirmada exitosamente.');
    } catch (err) {
      setError(err.message || 'Error al confirmar la cita.');
    } finally {
      setAccionEnCurso(null);
    }
  };

  const manejarRechazar = async (id) => {
    setAccionEnCurso(id);
    setError('');
    setMensajeEstado('');
    try {
      const citaActualizada = await servicioCitas.rechazarCita(id);
      setCitas((prev) =>
        prev.map((c) => (c.id === id ? { ...c, ...citaActualizada, estado: 'RECHAZADA' } : c))
      );
      setMensajeEstado('Cita rechazada.');
    } catch (err) {
      setError(err.message || 'Error al rechazar la cita.');
    } finally {
      setAccionEnCurso(null);
    }
  };

  const manejarCancelar = async (id) => {
    setAccionEnCurso(id);
    setError('');
    setMensajeEstado('');
    try {
      const citaActualizada = await servicioCitas.cancelarCitaPaciente(id);
      setCitas((prev) =>
        prev.map((c) => (c.id === id ? { ...c, ...citaActualizada, estado: 'CANCELADA' } : c))
      );
      setMensajeEstado('Cita cancelada.');
    } catch (err) {
      setError(err.message || 'Error al cancelar la cita.');
    } finally {
      setAccionEnCurso(null);
    }
  };

  const citasActivas = citas.filter((c) => c.estado === 'PROPUESTA' || c.estado === 'CONFIRMADA');
  const citasPasadas = citas.filter((c) => c.estado === 'CUMPLIDA' || c.estado === 'CANCELADA' || c.estado === 'RECHAZADA');

  const obtenerEstiloEstado = (estado) => {
    switch (estado) {
      case 'CONFIRMADA':
        return 'bg-emerald-100 text-emerald-800 border-emerald-200';
      case 'PROPUESTA':
        return 'bg-amber-100 text-amber-800 border-amber-200';
      case 'CUMPLIDA':
        return 'bg-teal-100 text-teal-800 border-teal-200';
      case 'CANCELADA':
        return 'bg-rose-100 text-rose-800 border-rose-200';
      case 'RECHAZADA':
        return 'bg-slate-100 text-slate-600 border-slate-200';
      default:
        return 'bg-slate-100 text-slate-700 border-slate-200';
    }
  };

  return (
    <main className="max-w-4xl mx-auto px-4 py-8 sm:py-12">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 flex items-center gap-2">
            <Calendar className="w-6 h-6 text-teal-600" aria-hidden="true" />
            <span>Mis citas de orientación</span>
          </h1>
          <p className="text-xs text-slate-500 mt-1">
            Espacio de seguimiento a citas propuestas por profesionales con los que compartiste evaluaciones.
          </p>
        </div>

        <button
          type="button"
          onClick={cargarCitas}
          className="px-3 py-1.5 border border-slate-300 hover:bg-slate-50 rounded-xl text-xs font-semibold text-slate-700 flex items-center gap-1.5 transition-colors self-start sm:self-center shadow-xs"
          aria-label="Actualizar lista de citas"
        >
          <RefreshCw className="w-3.5 h-3.5" aria-hidden="true" />
          <span>Actualizar</span>
        </button>
      </div>

      {/* Pestañas: Citas Activas vs Historial de Citas Pasadas */}
      <div className="flex gap-2 border-b border-slate-200 mb-6">
        <button
          type="button"
          onClick={() => setPestanaActiva('activas')}
          className={`pb-3 px-4 text-xs font-bold border-b-2 transition-colors flex items-center gap-2 ${
            pestanaActiva === 'activas'
              ? 'border-teal-600 text-teal-700'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          <Calendar className="w-4 h-4" aria-hidden="true" />
          <span>Citas Activas ({citasActivas.length})</span>
        </button>

        <button
          type="button"
          onClick={() => setPestanaActiva('historial')}
          className={`pb-3 px-4 text-xs font-bold border-b-2 transition-colors flex items-center gap-2 ${
            pestanaActiva === 'historial'
              ? 'border-teal-600 text-teal-700'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          <History className="w-4 h-4" aria-hidden="true" />
          <span>Historial de Citas Pasadas ({citasPasadas.length})</span>
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

      {cargando ? (
        <div className="bg-white border border-slate-200 rounded-2xl p-12 text-center" role="status" aria-live="polite">
          <div className="w-8 h-8 border-4 border-teal-600 border-t-transparent rounded-full animate-spin mx-auto mb-3" aria-hidden="true" />
          <p className="text-xs text-slate-500">Cargando tus citas...</p>
        </div>
      ) : pestanaActiva === 'activas' ? (
        /* ================= PESTAÑA: CITAS ACTIVAS ================= */
        citasActivas.length === 0 ? (
          <div className="bg-white border border-slate-200 rounded-2xl p-10 text-center shadow-sm">
            <Calendar className="w-10 h-10 text-slate-400 mx-auto mb-3" aria-hidden="true" />
            <h2 className="text-base font-bold text-slate-800">No tienes citas activas</h2>
            <p className="text-xs text-slate-500 mt-1 max-w-sm mx-auto">
              Cuando un profesional revise tu evaluación compartida y te proponga una fecha de orientación, aparecerá aquí.
            </p>
          </div>
        ) : (
          <div className="space-y-4">
            <div className="p-3.5 bg-teal-50 border border-teal-200 text-teal-900 text-xs rounded-xl">
              <strong>Nota sobre tus evaluaciones:</strong> Mientras mantengas una cita activa (propuesta o confirmada), no será posible iniciar un nuevo cribado hasta que la atención concluya o se cancele.
            </div>

            {citasActivas.map((cita) => {
              const fechaCita = new Date(cita.fechaHora);
              const esPropuesta = cita.estado === 'PROPUESTA';
              const esConfirmada = cita.estado === 'CONFIRMADA';
              const procesando = accionEnCurso === cita.id;

              return (
                <article
                  key={cita.id}
                  className="bg-white border border-slate-200 rounded-2xl p-5 sm:p-6 shadow-sm hover:shadow-md transition-shadow"
                >
                  <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-4">
                    <div className="space-y-2">
                      <div className="flex items-center gap-2">
                        <span className={`text-[11px] font-bold px-2.5 py-0.5 rounded-full border ${obtenerEstiloEstado(cita.estado)}`}>
                          {cita.estado}
                        </span>
                        <span className="text-xs text-slate-500 flex items-center gap-1">
                          {cita.modalidad === 'VIRTUAL' ? (
                            <>
                              <Video className="w-3.5 h-3.5 text-teal-600" aria-hidden="true" />
                              <span>Virtual</span>
                            </>
                          ) : (
                            <>
                              <MapPin className="w-3.5 h-3.5 text-amber-600" aria-hidden="true" />
                              <span>Presencial</span>
                            </>
                          )}
                        </span>
                      </div>

                      <h2 className="text-base font-bold text-slate-900 flex items-center gap-1.5">
                        <User className="w-4 h-4 text-teal-600" aria-hidden="true" />
                        <span>{cita.nombreClinico || 'Profesional de la salud'}</span>
                      </h2>

                      <div className="flex items-center gap-2 text-xs text-slate-600">
                        <Clock className="w-3.5 h-3.5 text-slate-500" aria-hidden="true" />
                        <span>
                          {fechaCita.toLocaleDateString('es-CO', {
                            weekday: 'long',
                            year: 'numeric',
                            month: 'long',
                            day: 'numeric'
                          })}{' '}
                          a las{' '}
                          {fechaCita.toLocaleTimeString('es-CO', {
                            hour: '2-digit',
                            minute: '2-digit'
                          })}
                        </span>
                      </div>

                      {cita.mensaje && (
                        <p className="text-xs text-slate-700 bg-slate-50 p-3 rounded-xl border border-slate-200 mt-2 italic">
                          &ldquo;{cita.mensaje}&rdquo;
                        </p>
                      )}
                    </div>

                    {/* Botones de accion segun estado */}
                    <div className="flex sm:flex-col gap-2 shrink-0 self-end sm:self-center">
                      {esPropuesta && (
                        <>
                          <button
                            type="button"
                            onClick={() => manejarConfirmar(cita.id)}
                            disabled={procesando}
                            className="px-3.5 py-2 bg-teal-600 hover:bg-teal-700 disabled:bg-slate-300 text-white rounded-xl text-xs font-semibold shadow-xs transition-colors flex items-center gap-1.5"
                            aria-label={`Confirmar cita con ${cita.nombreClinico || 'profesional'}`}
                          >
                            <CheckCircle className="w-3.5 h-3.5" aria-hidden="true" />
                            <span>Confirmar</span>
                          </button>
                          <button
                            type="button"
                            onClick={() => manejarRechazar(cita.id)}
                            disabled={procesando}
                            className="px-3.5 py-2 border border-slate-300 hover:bg-slate-50 text-slate-700 rounded-xl text-xs font-semibold transition-colors flex items-center gap-1.5"
                            aria-label={`Rechazar cita con ${cita.nombreClinico || 'profesional'}`}
                          >
                            <XCircle className="w-3.5 h-3.5" aria-hidden="true" />
                            <span>Rechazar</span>
                          </button>
                        </>
                      )}

                      {esConfirmada && (
                        <button
                          type="button"
                          onClick={() => manejarCancelar(cita.id)}
                          disabled={procesando}
                          className="px-3.5 py-2 border border-rose-300 hover:bg-rose-50 text-rose-700 rounded-xl text-xs font-semibold transition-colors flex items-center gap-1.5"
                          aria-label={`Cancelar cita con ${cita.nombreClinico || 'profesional'}`}
                        >
                          <XCircle className="w-3.5 h-3.5" aria-hidden="true" />
                          <span>Cancelar</span>
                        </button>
                      )}
                    </div>
                  </div>
                </article>
              );
            })}
          </div>
        )
      ) : (
        /* ================= PESTAÑA: HISTORIAL DE CITAS PASADAS ================= */
        citasPasadas.length === 0 ? (
          <div className="bg-white border border-slate-200 rounded-2xl p-10 text-center shadow-sm">
            <History className="w-10 h-10 text-slate-400 mx-auto mb-3" aria-hidden="true" />
            <h2 className="text-base font-bold text-slate-800">No hay citas pasadas en el historial</h2>
            <p className="text-xs text-slate-500 mt-1 max-w-sm mx-auto">
              Tus citas cumplidas o canceladas se conservarán aquí junto con sus observaciones registradas.
            </p>
          </div>
        ) : (
          <div className="space-y-4">
            {citasPasadas.map((cita) => {
              const fechaCita = new Date(cita.fechaHora);
              const esCumplida = cita.estado === 'CUMPLIDA';

              return (
                <article
                  key={cita.id}
                  className="bg-white border border-slate-200 rounded-2xl p-5 sm:p-6 shadow-sm opacity-95"
                >
                  <div className="space-y-2">
                    <div className="flex items-center gap-2">
                      <span className={`text-[11px] font-bold px-2.5 py-0.5 rounded-full border ${obtenerEstiloEstado(cita.estado)}`}>
                        {cita.estado}
                      </span>
                      <span className="text-xs text-slate-500 flex items-center gap-1">
                        {cita.modalidad === 'VIRTUAL' ? (
                          <>
                            <Video className="w-3.5 h-3.5 text-teal-600" aria-hidden="true" />
                            <span>Virtual</span>
                          </>
                        ) : (
                          <>
                            <MapPin className="w-3.5 h-3.5 text-amber-600" aria-hidden="true" />
                            <span>Presencial</span>
                          </>
                        )}
                      </span>
                      <span className="text-[11px] text-slate-400 ml-auto font-medium">
                        Cita finalizada
                      </span>
                    </div>

                    <h2 className="text-base font-bold text-slate-900 flex items-center gap-1.5">
                      <User className="w-4 h-4 text-teal-600" aria-hidden="true" />
                      <span>{cita.nombreClinico || 'Profesional de la salud'}</span>
                    </h2>

                    <div className="flex items-center gap-2 text-xs text-slate-600">
                      <Clock className="w-3.5 h-3.5 text-slate-500" aria-hidden="true" />
                      <span>
                        {fechaCita.toLocaleDateString('es-CO', {
                          weekday: 'long',
                          year: 'numeric',
                          month: 'long',
                          day: 'numeric'
                        })}{' '}
                        a las{' '}
                        {fechaCita.toLocaleTimeString('es-CO', {
                          hour: '2-digit',
                          minute: '2-digit'
                        })}
                      </span>
                    </div>

                    {/* Observaciones clínicas o motivo de cancelación */}
                    {cita.observaciones ? (
                      <div className={`p-3.5 rounded-xl border text-xs mt-2 ${
                        esCumplida
                          ? 'bg-teal-50/60 border-teal-200 text-teal-950'
                          : 'bg-rose-50/60 border-rose-200 text-rose-950'
                      }`}>
                        <strong className="block font-bold mb-1">
                          {esCumplida ? 'Observaciones de la sesión de orientación:' : 'Motivo de cancelación registrado:'}
                        </strong>
                        <p className="leading-relaxed whitespace-pre-wrap">{cita.observaciones}</p>
                      </div>
                    ) : cita.mensaje ? (
                      <p className="text-xs text-slate-600 bg-slate-50 p-3 rounded-xl border border-slate-200 mt-2 italic">
                        &ldquo;{cita.mensaje}&rdquo;
                      </p>
                    ) : null}
                  </div>
                </article>
              );
            })}
          </div>
        )
      )}
    </main>
  );
}

export default MisCitas;
