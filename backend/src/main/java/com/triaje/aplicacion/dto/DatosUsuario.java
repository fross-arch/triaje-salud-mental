package com.triaje.aplicacion.dto;

import com.triaje.dominio.RolUsuario;
import com.triaje.dominio.modelo.Usuario;

public record DatosUsuario(Long id, String nombreCompleto, String correo, RolUsuario rol, boolean activo, String especialidad) {

    public DatosUsuario(Long id, String nombreCompleto, String correo, RolUsuario rol, boolean activo) {
        this(id, nombreCompleto, correo, rol, activo, null);
    }

    public DatosUsuario(Long id, String nombreCompleto, String correo, RolUsuario rol) {
        this(id, nombreCompleto, correo, rol, true, null);
    }

    public static DatosUsuario desde(Usuario usuario) {
        return new DatosUsuario(usuario.getId(), usuario.getNombreCompleto(), usuario.getCorreo(), usuario.getRol(), usuario.isActivo(), usuario.getEspecialidad());
    }

    @Override
    public String toString() {
        return "DatosUsuario{id=" + id + ", rol=" + rol + ", activo=" + activo + "}";
    }
}
