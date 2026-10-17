package com.triaje.aplicacion.dto;

import com.triaje.dominio.modelo.Usuario;

import java.time.Instant;

public record DatosPacienteAdministracion(
        Long id,
        String nombre,
        String correo,
        Instant fechaRegistro,
        boolean activo
) {
    public static DatosPacienteAdministracion desde(Usuario usuario) {
        return new DatosPacienteAdministracion(
                usuario.getId(),
                usuario.getNombreCompleto(),
                usuario.getCorreo(),
                usuario.getFechaCreacion(),
                usuario.isActivo()
        );
    }
}
