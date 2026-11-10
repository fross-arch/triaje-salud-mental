package com.Cesde.triaje.Mapper;

import com.Cesde.triaje.Dto.RespuestaEvaluacionClinicoResumen;
import com.Cesde.triaje.Dto.RespuestaEvaluacionDetalle;
import com.Cesde.triaje.Dto.RespuestaEvaluacionResumen;
import com.Cesde.triaje.Dto.RespuestaItemDetalle;
import com.Cesde.triaje.Modelo.Enumeracion.EstadoEvaluacion;
import com.Cesde.triaje.Modelo.Enumeracion.SeveridadAnsiedad;
import com.Cesde.triaje.Modelo.Enumeracion.SeveridadDepresion;
import com.Cesde.triaje.Modelo.MEvaluacion;
import com.Cesde.triaje.Modelo.MRespuestaEvaluacion;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface IEvaluacionMapper {

    default boolean calcularResultadoMinimo(MEvaluacion evaluacion) {
        if (evaluacion == null) return false;
        if (evaluacion.isRiesgoCrisis()) return false;
        return evaluacion.getSeveridadDepresion() == SeveridadDepresion.MINIMA
                && evaluacion.getSeveridadAnsiedad() == SeveridadAnsiedad.MINIMA;
    }

    default Boolean obtenerResultadoMinimoResumen(MEvaluacion evaluacion) {
        if (evaluacion == null) return null;
        if (evaluacion.getEstado() == EstadoEvaluacion.COMPLETADA) {
            return calcularResultadoMinimo(evaluacion);
        }
        return null;
    }

    @Mapping(target = "resultadoMinimo", expression = "java(obtenerResultadoMinimoResumen(evaluacion))")
    RespuestaEvaluacionResumen aRespuestaEvaluacionResumen(MEvaluacion evaluacion);

    @Mapping(source = "id", target = "evaluacionId")
    @Mapping(source = "usuario.nombreCompleto", target = "nombrePaciente")
    @Mapping(target = "resultadoMinimo", expression = "java(calcularResultadoMinimo(evaluacion))")
    RespuestaEvaluacionClinicoResumen aRespuestaEvaluacionClinicoResumen(MEvaluacion evaluacion);

    RespuestaItemDetalle aRespuestaItemDetalle(MRespuestaEvaluacion respuesta);

    List<RespuestaItemDetalle> aListaRespuestaItemDetalle(List<MRespuestaEvaluacion> respuestas);

    default RespuestaEvaluacionDetalle aRespuestaEvaluacionDetalle(MEvaluacion evaluacion, List<MRespuestaEvaluacion> respuestas) {
        RespuestaEvaluacionResumen resumen = aRespuestaEvaluacionResumen(evaluacion);
        List<RespuestaItemDetalle> items = aListaRespuestaItemDetalle(respuestas);
        return new RespuestaEvaluacionDetalle(resumen, items);
    }

    List<RespuestaEvaluacionResumen> aListaRespuestaEvaluacionResumen(List<MEvaluacion> evaluaciones);

    List<RespuestaEvaluacionClinicoResumen> aListaRespuestaEvaluacionClinicoResumen(List<MEvaluacion> evaluaciones);
}
