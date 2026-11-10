package com.Cesde.triaje.Dto;

public record RespuestaUsuario(
        Long id,
        String nombreCompleto,
        String correo,
        String rol,
        String especialidad
) {
    public RespuestaUsuario(Long id, String nombreCompleto, String correo, String rol) {
        this(id, nombreCompleto, correo, rol, null);
    }
}
