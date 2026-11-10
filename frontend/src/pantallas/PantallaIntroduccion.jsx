import React, { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { servicioTriaje } from '../servicios/servicioTriaje.js';
import { servicioCitas } from '../servicios/servicioCitas.js';
import { BookOpen, ArrowRight, CheckCircle2, AlertTriangle, ShieldCheck } from 'lucide-react';
import { TEXTOS_INFORMACION_CUESTIONARIOS } from '../constantes/textosLegales.js';
import { PresentadorAvatar } from '../componentes/avatar/PresentadorAvatar.jsx';

export function PantallaIntroduccion() {
  const navegar = useNavigate();
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState('');
  const [bloqueoActivo, setBloqueoActivo] = useState(null);

  useEffect(() => {
    let cancelado = false;
    async function verificarEstado() {
      try {
        const citas = await servicioCitas.listarCitasPaciente();
        if (cancelado) return;
        const tieneCitaActiva = Array.isArray(citas) && citas.some(c => c.estado === 'PROPUESTA' || c.estado === 'CONFIRMADA');
        if (tieneCitaActiva) {
          setBloqueoActivo('Tienes una cita activa programada o propuesta. Debes completarla o cancelarla antes de iniciar un nuevo cribado.');
        }
      } catch {
        // En caso de error de red, el backend validará la regla en crearEvaluacion
      }
    }
    verificarEstado();
    return () => { cancelado = true; };
  }, []);

  const manejarIniciar = async () => {
    setCargando(true);
    setError('');
    try {
      const nuevaEvaluacion = await servicioTriaje.crearEvaluacion();
      if (nuevaEvaluacion && nuevaEvaluacion.id) {
        navegar(`/asistente/${nuevaEvaluacion.id}`);
      } else {
        throw new Error('No se recibió el identificador de la evaluación.');
      }
    } catch (err) {
      setError(err.message || 'Error al iniciar la evaluación. Verifica que tu consentimiento esté activo.');
    } finally {
      setCargando(false);
    }
  };

  return (
    <main className="max-w-3xl mx-auto px-4 py-8 sm:py-12">
      <div className="bg-white border border-slate-200 rounded-2xl shadow-sm p-6 sm:p-10">
        <header className="border-b border-slate-200 pb-6 mb-6 flex flex-col sm:flex-row items-center sm:items-start gap-5">
          <PresentadorAvatar estado="BIENVENIDA" className="shrink-0" />
          <div className="flex-1 text-center sm:text-left">
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-teal-50 text-teal-800 text-xs font-semibold mb-3">
              <BookOpen className="w-4 h-4 text-teal-600" aria-hidden="true" />
              <span>Marco Psicométrico y Orientativo</span>
            </div>
            <h1 className="text-xl sm:text-2xl font-bold text-slate-900 leading-snug">
              {TEXTOS_INFORMACION_CUESTIONARIOS.titulo}
            </h1>
            <p className="text-sm text-slate-600 mt-2 leading-relaxed">
              {TEXTOS_INFORMACION_CUESTIONARIOS.introduccion}
            </p>
          </div>
        </header>

        {bloqueoActivo && (
          <div
            role="alert"
            data-testid="alerta-bloqueo-cita-activa"
            className="mb-6 bg-amber-50 border border-amber-200 text-amber-900 text-sm p-4 rounded-xl flex items-start gap-3"
          >
            <AlertTriangle className="w-5 h-5 shrink-0 text-amber-600 mt-0.5" aria-hidden="true" />
            <div className="flex-1">
              <p className="font-semibold">Atención clínica en curso o programada</p>
              <p className="text-xs text-amber-800 mt-1">{bloqueoActivo}</p>
              <Link
                to="/mis-citas"
                className="inline-flex items-center gap-1.5 mt-2.5 text-xs font-semibold text-teal-700 hover:text-teal-900 underline"
              >
                Consultar Mis Citas
              </Link>
            </div>
          </div>
        )}

        {error && (
          <div
            role="alert"
            className="mb-6 bg-rose-50 border border-rose-200 text-rose-800 text-sm p-4 rounded-xl flex items-center gap-2.5"
          >
            <AlertTriangle className="w-5 h-5 shrink-0 text-rose-600" aria-hidden="true" />
            <span>{error}</span>
          </div>
        )}

        <div className="space-y-6 text-slate-700 text-sm leading-relaxed">
          {/* Cuestionarios */}
          <section>
            <h2 className="text-base font-bold text-slate-900 mb-3">Los dos instrumentos de cribado</h2>
            <div className="grid md:grid-cols-2 gap-4">
              {TEXTOS_INFORMACION_CUESTIONARIOS.instrumentos.map((inst) => (
                <div key={inst.nombre} className="border border-slate-200 rounded-xl p-4 bg-slate-50 flex flex-col justify-between">
                  <div>
                    <div className="flex items-center justify-between mb-1.5">
                      <span className="font-bold text-teal-900 text-base">{inst.nombre}</span>
                      <span className="text-xs bg-teal-100 text-teal-800 font-semibold px-2.5 py-0.5 rounded-full">
                        {inst.preguntas} preguntas
                      </span>
                    </div>
                    <p className="text-xs font-medium text-slate-800 mb-1">{inst.explora}</p>
                    <p className="text-xs text-slate-600 leading-normal">{inst.aspectos}</p>
                  </div>
                </div>
              ))}
            </div>
          </section>

          {/* Periodo y escala */}
          <section>
            <h2 className="text-base font-bold text-slate-900 mb-2">Periodo y escala de respuesta</h2>
            <p className="text-slate-600 mb-3">
              Todas las preguntas hacen referencia a: <strong>{TEXTOS_INFORMACION_CUESTIONARIOS.periodo}</strong>. Responde con base en la frecuencia de cada molestia:
            </p>
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5">
              {TEXTOS_INFORMACION_CUESTIONARIOS.escala.map((item) => (
                <div key={item.opcion} className="bg-slate-100 p-3 rounded-xl text-center border border-slate-200">
                  <div className="font-semibold text-slate-900 text-xs">{item.opcion}</div>
                  <div className="text-[11px] text-slate-500 mt-0.5">{item.puntos}</div>
                </div>
              ))}
            </div>
          </section>

          {/* Que no hacen */}
          <section className="bg-slate-50 border border-slate-200 rounded-xl p-4">
            <h2 className="text-sm font-bold text-slate-900 mb-2">Alcance orientativo y límites</h2>
            <ul className="space-y-1.5 list-disc list-inside text-xs text-slate-600">
              {TEXTOS_INFORMACION_CUESTIONARIOS.limites.map((lim, idx) => (
                <li key={idx}>{lim}</li>
              ))}
            </ul>
          </section>
        </div>

        {/* Accion de inicio */}
        <div className="mt-8 pt-6 border-t border-slate-200 flex justify-end">
          <button
            type="button"
            onClick={manejarIniciar}
            disabled={cargando || Boolean(bloqueoActivo)}
            className="w-full sm:w-auto px-6 py-3 bg-teal-600 hover:bg-teal-700 disabled:bg-slate-300 disabled:cursor-not-allowed text-white font-semibold rounded-xl text-sm transition-colors shadow-sm flex items-center justify-center gap-2"
          >
            {cargando ? (
              <>
                <span className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" aria-hidden="true" />
                <span>Iniciando cribado...</span>
              </>
            ) : (
              <>
                <span>Iniciar asistente de cribado</span>
                <ArrowRight className="w-4 h-4" aria-hidden="true" />
              </>
            )}
          </button>
        </div>
      </div>
    </main>
  );
}
