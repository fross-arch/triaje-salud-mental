package com.triaje.aplicacion.casouso;

import com.triaje.aplicacion.puerto.EjecutorTransaccional;
import com.triaje.aplicacion.puerto.RepositorioAuditoria;
import com.triaje.aplicacion.puerto.RepositorioUsuarios;
import com.triaje.dominio.RolUsuario;
import com.triaje.dominio.excepcion.ExcepcionUsuarioNoEncontrado;
import com.triaje.dominio.modelo.AccionAuditoria;
import com.triaje.dominio.modelo.RegistroAuditoria;
import com.triaje.dominio.modelo.Usuario;

import java.time.Clock;
import java.util.Objects;

/**
 * Caso de uso: el administrador elimina la cuenta de un clinico.
 * La base de datos borra en cascada sus citas, autorizaciones y tokens de renovacion.
 */
public class EliminarClinico {

    private final RepositorioUsuarios repositorioUsuarios;
    private final RepositorioAuditoria repositorioAuditoria;
    private final EjecutorTransaccional transacciones;
    private final Clock reloj;

    public EliminarClinico(RepositorioUsuarios repositorioUsuarios,
                           RepositorioAuditoria repositorioAuditoria,
                           EjecutorTransaccional transacciones,
                           Clock reloj) {
        this.repositorioUsuarios = Objects.requireNonNull(repositorioUsuarios);
        this.repositorioAuditoria = Objects.requireNonNull(repositorioAuditoria);
        this.transacciones = Objects.requireNonNull(transacciones);
        this.reloj = Objects.requireNonNull(reloj);
    }

    public void ejecutar(Long adminId, Long clinicoId) {
        transacciones.ejecutarSinResultado(() -> {
            Usuario clinico = repositorioUsuarios.buscarPorId(clinicoId)
                    .orElseThrow(() -> new ExcepcionUsuarioNoEncontrado("El clinico no existe."));

            if (clinico.getRol() != RolUsuario.CLINICO) {
                throw new ExcepcionUsuarioNoEncontrado("El usuario especificado no es un clinico.");
            }

            repositorioUsuarios.eliminarPorId(clinicoId);

            repositorioAuditoria.registrar(new RegistroAuditoria(
                    adminId,
                    AccionAuditoria.CLINICO_ELIMINADO,
                    String.valueOf(clinicoId),
                    reloj.instant()
            ));
        });
    }
}
