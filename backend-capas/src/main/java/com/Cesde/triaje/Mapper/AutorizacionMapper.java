package com.Cesde.triaje.Mapper;

import com.Cesde.triaje.Dto.RespuestaAutorizacion;
import com.Cesde.triaje.Modelo.MAutorizacion;

public final class AutorizacionMapper {

    private AutorizacionMapper() {
    }

    public static RespuestaAutorizacion aRespuestaAutorizacion(MAutorizacion autorizacion) {
        if (autorizacion == null) return null;
        boolean vigente = autorizacion.estaVigente();
        String estado = vigente ? "AUTORIZADA" : "REVOCADA";
        String evalId = autorizacion.getEvaluacion() != null ? autorizacion.getEvaluacion().getId() : null;
        Long clinId = autorizacion.getClinico() != null ? autorizacion.getClinico().getId() : null;

        return new RespuestaAutorizacion(
                autorizacion.getId(),
                evalId,
                clinId,
                autorizacion.getVersionTextoAutorizacion(),
                autorizacion.getFechaOtorgamiento(),
                autorizacion.getFechaRevocacion(),
                vigente,
                estado
        );
    }
}
