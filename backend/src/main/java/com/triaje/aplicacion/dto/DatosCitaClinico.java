package com.triaje.aplicacion.dto;

import com.triaje.dominio.Cita;
import com.triaje.dominio.EstadoCita;
import com.triaje.dominio.ModalidadCita;

import java.time.Instant;

public record DatosCitaClinico(
        String id,
        Instant fechaHora,
        ModalidadCita modalidad,
        String mensaje,
        EstadoCita estado,
        String observaciones,
        String nombrePaciente
) {
    public DatosCitaClinico(String id, Instant fechaHora, ModalidadCita modalidad, String mensaje, EstadoCita estado, String nombrePaciente) {
        this(id, fechaHora, modalidad, mensaje, estado, null, nombrePaciente);
    }

    public static DatosCitaClinico desde(Cita cita, String nombrePaciente) {
        return new DatosCitaClinico(
                cita.getId(),
                cita.getFechaHora(),
                cita.getModalidad(),
                cita.getMensaje(),
                cita.getEstado(),
                cita.getObservaciones(),
                nombrePaciente
        );
    }
}
