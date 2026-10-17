import React from 'react';
import { Info } from 'lucide-react';
import { TEXTO_RESULTADO_MINIMO } from '../../constantes/textosLegales.js';

export function MensajeResultadoMinimo() {
  return (
    <div
      role="region"
      aria-label="Mensaje de resultado minimo"
      className="bg-teal-50 border border-teal-200 rounded-2xl p-5 sm:p-6 text-teal-950 shadow-xs my-4"
    >
      <div className="flex items-start gap-3.5">
        <div className="w-8 h-8 rounded-full bg-teal-600 text-white flex items-center justify-center shrink-0">
          <Info className="w-5 h-5" aria-hidden="true" />
        </div>
        <div>
          <h3 className="font-bold text-base text-teal-900 mb-1">Resultado de cribado en nivel mínimo</h3>
          <p className="text-sm leading-relaxed text-teal-900">
            {TEXTO_RESULTADO_MINIMO}
          </p>
        </div>
      </div>
    </div>
  );
}
