import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import {
  ArrowLeft,
  Download,
  Calendar,
  AlertTriangle,
  User,
  FileText,
  CheckCircle,
  AlertCircle,
  HelpCircle,
  Clock,
  Info
} from 'lucide-react';
import { servicioClinicoEvaluaciones } from '../servicios/servicioClinicoEvaluaciones.js';
import { servicioCitas } from '../servicios/servicioCitas.js';
import { FormularioProponerCita } from '../componentes/clinico/FormularioProponerCita.jsx';
import { LISTA_PREGUNTAS, OPCIONES_ESCALA } from '../constantes/preguntasCuestionarios.js';

export const ENUNCIADOS_FALLBACK_PHQ9 = {
  1: 'Poco interés o placer en hacer cosas',
  2: 'Sentirse desanimado/a, deprimido/a o sin esperanza',
  3: 'Problemas para dormir o para mantenerse dormido/a, o dormir demasiado',
  4: 'Sentirse cansado/a o tener poca energía',
  5: 'Poco apetito o comer en exceso',
  6: 'Sentirse mal consigo mismo/a — o sentir que es un/a fracasado/a o que ha decepcionado a su familia o a usted mismo/a',
  7: 'Dificultad para concentrarse en cosas tales como leer el periódico o ver televisión',
  8: 'Moverse o hablar tan lentamente que otras personas lo han notado, o lo contrario: estar tan inquieto/a o intranquilo/a que se ha estado moviendo mucho más de lo habitual',
  9: 'Pensamientos de que estaría mejor muerto/a o de hacerse daño de alguna manera'
};

export const ENUNCIADOS_FALLBACK_GAD7 = {
  1: 'Sentirse nervioso/a, intranquilo/a o con los nervios de punta',
  2: 'No poder parar o controlar las preocupaciones',
  3: 'Preocuparse demasiado por diferentes cosas',
  4: 'Dificultad para relajarse',
  5: 'Estar tan inquieto/a que le cuesta permanecer quieto/a',
  6: 'Ponerse molesto/a o irritable con facilidad',
  7: 'Sentir miedo como si algo terrible fuera a pasar'
};

export const TRADUCCION_ESCALA_FALLBACK = {
  0: 'Ningún día',
  1: 'Varios días',
  2: 'Más de la mitad de los días',
  3: 'Casi todos los días'
};

export function resolverEnunciadoPregunta(item, instrumentoKey) {
  if (item?.enunciadoPregunta) {
    return item.enunciadoPregunta;
  }
  const esPhq9 =
    instrumentoKey === 'PHQ9' ||
    instrumentoKey === 'PHQ-9' ||
    item?.instrumento === 'PHQ9' ||
    item?.instrumento === 'PHQ-9';
  const catalogo = esPhq9 ? ENUNCIADOS_FALLBACK_PHQ9 : ENUNCIADOS_FALLBACK_GAD7;
  return catalogo[item?.indicePregunta] || `Ítem ${item?.indicePregunta} de ${instrumentoKey}`;
}

export function resolverTextoFrecuencia(item) {
  if (item?.textoFrecuencia) {
    return item.textoFrecuencia;
  }
  return TRADUCCION_ESCALA_FALLBACK[item?.valorPuntaje] ?? `${item?.valorPuntaje ?? 0} pts`;
}

export function DetalleEvaluacionClinico({ evaluacionIdProp = null }) {
  const { id: evaluacionIdParam } = useParams();
  const evaluacionId = evaluacionIdProp || evaluacionIdParam;

  const [detalle, setDetalle] = useState(null);
  const [citasClinico, setCitasClinico] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [descargandoPdf, setDescargandoPdf] = useState(false);
  const [modalCitaAbierto, setModalCitaAbierto] = useState(false);
  const [mensajeExitoCita, setMensajeExitoCita] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    let montado = true;
    async function cargarDatos() {
      setCargando(true);
      setError('');
      try {
        const [respuestaDetalle, respuestaCitas] = await Promise.all([
          servicioClinicoEvaluaciones.obtenerDetalle(evaluacionId),
          servicioCitas.listarCitasClinico().catch(() => [])
        ]);
        if (montado) {
          setDetalle(respuestaDetalle);
          setCitasClinico(Array.isArray(respuestaCitas) ? respuestaCitas : []);
        }
      } catch (err) {
        if (montado) {
          setError(err.message || 'No fue posible cargar el detalle clínico de la evaluación.');
        }
      } finally {
        if (montado) {
          setCargando(false);
        }
      }
    }

    if (evaluacionId) {
      cargarDatos();
    }

    return () => {
      montado = false;
    };
  }, [evaluacionId]);

  const manejarDescargarPdf = async () => {
    if (!evaluacionId || descargandoPdf) return;
    setDescargandoPdf(true);
    try {
      const blob = await servicioClinicoEvaluaciones.descargarPdf(evaluacionId);
      const urlBlob = window.URL.createObjectURL(blob);
      const enlaceTemporal = document.createElement('a');
      enlaceTemporal.href = urlBlob;
      enlaceTemporal.download = `reporte-clinico-${evaluacionId}.pdf`;
      document.body.appendChild(enlaceTemporal);
      enlaceTemporal.click();
      document.body.removeChild(enlaceTemporal);
      window.URL.revokeObjectURL(urlBlob);
    } catch (err) {
      setError(err.message || 'Error al descargar el PDF de la evaluación.');
    } finally {
      setDescargandoPdf(false);
    }
  };

  if (cargando) {
    return (
      <main className="max-w-4xl mx-auto px-4 py-16 text-center" role="status" aria-live="polite">
        <div className="w-8 h-8 border-4 border-teal-600 border-t-transparent rounded-full animate-spin mx-auto mb-3" aria-hidden="true" />
        <p className="text-xs text-slate-500">Cargando evaluación clínica autorizada...</p>
      </main>
    );
  }

  if (error && !detalle) {
    return (
      <main className="max-w-md mx-auto px-4 py-16">
        <div className="bg-rose-50 border border-rose-200 text-rose-800 p-6 rounded-2xl text-center" role="alert">
          <AlertCircle className="w-8 h-8 text-rose-600 mx-auto mb-2" aria-hidden="true" />
          <h1 className="text-base font-bold mb-1">Evaluación no disponible</h1>
          <p className="text-xs mb-4">{error}</p>
          <Link
            to="/clinico"
            className="px-4 py-2 bg-rose-600 text-white rounded-xl text-xs font-semibold hover:bg-rose-700 transition-colors"
          >
            Volver a la bandeja clínica
          </Link>
        </div>
      </main>
    );
  }

  const {
    nombrePaciente,
    fechaFinalizacion,
    puntajePhq9,
    puntajeGad7,
    severidadDepresion,
    severidadAnsiedad,
    riesgoCrisis
  } = detalle || {};

  const listaRespuestas = detalle?.respuestas || detalle?.respuestasResumen || [];

  const respuestasPhq9 = listaRespuestas
    .filter((r) => r.instrumento === 'PHQ9' || r.instrumento === 'PHQ-9')
    .sort((a, b) => a.indicePregunta - b.indicePregunta);

  const respuestasGad7 = listaRespuestas
    .filter((r) => r.instrumento === 'GAD7' || r.instrumento === 'GAD-7')
    .sort((a, b) => a.indicePregunta - b.indicePregunta);

  const citaActiva =
    citasClinico.find(
      (c) =>
        (String(c.evaluacionId) === String(evaluacionId) ||
          (c.pacienteId && detalle?.pacienteId && String(c.pacienteId) === String(detalle.pacienteId))) &&
        (c.estado === 'PROPUESTA' || c.estado === 'CONFIRMADA')
    ) || (detalle?.citaActiva || null);

  return (
    <main className="max-w-4xl mx-auto px-4 py-8 sm:py-12">
      <div className="mb-4">
        <Link
          to="/clinico"
          className="inline-flex items-center gap-1.5 text-xs font-semibold text-teal-700 hover:text-teal-900 transition-colors"
        >
          <ArrowLeft className="w-4 h-4" aria-hidden="true" />
          <span>Volver al portal clínico</span>
        </Link>
      </div>

      {mensajeExitoCita && (
        <div role="status" className="mb-4 p-3.5 bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs rounded-xl flex items-center justify-between shadow-xs">
          <div className="flex items-center gap-2">
            <CheckCircle className="w-4 h-4 text-emerald-600 shrink-0" aria-hidden="true" />
            <span>{mensajeExitoCita}</span>
          </div>
          <button
            type="button"
            onClick={() => setMensajeExitoCita('')}
            className="text-emerald-700 hover:text-emerald-900 font-bold"
          >
            ×
          </button>
        </div>
      )}

      {/* Alerta de bloqueo preventivo si ya hay cita activa */}
      {citaActiva && (
        <div
          role="note"
          className="mb-4 p-3.5 bg-blue-50 border border-blue-200 text-blue-900 text-xs rounded-xl flex items-center gap-2.5 shadow-xs"
        >
          <Info className="w-4 h-4 text-blue-600 shrink-0" aria-hidden="true" />
          <span>
            <strong>Cita en curso:</strong> El paciente ya cuenta con una cita activa en estado{' '}
            <strong className="uppercase">{citaActiva.estado}</strong>. No es posible proponer otra cita hasta que la actual pase a cumplida o cancelada.
          </span>
        </div>
      )}

      {/* Alerta visible si riesgo de crisis */}
      {riesgoCrisis && (
        <div
          role="note"
          className="mb-6 p-4 bg-amber-50 border-2 border-amber-300 rounded-2xl flex items-start gap-3 shadow-xs"
        >
          <AlertTriangle className="w-5 h-5 text-amber-700 shrink-0 mt-0.5" aria-hidden="true" />
          <div>
            <h2 className="text-sm font-bold text-amber-950">
              Indicador de riesgo de crisis presente
            </h2>
            <p className="text-xs text-amber-900 mt-0.5 leading-relaxed">
              El paciente marcó un puntaje positivo en la pregunta 9 del PHQ-9 (pensamientos de autolesión). Se sugiere priorizar la revisión de este resultado orientativo.
            </p>
          </div>
        </div>
      )}

      <div className="bg-white border border-slate-200 rounded-2xl p-6 sm:p-8 shadow-sm mb-6">
        {/* Cabecera con datos y acciones */}
        <header className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-200 pb-6 mb-6">
          <div>
            <div className="flex items-center gap-2 mb-1.5">
              <span className="text-[11px] font-bold px-2.5 py-0.5 rounded-full bg-teal-50 text-teal-800 border border-teal-200">
                Evaluación Autorizada
              </span>
              <span className="text-xs text-slate-500">ID: {evaluacionId}</span>
            </div>
            <h1 className="text-xl sm:text-2xl font-bold text-slate-900 flex items-center gap-2">
              <User className="w-5 h-5 text-teal-600" aria-hidden="true" />
              <span>{nombrePaciente || 'Paciente anónimo'}</span>
            </h1>
            {fechaFinalizacion && (
              <p className="text-xs text-slate-500 mt-1 flex items-center gap-1.5">
                <Clock className="w-3.5 h-3.5 text-slate-400" aria-hidden="true" />
                <span>Completada el: {new Date(fechaFinalizacion).toLocaleDateString('es-CO')}</span>
              </p>
            )}
          </div>

          <div className="flex flex-wrap items-center gap-2.5">
            <button
              type="button"
              disabled={Boolean(citaActiva)}
              onClick={() => setModalCitaAbierto(true)}
              aria-label={citaActiva ? 'Cita ya activa para esta evaluación' : 'Proponer cita'}
              className="px-4 py-2 bg-teal-600 hover:bg-teal-700 disabled:bg-slate-300 disabled:cursor-not-allowed text-white rounded-xl text-xs font-semibold shadow-xs transition-colors flex items-center gap-1.5"
            >
              <Calendar className="w-4 h-4" aria-hidden="true" />
              <span>Proponer cita</span>
            </button>

            <button
              type="button"
              onClick={manejarDescargarPdf}
              disabled={descargandoPdf}
              className="px-4 py-2 border border-slate-300 hover:bg-slate-50 text-slate-700 rounded-xl text-xs font-semibold transition-colors flex items-center gap-1.5"
            >
              <Download className="w-4 h-4 text-slate-600" aria-hidden="true" />
              <span>{descargandoPdf ? 'Descargando...' : 'Descargar PDF'}</span>
            </button>
          </div>
        </header>

        {/* Resumen de puntajes y severidades */}
        <div className="grid sm:grid-cols-2 gap-4 mb-8">
          <div className="bg-slate-50 border border-slate-200 rounded-xl p-4">
            <h2 className="text-xs font-bold text-slate-700 uppercase tracking-wider mb-2">
              PHQ-9 (Depresión)
            </h2>
            <div className="flex items-baseline gap-2">
              <span className="text-2xl font-black text-slate-900">{puntajePhq9 ?? '-'}</span>
              <span className="text-xs text-slate-500">/ 27 puntos</span>
            </div>
            <p className="mt-2 text-xs font-semibold text-teal-800 bg-teal-50 px-2 py-1 rounded inline-block">
              Severidad: {severidadDepresion || 'No especificada'}
            </p>
          </div>

          <div className="bg-slate-50 border border-slate-200 rounded-xl p-4">
            <h2 className="text-xs font-bold text-slate-700 uppercase tracking-wider mb-2">
              GAD-7 (Ansiedad)
            </h2>
            <div className="flex items-baseline gap-2">
              <span className="text-2xl font-black text-slate-900">{puntajeGad7 ?? '-'}</span>
              <span className="text-xs text-slate-500">/ 21 puntos</span>
            </div>
            <p className="mt-2 text-xs font-semibold text-teal-800 bg-teal-50 px-2 py-1 rounded inline-block">
              Severidad: {severidadAnsiedad || 'No especificada'}
            </p>
          </div>
        </div>

        {/* Desglose de respuestas item a item */}
        <section aria-labelledby="titulo-desglose">
          <h2 id="titulo-desglose" className="text-base font-bold text-slate-900 mb-4 flex items-center gap-2">
            <FileText className="w-4 h-4 text-teal-600" aria-hidden="true" />
            <span>Desglose de respuestas ítem a ítem</span>
          </h2>

          {listaRespuestas.length === 0 ? (
            <div className="p-8 text-center bg-slate-50 border border-slate-200 rounded-xl text-slate-500 text-xs">
              <FileText className="w-8 h-8 text-slate-400 mx-auto mb-2" aria-hidden="true" />
              <p className="font-semibold text-slate-700">No se encontraron respuestas registradas para esta evaluación.</p>
              <p className="mt-1">Los datos de las respuestas individuales no están disponibles.</p>
            </div>
          ) : (
            <div className="space-y-6">
              {/* Respuestas PHQ-9 */}
              <div>
                <h3 className="text-xs font-bold text-slate-700 uppercase tracking-wider mb-2">
                  PHQ-9 · Síntomas en las últimas 2 semanas
                </h3>
                {respuestasPhq9.length === 0 ? (
                  <div className="p-4 text-center bg-slate-50 border border-slate-200 rounded-xl text-slate-500 text-xs">
                    No hay respuestas registradas para PHQ-9.
                  </div>
                ) : (
                  <div className="overflow-x-auto rounded-xl border border-slate-200 bg-white">
                    <table className="w-full text-left text-xs border-collapse">
                      <thead>
                        <tr className="border-b border-slate-200 bg-slate-50 text-slate-700 font-bold">
                          <th scope="col" className="py-2.5 px-3 w-12 text-center">#</th>
                          <th scope="col" className="py-2.5 px-3">Pregunta formulada</th>
                          <th scope="col" className="py-2.5 px-3">Respuesta del paciente</th>
                          <th scope="col" className="py-2.5 px-3 text-center w-28">Puntaje</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-slate-100">
                        {respuestasPhq9.map((item) => {
                          const esCrisisItem = item.indicePregunta === 9 && item.valorPuntaje > 0;
                          return (
                            <tr
                              key={`phq9-${item.indicePregunta}`}
                              className={esCrisisItem ? 'bg-amber-50/70' : 'hover:bg-slate-50/50'}
                            >
                              <td className="py-2.5 px-3 font-bold text-slate-900 text-center">
                                {item.indicePregunta}
                              </td>
                              <td className="py-2.5 px-3 text-slate-800">
                                <div>{resolverEnunciadoPregunta(item, 'PHQ9')}</div>
                                {esCrisisItem && (
                                  <div className="mt-1 inline-flex items-center gap-1 px-2 py-0.5 rounded text-[10px] font-bold bg-amber-100 text-amber-900 border border-amber-300">
                                    <AlertTriangle className="w-3 h-3 text-amber-700" aria-hidden="true" />
                                    <span>Alerta de riesgo de autolesión</span>
                                  </div>
                                )}
                              </td>
                              <td className="py-2.5 px-3">
                                <span
                                  className={`inline-block px-2.5 py-1 rounded-md text-xs font-medium border ${
                                    esCrisisItem
                                      ? 'bg-amber-100 text-amber-900 border-amber-300'
                                      : 'bg-slate-100 text-slate-800 border-slate-200'
                                  }`}
                                >
                                  {resolverTextoFrecuencia(item)}
                                </span>
                              </td>
                              <td className="py-2.5 px-3 text-center">
                                <span
                                  className={`inline-block px-2.5 py-0.5 rounded text-[11px] font-bold ${
                                    esCrisisItem
                                      ? 'bg-amber-200 text-amber-950 border border-amber-300'
                                      : 'bg-slate-100 text-slate-700 border border-slate-200'
                                  }`}
                                >
                                  {item.valorPuntaje} pts
                                </span>
                              </td>
                            </tr>
                          );
                        })}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>

              {/* Respuestas GAD-7 */}
              <div>
                <h3 className="text-xs font-bold text-slate-700 uppercase tracking-wider mb-2">
                  GAD-7 · Síntomas de ansiedad
                </h3>
                {respuestasGad7.length === 0 ? (
                  <div className="p-4 text-center bg-slate-50 border border-slate-200 rounded-xl text-slate-500 text-xs">
                    No hay respuestas registradas para GAD-7.
                  </div>
                ) : (
                  <div className="overflow-x-auto rounded-xl border border-slate-200 bg-white">
                    <table className="w-full text-left text-xs border-collapse">
                      <thead>
                        <tr className="border-b border-slate-200 bg-slate-50 text-slate-700 font-bold">
                          <th scope="col" className="py-2.5 px-3 w-12 text-center">#</th>
                          <th scope="col" className="py-2.5 px-3">Pregunta formulada</th>
                          <th scope="col" className="py-2.5 px-3">Respuesta del paciente</th>
                          <th scope="col" className="py-2.5 px-3 text-center w-28">Puntaje</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-slate-100">
                        {respuestasGad7.map((item) => (
                          <tr key={`gad7-${item.indicePregunta}`} className="hover:bg-slate-50/50">
                            <td className="py-2.5 px-3 font-bold text-slate-900 text-center">
                              {item.indicePregunta}
                            </td>
                            <td className="py-2.5 px-3 text-slate-800">
                              <div>{resolverEnunciadoPregunta(item, 'GAD7')}</div>
                            </td>
                            <td className="py-2.5 px-3">
                              <span className="inline-block px-2.5 py-1 rounded-md text-xs font-medium border bg-slate-100 text-slate-800 border-slate-200">
                                {resolverTextoFrecuencia(item)}
                              </span>
                            </td>
                            <td className="py-2.5 px-3 text-center">
                              <span className="inline-block px-2.5 py-0.5 rounded text-[11px] font-bold bg-slate-100 text-slate-700 border border-slate-200">
                                {item.valorPuntaje} pts
                              </span>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>
            </div>
          )}
        </section>
      </div>

      {/* Modal para proponer cita */}
      <FormularioProponerCita
        evaluacionId={evaluacionId}
        abierto={modalCitaAbierto}
        alCerrar={() => setModalCitaAbierto(false)}
        alProponer={(cita) => {
          setMensajeExitoCita('Cita propuesta exitosamente. El paciente la verá en su panel de citas.');
          setCitasClinico((prev) => [cita, ...prev]);
        }}
      />
    </main>
  );
}

export default DetalleEvaluacionClinico;
