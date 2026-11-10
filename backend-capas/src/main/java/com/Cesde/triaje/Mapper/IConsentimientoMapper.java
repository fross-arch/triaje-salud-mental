package com.Cesde.triaje.Mapper;

import com.Cesde.triaje.Dto.RespuestaConsentimiento;
import com.Cesde.triaje.Dto.RespuestaConsentimientoVigente;
import com.Cesde.triaje.Modelo.MConsentimiento;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface IConsentimientoMapper {

    @Mapping(target = "vigente", expression = "java(consentimiento.estaVigente())")
    RespuestaConsentimiento aRespuestaConsentimiento(MConsentimiento consentimiento);

    @Mapping(target = "vigente", expression = "java(consentimiento.estaVigente())")
    RespuestaConsentimientoVigente aRespuestaConsentimientoVigente(MConsentimiento consentimiento);

    List<RespuestaConsentimiento> aListaRespuestaConsentimiento(List<MConsentimiento> consentimientos);
}
