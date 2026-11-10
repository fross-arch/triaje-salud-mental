package com.Cesde.triaje.Dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Locale;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SolicitudActualizarPerfil(
        @NotBlank(message = "El nombre completo es obligatorio.")
        @Size(max = 120, message = "El nombre completo no puede superar 120 caracteres.")
        String nombreCompleto,

        @NotBlank(message = "El correo es obligatorio.")
        @Email(message = "El correo no tiene un formato válido.")
        @Size(max = 120, message = "El correo no puede superar 120 caracteres.")
        String correo,

        @Size(max = 100, message = "La especialidad no puede superar 100 caracteres.")
        String especialidad,

        String contrasenaActual,

        String nuevaContrasena
) {
    public SolicitudActualizarPerfil {
        if (nombreCompleto != null) {
            nombreCompleto = nombreCompleto.trim();
        }
        if (correo != null) {
            correo = correo.trim().toLowerCase(Locale.ROOT);
        }
        if (especialidad != null) {
            especialidad = especialidad.trim();
        }
    }
}
