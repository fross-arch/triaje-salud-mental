package com.triaje.infraestructura.web.dto;

import com.triaje.dominio.ModalidadCita;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record SolicitudProponerCita(
        @NotBlank(message = "El identificador de la evaluacion es obligatorio.")
        String evaluacionId,

        @NotNull(message = "La fecha y hora de la cita es obligatoria.")
        Instant fechaHora,

        @NotNull(message = "La modalidad de la cita es obligatoria.")
        ModalidadCita modalidad,

        @Size(max = 300, message = "El mensaje no puede superar los 300 caracteres.")
        String mensaje
) {
}
