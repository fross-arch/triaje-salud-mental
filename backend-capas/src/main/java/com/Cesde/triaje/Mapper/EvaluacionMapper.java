package com.Cesde.triaje.Mapper;

import com.Cesde.triaje.Dto.RespuestaEvaluacionDetalle;
import com.Cesde.triaje.Dto.RespuestaEvaluacionResumen;
import com.Cesde.triaje.Dto.RespuestaItemDetalle;
import com.Cesde.triaje.Modelo.MEvaluacion;
import com.Cesde.triaje.Modelo.MRespuestaEvaluacion;
import com.Cesde.triaje.Modelo.Enumeracion.SeveridadAnsiedad;
import com.Cesde.triaje.Modelo.Enumeracion.SeveridadDepresion;

import java.util.List;

public final class EvaluacionMapper {

    private EvaluacionMapper() {
    }

    public static boolean calcularResultadoMinimo(MEvaluacion evaluacion) {
        if (evaluacion == null) return false;
        if (evaluacion.isRiesgoCrisis()) return false;
        return evaluacion.getSeveridadDepresion() == SeveridadDepresion.MINIMA
                && evaluacion.getSeveridadAnsiedad() == SeveridadAnsiedad.MINIMA;
    }

    public static RespuestaEvaluacionResumen aRespuestaEvaluacionResumen(MEvaluacion evaluacion) {
        if (evaluacion == null) return null;
        Boolean resultadoMinimo = null;
        if (evaluacion.getEstado() == com.Cesde.triaje.Modelo.Enumeracion.EstadoEvaluacion.COMPLETADA) {
            resultadoMinimo = calcularResultadoMinimo(evaluacion);
        }

        return new RespuestaEvaluacionResumen(
                evaluacion.getId(),
                evaluacion.getEstado(),
                evaluacion.getPuntajePhq9(),
                evaluacion.getPuntajeGad7(),
                evaluacion.getSeveridadDepresion(),
                evaluacion.getSeveridadAnsiedad(),
                evaluacion.isRiesgoCrisis(),
                resultadoMinimo,
                evaluacion.getFechaCreacion(),
                evaluacion.getFechaFinalizacion()
        );
    }

    public static RespuestaEvaluacionDetalle aRespuestaEvaluacionDetalle(
            MEvaluacion evaluacion, List<MRespuestaEvaluacion> respuestas) {
        RespuestaEvaluacionResumen resumen = aRespuestaEvaluacionResumen(evaluacion);
        List<RespuestaItemDetalle> items = respuestas == null ? List.of() : respuestas.stream()
                .map(r -> new RespuestaItemDetalle(r.getInstrumento(), r.getIndicePregunta(), r.getValorPuntaje()))
                .toList();

        return new RespuestaEvaluacionDetalle(resumen, items);
    }
}
