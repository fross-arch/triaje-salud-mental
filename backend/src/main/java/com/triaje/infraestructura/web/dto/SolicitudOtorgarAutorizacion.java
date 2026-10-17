package com.triaje.infraestructura.web.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

public record SolicitudOtorgarAutorizacion(
        @NotNull(message = "El identificador del clinico es obligatorio.")
        Long clinicoId,

        @NotNull(message = "Debes confirmar la autorizacion.")
        @AssertTrue(message = "Debes confirmar la autorizacion para otorgar el acceso.")
        Boolean confirmaAutorizacion
) {
}
