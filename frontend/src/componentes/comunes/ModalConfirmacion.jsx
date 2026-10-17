import React, { useEffect, useRef } from 'react';
import { AlertTriangle, X } from 'lucide-react';

export function ModalConfirmacion({
  abierto,
  titulo,
  mensaje,
  textoConfirmar = 'Confirmar',
  textoCancelar = 'Cancelar',
  esPeligroso = false,
  alConfirmar,
  alCancelar
}) {
  const botonCancelarRef = useRef(null);

  useEffect(() => {
    if (!abierto) return;

    const manejarTeclaEscape = (evento) => {
      if (evento.key === 'Escape') {
        alCancelar();
      }
    };

    window.addEventListener('keydown', manejarTeclaEscape);
    botonCancelarRef.current?.focus();

    return () => {
      window.removeEventListener('keydown', manejarTeclaEscape);
    };
  }, [abierto, alCancelar]);

  if (!abierto) return null;

  return (
    <div
      className="fixed inset-0 z-50 overflow-y-auto bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4"
      role="dialog"
      aria-modal="true"
      aria-labelledby="titulo-modal-confirmacion"
    >
      <div className="bg-white rounded-2xl max-w-md w-full shadow-2xl border border-slate-200 overflow-hidden">
        <div className="p-6">
          <div className="flex items-start gap-3.5">
            <div
              className={`w-10 h-10 rounded-full flex items-center justify-center shrink-0 ${
                esPeligroso ? 'bg-rose-100 text-rose-600' : 'bg-amber-100 text-amber-600'
              }`}
            >
              <AlertTriangle className="w-5 h-5" aria-hidden="true" />
            </div>
            <div>
              <h3 id="titulo-modal-confirmacion" className="text-base font-bold text-slate-900">
                {titulo}
              </h3>
              <p className="mt-2 text-sm text-slate-600 leading-relaxed">
                {mensaje}
              </p>
            </div>
          </div>
        </div>

        <div className="bg-slate-50 px-6 py-3.5 flex justify-end gap-2.5 border-t border-slate-200">
          {textoCancelar ? (
            <button
              ref={botonCancelarRef}
              type="button"
              onClick={alCancelar}
              className="px-4 py-2 border border-slate-300 text-slate-700 hover:bg-slate-100 rounded-lg text-sm font-medium transition-colors"
            >
              {textoCancelar}
            </button>
          ) : null}
          <button
            type="button"
            onClick={alConfirmar}
            className={`px-4 py-2 rounded-lg text-sm font-medium text-white transition-colors shadow-sm ${
              esPeligroso
                ? 'bg-rose-600 hover:bg-rose-700'
                : 'bg-teal-600 hover:bg-teal-700'
            }`}
          >
            {textoConfirmar}
          </button>
        </div>
      </div>
    </div>
  );
}
