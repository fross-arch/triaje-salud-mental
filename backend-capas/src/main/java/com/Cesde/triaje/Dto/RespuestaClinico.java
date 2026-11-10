package com.Cesde.triaje.Dto;

public record RespuestaClinico(
        Long id,
        String nombreCompleto,
        String correo,
        String rol,
        boolean activo,
        String especialidad
) {
    public RespuestaClinico(Long id, String nombreCompleto, String correo, String rol, boolean activo) {
        this(id, nombreCompleto, correo, rol, activo, null);
    }
}
