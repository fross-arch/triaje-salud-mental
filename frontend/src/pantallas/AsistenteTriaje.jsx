import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  ENUNCIADO_TEMPORAL,
  OPCIONES_ESCALA,
  LISTA_PREGUNTAS,
  TOTAL_PREGUNTAS
} from '../constantes/preguntasCuestionarios.js';
import { servicioTriaje } from '../servicios/servicioTriaje.js';
import { AvisoCrisis } from '../componentes/triaje/AvisoCrisis.jsx';
import { ModalInformacionCuestionarios } from '../componentes/comunes/ModalInformacionCuestionarios.jsx';
import { ModalConfirmacion } from '../componentes/comunes/ModalConfirmacion.jsx';
import { PresentadorAvatar } from '../componentes/avatar/PresentadorAvatar.jsx';
import { HelpCircle, LogOut, CheckCircle2, AlertCircle } from 'lucide-react';

export function AsistenteTriaje() {
  const { id: evaluacionId } = useParams();
  const navegar = useNavigate();

  const [indiceActual, setIndiceActual] = useState(0);
  const [guardando, setGuardando] = useState(false);
  const [errorEnvio, setErrorEnvio] = useState('');
  const [riesgoCrisisActivo, setRiesgoCrisisActivo] = useState(false);
  const [avisoCrisisCerradoPorUsuario, setAvisoCrisisCerradoPorUsuario] = useState(false);
  const [modalInfoAbierto, setModalInfoAbierto] = useState(false);
  const [modalSalirAbierto, setModalSalirAbierto] = useState(false);
  const [respuestasRegistradas, setRespuestasRegistradas] = useState({});

  useEffect(() => {
    let cancelado = false;
    async function cargarRespuestasPrevias() {
      if (!evaluacionId) return;
      try {
        const detalle = await servicioTriaje.obtenerDetalle(evaluacionId);
        if (cancelado || !detalle) return;

        if (detalle.evaluacion?.riesgoCrisis) {
          setRiesgoCrisisActivo(true);
        }

        if (Array.isArray(detalle.respuestas) && detalle.respuestas.length > 0) {
          const mapa = {};
          detalle.respuestas.forEach((r) => {
            const pEncontrada = LISTA_PREGUNTAS.find(
              (p) => p.instrumento === r.instrumento && p.indicePregunta === r.indicePregunta
            );
            if (pEncontrada) {
              mapa[pEncontrada.numeroGlobal] = r.valorPuntaje;
            }
          });
          setRespuestasRegistradas(mapa);

          const primerSinResponder = LISTA_PREGUNTAS.findIndex(
            (p) => mapa[p.numeroGlobal] === undefined
          );

          if (primerSinResponder !== -1) {
            setIndiceActual(primerSinResponder);
          } else {
            setIndiceActual(TOTAL_PREGUNTAS - 1);
          }
        }
      } catch (err) {
        console.error('Error al cargar respuestas previas:', err);
      }
    }

    cargarRespuestasPrevias();
    return () => {
      cancelado = true;
    };
  }, [evaluacionId]);

  const preguntaActual = LISTA_PREGUNTAS[indiceActual];
  const numeroPregunta = indiceActual + 1;
  const porcentajeProgreso = Math.round((numeroPregunta / TOTAL_PREGUNTAS) * 100);

  const manejarSeleccionarOpcion = async (valorPuntaje) => {
    if (guardando) return;
    setErrorEnvio('');
    setGuardando(true);

    try {
      const respuestaBackend = await servicioTriaje.registrarRespuesta(
        evaluacionId,
        preguntaActual.instrumento,
        preguntaActual.indicePregunta,
        valorPuntaje
      );

      setRespuestasRegistradas((prev) => ({
        ...prev,
        [preguntaActual.numeroGlobal]: valorPuntaje
      }));

      // Si el backend indica riesgo de crisis, activamos el aviso
      if (respuestaBackend && respuestaBackend.riesgoCrisis) {
        setRiesgoCrisisActivo(true);
        setAvisoCrisisCerradoPorUsuario(false);
      }

      // Si es la ultima pregunta (16), finalizamos la evaluacion
      if (indiceActual === TOTAL_PREGUNTAS - 1) {
        const respuestaFinal = await servicioTriaje.finalizarEvaluacion(evaluacionId);
        navegar(`/resultados/${evaluacionId}`, { state: { evaluacion: respuestaFinal } });
      } else {
        setIndiceActual((prev) => prev + 1);
      }
    } catch (err) {
      setErrorEnvio(err.message || 'Error al guardar la respuesta. Por favor intenta de nuevo.');
    } finally {
      setGuardando(false);
    }
  };

  const manejarContinuarTrasCrisis = () => {
    // Permite al usuario continuar la evaluacion ocultando momentaneamente el banner en esta pantalla
    setAvisoCrisisCerradoPorUsuario(true);
  };

  const manejarSalirConfirmado = () => {
    setModalSalirAbierto(false);
    navegar('/perfil');
  };

  return (
    <main className="max-w-3xl mx-auto px-4 py-8 sm:py-12">
      {/* Barra superior de controles del asistente */}
      <div className="flex items-center justify-between gap-4 mb-6">
        <button
          type="button"
          onClick={() => setModalSalirAbierto(true)}
          className="text-slate-600 hover:text-rose-700 px-3 py-1.5 rounded-lg hover:bg-slate-100 flex items-center gap-1.5 text-sm font-medium transition-colors"
          aria-label="Salir de la evaluacion"
        >
          <LogOut className="w-4 h-4" aria-hidden="true" />
          <span>Salir</span>
        </button>

        <button
          type="button"
          onClick={() => setModalInfoAbierto(true)}
          className="text-teal-700 hover:text-teal-900 px-3 py-1.5 rounded-lg hover:bg-teal-50 flex items-center gap-1.5 text-sm font-medium transition-colors"
          aria-label="Informacion sobre los cuestionarios"
        >
          <HelpCircle className="w-4 h-4" aria-hidden="true" />
          <span>Sobre los cuestionarios</span>
        </button>
      </div>

      {/* Protocolo de crisis no bloqueante si riesgoCrisis es true */}
      {riesgoCrisisActivo && !avisoCrisisCerradoPorUsuario && (
        <AvisoCrisis
          alContinuar={manejarContinuarTrasCrisis}
          alSalir={() => setModalSalirAbierto(true)}
          mostrarBotonContinuar={true}
        />
      )}

      {/* Tarjeta principal del reactivo */}
      <div className="bg-white border border-slate-200 rounded-2xl shadow-sm p-6 sm:p-10">
        {/* Barra de progreso accesible */}
        <div className="mb-8">
          <div className="flex justify-between items-center text-xs text-slate-500 font-semibold uppercase tracking-wider mb-2">
            <span>{preguntaActual.nombreInstrumento}</span>
            <span>Pregunta {numeroPregunta} de {TOTAL_PREGUNTAS}</span>
          </div>

          <div
            className="w-full bg-slate-100 rounded-full h-2.5 overflow-hidden"
            role="progressbar"
            aria-valuenow={numeroPregunta}
            aria-valuemin="1"
            aria-valuemax={TOTAL_PREGUNTAS}
            aria-label="Progreso del cribado"
          >
            <div
              className="bg-teal-600 h-2.5 rounded-full transition-all duration-300 ease-out"
              style={{ width: `${porcentajeProgreso}%` }}
            />
          </div>
        </div>

        {/* Enunciado temporal superior obligatorio */}
        <div className="bg-slate-50 border border-slate-200 rounded-xl px-4 py-3 text-slate-600 text-xs sm:text-sm font-medium mb-6">
          <p>{ENUNCIADO_TEMPORAL}</p>
        </div>

        {/* Texto del reactivo acompañado del avatar sobrio */}
        <div className="flex flex-col sm:flex-row items-center sm:items-start gap-4 sm:gap-6 mb-8">
          <PresentadorAvatar
            estado={riesgoCrisisActivo ? 'CRISIS' : 'PREGUNTANDO'}
            riesgoCrisis={riesgoCrisisActivo}
            className="shrink-0"
          />
          <div className="flex-1 text-center sm:text-left self-center">
            <h1 className="text-lg sm:text-2xl font-bold text-slate-900 leading-snug">
              {preguntaActual.texto}
            </h1>
          </div>
        </div>

        {errorEnvio && (
          <div
            role="alert"
            className="mb-6 bg-rose-50 border border-rose-200 text-rose-800 text-sm p-3.5 rounded-xl flex items-center gap-2"
          >
            <AlertCircle className="w-4 h-4 shrink-0" aria-hidden="true" />
            <span>{errorEnvio}</span>
          </div>
        )}

        {/* Escala de 4 opciones */}
        <div className="space-y-3" role="group" aria-label={`Opciones de respuesta para la pregunta ${numeroPregunta}`}>
          {OPCIONES_ESCALA.map((opcion) => {
            const estaSeleccionada = respuestasRegistradas[preguntaActual.numeroGlobal] === opcion.valor;
            return (
              <button
                key={opcion.valor}
                type="button"
                disabled={guardando}
                onClick={() => manejarSeleccionarOpcion(opcion.valor)}
                className={`w-full text-left p-4 rounded-xl border transition-all flex items-center justify-between group focus:outline-none focus:ring-2 focus:ring-teal-500 ${
                  estaSeleccionada
                    ? 'border-teal-500 bg-teal-50/70 text-teal-950 font-semibold ring-1 ring-teal-500'
                    : 'border-slate-200 bg-white hover:bg-slate-50 hover:border-slate-300 text-slate-800'
                } ${guardando ? 'opacity-70 cursor-wait' : ''}`}
              >
                <div className="flex items-center gap-3">
                  <span className="w-7 h-7 rounded-full bg-slate-100 group-hover:bg-teal-100 text-slate-700 group-hover:text-teal-800 text-xs font-bold flex items-center justify-center shrink-0">
                    {opcion.valor}
                  </span>
                  <span className="text-sm sm:text-base">{opcion.etiqueta}</span>
                </div>

                <div className="w-5 h-5 rounded-full border border-slate-300 flex items-center justify-center group-hover:border-teal-500">
                  {estaSeleccionada && (
                    <div className="w-2.5 h-2.5 rounded-full bg-teal-600" />
                  )}
                </div>
              </button>
            );
          })}
        </div>

        {guardando && (
          <p className="mt-4 text-xs text-slate-500 text-center animate-pulse" role="status" aria-live="polite">
            Guardando respuesta...
          </p>
        )}
      </div>

      {/* Modal accesible informativo (sin perder avance) */}
      <ModalInformacionCuestionarios
        abierto={modalInfoAbierto}
        alCerrar={() => setModalInfoAbierto(false)}
      />

      {/* Modal de confirmacion para salir */}
      <ModalConfirmacion
        abierto={modalSalirAbierto}
        titulo="¿Deseas salir de la evaluación?"
        mensaje="Tus respuestas previas ya se encuentran guardadas de forma segura. Podrás revisar tu historial o continuar posteriormente desde tu perfil."
        textoConfirmar="Sí, salir"
        textoCancelar="Continuar respondiendo"
        alConfirmar={manejarSalirConfirmado}
        alCancelar={() => setModalSalirAbierto(false)}
      />
    </main>
  );
}
