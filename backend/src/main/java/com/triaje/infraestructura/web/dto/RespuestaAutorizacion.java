package com.triaje.infraestructura.web.dto;

import com.triaje.aplicacion.dto.DatosAutorizacion;

import java.time.Instant;

public record RespuestaAutorizacion(
        String id,
        String evaluacionId,
        Long clinicoId,
        String versionTextoAutorizacion,
        Instant fechaOtorgamiento,
        Instant fechaRevocacion,
        boolean vigente,
        String estado
) {
    public static RespuestaAutorizacion desde(DatosAutorizacion datos) {
        return new RespuestaAutorizacion(
                datos.id(),
                datos.evaluacionId(),
                datos.clinicoId(),
                datos.versionTextoAutorizacion(),
                datos.fechaOtorgamiento(),
                datos.fechaRevocacion(),
                datos.vigente(),
                datos.estado()
        );
    }
}
