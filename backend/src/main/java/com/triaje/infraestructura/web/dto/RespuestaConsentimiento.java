package com.triaje.infraestructura.web.dto;

import com.triaje.aplicacion.dto.DatosConsentimiento;

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
    public static RespuestaConsentimiento desde(DatosConsentimiento datos) {
        return new RespuestaConsentimiento(
                datos.id(),
                datos.versionConsentimiento(),
                datos.versionPolitica(),
                datos.confirmaMayoriaEdad(),
                datos.fechaAceptacion(),
                datos.fechaRevocacion(),
                datos.vigente()
        );
    }
}
