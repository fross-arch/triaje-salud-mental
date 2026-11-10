package com.Cesde.triaje.Dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SolicitudCumplirCita(
        @NotBlank(message = "Las observaciones clínicas son obligatorias.")
        @Size(max = 1000, message = "Las observaciones no pueden superar 1000 caracteres.")
        String observaciones
) {
    public SolicitudCumplirCita {
        if (observaciones != null) {
            observaciones = observaciones.trim();
        }
    }
}
