package com.triaje.aplicacion.casouso;

import com.triaje.aplicacion.dto.DatosClinicoActivo;
import com.triaje.aplicacion.puerto.RepositorioUsuarios;
import com.triaje.dominio.RolUsuario;

import java.util.List;
import java.util.Objects;

/**
 * Caso de uso: el paciente lista los clinicos activos para poder otorgar autorizaciones.
 * Devuelve unicamente el identificador y el nombre completo.
 */
public class ListarClinicosActivos {

    private final RepositorioUsuarios repositorioUsuarios;

    public ListarClinicosActivos(RepositorioUsuarios repositorioUsuarios) {
        this.repositorioUsuarios = Objects.requireNonNull(repositorioUsuarios);
    }

    public List<DatosClinicoActivo> ejecutar() {
        return repositorioUsuarios.listarPorRol(RolUsuario.CLINICO).stream()
                .filter(u -> u.isActivo())
                .map(u -> new DatosClinicoActivo(u.getId(), u.getNombreCompleto(), u.getEspecialidad()))
                .toList();
    }
}
