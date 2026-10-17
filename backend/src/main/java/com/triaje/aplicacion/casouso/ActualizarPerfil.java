package com.triaje.aplicacion.casouso;

import com.triaje.aplicacion.dto.ComandoActualizarPerfil;
import com.triaje.aplicacion.dto.DatosUsuario;
import com.triaje.aplicacion.puerto.CodificadorContrasenas;
import com.triaje.aplicacion.puerto.EjecutorTransaccional;
import com.triaje.aplicacion.puerto.RepositorioAuditoria;
import com.triaje.aplicacion.puerto.RepositorioUsuarios;
import com.triaje.dominio.RolUsuario;
import com.triaje.dominio.excepcion.ExcepcionCorreoYaRegistrado;
import com.triaje.dominio.excepcion.ExcepcionDatoInvalido;
import com.triaje.dominio.excepcion.ExcepcionUsuarioNoEncontrado;
import com.triaje.dominio.modelo.AccionAuditoria;
import com.triaje.dominio.modelo.RegistroAuditoria;
import com.triaje.dominio.modelo.Usuario;
import com.triaje.dominio.regla.PoliticaContrasena;

import java.time.Clock;
import java.util.Objects;

/**
 * Caso de uso: Cualquier usuario autenticado actualiza sus propios datos personales
 * y opcionalmente su contraseña bajo estrictas medidas de seguridad y auditoría sin PII.
 */
public class ActualizarPerfil {

    private final RepositorioUsuarios repositorioUsuarios;
    private final RepositorioAuditoria repositorioAuditoria;
    private final CodificadorContrasenas codificador;
    private final EjecutorTransaccional transacciones;
    private final Clock reloj;

    public ActualizarPerfil(RepositorioUsuarios repositorioUsuarios,
                            RepositorioAuditoria repositorioAuditoria,
                            CodificadorContrasenas codificador,
                            EjecutorTransaccional transacciones,
                            Clock reloj) {
        this.repositorioUsuarios = Objects.requireNonNull(repositorioUsuarios);
        this.repositorioAuditoria = Objects.requireNonNull(repositorioAuditoria);
        this.codificador = Objects.requireNonNull(codificador);
        this.transacciones = Objects.requireNonNull(transacciones);
        this.reloj = Objects.requireNonNull(reloj);
    }

    public DatosUsuario ejecutar(ComandoActualizarPerfil comando) {
        Objects.requireNonNull(comando, "El comando no puede ser nulo.");
        if (comando.usuarioId() == null) {
            throw new ExcepcionUsuarioNoEncontrado("El usuario no existe.");
        }

        return transacciones.ejecutar(() -> {
            Usuario usuario = repositorioUsuarios.buscarPorId(comando.usuarioId())
                    .orElseThrow(ExcepcionUsuarioNoEncontrado::new);

            String correoNormalizado = Usuario.normalizarCorreo(comando.correo());
            if (!correoNormalizado.equals(usuario.getCorreo()) && repositorioUsuarios.existePorCorreo(correoNormalizado)) {
                throw new ExcepcionCorreoYaRegistrado();
            }

            String nuevoHash = usuario.getContrasenaHash();
            if (comando.nuevaContrasena() != null && !comando.nuevaContrasena().isBlank()) {
                if (comando.contrasenaActual() == null || comando.contrasenaActual().isBlank()
                        || !codificador.coincide(comando.contrasenaActual(), usuario.getContrasenaHash())) {
                    throw new ExcepcionDatoInvalido("La contraseña actual no coincide");
                }
                PoliticaContrasena.validar(comando.nuevaContrasena());
                nuevoHash = codificador.codificar(comando.nuevaContrasena());
            }

            String nuevaEspecialidad = usuario.getRol() == RolUsuario.CLINICO ? comando.especialidad() : null;

            Usuario actualizado = usuario.conPerfil(
                    comando.nombreCompleto(),
                    correoNormalizado,
                    nuevaEspecialidad,
                    nuevoHash
            );
            Usuario guardado = repositorioUsuarios.guardar(actualizado);

            repositorioAuditoria.registrar(new RegistroAuditoria(
                    guardado.getId(),
                    AccionAuditoria.PERFIL_ACTUALIZADO,
                    String.valueOf(guardado.getId()),
                    reloj.instant()
            ));

            return DatosUsuario.desde(guardado);
        });
    }
}
