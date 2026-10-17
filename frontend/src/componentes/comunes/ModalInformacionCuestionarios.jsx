import React, { useEffect, useRef } from 'react';
import { X, BookOpen, AlertCircle, Info, ShieldAlert } from 'lucide-react';
import { TEXTOS_INFORMACION_CUESTIONARIOS } from '../../constantes/textosLegales.js';

export function ModalInformacionCuestionarios({ abierto, alCerrar }) {
  const botonCerrarRef = useRef(null);

  useEffect(() => {
    if (!abierto) return;

    const manejarTeclaEscape = (evento) => {
      if (evento.key === 'Escape') {
        alCerrar();
      }
    };

    window.addEventListener('keydown', manejarTeclaEscape);
    botonCerrarRef.current?.focus();

    return () => {
      window.removeEventListener('keydown', manejarTeclaEscape);
    };
  }, [abierto, alCerrar]);

  if (!abierto) return null;

  return (
    <div
      className="fixed inset-0 z-50 overflow-y-auto bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4 sm:p-6"
      role="dialog"
      aria-modal="true"
      aria-labelledby="titulo-modal-cuestionarios"
    >
      <div className="bg-white rounded-2xl max-w-3xl w-full max-h-[90vh] overflow-y-auto shadow-2xl border border-slate-200">
        {/* Cabecera del modal */}
        <div className="sticky top-0 bg-white border-b border-slate-200 px-6 py-4 flex items-center justify-between z-10">
          <div className="flex items-center gap-2 text-teal-800">
            <BookOpen className="w-5 h-5 text-teal-600" aria-hidden="true" />
            <h2 id="titulo-modal-cuestionarios" className="text-lg font-bold">
              {TEXTOS_INFORMACION_CUESTIONARIOS.titulo}
            </h2>
          </div>
          <button
            ref={botonCerrarRef}
            type="button"
            onClick={alCerrar}
            className="text-slate-400 hover:text-slate-600 p-1.5 rounded-lg hover:bg-slate-100 transition-colors"
            aria-label="Cerrar modal de informacion de cuestionarios"
          >
            <X className="w-5 h-5" aria-hidden="true" />
          </button>
        </div>

        {/* Contenido explicativo */}
        <div className="px-6 py-6 space-y-6 text-slate-700 text-sm leading-relaxed">
          <div className="bg-teal-50 border border-teal-200 rounded-xl p-4 flex gap-3 text-teal-900">
            <Info className="w-5 h-5 text-teal-700 shrink-0 mt-0.5" aria-hidden="true" />
            <p>{TEXTOS_INFORMACION_CUESTIONARIOS.introduccion}</p>
          </div>

          <section>
            <h3 className="font-semibold text-slate-900 text-base mb-3">Los dos instrumentos de cribado</h3>
            <div className="grid md:grid-cols-2 gap-4">
              {TEXTOS_INFORMACION_CUESTIONARIOS.instrumentos.map((inst) => (
                <div key={inst.nombre} className="border border-slate-200 rounded-xl p-4 bg-slate-50">
                  <div className="flex items-center justify-between mb-2">
                    <span className="font-bold text-teal-800 text-base">{inst.nombre}</span>
                    <span className="text-xs bg-teal-100 text-teal-800 font-semibold px-2 py-0.5 rounded-full">
                      {inst.preguntas} reactivos
                    </span>
                  </div>
                  <p className="font-medium text-slate-800 text-xs mb-1">Explora: {inst.explora}</p>
                  <p className="text-slate-600 text-xs">{inst.aspectos}</p>
                </div>
              ))}
            </div>
          </section>

          <section>
            <h3 className="font-semibold text-slate-900 text-base mb-2">Marco temporal y escala de respuesta</h3>
            <p className="mb-3 text-slate-600">
              Todas las preguntas evalúan el periodo de <strong>{TEXTOS_INFORMACION_CUESTIONARIOS.periodo}</strong>. Cada pregunta se responde con una escala de frecuencia de 4 opciones:
            </p>
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
              {TEXTOS_INFORMACION_CUESTIONARIOS.escala.map((item) => (
                <div key={item.opcion} className="bg-slate-100 p-2.5 rounded-lg text-center border border-slate-200">
                  <div className="font-medium text-slate-800 text-xs">{item.opcion}</div>
                  <div className="text-slate-500 text-xs">{item.puntos}</div>
                </div>
              ))}
            </div>
          </section>

          <section>
            <h3 className="font-semibold text-slate-900 text-base mb-2 flex items-center gap-1.5 text-rose-800">
              <ShieldAlert className="w-4 h-4 text-rose-600" aria-hidden="true" />
              Sobre la pregunta 9 del PHQ-9 (pensamientos de autolesión)
            </h3>
            <p className="bg-rose-50 border border-rose-200 text-rose-900 p-3.5 rounded-xl text-xs">
              {TEXTOS_INFORMACION_CUESTIONARIOS.sobreItem9}
            </p>
          </section>

          <section>
            <h3 className="font-semibold text-slate-900 text-base mb-2">Qué NO hacen estos cuestionarios</h3>
            <ul className="space-y-2 list-disc list-inside text-slate-600 text-xs">
              {TEXTOS_INFORMACION_CUESTIONARIOS.limites.map((limite, idx) => (
                <li key={idx}>{limite}</li>
              ))}
            </ul>
          </section>
        </div>

        {/* Pie del modal */}
        <div className="sticky bottom-0 bg-slate-50 border-t border-slate-200 px-6 py-3.5 flex justify-end">
          <button
            type="button"
            onClick={alCerrar}
            className="px-4 py-2 bg-teal-600 hover:bg-teal-700 text-white rounded-lg text-sm font-medium transition-colors shadow-sm"
          >
            Entendido, volver a la evaluacion
          </button>
        </div>
      </div>
    </div>
  );
}
