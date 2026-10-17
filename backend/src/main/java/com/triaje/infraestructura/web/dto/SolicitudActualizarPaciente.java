package com.triaje.infraestructura.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SolicitudActualizarPaciente(
        @NotBlank(message = "El nombre completo es obligatorio.")
        @Size(max = 120, message = "El nombre completo no puede superar 120 caracteres.")
        String nombreCompleto,

        @NotBlank(message = "El correo es obligatorio.")
        @Email(message = "El correo no tiene un formato válido.")
        @Size(max = 120, message = "El correo no puede superar 120 caracteres.")
        String correo,

        String nuevaContrasena
) {
    public SolicitudActualizarPaciente(String nombreCompleto, String correo) {
        this(nombreCompleto, correo, null);
    }

    public SolicitudActualizarPaciente {
        if (nombreCompleto != null) {
            nombreCompleto = nombreCompleto.trim();
        }
        if (correo != null) {
            correo = correo.trim().toLowerCase(java.util.Locale.ROOT);
        }
    }
}
