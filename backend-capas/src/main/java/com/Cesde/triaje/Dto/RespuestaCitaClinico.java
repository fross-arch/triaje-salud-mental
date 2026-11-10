package com.Cesde.triaje.Dto;

import com.Cesde.triaje.Modelo.Enumeracion.EstadoCita;
import com.Cesde.triaje.Modelo.Enumeracion.ModalidadCita;

import java.time.Instant;

public record RespuestaCitaClinico(
        String id,
        Instant fechaHora,
        ModalidadCita modalidad,
        String mensaje,
        EstadoCita estado,
        String observaciones,
        String nombrePaciente
) {
}
