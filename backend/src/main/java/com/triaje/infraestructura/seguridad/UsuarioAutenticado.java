package com.triaje.infraestructura.seguridad;

import com.triaje.dominio.RolUsuario;
import com.triaje.dominio.modelo.Rol;

/**
 * Principal de Spring Security: solo identificador y rol, sin datos personales.
 */
public record UsuarioAutenticado(Long id, RolUsuario rol) {

    public UsuarioAutenticado(Long id, Rol rol) {
        this(id, RolUsuario.valueOf(rol.name()));
    }
}
