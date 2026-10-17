package com.triaje.infraestructura.web.dto;

import com.triaje.aplicacion.dto.DatosCita;
import com.triaje.aplicacion.dto.DatosCitaPaciente;
import com.triaje.dominio.EstadoCita;
import com.triaje.dominio.ModalidadCita;

import java.time.Instant;

public record RespuestaCitaPaciente(
        String id,
        Instant fechaHora,
        ModalidadCita modalidad,
        String mensaje,
        EstadoCita estado,
        String observaciones,
        String nombreClinico
) {
    public static RespuestaCitaPaciente desde(DatosCitaPaciente datos) {
        return new RespuestaCitaPaciente(
                datos.id(),
                datos.fechaHora(),
                datos.modalidad(),
                datos.mensaje(),
                datos.estado(),
                datos.observaciones(),
                datos.nombreClinico()
        );
    }

    public static RespuestaCitaPaciente desde(DatosCita datos, String nombreClinico) {
        return new RespuestaCitaPaciente(
                datos.id(),
                datos.fechaHora(),
                datos.modalidad(),
                datos.mensaje(),
                datos.estado(),
                datos.observaciones(),
                nombreClinico
        );
    }
}
