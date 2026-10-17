package com.triaje.aplicacion.casouso;

import com.triaje.aplicacion.dto.DatosUsuario;
import com.triaje.aplicacion.puerto.EjecutorTransaccional;
import com.triaje.aplicacion.puerto.RepositorioAuditoria;
import com.triaje.aplicacion.puerto.RepositorioTokensRenovacion;
import com.triaje.aplicacion.puerto.RepositorioUsuarios;
import com.triaje.dominio.RolUsuario;
import com.triaje.dominio.excepcion.ExcepcionUsuarioNoEncontrado;
import com.triaje.dominio.modelo.AccionAuditoria;
import com.triaje.dominio.modelo.RegistroAuditoria;
import com.triaje.dominio.modelo.Usuario;

import java.time.Clock;

/**
 * Caso de uso: el administrador activa o desactiva una cuenta con rol CLINICO.
 * Registra la acción CLINICO_ACTIVADO o CLINICO_DESACTIVADO en auditoría.
 */
public class CambiarEstadoClinico {

    private final RepositorioUsuarios repositorioUsuarios;
    private final RepositorioAuditoria repositorioAuditoria;
    private final RepositorioTokensRenovacion repositorioTokens;
    private final EjecutorTransaccional transacciones;
    private final Clock reloj;

    public CambiarEstadoClinico(RepositorioUsuarios repositorioUsuarios,
                                RepositorioAuditoria repositorioAuditoria,
                                RepositorioTokensRenovacion repositorioTokens,
                                EjecutorTransaccional transacciones,
                                Clock reloj) {
        this.repositorioUsuarios = repositorioUsuarios;
        this.repositorioAuditoria = repositorioAuditoria;
        this.repositorioTokens = repositorioTokens;
        this.transacciones = transacciones;
        this.reloj = reloj;
    }

    public DatosUsuario ejecutar(Long actorId, Long clinicoId, boolean nuevoEstado) {
        return transacciones.ejecutar(() -> {
            Usuario usuario = repositorioUsuarios.buscarPorId(clinicoId)
                    .orElseThrow(() -> new ExcepcionUsuarioNoEncontrado("El usuario clínico no fue encontrado."));

            if (usuario.getRol() != RolUsuario.CLINICO) {
                throw new ExcepcionUsuarioNoEncontrado("El usuario especificado no es una cuenta clínica.");
            }

            Usuario actualizado = usuario.conActivo(nuevoEstado);
            repositorioUsuarios.guardar(actualizado);

            if (!nuevoEstado) {
                repositorioTokens.revocarTodosDelUsuario(clinicoId, reloj.instant());
            }

            AccionAuditoria accion = nuevoEstado ? AccionAuditoria.CLINICO_ACTIVADO : AccionAuditoria.CLINICO_DESACTIVADO;
            repositorioAuditoria.registrar(new RegistroAuditoria(
                    actorId,
                    accion,
                    String.valueOf(clinicoId),
                    reloj.instant()
            ));

            return DatosUsuario.desde(actualizado);
        });
    }
}
