package com.triaje.aplicacion.casouso;

import com.triaje.aplicacion.dto.ComandoActualizarClinico;
import com.triaje.aplicacion.dto.DatosUsuario;
import com.triaje.aplicacion.puerto.CodificadorContrasenas;
import com.triaje.aplicacion.puerto.EjecutorTransaccional;
import com.triaje.aplicacion.puerto.RepositorioAuditoria;
import com.triaje.aplicacion.puerto.RepositorioUsuarios;
import com.triaje.dominio.RolUsuario;
import com.triaje.dominio.excepcion.ExcepcionCorreoYaRegistrado;
import com.triaje.dominio.excepcion.ExcepcionUsuarioNoEncontrado;
import com.triaje.dominio.modelo.AccionAuditoria;
import com.triaje.dominio.modelo.RegistroAuditoria;
import com.triaje.dominio.modelo.Usuario;
import com.triaje.dominio.regla.PoliticaContrasena;

import java.time.Clock;
import java.util.Objects;

/**
 * Caso de uso: el administrador actualiza los datos de un clinico (nombre, correo, especialidad y opcionalmente restablece contrasena).
 */
public class ActualizarClinico {

    private final RepositorioUsuarios repositorioUsuarios;
    private final RepositorioAuditoria repositorioAuditoria;
    private final CodificadorContrasenas codificador;
    private final EjecutorTransaccional transacciones;
    private final Clock reloj;

    public ActualizarClinico(RepositorioUsuarios repositorioUsuarios,
                             RepositorioAuditoria repositorioAuditoria,
                             CodificadorContrasenas codificador,
                             EjecutorTransaccional transacciones,
                             Clock reloj) {
        this.repositorioUsuarios = Objects.requireNonNull(repositorioUsuarios);
        this.repositorioAuditoria = Objects.requireNonNull(repositorioAuditoria);
        this.codificador = codificador;
        this.transacciones = Objects.requireNonNull(transacciones);
        this.reloj = Objects.requireNonNull(reloj);
    }

    public ActualizarClinico(RepositorioUsuarios repositorioUsuarios,
                             RepositorioAuditoria repositorioAuditoria,
                             EjecutorTransaccional transacciones,
                             Clock reloj) {
        this(repositorioUsuarios, repositorioAuditoria, null, transacciones, reloj);
    }

    public DatosUsuario ejecutar(ComandoActualizarClinico comando) {
        return transacciones.ejecutar(() -> {
            Usuario clinico = repositorioUsuarios.buscarPorId(comando.clinicoId())
                    .orElseThrow(() -> new ExcepcionUsuarioNoEncontrado("El clinico no existe."));

            if (clinico.getRol() != RolUsuario.CLINICO) {
                throw new ExcepcionUsuarioNoEncontrado("El usuario especificado no es un clinico.");
            }

            String correoNormalizado = Usuario.normalizarCorreo(comando.correo());
            if (!correoNormalizado.equals(clinico.getCorreo()) && repositorioUsuarios.existePorCorreo(correoNormalizado)) {
                throw new ExcepcionCorreoYaRegistrado();
            }

            String nuevoHash = clinico.getContrasenaHash();
            if (comando.nuevaContrasena() != null && !comando.nuevaContrasena().isBlank()) {
                PoliticaContrasena.validar(comando.nuevaContrasena());
                if (codificador != null) {
                    nuevoHash = codificador.codificar(comando.nuevaContrasena());
                }
            }

            Usuario actualizado = clinico.conDatosClinico(comando.nombreCompleto(), correoNormalizado, comando.especialidad(), nuevoHash);
            Usuario guardado = repositorioUsuarios.guardar(actualizado);

            repositorioAuditoria.registrar(new RegistroAuditoria(
                    comando.adminId(),
                    AccionAuditoria.CLINICO_ACTUALIZADO,
                    String.valueOf(guardado.getId()),
                    reloj.instant()
            ));

            return DatosUsuario.desde(guardado);
        });
    }
}
