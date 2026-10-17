import React from 'react';
import { AlertCircle } from 'lucide-react';
import { TEXTO_NOTA_INFORMATIVA } from '../../constantes/textosLegales.js';

export function NotaInformativa() {
  return (
    <div
      role="note"
      aria-label="Nota informativa sobre el alcance del cribado"
      className="bg-slate-100 border border-slate-200 rounded-xl p-4 text-slate-700 text-xs sm:text-sm my-4 flex items-start gap-3"
    >
      <AlertCircle className="w-5 h-5 text-slate-500 shrink-0 mt-0.5" aria-hidden="true" />
      <p className="leading-relaxed">
        {TEXTO_NOTA_INFORMATIVA}
      </p>
    </div>
  );
}
