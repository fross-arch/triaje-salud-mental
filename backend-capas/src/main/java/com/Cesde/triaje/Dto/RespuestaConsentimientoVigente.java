package com.Cesde.triaje.Dto;

import java.time.Instant;

public record RespuestaConsentimientoVigente(
        boolean vigente,
        Instant fechaAceptacion,
        String versionConsentimiento
) {
    public static RespuestaConsentimientoVigente activo(Instant fechaAceptacion, String versionConsentimiento) {
        return new RespuestaConsentimientoVigente(true, fechaAceptacion, versionConsentimiento);
    }

    public static RespuestaConsentimientoVigente inactivo() {
        return new RespuestaConsentimientoVigente(false, null, null);
    }
}
