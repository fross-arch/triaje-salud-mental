package com.triaje.infraestructura.web.dto;

import com.triaje.aplicacion.dto.DatosUsuario;

public record RespuestaUsuario(Long id, String nombreCompleto, String correo, String rol, String especialidad) {

    public RespuestaUsuario(Long id, String nombreCompleto, String correo, String rol) {
        this(id, nombreCompleto, correo, rol, null);
    }

    public static RespuestaUsuario desde(DatosUsuario datos) {
        return new RespuestaUsuario(datos.id(), datos.nombreCompleto(), datos.correo(), datos.rol().name(), datos.especialidad());
    }

    @Override
    public String toString() {
        return "RespuestaUsuario{id=" + id + "}";
    }
}
