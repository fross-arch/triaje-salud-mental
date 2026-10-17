package com.triaje.infraestructura.web.dto;

import com.triaje.aplicacion.dto.DatosUsuario;

public record RespuestaClinico(Long id, String nombreCompleto, String correo, String rol, boolean activo, String especialidad) {

    public RespuestaClinico(Long id, String nombreCompleto, String correo, String rol, boolean activo) {
        this(id, nombreCompleto, correo, rol, activo, null);
    }

    public static RespuestaClinico desde(DatosUsuario datos) {
        return new RespuestaClinico(
                datos.id(),
                datos.nombreCompleto(),
                datos.correo(),
                datos.rol().name(),
                datos.activo(),
                datos.especialidad()
        );
    }

    @Override
    public String toString() {
        return "RespuestaClinico{id=" + id + ", rol=" + rol + ", activo=" + activo + "}";
    }
}
