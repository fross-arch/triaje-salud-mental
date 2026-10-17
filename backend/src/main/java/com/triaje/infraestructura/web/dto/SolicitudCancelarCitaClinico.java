package com.triaje.infraestructura.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SolicitudCancelarCitaClinico(
        @Size(max = 1000, message = "Las observaciones no pueden superar 1000 caracteres.")
        String observaciones,

        @Size(max = 1000, message = "El motivo no puede superar 1000 caracteres.")
        String motivo
) {
    public String motivoEfectivo() {
        if (observaciones != null && !observaciones.isBlank()) {
            return observaciones.trim();
        }
        if (motivo != null && !motivo.isBlank()) {
            return motivo.trim();
        }
        return null;
    }
}
