package com.Cesde.triaje.Mapper;

import com.Cesde.triaje.Dto.RespuestaAutorizacion;
import com.Cesde.triaje.Modelo.MAutorizacion;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface IAutorizacionMapper {

    @Mapping(source = "evaluacion.id", target = "evaluacionId")
    @Mapping(source = "clinico.id", target = "clinicoId")
    @Mapping(target = "vigente", expression = "java(autorizacion.estaVigente())")
    @Mapping(target = "estado", expression = "java(autorizacion.estaVigente() ? \"VIGENTE\" : \"REVOCADA\")")
    RespuestaAutorizacion aRespuestaAutorizacion(MAutorizacion autorizacion);

    List<RespuestaAutorizacion> aListaRespuestaAutorizacion(List<MAutorizacion> autorizaciones);
}
