package com.triaje.aplicacion.casouso;

import com.triaje.aplicacion.dto.ComandoActualizarPaciente;
import com.triaje.aplicacion.dto.DatosPacienteAdministracion;
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
 * Caso de uso: el administrador actualiza los datos básicos de un paciente (nombre, correo)
 * y opcionalmente restablece su contraseña, respetando el principio de minimización.
 */
public class ActualizarPaciente {

    private final RepositorioUsuarios repositorioUsuarios;
    private final RepositorioAuditoria repositorioAuditoria;
    private final CodificadorContrasenas codificador;
    private final EjecutorTransaccional transacciones;
    private final Clock reloj;

    public ActualizarPaciente(RepositorioUsuarios repositorioUsuarios,
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

    public ActualizarPaciente(RepositorioUsuarios repositorioUsuarios,
                              RepositorioAuditoria repositorioAuditoria,
                              EjecutorTransaccional transacciones,
                              Clock reloj) {
        this(repositorioUsuarios, repositorioAuditoria, null, transacciones, reloj);
    }

    public DatosPacienteAdministracion ejecutar(ComandoActualizarPaciente comando) {
        return transacciones.ejecutar(() -> {
            Usuario paciente = repositorioUsuarios.buscarPorId(comando.pacienteId())
                    .orElseThrow(() -> new ExcepcionUsuarioNoEncontrado("El paciente no existe."));

            if (paciente.getRol() != RolUsuario.PACIENTE) {
                throw new ExcepcionUsuarioNoEncontrado("El usuario especificado no es un paciente.");
            }

            String correoNormalizado = Usuario.normalizarCorreo(comando.correo());
            if (!correoNormalizado.equals(paciente.getCorreo()) && repositorioUsuarios.existePorCorreo(correoNormalizado)) {
                throw new ExcepcionCorreoYaRegistrado();
            }

            String nuevoHash = paciente.getContrasenaHash();
            if (comando.nuevaContrasena() != null && !comando.nuevaContrasena().isBlank()) {
                PoliticaContrasena.validar(comando.nuevaContrasena());
                if (codificador != null) {
                    nuevoHash = codificador.codificar(comando.nuevaContrasena());
                }
            }

            Usuario actualizado = paciente.conDatosPaciente(comando.nombreCompleto(), correoNormalizado, nuevoHash);
            Usuario guardado = repositorioUsuarios.guardar(actualizado);

            repositorioAuditoria.registrar(new RegistroAuditoria(
                    comando.adminId(),
                    AccionAuditoria.PACIENTE_ACTUALIZADO,
                    String.valueOf(guardado.getId()),
                    reloj.instant()
            ));

            return DatosPacienteAdministracion.desde(guardado);
        });
    }
}
