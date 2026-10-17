package com.triaje.aplicacion.casouso;

import com.triaje.aplicacion.dto.DatosUsuario;
import com.triaje.aplicacion.puerto.RepositorioUsuarios;
import com.triaje.dominio.excepcion.ExcepcionUsuarioNoEncontrado;

public class ObtenerUsuarioActual {

    private final RepositorioUsuarios repositorioUsuarios;

    public ObtenerUsuarioActual(RepositorioUsuarios repositorioUsuarios) {
        this.repositorioUsuarios = repositorioUsuarios;
    }

    public DatosUsuario ejecutar(Long usuarioId) {
        return repositorioUsuarios.buscarPorId(usuarioId)
                .map(DatosUsuario::desde)
                .orElseThrow(ExcepcionUsuarioNoEncontrado::new);
    }
}
