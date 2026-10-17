package com.triaje.aplicacion.casouso;

import com.triaje.aplicacion.puerto.EjecutorTransaccional;
import com.triaje.aplicacion.puerto.RepositorioAuditoria;
import com.triaje.aplicacion.puerto.RepositorioUsuarios;
import com.triaje.dominio.excepcion.ExcepcionUsuarioNoEncontrado;
import com.triaje.dominio.modelo.AccionAuditoria;
import com.triaje.dominio.modelo.RegistroAuditoria;

import java.time.Clock;

/**
 * Elimina la cuenta del usuario y todos sus datos (derecho de supresión).
 * La base de datos borra en cascada consentimientos, evaluaciones, respuestas y tokens.
 * El evento CUENTA_ELIMINADA se registra sin usuario (la FK es ON DELETE SET NULL) y con el
 * identificador numérico eliminado en entidad_id, sin nombre ni correo.
 */
public class EliminarCuenta {

    private final RepositorioUsuarios repositorioUsuarios;
    private final RepositorioAuditoria repositorioAuditoria;
    private final EjecutorTransaccional transacciones;
    private final Clock reloj;

    public EliminarCuenta(RepositorioUsuarios repositorioUsuarios, RepositorioAuditoria repositorioAuditoria,
                          EjecutorTransaccional transacciones, Clock reloj) {
        this.repositorioUsuarios = repositorioUsuarios;
        this.repositorioAuditoria = repositorioAuditoria;
        this.transacciones = transacciones;
        this.reloj = reloj;
    }

    public void ejecutar(Long usuarioId) {
        transacciones.ejecutarSinResultado(() -> {
            if (repositorioUsuarios.buscarPorId(usuarioId).isEmpty()) {
                throw new ExcepcionUsuarioNoEncontrado();
            }
            repositorioUsuarios.eliminarPorId(usuarioId);
            repositorioAuditoria.registrar(new RegistroAuditoria(
                    null, AccionAuditoria.CUENTA_ELIMINADA, String.valueOf(usuarioId), reloj.instant()));
        });
    }
}
