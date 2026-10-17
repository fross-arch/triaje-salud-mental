package com.triaje.infraestructura.web.dto;

import com.triaje.aplicacion.dto.DatosConsentimiento;

import java.time.Instant;

public record RespuestaConsentimientoVigente(
        boolean vigente,
        Instant fechaAceptacion,
        String versionConsentimiento
) {
    public static RespuestaConsentimientoVigente activo(DatosConsentimiento datos) {
        return new RespuestaConsentimientoVigente(true, datos.fechaAceptacion(), datos.versionConsentimiento());
    }

    public static RespuestaConsentimientoVigente inactivo() {
        return new RespuestaConsentimientoVigente(false, null, null);
    }
}
