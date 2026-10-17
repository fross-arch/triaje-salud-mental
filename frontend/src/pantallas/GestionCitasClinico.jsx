import React, { useState, useEffect } from 'react';
import {
  Calendar,
  Clock,
  Video,
  MapPin,
  XCircle,
  CheckCircle,
  AlertCircle,
  RefreshCw,
  User,
  History,
  AlertTriangle,
  RotateCcw,
  CheckSquare
} from 'lucide-react';
import { servicioCitas } from '../servicios/servicioCitas.js';

export function GestionCitasClinico() {
  const [citas, setCitas] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [pestanaActiva, setPestanaActiva] = useState('activas'); // 'activas' | 'historial'

  // Modal Posponer / Reprogramar
  const [citaAReprogramar, setCitaAReprogramar] = useState(null);
  const [nuevaFechaHora, setNuevaFechaHora] = useState('');
  const [mensajeReprogramar, setMensajeReprogramar] = useState('');
  const [guardandoReprogramar, setGuardandoReprogramar] = useState(false);
  const [errorReprogramar, setErrorReprogramar] = useState('');

  // Modal Dar por Cumplida
  const [citaACumplir, setCitaACumplir] = useState(null);
  const [observacionesCumplir, setObservacionesCumplir] = useState('');
  const [guardandoCumplir, setGuardandoCumplir] = useState(false);
  const [errorCumplir, setErrorCumplir] = useState('');

  // Modal Cancelar Cita
  const [citaACancelar, setCitaACancelar] = useState(null);
  const [motivoCancelar, setMotivoCancelar] = useState('');
  const [guardandoCancelar, setGuardandoCancelar] = useState(false);
  const [errorCancelar, setErrorCancelar] = useState('');

  const [mensajeEstado, setMensajeEstado] = useState('');
  const [error, setError] = useState('');

  const cargarCitas = async () => {
    setCargando(true);
    setError('');
    try {
      const datos = await servicioCitas.listarCitasClinico();
      setCitas(Array.isArray(datos) ? datos : []);
    } catch (err) {
      setError(err.message || 'No fue posible cargar tus citas clínicas.');
    } finally {
      setCargando(false);
    }
  };

  useEffect(() => {
    cargarCitas();
  }, []);

  const validarFechaFutura = (fecha) => {
    if (!fecha) return false;
    return new Date(fecha).getTime() > Date.now();
  };

  const manejarAbrirReprogramar = (cita) => {
    setCitaAReprogramar(cita);
    setNuevaFechaHora('');
    setMensajeReprogramar('');
    setErrorReprogramar('');
  };

  const manejarGuardarReprogramar = async (e) => {
    e.preventDefault();
    if (!citaAReprogramar) return;
    setErrorReprogramar('');

    if (!nuevaFechaHora) {
      setErrorReprogramar('Debes seleccionar una fecha y hora futura.');
      return;
    }

    if (!validarFechaFutura(nuevaFechaHora)) {
      setErrorReprogramar('La nueva fecha y hora debe ser estrictamente en el futuro.');
      return;
    }

    setGuardandoReprogramar(true);
    try {
      const fechaIso = new Date(nuevaFechaHora).toISOString();
      const pospuesta = await servicioCitas.posponerCita(citaAReprogramar.id, {
        nuevaFechaHora: fechaIso,
        mensaje: mensajeReprogramar.trim()
      });

      setCitas((prev) =>
        prev.map((c) => (c.id === citaAReprogramar.id ? { ...c, ...pospuesta } : c))
      );
      setCitaAReprogramar(null);
      setMensajeEstado('Cita reprogramada exitosamente.');
    } catch (err) {
      setErrorReprogramar(err.message || 'Error al reprogramar la cita.');
    } finally {
      setGuardandoReprogramar(false);
    }
  };

  const manejarAbrirCumplir = (cita) => {
    setCitaACumplir(cita);
    setObservacionesCumplir('');
    setErrorCumplir('');
  };

  const manejarGuardarCumplir = async (e) => {
    e.preventDefault();
    if (!citaACumplir) return;
    setErrorCumplir('');

    if (!observacionesCumplir.trim()) {
      setErrorCumplir('Las observaciones de la atención o cierre son obligatorias.');
      return;
    }

    setGuardandoCumplir(true);
    try {
      const cumplida = await servicioCitas.cumplirCita(citaACumplir.id, {
        observaciones: observacionesCumplir.trim()
      });

      setCitas((prev) =>
        prev.map((c) =>
          c.id === citaACumplir.id
            ? { ...c, ...cumplida, estado: 'CUMPLIDA', observaciones: observacionesCumplir.trim() }
            : c
        )
      );
      setCitaACumplir(null);
      setMensajeEstado('Cita registrada como cumplida exitosamente.');
    } catch (err) {
      setErrorCumplir(err.message || 'Error al marcar la cita como cumplida.');
    } finally {
      setGuardandoCumplir(false);
    }
  };

  const manejarAbrirCancelar = (cita) => {
    setCitaACancelar(cita);
    setMotivoCancelar('');
    setErrorCancelar('');
  };

  const manejarGuardarCancelar = async (e) => {
    e.preventDefault();
    if (!citaACancelar) return;
    setErrorCancelar('');

    if (!motivoCancelar.trim()) {
      setErrorCancelar('El motivo de cancelación es obligatorio.');
      return;
    }

    setGuardandoCancelar(true);
    try {
      const cancelada = await servicioCitas.cancelarCitaClinico(citaACancelar.id, {
        motivo: motivoCancelar.trim(),
        observaciones: motivoCancelar.trim()
      });

      setCitas((prev) =>
        prev.map((c) =>
          c.id === citaACancelar.id
            ? { ...c, ...cancelada, estado: 'CANCELADA', observaciones: motivoCancelar.trim() }
            : c
        )
      );
      setCitaACancelar(null);
      setMensajeEstado('Cita cancelada con motivo registrado. No podrá ser reabierta.');
    } catch (err) {
      setErrorCancelar(err.message || 'Error al cancelar la cita.');
    } finally {
      setGuardandoCancelar(false);
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
            <span>Gestión de Citas Clínicas</span>
          </h1>
          <p className="text-xs text-slate-500 mt-1">
            Revisión, reprogramación, cumplimiento y cancelación de citas de orientación.
          </p>
        </div>

        <button
          type="button"
          onClick={cargarCitas}
          className="px-3 py-1.5 border border-slate-300 hover:bg-slate-50 rounded-xl text-xs font-semibold text-slate-700 flex items-center gap-1.5 transition-colors self-start sm:self-center shadow-xs"
          aria-label="Actualizar listado de citas clínicas"
        >
          <RefreshCw className="w-3.5 h-3.5" aria-hidden="true" />
          <span>Actualizar</span>
        </button>
      </div>

      {/* Pestañas de citas: Activas vs Historial Pasado */}
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
          <p className="text-xs text-slate-500">Cargando citas clínicas...</p>
        </div>
      ) : pestanaActiva === 'activas' ? (
        /* ================= PESTAÑA: CITAS ACTIVAS ================= */
        citasActivas.length === 0 ? (
          <div className="bg-white border border-slate-200 rounded-2xl p-10 text-center shadow-sm">
            <Calendar className="w-10 h-10 text-slate-400 mx-auto mb-3" aria-hidden="true" />
            <h2 className="text-base font-bold text-slate-800">No hay citas activas pendientes</h2>
            <p className="text-xs text-slate-500 mt-1 max-w-sm mx-auto">
              Puedes proponer una cita directamente desde el detalle de cualquier evaluación autorizada.
            </p>
          </div>
        ) : (
          <div className="space-y-4">
            {citasActivas.map((cita) => {
              const fechaValor = cita.fechaHora || cita.fechaHoraInicio;
              const fechaCita = new Date(fechaValor);

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
                        <span>{cita.nombrePaciente || cita.pacienteNombre || `Evaluación #${cita.evaluacionId}`}</span>
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

                    {/* Botones de acción clínica para PROPUESTA o CONFIRMADA */}
                    <div className="flex flex-wrap sm:flex-col gap-2 shrink-0 self-end sm:self-center">
                      <button
                        type="button"
                        onClick={() => manejarAbrirReprogramar(cita)}
                        className="px-3 py-1.5 border border-amber-300 hover:bg-amber-50 text-amber-800 rounded-xl text-xs font-semibold transition-colors flex items-center gap-1.5"
                        aria-label={`Posponer o reprogramar cita con ${cita.nombrePaciente || 'paciente'}`}
                      >
                        <RotateCcw className="w-3.5 h-3.5 text-amber-600" aria-hidden="true" />
                        <span>Posponer / Reprogramar</span>
                      </button>

                      <button
                        type="button"
                        onClick={() => manejarAbrirCumplir(cita)}
                        className="px-3 py-1.5 bg-teal-600 hover:bg-teal-700 text-white rounded-xl text-xs font-semibold shadow-xs transition-colors flex items-center gap-1.5"
                        aria-label={`Dar por cumplida cita con ${cita.nombrePaciente || 'paciente'}`}
                      >
                        <CheckSquare className="w-3.5 h-3.5" aria-hidden="true" />
                        <span>Dar por Cumplida</span>
                      </button>

                      <button
                        type="button"
                        onClick={() => manejarAbrirCancelar(cita)}
                        className="px-3 py-1.5 border border-rose-300 hover:bg-rose-50 text-rose-700 rounded-xl text-xs font-semibold transition-colors flex items-center gap-1.5"
                        aria-label={`Cancelar cita con ${cita.nombrePaciente || 'paciente'}`}
                      >
                        <XCircle className="w-3.5 h-3.5 text-rose-600" aria-hidden="true" />
                        <span>Cancelar Cita</span>
                      </button>
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
              Las citas cumplidas o canceladas aparecerán aquí con sus observaciones registradas.
            </p>
          </div>
        ) : (
          <div className="space-y-4">
            {citasPasadas.map((cita) => {
              const fechaValor = cita.fechaHora || cita.fechaHoraInicio;
              const fechaCita = new Date(fechaValor);
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
                        Cita finalizada (sin opción de reabrirse)
                      </span>
                    </div>

                    <h2 className="text-base font-bold text-slate-900 flex items-center gap-1.5">
                      <User className="w-4 h-4 text-teal-600" aria-hidden="true" />
                      <span>{cita.nombrePaciente || cita.pacienteNombre || `Evaluación #${cita.evaluacionId}`}</span>
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

                    {/* Observaciones de atención o motivo de cancelación */}
                    {cita.observaciones ? (
                      <div className={`p-3.5 rounded-xl border text-xs mt-2 ${
                        esCumplida
                          ? 'bg-teal-50/60 border-teal-200 text-teal-950'
                          : 'bg-rose-50/60 border-rose-200 text-rose-950'
                      }`}>
                        <strong className="block font-bold mb-1">
                          {esCumplida ? 'Observaciones de la atención / cierre:' : 'Motivo de cancelación registrado:'}
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

      {/* ================= MODAL: POSPONER / REPROGRAMAR ================= */}
      {citaAReprogramar && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-xs"
          role="dialog"
          aria-modal="true"
          aria-labelledby="titulo-modal-reprogramar"
        >
          <div className="bg-white rounded-2xl shadow-xl border border-slate-200 max-w-lg w-full p-6 animate-in fade-in zoom-in-95">
            <h2 id="titulo-modal-reprogramar" className="text-base font-bold text-slate-900 mb-2 flex items-center gap-2">
              <RotateCcw className="w-5 h-5 text-amber-600" aria-hidden="true" />
              <span>Posponer / Reprogramar cita</span>
            </h2>
            <p className="text-xs text-slate-500 mb-4">
              Paciente: {citaAReprogramar.nombrePaciente || `Evaluación #${citaAReprogramar.evaluacionId}`}
            </p>

            {errorReprogramar && (
              <div role="alert" className="mb-4 p-3 bg-rose-50 border border-rose-200 text-rose-800 text-xs rounded-xl flex items-center gap-2">
                <AlertCircle className="w-4 h-4 text-rose-600 shrink-0" aria-hidden="true" />
                <span>{errorReprogramar}</span>
              </div>
            )}

            <form onSubmit={manejarGuardarReprogramar} className="space-y-4" noValidate>
              <div>
                <label htmlFor="campo-nueva-fecha" className="block text-xs font-bold text-slate-700 mb-1">
                  Nueva fecha y hora (debe ser futura):
                </label>
                <input
                  type="datetime-local"
                  id="campo-nueva-fecha"
                  value={nuevaFechaHora}
                  onChange={(e) => setNuevaFechaHora(e.target.value)}
                  className="w-full px-3 py-2 border border-slate-300 rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-amber-500"
                  required
                />
              </div>

              <div>
                <label htmlFor="campo-mensaje-reprogramar" className="block text-xs font-bold text-slate-700 mb-1">
                  Mensaje o motivo de la reprogramación para el paciente:
                </label>
                <textarea
                  id="campo-mensaje-reprogramar"
                  rows={3}
                  value={mensajeReprogramar}
                  onChange={(e) => setMensajeReprogramar(e.target.value)}
                  placeholder="Ej. Por motivos de fuerza mayor se traslada el horario a la nueva fecha indicada."
                  className="w-full px-3 py-2 border border-slate-300 rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-amber-500 resize-none"
                />
              </div>

              <div className="flex justify-end gap-2.5 pt-3 border-t border-slate-100">
                <button
                  type="button"
                  onClick={() => setCitaAReprogramar(null)}
                  className="px-4 py-2 border border-slate-300 hover:bg-slate-50 text-slate-700 rounded-xl text-xs font-semibold transition-colors"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  disabled={guardandoReprogramar}
                  className="px-4 py-2 bg-amber-600 hover:bg-amber-700 disabled:bg-slate-300 text-white rounded-xl text-xs font-semibold shadow-xs transition-colors"
                >
                  {guardandoReprogramar ? 'Guardando...' : 'Confirmar reprogramación'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ================= MODAL: DAR POR CUMPLIDA ================= */}
      {citaACumplir && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-xs"
          role="dialog"
          aria-modal="true"
          aria-labelledby="titulo-modal-cumplir"
        >
          <div className="bg-white rounded-2xl shadow-xl border border-slate-200 max-w-lg w-full p-6 animate-in fade-in zoom-in-95">
            <h2 id="titulo-modal-cumplir" className="text-base font-bold text-slate-900 mb-2 flex items-center gap-2">
              <CheckSquare className="w-5 h-5 text-teal-600" aria-hidden="true" />
              <span>Dar cita por cumplida</span>
            </h2>
            <p className="text-xs text-slate-500 mb-4">
              Registra las observaciones y recomendaciones orientativas para el cierre de esta sesión de orientación.
            </p>

            {errorCumplir && (
              <div role="alert" className="mb-4 p-3 bg-rose-50 border border-rose-200 text-rose-800 text-xs rounded-xl flex items-center gap-2">
                <AlertCircle className="w-4 h-4 text-rose-600 shrink-0" aria-hidden="true" />
                <span>{errorCumplir}</span>
              </div>
            )}

            <form onSubmit={manejarGuardarCumplir} className="space-y-4" noValidate>
              <div>
                <label htmlFor="campo-obs-cumplir" className="block text-xs font-bold text-slate-700 mb-1">
                  Observaciones de la atención / cierre: <span className="text-rose-600">*</span>
                </label>
                <textarea
                  id="campo-obs-cumplir"
                  rows={4}
                  value={observacionesCumplir}
                  onChange={(e) => setObservacionesCumplir(e.target.value)}
                  placeholder="Ej. Se brindó contención orientativa inicial y se acordó remisión a servicio de psicología clínica externa."
                  className="w-full px-3 py-2 border border-slate-300 rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-teal-500 resize-none"
                  required
                />
              </div>

              <div className="flex justify-end gap-2.5 pt-3 border-t border-slate-100">
                <button
                  type="button"
                  onClick={() => setCitaACumplir(null)}
                  className="px-4 py-2 border border-slate-300 hover:bg-slate-50 text-slate-700 rounded-xl text-xs font-semibold transition-colors"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  disabled={guardandoCumplir}
                  className="px-4 py-2 bg-teal-600 hover:bg-teal-700 disabled:bg-slate-300 text-white rounded-xl text-xs font-semibold shadow-xs transition-colors"
                >
                  {guardandoCumplir ? 'Guardando...' : 'Confirmar atención cumplida'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ================= MODAL: CANCELAR CITA ================= */}
      {citaACancelar && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-xs"
          role="dialog"
          aria-modal="true"
          aria-labelledby="titulo-modal-cancelar"
        >
          <div className="bg-white rounded-2xl shadow-xl border border-slate-200 max-w-lg w-full p-6 animate-in fade-in zoom-in-95">
            <h2 id="titulo-modal-cancelar" className="text-base font-bold text-slate-900 mb-2 flex items-center gap-2">
              <XCircle className="w-5 h-5 text-rose-600" aria-hidden="true" />
              <span>Cancelar cita</span>
            </h2>
            <p className="text-xs text-rose-700 mb-4">
              Esta acción pasará la cita a estado CANCELADA con sus notas visibles y no podrá reabrirse.
            </p>

            {errorCancelar && (
              <div role="alert" className="mb-4 p-3 bg-rose-50 border border-rose-200 text-rose-800 text-xs rounded-xl flex items-center gap-2">
                <AlertCircle className="w-4 h-4 text-rose-600 shrink-0" aria-hidden="true" />
                <span>{errorCancelar}</span>
              </div>
            )}

            <form onSubmit={manejarGuardarCancelar} className="space-y-4" noValidate>
              <div>
                <label htmlFor="campo-motivo-cancelar" className="block text-xs font-bold text-slate-700 mb-1">
                  Motivo de cancelación: <span className="text-rose-600">*</span>
                </label>
                <textarea
                  id="campo-motivo-cancelar"
                  rows={3}
                  value={motivoCancelar}
                  onChange={(e) => setMotivoCancelar(e.target.value)}
                  placeholder="Ej. Inasistencia reiterada o cancelación voluntaria comunicada por el usuario."
                  className="w-full px-3 py-2 border border-slate-300 rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-rose-500 resize-none"
                  required
                />
              </div>

              <div className="flex justify-end gap-2.5 pt-3 border-t border-slate-100">
                <button
                  type="button"
                  onClick={() => setCitaACancelar(null)}
                  className="px-4 py-2 border border-slate-300 hover:bg-slate-50 text-slate-700 rounded-xl text-xs font-semibold transition-colors"
                >
                  Regresar
                </button>
                <button
                  type="submit"
                  disabled={guardandoCancelar}
                  className="px-4 py-2 bg-rose-600 hover:bg-rose-700 disabled:bg-slate-300 text-white rounded-xl text-xs font-semibold shadow-xs transition-colors"
                >
                  {guardandoCancelar ? 'Cancelando...' : 'Confirmar cancelación'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </main>
  );
}

export default GestionCitasClinico;
