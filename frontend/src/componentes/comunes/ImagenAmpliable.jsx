import React, { useState, useEffect, useRef } from 'react';
import { ZoomIn, X } from 'lucide-react';

export function ImagenAmpliable({
  src,
  alt,
  className = 'w-full h-auto rounded-xl',
  loading = 'lazy',
  tituloModal
}) {
  const [abierto, setAbierto] = useState(false);
  const botonCerrarRef = useRef(null);
  const botonDisparadorRef = useRef(null);

  useEffect(() => {
    if (!abierto) return;

    const manejarTecla = (evento) => {
      if (evento.key === 'Escape') {
        setAbierto(false);
      }
    };

    window.addEventListener('keydown', manejarTecla);
    botonCerrarRef.current?.focus();

    return () => {
      window.removeEventListener('keydown', manejarTecla);
    };
  }, [abierto]);

  const cerrarModal = () => {
    setAbierto(false);
    botonDisparadorRef.current?.focus();
  };

  return (
    <>
      <button
        ref={botonDisparadorRef}
        type="button"
        onClick={() => setAbierto(true)}
        className="relative group w-full text-left cursor-zoom-in focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2 rounded-xl block overflow-hidden"
        aria-label={`Ampliar imagen: ${alt}`}
        title="Haz clic para ampliar la imagen"
      >
        <img
          src={src}
          alt={alt}
          className={className}
          loading={loading}
        />
        <div className="absolute inset-0 bg-slate-900/0 group-hover:bg-slate-900/10 transition-colors flex items-center justify-center pointer-events-none">
          <span className="opacity-0 group-hover:opacity-100 transition-opacity bg-slate-900/75 text-white text-xs px-2.5 py-1 rounded-lg flex items-center gap-1.5 shadow-md">
            <ZoomIn className="w-3.5 h-3.5" aria-hidden="true" />
            <span>Ampliar</span>
          </span>
        </div>
      </button>

      {abierto && (
        <div
          className="fixed inset-0 z-50 overflow-y-auto bg-slate-900/80 backdrop-blur-sm flex items-center justify-center p-4"
          role="dialog"
          aria-modal="true"
          aria-label={tituloModal || alt || 'Vista ampliada del gráfico'}
          onClick={cerrarModal}
        >
          <div
            className="relative max-w-4xl w-full bg-white rounded-2xl p-4 sm:p-6 shadow-2xl flex flex-col items-center border border-slate-200"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="w-full flex items-center justify-between pb-3 mb-2 border-b border-slate-100">
              <h3 className="text-sm font-semibold text-slate-800 truncate pr-4">
                {tituloModal || alt}
              </h3>
              <button
                ref={botonCerrarRef}
                type="button"
                onClick={cerrarModal}
                className="p-1.5 text-slate-500 hover:text-slate-800 hover:bg-slate-100 rounded-lg transition-colors"
                aria-label="Cerrar vista ampliada"
              >
                <X className="w-5 h-5" aria-hidden="true" />
              </button>
            </div>
            <div className="w-full max-h-[80vh] overflow-auto flex justify-center p-2">
              <img
                src={src}
                alt={alt}
                className="max-w-full max-h-[75vh] object-contain rounded-lg"
              />
            </div>
          </div>
        </div>
      )}
    </>
  );
}

export default ImagenAmpliable;
