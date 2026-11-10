package com.Cesde.triaje.Dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Locale;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SolicitudRegistro(
        @NotBlank(message = "El nombre completo es obligatorio.")
        @Size(max = 120, message = "El nombre completo no puede superar 120 caracteres.")
        String nombreCompleto,

        @NotBlank(message = "El correo es obligatorio.")
        @Email(message = "El correo no tiene un formato válido.")
        @Size(max = 120, message = "El correo no puede superar 120 caracteres.")
        String correo,

        @NotBlank(message = "La contraseña es obligatoria.")
        @Size(min = 10, message = "La contraseña debe tener al menos 10 caracteres")
        @Size(max = 72, message = "La contraseña no puede superar 72 caracteres.")
        String contrasena
) {
    public SolicitudRegistro {
        if (nombreCompleto != null) {
            nombreCompleto = nombreCompleto.trim();
        }
        if (correo != null) {
            correo = correo.trim().toLowerCase(Locale.ROOT);
        }
    }
}
