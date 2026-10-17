package com.triaje.aplicacion.casouso;

import com.triaje.aplicacion.dto.ComandoRegistrarRespuesta;
import com.triaje.aplicacion.dto.ResultadoRegistroRespuesta;
import com.triaje.aplicacion.puerto.EjecutorTransaccional;
import com.triaje.aplicacion.puerto.RepositorioEvaluaciones;
import com.triaje.aplicacion.puerto.RepositorioRespuestasEvaluacion;
import com.triaje.dominio.excepcion.ExcepcionDominio;
import com.triaje.dominio.excepcion.ExcepcionEvaluacionNoEncontrada;
import com.triaje.dominio.modelo.EstadoEvaluacion;
import com.triaje.dominio.modelo.Evaluacion;
import com.triaje.dominio.modelo.InstrumentoCribado;
import com.triaje.dominio.modelo.RespuestaEvaluacion;

import java.util.Objects;
import java.util.Optional;

/**
 * Guarda o actualiza una respuesta individual a una pregunta de la evaluacion.
 * Si se detecta un indicador de crisis (item 9 de PHQ-9 con puntaje >= 1),
 * marca de inmediato riesgo_crisis = true en la base de datos.
 */
public class RegistrarRespuestaEvaluacion {

    private final RepositorioEvaluaciones repositorioEvaluaciones;
    private final RepositorioRespuestasEvaluacion repositorioRespuestas;
    private final EjecutorTransaccional transacciones;

    public RegistrarRespuestaEvaluacion(RepositorioEvaluaciones repositorioEvaluaciones,
                                        RepositorioRespuestasEvaluacion repositorioRespuestas,
                                        EjecutorTransaccional transacciones) {
        this.repositorioEvaluaciones = Objects.requireNonNull(repositorioEvaluaciones);
        this.repositorioRespuestas = Objects.requireNonNull(repositorioRespuestas);
        this.transacciones = Objects.requireNonNull(transacciones);
    }

    public ResultadoRegistroRespuesta ejecutar(ComandoRegistrarRespuesta comando) {
        return transacciones.ejecutar(() -> {
            Evaluacion evaluacion = repositorioEvaluaciones.buscarPorId(comando.evaluacionId())
                    .orElseThrow(() -> new ExcepcionEvaluacionNoEncontrada("La evaluación no existe o no tienes acceso a ella."));

            if (!evaluacion.getUsuarioId().equals(comando.usuarioId())) {
                throw new ExcepcionEvaluacionNoEncontrada("La evaluación no existe o no tienes acceso a ella.");
            }

            if (evaluacion.getEstado() == EstadoEvaluacion.COMPLETADA || evaluacion.getEstado() == EstadoEvaluacion.ABANDONADA) {
                throw new ExcepcionDominio("No se pueden modificar respuestas en una evaluación cerrada o no disponible (estado: "
                        + evaluacion.getEstado() + ").");
            }

            Optional<RespuestaEvaluacion> respuestaExistente = repositorioRespuestas.buscarPorEvaluacionEInstrumentoEIndice(
                    comando.evaluacionId(),
                    comando.instrumento(),
                    comando.indicePregunta()
            );

            if (respuestaExistente.isPresent()) {
                RespuestaEvaluacion respuesta = respuestaExistente.get();
                respuesta.actualizarValor(comando.valorPuntaje());
                repositorioRespuestas.guardar(respuesta);
            } else {
                RespuestaEvaluacion nueva = RespuestaEvaluacion.nueva(
                        comando.evaluacionId(),
                        comando.instrumento(),
                        comando.indicePregunta(),
                        comando.valorPuntaje()
                );
                repositorioRespuestas.guardar(nueva);
            }

            // Deteccion inmediata de crisis en item 9 de PHQ-9
            if (comando.instrumento() == InstrumentoCribado.PHQ9
                    && comando.indicePregunta() == 9
                    && comando.valorPuntaje() >= 1) {
                evaluacion.marcarRiesgoCrisis();
                repositorioEvaluaciones.guardar(evaluacion);
            }

            return new ResultadoRegistroRespuesta(
                    evaluacion.getId(),
                    comando.instrumento(),
                    comando.indicePregunta(),
                    evaluacion.isRiesgoCrisis()
            );
        });
    }
}
