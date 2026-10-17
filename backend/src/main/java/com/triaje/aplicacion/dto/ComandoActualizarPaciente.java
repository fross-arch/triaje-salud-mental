package com.triaje.aplicacion.dto;

public record ComandoActualizarPaciente(
        Long adminId,
        Long pacienteId,
        String nombreCompleto,
        String correo,
        String nuevaContrasena
) {
    public ComandoActualizarPaciente(Long adminId, Long pacienteId, String nombreCompleto, String correo) {
        this(adminId, pacienteId, nombreCompleto, correo, null);
    }
}
