package com.Cesde.triaje.Mapper;

import com.Cesde.triaje.Dto.RespuestaItemDetalle;
import com.Cesde.triaje.Dto.RespuestaRegistroItem;
import com.Cesde.triaje.Modelo.MRespuestaEvaluacion;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface IRespuestaEvaluacionMapper {

    RespuestaItemDetalle aRespuestaItemDetalle(MRespuestaEvaluacion respuesta);

    @Mapping(source = "evaluacion.id", target = "evaluacionId")
    @Mapping(source = "evaluacion.riesgoCrisis", target = "riesgoCrisis")
    RespuestaRegistroItem aRespuestaRegistroItem(MRespuestaEvaluacion respuesta);

    List<RespuestaItemDetalle> aListaRespuestaItemDetalle(List<MRespuestaEvaluacion> respuestas);
}
