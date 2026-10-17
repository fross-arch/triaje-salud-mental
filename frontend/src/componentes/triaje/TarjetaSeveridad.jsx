import React from 'react';
import { TEXTO_CUESTIONARIO_INDIVIDUAL_MINIMO } from '../../constantes/textosLegales.js';

const ETIQUETAS_SEVERIDAD = {
  MINIMA: { texto: 'Mínima', clase: 'bg-emerald-100 text-emerald-800 border-emerald-300' },
  LEVE: { texto: 'Leve', clase: 'bg-blue-100 text-blue-800 border-blue-300' },
  MODERADA: { texto: 'Moderada', clase: 'bg-amber-100 text-amber-800 border-amber-300' },
  MODERADAMENTE_SEVERA: { texto: 'Moderadamente severa', clase: 'bg-orange-100 text-orange-800 border-orange-300' },
  SEVERA: { texto: 'Severa', clase: 'bg-rose-100 text-rose-800 border-rose-300' }
};

export function TarjetaSeveridad({
  titulo,
  subtitulo,
  puntaje,
  puntajeMaximo,
  severidad,
  mostrarMensajeIndividualMinimo = false
}) {
  const severidadNormalizada = String(severidad || '').trim().toUpperCase();
  const infoSeveridad = ETIQUETAS_SEVERIDAD[severidadNormalizada] || ETIQUETAS_SEVERIDAD[severidad] || {
    texto: severidad || 'No evaluada',
    clase: 'bg-slate-100 text-slate-800 border-slate-300'
  };

  return (
    <article className="bg-white border border-slate-200 rounded-2xl p-5 shadow-xs flex flex-col justify-between">
      <div>
        <div className="flex items-start justify-between gap-2">
          <div>
            <h3 className="text-base font-bold text-slate-900">{titulo}</h3>
            <p className="text-xs text-slate-500 mt-0.5">{subtitulo}</p>
          </div>
          <span
            className={`text-xs font-semibold px-2.5 py-1 rounded-full border ${infoSeveridad.clase}`}
            aria-label={`Severidad preliminar: ${infoSeveridad.texto}`}
          >
            {infoSeveridad.texto}
          </span>
        </div>

        <div className="mt-4 flex items-baseline gap-1.5">
          <span className="text-3xl font-extrabold text-slate-900">{puntaje ?? 0}</span>
          <span className="text-sm text-slate-500">/ {puntajeMaximo} puntos</span>
        </div>

        {mostrarMensajeIndividualMinimo && severidadNormalizada === 'MINIMA' && (
          <p className="mt-3 text-xs text-slate-600 bg-slate-50 p-2.5 rounded-lg border border-slate-200">
            {TEXTO_CUESTIONARIO_INDIVIDUAL_MINIMO}
          </p>
        )}
      </div>

      <div className="mt-4 pt-3 border-t border-slate-100">
        <span className="text-[11px] text-slate-600">
          Cribado preliminar orientativo. No constituye diagnóstico clínico formal.
        </span>
      </div>
    </article>
  );
}
