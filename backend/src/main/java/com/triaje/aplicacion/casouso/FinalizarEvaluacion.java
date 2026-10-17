package com.triaje.aplicacion.casouso;

import com.triaje.aplicacion.dto.DatosEvaluacion;
import com.triaje.aplicacion.puerto.EjecutorTransaccional;
import com.triaje.aplicacion.puerto.RepositorioAuditoria;
import com.triaje.aplicacion.puerto.RepositorioEvaluaciones;
import com.triaje.aplicacion.puerto.RepositorioRespuestasEvaluacion;
import com.triaje.dominio.EvaluadorGad7;
import com.triaje.dominio.EvaluadorPhq9;
import com.triaje.dominio.ReglaCrisis;
import com.triaje.dominio.ReglaResultadoMinimo;
import com.triaje.dominio.SeveridadAnsiedad;
import com.triaje.dominio.SeveridadDepresion;
import com.triaje.dominio.excepcion.ExcepcionDominio;
import com.triaje.dominio.excepcion.ExcepcionEvaluacionIncompleta;
import com.triaje.dominio.excepcion.ExcepcionEvaluacionNoEncontrada;
import com.triaje.dominio.modelo.AccionAuditoria;
import com.triaje.dominio.modelo.EstadoEvaluacion;
import com.triaje.dominio.modelo.Evaluacion;
import com.triaje.dominio.modelo.InstrumentoCribado;
import com.triaje.dominio.modelo.RegistroAuditoria;
import com.triaje.dominio.modelo.RespuestaEvaluacion;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Finaliza la evaluacion de cribado calculando los puntajes, niveles de severidad
 * y resultado minimo segun los evaluadores y reglas del dominio.
 */
public class FinalizarEvaluacion {

    private final RepositorioEvaluaciones repositorioEvaluaciones;
    private final RepositorioRespuestasEvaluacion repositorioRespuestas;
    private final RepositorioAuditoria repositorioAuditoria;
    private final EvaluadorPhq9 evaluadorPhq9;
    private final EvaluadorGad7 evaluadorGad7;
    private final ReglaCrisis reglaCrisis;
    private final ReglaResultadoMinimo reglaResultadoMinimo;
    private final EjecutorTransaccional transacciones;
    private final Clock reloj;

    public FinalizarEvaluacion(RepositorioEvaluaciones repositorioEvaluaciones,
                               RepositorioRespuestasEvaluacion repositorioRespuestas,
                               RepositorioAuditoria repositorioAuditoria,
                               EvaluadorPhq9 evaluadorPhq9,
                               EvaluadorGad7 evaluadorGad7,
                               ReglaCrisis reglaCrisis,
                               ReglaResultadoMinimo reglaResultadoMinimo,
                               EjecutorTransaccional transacciones,
                               Clock reloj) {
        this.repositorioEvaluaciones = Objects.requireNonNull(repositorioEvaluaciones);
        this.repositorioRespuestas = Objects.requireNonNull(repositorioRespuestas);
        this.repositorioAuditoria = Objects.requireNonNull(repositorioAuditoria);
        this.evaluadorPhq9 = Objects.requireNonNull(evaluadorPhq9);
        this.evaluadorGad7 = Objects.requireNonNull(evaluadorGad7);
        this.reglaCrisis = Objects.requireNonNull(reglaCrisis);
        this.reglaResultadoMinimo = Objects.requireNonNull(reglaResultadoMinimo);
        this.transacciones = Objects.requireNonNull(transacciones);
        this.reloj = Objects.requireNonNull(reloj);
    }

    public DatosEvaluacion ejecutar(String evaluacionId, Long usuarioId) {
        return transacciones.ejecutar(() -> {
            Evaluacion evaluacion = repositorioEvaluaciones.buscarPorId(evaluacionId)
                    .orElseThrow(() -> new ExcepcionEvaluacionNoEncontrada("La evaluación no existe o no tienes acceso a ella."));

            if (!evaluacion.getUsuarioId().equals(usuarioId)) {
                throw new ExcepcionEvaluacionNoEncontrada("La evaluación no existe o no tienes acceso a ella.");
            }

            if (evaluacion.getEstado() != EstadoEvaluacion.EN_CURSO) {
                throw new ExcepcionDominio("Solo se pueden finalizar evaluaciones en estado EN_CURSO.");
            }

            List<RespuestaEvaluacion> respuestas = repositorioRespuestas.buscarPorEvaluacionId(evaluacionId);

            Map<Integer, Integer> mapaPhq9 = respuestas.stream()
                    .filter(r -> r.getInstrumento() == InstrumentoCribado.PHQ9)
                    .collect(Collectors.toMap(RespuestaEvaluacion::getIndicePregunta, RespuestaEvaluacion::getValorPuntaje, (a, b) -> b));

            Map<Integer, Integer> mapaGad7 = respuestas.stream()
                    .filter(r -> r.getInstrumento() == InstrumentoCribado.GAD7)
                    .collect(Collectors.toMap(RespuestaEvaluacion::getIndicePregunta, RespuestaEvaluacion::getValorPuntaje, (a, b) -> b));

            if (mapaPhq9.size() != EvaluadorPhq9.CANTIDAD_ITEMS || mapaGad7.size() != EvaluadorGad7.CANTIDAD_ITEMS) {
                throw new ExcepcionEvaluacionIncompleta("La evaluación está incompleta: deben responderse las "
                        + EvaluadorPhq9.CANTIDAD_ITEMS + " preguntas de PHQ-9 y las "
                        + EvaluadorGad7.CANTIDAD_ITEMS + " de GAD-7 antes de finalizar.");
            }

            List<Integer> listaPhq9 = new ArrayList<>();
            for (int i = 1; i <= EvaluadorPhq9.CANTIDAD_ITEMS; i++) {
                Integer valor = mapaPhq9.get(i);
                if (valor == null) {
                    throw new ExcepcionEvaluacionIncompleta("Falta la respuesta a la pregunta " + i + " de PHQ-9.");
                }
                listaPhq9.add(valor);
            }

            List<Integer> listaGad7 = new ArrayList<>();
            for (int i = 1; i <= EvaluadorGad7.CANTIDAD_ITEMS; i++) {
                Integer valor = mapaGad7.get(i);
                if (valor == null) {
                    throw new ExcepcionEvaluacionIncompleta("Falta la respuesta a la pregunta " + i + " de GAD-7.");
                }
                listaGad7.add(valor);
            }

            int puntajePhq9 = evaluadorPhq9.calcularPuntaje(listaPhq9);
            SeveridadDepresion severidadDepresion = evaluadorPhq9.clasificar(puntajePhq9);

            int puntajeGad7 = evaluadorGad7.calcularPuntaje(listaGad7);
            SeveridadAnsiedad severidadAnsiedad = evaluadorGad7.clasificar(puntajeGad7);

            // Revalida si las respuestas definitivas contienen indicador de crisis delegando en ReglaCrisis;
            // preserva el estado de pestillo (latch) si ya habia sido activado previamente en la sesion.
            if (reglaCrisis.evaluar(listaPhq9)) {
                evaluacion.marcarRiesgoCrisis();
            }

            boolean resultadoMinimo = reglaResultadoMinimo.evaluar(
                    severidadDepresion,
                    severidadAnsiedad,
                    evaluacion.isRiesgoCrisis()
            );

            Instant ahora = Instant.now(reloj);
            evaluacion.finalizar(puntajePhq9, puntajeGad7, severidadDepresion, severidadAnsiedad, ahora);
            Evaluacion guardada = repositorioEvaluaciones.guardar(evaluacion);

            repositorioAuditoria.registrar(new RegistroAuditoria(
                    usuarioId,
                    AccionAuditoria.EVALUACION_COMPLETADA,
                    guardada.getId(),
                    ahora
            ));

            return DatosEvaluacion.desde(guardada, resultadoMinimo);
        });
    }
}
