package com.Cesde.triaje.Dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SolicitudPosponerCita(
        @NotNull(message = "La nueva fecha y hora es obligatoria.")
        Instant nuevaFechaHora,

        @Size(max = 300, message = "El mensaje no puede superar 300 caracteres.")
        String mensaje,

        @Size(max = 1000, message = "Las observaciones no pueden superar 1000 caracteres.")
        String observaciones
) {
    public String mensajeEfectivo() {
        if (mensaje != null && !mensaje.isBlank()) {
            return mensaje.trim();
        }
        if (observaciones != null && !observaciones.isBlank()) {
            return observaciones.trim();
        }
        return null;
    }
}
