package com.triaje.aplicacion.casouso;

import com.triaje.aplicacion.dto.DatosUsuario;
import com.triaje.aplicacion.puerto.RepositorioUsuarios;
import com.triaje.dominio.RolUsuario;

import java.util.List;

/**
 * Caso de uso: el administrador lista todas las cuentas de rol CLINICO.
 */
public class ListarClinicos {

    private final RepositorioUsuarios repositorioUsuarios;

    public ListarClinicos(RepositorioUsuarios repositorioUsuarios) {
        this.repositorioUsuarios = repositorioUsuarios;
    }

    public List<DatosUsuario> ejecutar() {
        return repositorioUsuarios.listarPorRol(RolUsuario.CLINICO).stream()
                .map(DatosUsuario::desde)
                .toList();
    }
}
