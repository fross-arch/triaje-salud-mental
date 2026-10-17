import React, { useState } from 'react';
import { Calendar, Clock, Video, MapPin, AlertTriangle, X, CheckCircle, AlertCircle } from 'lucide-react';
import { servicioCitas } from '../../servicios/servicioCitas.js';
import { TEXTO_ADVERTENCIA_DATOS_CLINICOS_CITA } from '../../constantes/textosLegales.js';

export function FormularioProponerCita({
  evaluacionId,
  abierto = true,
  alCerrar,
  alProponer
}) {
  const [fechaHora, setFechaHora] = useState('');
  const [modalidad, setModalidad] = useState('VIRTUAL');
  const [mensaje, setMensaje] = useState('');
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState('');

  if (!abierto) return null;

  const validarFechaFutura = (valorFecha) => {
    if (!valorFecha) return false;
    const fechaSeleccionada = new Date(valorFecha);
    const ahora = new Date();
    return fechaSeleccionada.getTime() > ahora.getTime();
  };

  const manejarEnvio = async (e) => {
    e.preventDefault();
    setError('');

    if (!fechaHora) {
      setError('Debes seleccionar la fecha y hora de la cita.');
      return;
    }

    if (!validarFechaFutura(fechaHora)) {
      setError('La fecha y hora de la cita debe ser en el futuro.');
      return;
    }

    if (mensaje.length > 300) {
      setError('El mensaje no puede superar los 300 caracteres.');
      return;
    }

    setEnviando(true);
    try {
      const fechaHoraIso = new Date(fechaHora).toISOString();
      const resultado = await servicioCitas.proponerCita({
        evaluacionId,
        fechaHora: fechaHoraIso,
        modalidad,
        mensaje: mensaje.trim()
      });

      if (alProponer) {
        alProponer(resultado);
      }
      if (alCerrar) {
        alCerrar();
      }
    } catch (err) {
      setError(err.message || 'Error al proponer la cita.');
    } finally {
      setEnviando(false);
    }
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-xs"
      role="dialog"
      aria-modal="true"
      aria-labelledby="titulo-modal-cita"
    >
      <div className="bg-white rounded-2xl shadow-xl border border-slate-200 max-w-lg w-full p-6 sm:p-8 animate-in fade-in zoom-in-95">
        <div className="flex items-center justify-between pb-4 border-b border-slate-200 mb-5">
          <div className="flex items-center gap-2.5">
            <div className="w-9 h-9 rounded-xl bg-teal-100 text-teal-800 flex items-center justify-center">
              <Calendar className="w-5 h-5 text-teal-700" aria-hidden="true" />
            </div>
            <div>
              <h2 id="titulo-modal-cita" className="text-lg font-bold text-slate-900">
                Proponer cita de orientación
              </h2>
              <p className="text-xs text-slate-500">Evaluación #{evaluacionId}</p>
            </div>
          </div>
          {alCerrar && (
            <button
              type="button"
              onClick={alCerrar}
              className="p-1.5 text-slate-400 hover:text-slate-700 rounded-lg hover:bg-slate-100 transition-colors"
              aria-label="Cerrar formulario de propuesta de cita"
            >
              <X className="w-5 h-5" aria-hidden="true" />
            </button>
          )}
        </div>

        {/* Advertencia obligatoria sobre datos clinicos */}
        <div
          role="note"
          className="mb-5 p-3.5 bg-amber-50 border border-amber-300 rounded-xl text-xs text-amber-950 flex items-start gap-2.5 font-medium shadow-xs"
        >
          <AlertTriangle className="w-4 h-4 text-amber-700 shrink-0 mt-0.5" aria-hidden="true" />
          <div>
            <span className="font-bold">{TEXTO_ADVERTENCIA_DATOS_CLINICOS_CITA}.</span>
            <p className="mt-0.5 text-amber-900 text-[11px] font-normal">
              Este canal es exclusivamente organizativo para coordinar día, hora y enlace de consulta.
            </p>
          </div>
        </div>

        {error && (
          <div role="alert" className="mb-4 p-3 bg-rose-50 border border-rose-200 text-rose-800 text-xs rounded-xl flex items-center gap-2">
            <AlertCircle className="w-4 h-4 text-rose-600 shrink-0" aria-hidden="true" />
            <span>{error}</span>
          </div>
        )}

        <form onSubmit={manejarEnvio} className="space-y-4">
          {/* Fecha y hora */}
          <div>
            <label htmlFor="campo-fecha-hora" className="block text-xs font-bold text-slate-700 mb-1">
              Fecha y hora de la cita (debe ser futura):
            </label>
            <input
              type="datetime-local"
              id="campo-fecha-hora"
              value={fechaHora}
              onChange={(e) => setFechaHora(e.target.value)}
              className="w-full px-3.5 py-2 border border-slate-300 rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-teal-500"
              required
            />
          </div>

          {/* Modalidad */}
          <div>
            <label htmlFor="selector-modalidad" className="block text-xs font-bold text-slate-700 mb-1">
              Modalidad de atención:
            </label>
            <select
              id="selector-modalidad"
              value={modalidad}
              onChange={(e) => setModalidad(e.target.value)}
              className="w-full px-3.5 py-2 bg-white border border-slate-300 rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-teal-500"
            >
              <option value="VIRTUAL">Virtual (videollamada / teleconsulta)</option>
              <option value="PRESENCIAL">Presencial (en consultorio)</option>
            </select>
          </div>

          {/* Mensaje */}
          <div>
            <div className="flex justify-between items-center mb-1">
              <label htmlFor="campo-mensaje-cita" className="block text-xs font-bold text-slate-700">
                Mensaje o indicaciones para el paciente:
              </label>
              <span className={`text-[11px] ${mensaje.length > 300 ? 'text-rose-600 font-bold' : 'text-slate-400'}`}>
                {mensaje.length} / 300
              </span>
            </div>
            <textarea
              id="campo-mensaje-cita"
              rows={3}
              value={mensaje}
              maxLength={300}
              onChange={(e) => setMensaje(e.target.value)}
              placeholder="Ej: Te espero en el consultorio 301 o te enviaré el enlace 10 minutos antes."
              className="w-full px-3.5 py-2 border border-slate-300 rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-teal-500 resize-none"
            />
          </div>

          {/* Botones de accion */}
          <div className="pt-3 border-t border-slate-200 flex justify-end gap-2.5">
            {alCerrar && (
              <button
                type="button"
                onClick={alCerrar}
                className="px-4 py-2 border border-slate-300 hover:bg-slate-50 text-slate-700 rounded-xl text-xs font-semibold transition-colors"
              >
                Cancelar
              </button>
            )}
            <button
              type="submit"
              disabled={enviando}
              className="px-4 py-2 bg-teal-600 hover:bg-teal-700 disabled:bg-slate-300 text-white rounded-xl text-xs font-semibold shadow-xs transition-colors flex items-center gap-1.5"
            >
              <Calendar className="w-4 h-4" aria-hidden="true" />
              <span>{enviando ? 'Proponiendo...' : 'Proponer cita'}</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default FormularioProponerCita;
