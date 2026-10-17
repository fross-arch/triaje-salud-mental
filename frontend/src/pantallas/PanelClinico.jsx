import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { ClipboardList, AlertTriangle, ArrowRight, RefreshCw, User, Calendar, ShieldCheck } from 'lucide-react';
import { servicioClinicoEvaluaciones } from '../servicios/servicioClinicoEvaluaciones.js';
import { SeccionPerfil } from '../componentes/perfil/SeccionPerfil.jsx';

export function PanelClinico() {
  const [pestanaActiva, setPestanaActiva] = useState('evaluaciones'); // 'evaluaciones' | 'perfil'
  const [evaluaciones, setEvaluaciones] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState('');

  const cargarEvaluaciones = async () => {
    setCargando(true);
    setError('');
    try {
      const respuesta = await servicioClinicoEvaluaciones.listarEvaluaciones();
      const lista = Array.isArray(respuesta) ? respuesta : [];

      // Ordenar: primero las que tienen riesgoCrisis=true, luego por fechaFinalizacion descendente
      const ordenada = [...lista].sort((a, b) => {
        if (Boolean(a.riesgoCrisis) !== Boolean(b.riesgoCrisis)) {
          return a.riesgoCrisis ? -1 : 1;
        }
        const fechaA = new Date(a.fechaFinalizacion || 0).getTime();
        const fechaB = new Date(b.fechaFinalizacion || 0).getTime();
        return fechaB - fechaA;
      });

      setEvaluaciones(ordenada);
    } catch (err) {
      setError(err.message || 'No fue posible cargar las evaluaciones autorizadas.');
    } finally {
      setCargando(false);
    }
  };

  useEffect(() => {
    cargarEvaluaciones();
  }, []);

  return (
    <main className="max-w-5xl mx-auto px-4 py-8 sm:py-12">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 flex items-center gap-2">
            <ClipboardList className="w-6 h-6 text-teal-600" aria-hidden="true" />
            <span>Portal Clínico · Evaluaciones Recibidas</span>
          </h1>
          <p className="text-xs text-slate-500 mt-1">
            Bandeja de evaluaciones autorizadas por pacientes. Las evaluaciones con indicador de riesgo de crisis se priorizan al tope de la lista.
          </p>
        </div>

        <div className="flex items-center gap-2 self-start sm:self-center">
          <Link
            to="/clinico/citas"
            className="px-3.5 py-1.5 bg-teal-600 hover:bg-teal-700 text-white rounded-xl text-xs font-semibold flex items-center gap-1.5 shadow-xs transition-colors"
          >
            <Calendar className="w-3.5 h-3.5" aria-hidden="true" />
            <span>Gestión de Citas e Historial</span>
          </Link>

          {pestanaActiva === 'evaluaciones' && (
            <button
              type="button"
              onClick={cargarEvaluaciones}
              className="px-3 py-1.5 border border-slate-300 hover:bg-slate-50 rounded-xl text-xs font-semibold text-slate-700 flex items-center gap-1.5 transition-colors shadow-xs"
              aria-label="Actualizar listado de evaluaciones clínicas"
            >
              <RefreshCw className="w-3.5 h-3.5" aria-hidden="true" />
              <span>Actualizar</span>
            </button>
          )}
        </div>
      </div>

      {/* Pestañas de navegación clínica */}
      <div className="flex gap-2 border-b border-slate-200 mb-6">
        <button
          type="button"
          onClick={() => setPestanaActiva('evaluaciones')}
          className={`pb-3 px-4 text-xs font-bold border-b-2 transition-colors flex items-center gap-2 ${
            pestanaActiva === 'evaluaciones'
              ? 'border-teal-600 text-teal-700'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          <ClipboardList className="w-4 h-4" aria-hidden="true" />
          <span>Evaluaciones Recibidas</span>
        </button>

        <button
          type="button"
          onClick={() => setPestanaActiva('perfil')}
          className={`pb-3 px-4 text-xs font-bold border-b-2 transition-colors flex items-center gap-2 ${
            pestanaActiva === 'perfil'
              ? 'border-teal-600 text-teal-700'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          <User className="w-4 h-4" aria-hidden="true" />
          <span>Mi Perfil</span>
        </button>
      </div>

      {pestanaActiva === 'evaluaciones' && (
        <>
          {error && (
            <div role="alert" className="mb-6 p-4 bg-rose-50 border border-rose-200 text-rose-800 text-xs rounded-xl">
              {error}
            </div>
          )}

          {cargando ? (
        <div className="bg-white border border-slate-200 rounded-2xl p-12 text-center" role="status" aria-live="polite">
          <div className="w-8 h-8 border-4 border-teal-600 border-t-transparent rounded-full animate-spin mx-auto mb-3" aria-hidden="true" />
          <p className="text-xs text-slate-500">Cargando evaluaciones autorizadas...</p>
        </div>
      ) : evaluaciones.length === 0 ? (
        <div className="bg-white border border-slate-200 rounded-2xl p-10 text-center shadow-sm">
          <ClipboardList className="w-10 h-10 text-slate-400 mx-auto mb-3" aria-hidden="true" />
          <h2 className="text-base font-bold text-slate-800">No hay evaluaciones autorizadas aún</h2>
          <p className="text-xs text-slate-500 mt-1 max-w-sm mx-auto">
            Cuando un paciente decida compartir los resultados de su cribado contigo, aparecerán listados aquí.
          </p>
        </div>
      ) : (
        <div className="space-y-3.5" data-testid="lista-evaluaciones-clinicas">
          {evaluaciones.map((item) => {
            const fecha = item.fechaFinalizacion ? new Date(item.fechaFinalizacion) : null;
            const tieneCrisis = Boolean(item.riesgoCrisis);

            return (
              <article
                key={item.evaluacionId}
                data-testid={`evaluacion-card-${item.evaluacionId}`}
                className={`bg-white rounded-2xl p-5 sm:p-6 shadow-sm border transition-all hover:shadow-md flex flex-col sm:flex-row sm:items-center justify-between gap-4 ${
                  tieneCrisis ? 'border-amber-300 bg-amber-50/20' : 'border-slate-200'
                }`}
              >
                <div className="space-y-2">
                  <div className="flex items-center gap-2">
                    {tieneCrisis && (
                      <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-amber-100 text-amber-900 border border-amber-300">
                        <AlertTriangle className="w-3 h-3 text-amber-700" aria-hidden="true" />
                        <span>Prioridad: Indicador de crisis</span>
                      </span>
                    )}
                    <span className="text-[11px] px-2.5 py-0.5 rounded-full font-semibold bg-slate-100 text-slate-700">
                      ID: {item.evaluacionId}
                    </span>
                  </div>

                  <h2 className="text-base font-bold text-slate-900 flex items-center gap-2">
                    <User className="w-4 h-4 text-teal-600" aria-hidden="true" />
                    <span>{item.nombrePaciente || 'Paciente anónimo'}</span>
                  </h2>

                  <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-slate-500">
                    {fecha && (
                      <span className="flex items-center gap-1">
                        <Calendar className="w-3.5 h-3.5 text-slate-400" aria-hidden="true" />
                        <span>Completada: {fecha.toLocaleDateString('es-CO')}</span>
                      </span>
                    )}
                    <span>
                      PHQ-9: <strong className="text-slate-700">{item.severidadDepresion || 'N/A'}</strong>
                    </span>
                    <span>
                      GAD-7: <strong className="text-slate-700">{item.severidadAnsiedad || 'N/A'}</strong>
                    </span>
                  </div>
                </div>

                <Link
                  to={`/clinico/evaluaciones/${item.evaluacionId}`}
                  className="px-4 py-2 bg-teal-600 hover:bg-teal-700 text-white rounded-xl text-xs font-semibold shadow-xs transition-colors flex items-center justify-center gap-1.5 shrink-0 self-end sm:self-center"
                  aria-label={`Ver detalle clínico de la evaluación de ${item.nombrePaciente || 'paciente'}`}
                >
                  <span>Revisar evaluación</span>
                  <ArrowRight className="w-3.5 h-3.5" aria-hidden="true" />
                </Link>
              </article>
            );
          })}
        </div>
      )}
        </>
      )}

      {pestanaActiva === 'perfil' && (
        <SeccionPerfil />
      )}
    </main>
  );
}

export default PanelClinico;
