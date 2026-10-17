import React from 'react';
import { Link } from 'react-router-dom';
import { Share2, Info } from 'lucide-react';
import { TEXTO_ADVERTENCIA_CRISIS_COMPARTIR } from '../../constantes/textosLegales.js';

export function AvisoCrisisCompartir({ evaluacionId, alCompartir, mostrarBoton = true }) {
  return (
    <aside
      aria-label="Opcion orientativa de compartir en caso de crisis"
      className="mt-3 bg-amber-50/70 border border-amber-200 rounded-xl p-4 text-xs text-amber-950 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 shadow-xs"
    >
      <div className="flex items-start gap-2.5 flex-1">
        <Info className="w-4 h-4 text-amber-700 shrink-0 mt-0.5" aria-hidden="true" />
        <p className="leading-relaxed font-normal">
          {TEXTO_ADVERTENCIA_CRISIS_COMPARTIR}
        </p>
      </div>

      {mostrarBoton && (
        <div className="shrink-0 self-end sm:self-center">
          {alCompartir ? (
            <button
              type="button"
              onClick={alCompartir}
              className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-amber-700 hover:bg-amber-800 text-white rounded-lg font-medium text-xs shadow-xs transition-colors"
            >
              <Share2 className="w-3.5 h-3.5" aria-hidden="true" />
              <span>Compartir con un psicólogo</span>
            </button>
          ) : (
            <Link
              to={`/resultados/${evaluacionId}/compartir`}
              className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-amber-700 hover:bg-amber-800 text-white rounded-lg font-medium text-xs shadow-xs transition-colors"
            >
              <Share2 className="w-3.5 h-3.5" aria-hidden="true" />
              <span>Compartir con un psicólogo</span>
            </Link>
          )}
        </div>
      )}
    </aside>
  );
}

export default AvisoCrisisCompartir;
