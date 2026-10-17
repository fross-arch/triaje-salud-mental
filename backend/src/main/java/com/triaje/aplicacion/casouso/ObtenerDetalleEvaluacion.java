package com.triaje.aplicacion.casouso;

import com.triaje.aplicacion.dto.DatosEvaluacion;
import com.triaje.aplicacion.dto.DatosRespuestaEvaluacion;
import com.triaje.aplicacion.dto.DetalleEvaluacion;
import com.triaje.aplicacion.puerto.RepositorioEvaluaciones;
import com.triaje.aplicacion.puerto.RepositorioRespuestasEvaluacion;
import com.triaje.dominio.ReglaResultadoMinimo;
import com.triaje.dominio.excepcion.ExcepcionEvaluacionNoEncontrada;
import com.triaje.dominio.modelo.EstadoEvaluacion;
import com.triaje.dominio.modelo.Evaluacion;
import com.triaje.dominio.modelo.RespuestaEvaluacion;

import java.util.List;
import java.util.Objects;

/**
 * Obtiene el detalle de una evaluacion incluyendo sus respuestas y resultado minimo calculado.
 * Valida la propiedad del recurso para evitar accesos indebidos de otros usuarios.
 */
public class ObtenerDetalleEvaluacion {

    private final RepositorioEvaluaciones repositorioEvaluaciones;
    private final RepositorioRespuestasEvaluacion repositorioRespuestas;
    private final ReglaResultadoMinimo reglaResultadoMinimo;

    public ObtenerDetalleEvaluacion(RepositorioEvaluaciones repositorioEvaluaciones,
                                    RepositorioRespuestasEvaluacion repositorioRespuestas,
                                    ReglaResultadoMinimo reglaResultadoMinimo) {
        this.repositorioEvaluaciones = Objects.requireNonNull(repositorioEvaluaciones);
        this.repositorioRespuestas = Objects.requireNonNull(repositorioRespuestas);
        this.reglaResultadoMinimo = Objects.requireNonNull(reglaResultadoMinimo);
    }

    public DetalleEvaluacion ejecutar(String evaluacionId, Long usuarioId) {
        Evaluacion evaluacion = repositorioEvaluaciones.buscarPorId(evaluacionId)
                .orElseThrow(() -> new ExcepcionEvaluacionNoEncontrada("La evaluación no existe o no tienes acceso a ella."));

        if (!evaluacion.getUsuarioId().equals(usuarioId)) {
            throw new ExcepcionEvaluacionNoEncontrada("La evaluación no existe o no tienes acceso a ella.");
        }

        List<RespuestaEvaluacion> respuestas = repositorioRespuestas.buscarPorEvaluacionId(evaluacionId);
        List<DatosRespuestaEvaluacion> respuestasDto = respuestas.stream()
                .map(DatosRespuestaEvaluacion::desde)
                .toList();

        Boolean resultadoMinimo = null;
        if (evaluacion.getEstado() == EstadoEvaluacion.COMPLETADA
                && evaluacion.getSeveridadDepresion() != null
                && evaluacion.getSeveridadAnsiedad() != null) {
            resultadoMinimo = reglaResultadoMinimo.evaluar(
                    evaluacion.getSeveridadDepresion(),
                    evaluacion.getSeveridadAnsiedad(),
                    evaluacion.isRiesgoCrisis()
            );
        }

        return new DetalleEvaluacion(DatosEvaluacion.desde(evaluacion, resultadoMinimo), respuestasDto);
    }
}
