package com.Cesde.triaje.Dto;

import java.time.Instant;

public record RespuestaPacienteAdministracion(
        Long id,
        String nombre,
        String correo,
        Instant fechaRegistro,
        boolean activo
) {
}
