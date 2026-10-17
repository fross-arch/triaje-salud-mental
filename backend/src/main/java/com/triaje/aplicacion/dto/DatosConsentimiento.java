package com.triaje.aplicacion.dto;

import com.triaje.dominio.modelo.Consentimiento;

import java.time.Instant;

public record DatosConsentimiento(
        Long id,
        Long usuarioId,
        String versionConsentimiento,
        String versionPolitica,
        boolean confirmaMayoriaEdad,
        Instant fechaAceptacion,
        Instant fechaRevocacion,
        boolean vigente
) {
    public static DatosConsentimiento desde(Consentimiento consentimiento) {
        return new DatosConsentimiento(
                consentimiento.getId(),
                consentimiento.getUsuarioId(),
                consentimiento.getVersionConsentimiento(),
                consentimiento.getVersionPolitica(),
                consentimiento.isConfirmaMayoriaEdad(),
                consentimiento.getFechaAceptacion(),
                consentimiento.getFechaRevocacion(),
                consentimiento.estaVigente()
        );
    }
}
