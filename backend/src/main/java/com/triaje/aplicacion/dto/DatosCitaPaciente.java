package com.triaje.aplicacion.dto;

import com.triaje.dominio.Cita;
import com.triaje.dominio.EstadoCita;
import com.triaje.dominio.ModalidadCita;

import java.time.Instant;

public record DatosCitaPaciente(
        String id,
        Instant fechaHora,
        ModalidadCita modalidad,
        String mensaje,
        EstadoCita estado,
        String observaciones,
        String nombreClinico
) {
    public DatosCitaPaciente(String id, Instant fechaHora, ModalidadCita modalidad, String mensaje, EstadoCita estado, String nombreClinico) {
        this(id, fechaHora, modalidad, mensaje, estado, null, nombreClinico);
    }

    public static DatosCitaPaciente desde(Cita cita, String nombreClinico) {
        return new DatosCitaPaciente(
                cita.getId(),
                cita.getFechaHora(),
                cita.getModalidad(),
                cita.getMensaje(),
                cita.getEstado(),
                cita.getObservaciones(),
                nombreClinico
        );
    }
}
