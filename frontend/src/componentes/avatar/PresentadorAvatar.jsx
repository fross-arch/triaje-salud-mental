import React, { useState, useEffect } from 'react';
import { Eye, EyeOff } from 'lucide-react';

const CLAVE_ALMACENAMIENTO_SESION = 'triaje_avatar_visible';

export function PresentadorAvatar({
  estado = 'PREGUNTANDO',
  riesgoCrisis = false,
  className = '',
  mostrarInterruptor = true
}) {
  // Estado de visibilidad con recuperacion segura de la sesion actual
  const [visible, setVisible] = useState(() => {
    try {
      if (typeof window !== 'undefined' && window.sessionStorage) {
        const guardado = window.sessionStorage.getItem(CLAVE_ALMACENAMIENTO_SESION);
        return guardado !== null ? JSON.parse(guardado) : true;
      }
    } catch {
      // Fallback seguro ante navegacion privada o entornos restringidos
    }
    return true;
  });

  // Deteccion en tiempo de ejecucion de la preferencia del sistema operativo prefers-reduced-motion
  const [prefiereReducirMovimiento, setPrefiereReducirMovimiento] = useState(() => {
    if (typeof window !== 'undefined' && window.matchMedia) {
      return window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    }
    return false;
  });

  useEffect(() => {
    if (typeof window === 'undefined' || !window.matchMedia) return;

    const consultaMedia = window.matchMedia('(prefers-reduced-motion: reduce)');
    const manejarCambioPreferencia = (evento) => {
      setPrefiereReducirMovimiento(evento.matches);
    };

    if (consultaMedia.addEventListener) {
      consultaMedia.addEventListener('change', manejarCambioPreferencia);
    } else if (consultaMedia.addListener) {
      consultaMedia.addListener(manejarCambioPreferencia);
    }

    return () => {
      if (consultaMedia.removeEventListener) {
        consultaMedia.removeEventListener('change', manejarCambioPreferencia);
      } else if (consultaMedia.removeListener) {
        consultaMedia.removeListener(manejarCambioPreferencia);
      }
    };
  }, []);

  const alternarVisibilidad = () => {
    setVisible((previo) => {
      const nuevoEstado = !previo;
      try {
        if (typeof window !== 'undefined' && window.sessionStorage) {
          window.sessionStorage.setItem(
            CLAVE_ALMACENAMIENTO_SESION,
            JSON.stringify(nuevoEstado)
          );
        }
      } catch {
        // Almacenamiento no disponible
      }
      return nuevoEstado;
    });
  };

  // Regla prioritaria: riesgoCrisis fuerza inmediatamente el estado a 'CRISIS'
  const estadoEfectivo = riesgoCrisis ? 'CRISIS' : (estado || 'PREGUNTANDO');

  // Regla de animacion: en estado CRISIS o si prefiereReducirMovimiento esta activo, CERO animaciones
  const animacionesActivas = !prefiereReducirMovimiento && estadoEfectivo !== 'CRISIS';

  return (
    <div
      className={`inline-flex flex-col items-center select-none ${className}`}
      data-testid="contenedor-presentador-avatar"
      data-estado={estadoEfectivo}
    >
      {/* Contenedor del avatar grafico condicional a la visibilidad */}
      {visible && (
        <div
          className="relative flex items-center justify-center"
          data-testid="envoltorio-avatar"
        >
          <svg
            viewBox="0 0 100 100"
            className={`w-16 h-16 sm:w-20 sm:h-20 drop-shadow-sm transition-transform duration-300 motion-reduce:transition-none motion-reduce:animate-none ${
              animacionesActivas ? 'hover:scale-105' : ''
            }`}
            aria-hidden="true"
            focusable="false"
            data-testid="svg-presentador-avatar"
            data-estado-visual={estadoEfectivo}
            data-animaciones-activas={animacionesActivas ? 'true' : 'false'}
          >
            <defs>
              {/* Degradados sutiles y sobrios acordes a cada estado */}
              <linearGradient id="degradadoFondoBienvenida" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" stopColor="#ccfbf1" />
                <stop offset="100%" stopColor="#99f6e4" />
              </linearGradient>
              <linearGradient id="degradadoFondoPreguntando" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" stopColor="#f0fdfa" />
                <stop offset="100%" stopColor="#ccfbf1" />
              </linearGradient>
              <linearGradient id="degradadoFondoCrisis" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" stopColor="#f1f5f9" />
                <stop offset="100%" stopColor="#e2e8f0" />
              </linearGradient>
              <linearGradient id="degradadoFondoFinalizado" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" stopColor="#e0f2fe" />
                <stop offset="100%" stopColor="#bae6fd" />
              </linearGradient>
            </defs>

            {/* Aura circular de fondo con color representativo sobrio */}
            <circle
              cx="50"
              cy="50"
              r="46"
              fill={
                estadoEfectivo === 'CRISIS'
                  ? 'url(#degradadoFondoCrisis)'
                  : estadoEfectivo === 'BIENVENIDA'
                  ? 'url(#degradadoFondoBienvenida)'
                  : estadoEfectivo === 'FINALIZADO'
                  ? 'url(#degradadoFondoFinalizado)'
                  : 'url(#degradadoFondoPreguntando)'
              }
              stroke={
                estadoEfectivo === 'CRISIS'
                  ? '#94a3b8'
                  : estadoEfectivo === 'BIENVENIDA'
                  ? '#2dd4bf'
                  : estadoEfectivo === 'FINALIZADO'
                  ? '#38bdf8'
                  : '#14b8a6'
              }
              strokeWidth="2"
            />

            {/* Cuerpo / hombros neutros y minimalistas (sin simbologia clinica) */}
            <path
              d="M 24 88 C 24 72 35 68 50 68 C 65 68 76 72 76 88 Z"
              fill={
                estadoEfectivo === 'CRISIS'
                  ? '#475569'
                  : estadoEfectivo === 'FINALIZADO'
                  ? '#0369a1'
                  : '#0f766e'
              }
            />

            {/* Cuello neutro */}
            <rect x="45" y="56" width="10" height="12" rx="4" fill="#f8fafc" />

            {/* Cabeza neutra y armoniosa */}
            <circle
              cx="50"
              cy="44"
              r="22"
              fill="#ffffff"
              stroke={estadoEfectivo === 'CRISIS' ? '#64748b' : '#0d9488'}
              strokeWidth="1.5"
            />

            {/* Rasgos segun el estado psicometrico y conversacional */}
            {estadoEfectivo === 'BIENVENIDA' && (
              <g data-testid="variante-bienvenida">
                {/* Cejas cordiales y abiertas */}
                <path d="M 38 34 Q 43 32 46 34" stroke="#0f766e" strokeWidth="1.8" strokeLinecap="round" fill="none" />
                <path d="M 54 34 Q 57 32 62 34" stroke="#0f766e" strokeWidth="1.8" strokeLinecap="round" fill="none" />
                {/* Ojos afables con brillo */}
                <circle cx="42" cy="42" r="3.2" fill="#0f766e" />
                <circle cx="58" cy="42" r="3.2" fill="#0f766e" />
                <circle cx="43" cy="41" r="1" fill="#ffffff" />
                <circle cx="59" cy="41" r="1" fill="#ffffff" />
                {/* Sonrisa acogedora y serena */}
                <path d="M 42 52 Q 50 58 58 52" stroke="#0f766e" strokeWidth="2" strokeLinecap="round" fill="none" />
              </g>
            )}

            {estadoEfectivo === 'PREGUNTANDO' && (
              <g data-testid="variante-preguntando">
                {/* Ceja atenta / receptiva */}
                <path d="M 38 35 Q 43 34 46 36" stroke="#0f766e" strokeWidth="1.8" strokeLinecap="round" fill="none" />
                <path d="M 54 34 Q 57 32 62 35" stroke="#0f766e" strokeWidth="1.8" strokeLinecap="round" fill="none" />
                {/* Ojos atentos */}
                <circle cx="42" cy="43" r="3" fill="#0f766e" />
                <circle cx="58" cy="43" r="3" fill="#0f766e" />
                {/* Boca atenta y receptiva */}
                <path d="M 44 54 Q 50 55 56 54" stroke="#0f766e" strokeWidth="2" strokeLinecap="round" fill="none" />
              </g>
            )}

            {estadoEfectivo === 'CRISIS' && (
              <g data-testid="variante-crisis">
                {/* Cejas serenas y calmas (sin fruncir, transmitiendo apoyo sobrio) */}
                <path d="M 38 36 L 46 36" stroke="#475569" strokeWidth="1.8" strokeLinecap="round" fill="none" />
                <path d="M 54 36 L 62 36" stroke="#475569" strokeWidth="1.8" strokeLinecap="round" fill="none" />
                {/* Ojos suaves de contencion compasiva */}
                <path d="M 39 44 Q 42 41 45 44" stroke="#475569" strokeWidth="2.2" strokeLinecap="round" fill="none" />
                <path d="M 55 44 Q 58 41 61 44" stroke="#475569" strokeWidth="2.2" strokeLinecap="round" fill="none" />
                {/* Boca neutra, firme y reconfortante */}
                <path d="M 44 54 L 56 54" stroke="#475569" strokeWidth="2" strokeLinecap="round" fill="none" />
              </g>
            )}

            {estadoEfectivo === 'FINALIZADO' && (
              <g data-testid="variante-finalizado">
                {/* Cejas relajadas */}
                <path d="M 38 35 Q 43 33 46 35" stroke="#0369a1" strokeWidth="1.8" strokeLinecap="round" fill="none" />
                <path d="M 54 35 Q 57 33 62 35" stroke="#0369a1" strokeWidth="1.8" strokeLinecap="round" fill="none" />
                {/* Ojos en reposo tranquilo */}
                <path d="M 39 42 Q 42 45 45 42" stroke="#0369a1" strokeWidth="2.2" strokeLinecap="round" fill="none" />
                <path d="M 55 42 Q 58 45 61 42" stroke="#0369a1" strokeWidth="2.2" strokeLinecap="round" fill="none" />
                {/* Leve sonrisa reflexiva y pacifica */}
                <path d="M 43 53 Q 50 57 57 53" stroke="#0369a1" strokeWidth="2" strokeLinecap="round" fill="none" />
              </g>
            )}
          </svg>
        </div>
      )}

      {/* Interruptor accesible para ocultar o mostrar el avatar */}
      {mostrarInterruptor && (
        <button
          type="button"
          onClick={alternarVisibilidad}
          aria-pressed={visible}
          aria-label={visible ? 'Ocultar asistente visual' : 'Mostrar asistente visual'}
          className="mt-1 inline-flex items-center gap-1 px-2 py-0.5 rounded-md text-[11px] font-medium text-slate-500 hover:text-slate-800 hover:bg-slate-100 transition-colors focus:outline-none focus:ring-2 focus:ring-teal-500"
          data-testid="boton-alternar-avatar"
        >
          {visible ? (
            <>
              <EyeOff className="w-3 h-3" aria-hidden="true" />
              <span>Ocultar guía</span>
            </>
          ) : (
            <>
              <Eye className="w-3 h-3" aria-hidden="true" />
              <span>Mostrar guía</span>
            </>
          )}
        </button>
      )}
    </div>
  );
}
