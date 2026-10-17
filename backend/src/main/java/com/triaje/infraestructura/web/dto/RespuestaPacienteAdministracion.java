package com.triaje.infraestructura.web.dto;

import com.triaje.aplicacion.dto.DatosPacienteAdministracion;

import java.time.Instant;

public record RespuestaPacienteAdministracion(
        Long id,
        String nombre,
        String correo,
        Instant fechaRegistro,
        boolean activo
) {
    public static RespuestaPacienteAdministracion desde(DatosPacienteAdministracion datos) {
        return new RespuestaPacienteAdministracion(
                datos.id(),
                datos.nombre(),
                datos.correo(),
                datos.fechaRegistro(),
                datos.activo()
        );
    }
}
