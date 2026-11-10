package com.Cesde.triaje.Dto;

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
}
