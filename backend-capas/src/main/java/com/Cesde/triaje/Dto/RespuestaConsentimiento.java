package com.Cesde.triaje.Dto;

import java.time.Instant;

public record RespuestaConsentimiento(
        Long id,
        String versionConsentimiento,
        String versionPolitica,
        boolean confirmaMayoriaEdad,
        Instant fechaAceptacion,
        Instant fechaRevocacion,
        boolean vigente
) {
}
