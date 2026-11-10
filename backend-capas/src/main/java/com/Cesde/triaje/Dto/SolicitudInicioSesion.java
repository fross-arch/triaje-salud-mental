package com.Cesde.triaje.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Locale;

public record SolicitudInicioSesion(
        @NotBlank(message = "El correo es obligatorio.")
        @Size(max = 120, message = "El correo no puede superar 120 caracteres.")
        String correo,

        @NotBlank(message = "La contraseña es obligatoria.")
        @Size(max = 72, message = "La contraseña no puede superar 72 caracteres.")
        String contrasena
) {
    public SolicitudInicioSesion {
        if (correo != null) {
            correo = correo.trim().toLowerCase(Locale.ROOT);
        }
    }
}
