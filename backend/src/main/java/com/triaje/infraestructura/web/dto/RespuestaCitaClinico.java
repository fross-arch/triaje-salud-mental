package com.triaje.infraestructura.web.dto;

import com.triaje.aplicacion.dto.DatosCita;
import com.triaje.aplicacion.dto.DatosCitaClinico;
import com.triaje.dominio.EstadoCita;
import com.triaje.dominio.ModalidadCita;

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
    public static RespuestaCitaClinico desde(DatosCitaClinico datos) {
        return new RespuestaCitaClinico(
                datos.id(),
                datos.fechaHora(),
                datos.modalidad(),
                datos.mensaje(),
                datos.estado(),
                datos.observaciones(),
                datos.nombrePaciente()
        );
    }

    public static RespuestaCitaClinico desde(DatosCita datos, String nombrePaciente) {
        return new RespuestaCitaClinico(
                datos.id(),
                datos.fechaHora(),
                datos.modalidad(),
                datos.mensaje(),
                datos.estado(),
                datos.observaciones(),
                nombrePaciente
        );
    }
}
