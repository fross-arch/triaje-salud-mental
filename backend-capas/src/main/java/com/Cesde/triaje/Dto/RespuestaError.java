package com.Cesde.triaje.Dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record RespuestaError(
        Instant fecha,
        int estado,
        String error,
        String mensaje,
        String ruta,
        List<DetalleError> detalles
) {
    public record DetalleError(String campo, String mensaje) {
    }
}
