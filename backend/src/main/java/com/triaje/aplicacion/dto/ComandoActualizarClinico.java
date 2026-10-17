package com.triaje.aplicacion.dto;

public record ComandoActualizarClinico(
        Long adminId,
        Long clinicoId,
        String nombreCompleto,
        String correo,
        String especialidad,
        String nuevaContrasena
) {
    public ComandoActualizarClinico(Long adminId, Long clinicoId, String nombreCompleto, String correo, String especialidad) {
        this(adminId, clinicoId, nombreCompleto, correo, especialidad, null);
    }
}
