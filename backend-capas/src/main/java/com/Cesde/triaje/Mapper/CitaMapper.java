package com.Cesde.triaje.Mapper;

import com.Cesde.triaje.Dto.RespuestaCitaClinico;
import com.Cesde.triaje.Dto.RespuestaCitaPaciente;
import com.Cesde.triaje.Modelo.MCita;

public final class CitaMapper {

    private CitaMapper() {
    }

    public static RespuestaCitaPaciente aRespuestaCitaPaciente(MCita cita) {
        if (cita == null) return null;
        String nombreClinico = cita.getClinico() != null ? cita.getClinico().getNombreCompleto() : "Clínico";
        return new RespuestaCitaPaciente(
                cita.getId(),
                cita.getFechaHora(),
                cita.getModalidad(),
                cita.getMensaje(),
                cita.getEstado(),
                cita.getObservaciones(),
                nombreClinico
        );
    }

    public static RespuestaCitaClinico aRespuestaCitaClinico(MCita cita) {
        if (cita == null) return null;
        String nombrePaciente = cita.getPaciente() != null ? cita.getPaciente().getNombreCompleto() : "Paciente";
        return new RespuestaCitaClinico(
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
