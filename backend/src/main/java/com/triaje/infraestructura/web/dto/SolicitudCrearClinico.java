package com.triaje.infraestructura.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SolicitudCrearClinico(
        @NotBlank(message = "El nombre completo es obligatorio.")
        @Size(max = 120, message = "El nombre completo no puede superar 120 caracteres.")
        String nombreCompleto,

        @NotBlank(message = "El correo es obligatorio.")
        @Email(message = "El correo no tiene un formato válido.")
        @Size(max = 120, message = "El correo no puede superar 120 caracteres.")
        String correo,

        @NotBlank(message = "La contraseña inicial es obligatoria.")
        @Size(min = 10, message = "La contraseña debe tener al menos 10 caracteres.")
        @Size(max = 72, message = "La contraseña no puede superar 72 caracteres.")
        String contrasenaInicial,

        @Size(max = 100, message = "La especialidad no puede superar 100 caracteres.")
        String especialidad) {

    public SolicitudCrearClinico {
        if (nombreCompleto != null) {
            nombreCompleto = nombreCompleto.trim();
        }
        if (correo != null) {
            correo = correo.trim().toLowerCase(java.util.Locale.ROOT);
        }
        if (especialidad != null) {
            especialidad = especialidad.trim();
        }
    }

    @Override
    public String toString() {
        return "SolicitudCrearClinico{datos ocultos}";
    }
}
