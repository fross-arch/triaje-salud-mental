package com.triaje.infraestructura.web.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

public record SolicitudConsentimiento(
        @NotNull(message = "La confirmación de mayoría de edad es obligatoria.")
        @AssertTrue(message = "Debes confirmar la mayoría de edad para otorgar consentimiento.")
        Boolean confirmaMayoriaEdad
) {
}
