import React, { useState, useEffect } from 'react';
import { PhoneCall, ShieldAlert, Clock, Info, ExternalLink } from 'lucide-react';
import { servicioRecursosAyuda } from '../../servicios/servicioRecursosAyuda.js';

export function AvisoCrisis({
  alContinuar,
  alSalir,
  recursosPersonalizados = null,
  mostrarBotonContinuar = true
}) {
  const [recursos, setRecursos] = useState(recursosPersonalizados || []);
  const [cargandoRecursos, setCargandoRecursos] = useState(!recursosPersonalizados);

  useEffect(() => {
    if (recursosPersonalizados) {
      setRecursos(recursosPersonalizados);
      setCargandoRecursos(false);
      return;
    }

    let montado = true;
    async function cargarRecursos() {
      try {
        const datos = await servicioRecursosAyuda.obtenerRecursos();
        if (montado && Array.isArray(datos)) {
          setRecursos(datos);
        }
      } catch {
        // En caso de fallo de red, se mantiene el aviso con la sugerencia general
      } finally {
        if (montado) {
          setCargandoRecursos(false);
        }
      }
    }

    cargarRecursos();

    return () => {
      montado = false;
    };
  }, [recursosPersonalizados]);

  return (
    <section
      role="alert"
      aria-live="assertive"
      aria-label="Aviso prioritario de apoyo y lineas de ayuda"
      className="bg-amber-50 border-2 border-amber-300 rounded-2xl p-5 sm:p-6 shadow-md transition-all my-4"
    >
      <div className="flex items-start gap-4">
        <div className="w-11 h-11 rounded-full bg-amber-500 text-white flex items-center justify-center shrink-0 shadow-sm">
          <PhoneCall className="w-6 h-6" aria-hidden="true" />
        </div>

        <div className="flex-1">
          <h2 className="text-lg font-bold text-amber-950">
            Estamos contigo: canales de orientación y apoyo profesional
          </h2>
          <p className="mt-1 text-sm text-amber-900 leading-relaxed">
            Tus respuestas señalan que podrías estar experimentando pensamientos difíciles o malestar intenso. Recuerda que no tienes que enfrentar esto en soledad: existen canales institucionales y profesionales listos para escucharte y orientarte de manera gratuita y confidencial.
          </p>
          <p className="mt-1 text-xs text-amber-800 font-medium">
            Este sistema no notifica automáticamente a servicios de emergencia. Si requieres apoyo, utiliza de manera voluntaria los siguientes recursos:
          </p>

          {/* Listado de recursos */}
          <div className="mt-4">
            {cargandoRecursos ? (
              <p className="text-xs text-amber-800 italic">Cargando líneas de ayuda oficiales...</p>
            ) : recursos.length > 0 ? (
              <div className="grid sm:grid-cols-2 gap-3">
                {recursos.map((rec, index) => (
                  <div
                    key={`${rec.nombre}-${index}`}
                    className="bg-white/90 border border-amber-200 rounded-xl p-3.5 shadow-xs"
                  >
                    <div className="flex items-center justify-between">
                      <span className="font-semibold text-slate-900 text-sm">{rec.nombre}</span>
                      {rec.ambito && (
                        <span className="text-[11px] font-medium px-2 py-0.5 rounded-full bg-amber-100 text-amber-800">
                          {rec.ambito}
                        </span>
                      )}
                    </div>
                    <div className="mt-1.5 flex items-center gap-1.5 text-base font-bold text-amber-900">
                      <PhoneCall className="w-4 h-4 text-amber-700" aria-hidden="true" />
                      <a
                        href={`tel:${rec.telefono.replace(/[^0-9]/g, '')}`}
                        className="hover:underline focus:outline-none focus:ring-2 focus:ring-amber-500 rounded"
                        aria-label={`Llamar a ${rec.nombre} al ${rec.telefono}`}
                      >
                        {rec.telefono}
                      </a>
                    </div>
                    {rec.horario && (
                      <div className="mt-1 flex items-center gap-1 text-xs text-slate-600">
                        <Clock className="w-3.5 h-3.5 text-slate-500" aria-hidden="true" />
                        <span>{rec.horario}</span>
                      </div>
                    )}
                    {rec.descripcion && (
                      <p className="mt-1 text-xs text-slate-600 leading-normal">{rec.descripcion}</p>
                    )}
                  </div>
                ))}
              </div>
            ) : (
              <div className="bg-white/80 p-3 rounded-lg border border-amber-200 text-xs text-amber-900">
                Si requieres apoyo de emergencia o atención prioritaria en salud mental, acude al centro médico de urgencias más cercano o consulta las líneas de orientación institucional oficiales de tu localidad.
              </div>
            )}
          </div>

          {/* Botones de acción no bloqueantes */}
          <div className="mt-5 flex flex-wrap items-center gap-3">
            {mostrarBotonContinuar && (
              <button
                type="button"
                onClick={alContinuar}
                className="px-4 py-2 bg-amber-600 hover:bg-amber-700 text-white font-medium rounded-lg text-sm transition-colors shadow-sm focus:outline-none focus:ring-2 focus:ring-amber-500 focus:ring-offset-2"
              >
                Continuar con la evaluación
              </button>
            )}

            {alSalir && (
              <button
                type="button"
                onClick={alSalir}
                className="px-4 py-2 border border-amber-400 bg-white hover:bg-amber-100/50 text-amber-950 font-medium rounded-lg text-sm transition-colors focus:outline-none focus:ring-2 focus:ring-amber-500"
              >
                Salir y buscar ayuda
              </button>
            )}
          </div>
        </div>
      </div>
    </section>
  );
}
