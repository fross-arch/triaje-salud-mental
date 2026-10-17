package com.triaje.aplicacion.dto;

import com.triaje.dominio.Cita;
import com.triaje.dominio.EstadoCita;
import com.triaje.dominio.ModalidadCita;

import java.time.Instant;

public record DatosCita(
        String id,
        String autorizacionId,
        Long pacienteId,
        Long clinicoId,
        Instant fechaHora,
        ModalidadCita modalidad,
        String mensaje,
        EstadoCita estado,
        String observaciones,
        Instant fechaCreacion,
        Instant fechaActualizacion
) {
    public DatosCita(String id, String autorizacionId, Long pacienteId, Long clinicoId,
                     Instant fechaHora, ModalidadCita modalidad, String mensaje,
                     EstadoCita estado, Instant fechaCreacion, Instant fechaActualizacion) {
        this(id, autorizacionId, pacienteId, clinicoId, fechaHora, modalidad, mensaje, estado, null, fechaCreacion, fechaActualizacion);
    }

    public static DatosCita desde(Cita cita) {
        return new DatosCita(
                cita.getId(),
                cita.getAutorizacionId(),
                cita.getPacienteId(),
                cita.getClinicoId(),
                cita.getFechaHora(),
                cita.getModalidad(),
                cita.getMensaje(),
                cita.getEstado(),
                cita.getObservaciones(),
                cita.getFechaCreacion(),
                cita.getFechaActualizacion()
        );
    }
}
