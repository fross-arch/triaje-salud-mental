package com.triaje.aplicacion.dto;

public record ComandoActualizarPerfil(
        Long usuarioId,
        String nombreCompleto,
        String correo,
        String especialidad,
        String contrasenaActual,
        String nuevaContrasena
) {
    public ComandoActualizarPerfil(Long usuarioId, String nombreCompleto, String correo) {
        this(usuarioId, nombreCompleto, correo, null, null, null);
    }
}
