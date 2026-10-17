import React, { useState, useEffect } from 'react';
import { useParams, Link, useLocation } from 'react-router-dom';
import { servicioTriaje } from '../servicios/servicioTriaje.js';
import { AvisoCrisis } from '../componentes/triaje/AvisoCrisis.jsx';
import { MensajeResultadoMinimo } from '../componentes/triaje/MensajeResultadoMinimo.jsx';
import { NotaInformativa } from '../componentes/triaje/NotaInformativa.jsx';
import { TarjetaSeveridad } from '../componentes/triaje/TarjetaSeveridad.jsx';
import { PresentadorAvatar } from '../componentes/avatar/PresentadorAvatar.jsx';
import { AvisoCrisisCompartir } from '../componentes/triaje/AvisoCrisisCompartir.jsx';
import { ListaAutorizaciones } from '../componentes/triaje/ListaAutorizaciones.jsx';
import { TEXTO_ERROR_REPORTE_503 } from '../constantes/textosLegales.js';
import { Download, ArrowLeft, AlertCircle, FileCheck, Calendar, X, Share2 } from 'lucide-react';

function extraerDatosEvaluacion(fuente) {
  if (!fuente) return null;
  return fuente.evaluacion || fuente.datosEvaluacion || fuente;
}

export function PanelResultados({ datosEvaluacion = null }) {
  const { id: evaluacionIdParam } = useParams();
  const location = useLocation();

  const datosIniciales = extraerDatosEvaluacion(datosEvaluacion) || extraerDatosEvaluacion(location?.state);

  const [evaluacion, setEvaluacion] = useState(datosIniciales);
  const [cargando, setCargando] = useState(!datosIniciales);
  const [descargandoPdf, setDescargandoPdf] = useState(false);
  const [errorCarga, setErrorCarga] = useState('');
  const [errorDescargaPdf, setErrorDescargaPdf] = useState('');

  const evaluacionId = evaluacion?.id || evaluacionIdParam;

  useEffect(() => {
    const datosEntrantes = extraerDatosEvaluacion(datosEvaluacion) || extraerDatosEvaluacion(location?.state);
    if (datosEntrantes && Object.keys(datosEntrantes).length > 0) {
      setEvaluacion(datosEntrantes);
      setCargando(false);
      return;
    }

    if (!evaluacionIdParam) return;

    let montado = true;
    async function cargarDetalle() {
      try {
        const respuesta = await servicioTriaje.obtenerDetalle(evaluacionIdParam);
        if (montado) {
          const datos = respuesta?.evaluacion || respuesta;
          setEvaluacion(datos);
        }
      } catch (err) {
        if (montado) {
          setErrorCarga(err.message || 'Error al cargar los resultados de la evaluación.');
        }
      } finally {
        if (montado) {
          setCargando(false);
        }
      }
    }

    cargarDetalle();

    return () => {
      montado = false;
    };
  }, [datosEvaluacion, evaluacionIdParam, location?.state]);

  const manejarDescargarPdf = async () => {
    if (!evaluacionId || descargandoPdf) return;
    setDescargandoPdf(true);
    setErrorDescargaPdf('');
    try {
      const blob = await servicioTriaje.descargarPdf(evaluacionId);
      const urlBlob = window.URL.createObjectURL(blob);
      const enlaceTemporal = document.createElement('a');
      enlaceTemporal.href = urlBlob;
      enlaceTemporal.download = `reporte-cribado-${evaluacionId}.pdf`;
      document.body.appendChild(enlaceTemporal);
      enlaceTemporal.click();
      document.body.removeChild(enlaceTemporal);
      window.URL.revokeObjectURL(urlBlob);
    } catch (err) {
      const codigoEstado = err?.estado || err?.status;
      const mensajeOriginal = String(err?.message || '');
      if (codigoEstado === 503 || mensajeOriginal.includes('503')) {
        setErrorDescargaPdf(TEXTO_ERROR_REPORTE_503);
      } else {
        setErrorDescargaPdf(
          'No fue posible descargar el reporte en PDF en este momento. Por favor, intenta de nuevo más tarde.'
        );
      }
    } finally {
      setDescargandoPdf(false);
    }
  };

  if (cargando) {
    return (
      <main className="max-w-4xl mx-auto px-4 py-16 text-center" role="status" aria-live="polite">
        <div className="w-10 h-10 border-4 border-teal-600 border-t-transparent rounded-full animate-spin mx-auto mb-3" aria-hidden="true" />
        <p className="text-sm text-slate-600">Cargando resultados del cribado...</p>
      </main>
    );
  }

  if (errorCarga && !evaluacion) {
    return (
      <main className="max-w-md mx-auto px-4 py-16">
        <div className="bg-rose-50 border border-rose-200 text-rose-800 p-6 rounded-2xl text-center" role="alert">
          <AlertCircle className="w-8 h-8 text-rose-600 mx-auto mb-2" aria-hidden="true" />
          <h1 className="text-base font-bold mb-1">No se pudieron cargar los resultados</h1>
          <p className="text-sm">{errorCarga}</p>
          <Link
            to="/perfil"
            className="mt-4 inline-block px-4 py-2 bg-rose-600 text-white rounded-lg text-sm font-medium hover:bg-rose-700 transition-colors"
          >
            Volver a mi perfil
          </Link>
        </div>
      </main>
    );
  }

  const datosLimpios = evaluacion?.evaluacion || evaluacion || {};

  const puntajePhq9 = datosLimpios.puntajePhq9 ?? datosLimpios.puntaje_phq9 ?? 0;
  const puntajeGad7 = datosLimpios.puntajeGad7 ?? datosLimpios.puntaje_gad7 ?? 0;
  const severidadDepresion = datosLimpios.severidadDepresion ?? datosLimpios.severidad_depresion;
  const severidadAnsiedad = datosLimpios.severidadAnsiedad ?? datosLimpios.severidad_ansiedad;
  const riesgoCrisis = Boolean(datosLimpios.riesgoCrisis ?? datosLimpios.riesgo_crisis);
  const resultadoMinimo = Boolean(datosLimpios.resultadoMinimo ?? datosLimpios.resultado_minimo);
  const fechaCreacion = datosLimpios.fechaCreacion ?? datosLimpios.fecha_creacion;

  // Determinar si solo uno de los instrumentos es minimo
  const esPhq9Minimo = String(severidadDepresion || '').trim().toUpperCase() === 'MINIMA';
  const esGad7Minimo = String(severidadAnsiedad || '').trim().toUpperCase() === 'MINIMA';
  const soloPhq9Minimo = esPhq9Minimo && !esGad7Minimo;
  const soloGad7Minimo = esGad7Minimo && !esPhq9Minimo;

  // REGLA CRITICA: Mensaje de resultado minimo SOLO si resultadoMinimo=true y riesgoCrisis=false.
  // Si riesgoCrisis=true, esta estrictamente prohibido mostrarlo.
  const debeMostrarResultadoMinimo = resultadoMinimo === true && riesgoCrisis === false;

  return (
    <main className="max-w-4xl mx-auto px-4 py-8 sm:py-12">
      {/* Aviso de crisis en encabezado si riesgoCrisis es true (NUNCA permite resultado minimo si hay crisis) */}
      {riesgoCrisis && (
        <div className="mb-6">
          <AvisoCrisis mostrarBotonContinuar={false} />
          <AvisoCrisisCompartir evaluacionId={evaluacionId} />
        </div>
      )}

      <div className="bg-white border border-slate-200 rounded-2xl shadow-sm p-6 sm:p-10">
        <header className="border-b border-slate-200 pb-6 mb-6 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div className="flex flex-col sm:flex-row items-center sm:items-start gap-4">
            <PresentadorAvatar
              estado={riesgoCrisis ? 'CRISIS' : 'FINALIZADO'}
              riesgoCrisis={riesgoCrisis}
              className="shrink-0"
            />
            <div className="text-center sm:text-left">
              <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-teal-50 text-teal-800 text-xs font-semibold mb-2">
                <FileCheck className="w-3.5 h-3.5 text-teal-600" aria-hidden="true" />
                <span>Resultados de Cribado Clínico Preliminar</span>
              </div>
              <h1 className="text-2xl sm:text-3xl font-bold text-slate-900">
                Devolución de resultados
              </h1>
              {fechaCreacion && (
                <p className="text-xs text-slate-500 mt-1 flex items-center justify-center sm:justify-start gap-1">
                  <Calendar className="w-3.5 h-3.5" aria-hidden="true" />
                  <span>Fecha de realización: {new Date(fechaCreacion).toLocaleDateString('es-CO')}</span>
                </p>
              )}
            </div>
          </div>

          <div className="flex flex-wrap items-center gap-2.5">
            <Link
              to={`/resultados/${evaluacionId}/compartir`}
              className="px-4 py-2.5 border border-teal-600 text-teal-700 hover:bg-teal-50 font-medium rounded-xl text-sm transition-colors shadow-xs flex items-center justify-center gap-2"
              aria-label="Compartir evaluación con un psicólogo"
            >
              <Share2 className="w-4 h-4 text-teal-600" aria-hidden="true" />
              <span>Compartir con un psicólogo</span>
            </Link>

            <button
              type="button"
              onClick={manejarDescargarPdf}
              disabled={descargandoPdf}
              className="px-4 py-2.5 bg-teal-600 hover:bg-teal-700 disabled:bg-teal-400 text-white font-medium rounded-xl text-sm transition-colors shadow-sm flex items-center justify-center gap-2"
              aria-label="Descargar reporte de resultados en formato PDF"
            >
              <Download className="w-4 h-4" aria-hidden="true" />
              <span>{descargandoPdf ? 'Generando PDF...' : 'Descargar PDF'}</span>
            </button>
          </div>
        </header>

        {/* Notificación amigable de error al descargar PDF (HTTP 503 u otros) */}
        {errorDescargaPdf && (
          <div
            role="alert"
            className="mb-6 p-4 bg-rose-50 border border-rose-200 text-rose-800 rounded-xl text-sm flex items-start justify-between gap-3 shadow-xs"
          >
            <div className="flex items-center gap-2.5">
              <AlertCircle className="w-5 h-5 text-rose-600 shrink-0 mt-0.5" aria-hidden="true" />
              <p className="font-medium leading-relaxed">{errorDescargaPdf}</p>
            </div>
            <button
              type="button"
              onClick={() => setErrorDescargaPdf('')}
              className="text-rose-700 hover:text-rose-900 text-xs font-semibold p-1 hover:bg-rose-100 rounded-lg transition-colors shrink-0"
              aria-label="Cerrar notificación de error de descarga"
            >
              <X className="w-4 h-4" aria-hidden="true" />
            </button>
          </div>
        )}

        {/* REGLA CRITICA: Mensaje de resultado minimo SOLO si resultadoMinimo=true y riesgoCrisis=false */}
        {debeMostrarResultadoMinimo && (
          <MensajeResultadoMinimo />
        )}

        {/* Tarjetas individuales de severidad */}
        <div className="grid md:grid-cols-2 gap-5 my-6">
          <TarjetaSeveridad
            titulo="PHQ-9 (Cuestionario de Salud del Paciente)"
            subtitulo="Explora síntomas preliminares de estado de ánimo y depresión"
            puntaje={puntajePhq9}
            puntajeMaximo={27}
            severidad={severidadDepresion}
            mostrarMensajeIndividualMinimo={soloPhq9Minimo}
          />

          <TarjetaSeveridad
            titulo="GAD-7 (Trastorno de Ansiedad Generalizada)"
            subtitulo="Explora síntomas preliminares de tensión y ansiedad"
            puntaje={puntajeGad7}
            puntajeMaximo={21}
            severidad={severidadAnsiedad}
            mostrarMensajeIndividualMinimo={soloGad7Minimo}
          />
        </div>

        {/* REGLA OBLIGATORIA: NotaInformativa en TODOS los resultados sin excepcion */}
        <NotaInformativa />

        {/* Lista de profesionales autorizados */}
        <ListaAutorizaciones evaluacionId={evaluacionId} />

        {/* Acciones de navegacion */}
        <div className="mt-8 pt-6 border-t border-slate-200 flex justify-between items-center">
          <Link
            to="/perfil"
            className="text-sm font-semibold text-teal-700 hover:text-teal-900 flex items-center gap-1.5"
          >
            <ArrowLeft className="w-4 h-4" aria-hidden="true" />
            <span>Ir a mi perfil e historial</span>
          </Link>
        </div>
      </div>
    </main>
  );
}
