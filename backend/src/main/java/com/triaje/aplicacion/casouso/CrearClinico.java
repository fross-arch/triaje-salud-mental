package com.triaje.aplicacion.casouso;

import com.triaje.aplicacion.dto.ComandoCrearClinico;
import com.triaje.aplicacion.dto.DatosUsuario;
import com.triaje.aplicacion.puerto.CodificadorContrasenas;
import com.triaje.aplicacion.puerto.EjecutorTransaccional;
import com.triaje.aplicacion.puerto.RepositorioAuditoria;
import com.triaje.aplicacion.puerto.RepositorioUsuarios;
import com.triaje.dominio.excepcion.ExcepcionCorreoYaRegistrado;
import com.triaje.dominio.modelo.AccionAuditoria;
import com.triaje.dominio.modelo.RegistroAuditoria;
import com.triaje.dominio.modelo.Usuario;
import com.triaje.dominio.regla.PoliticaContrasena;

import java.time.Clock;

/**
 * Caso de uso: el administrador crea una cuenta con rol CLINICO y estado activo.
 * Registra la acción CLINICO_CREADO en auditoría.
 */
public class CrearClinico {

    private final RepositorioUsuarios repositorioUsuarios;
    private final RepositorioAuditoria repositorioAuditoria;
    private final CodificadorContrasenas codificador;
    private final EjecutorTransaccional transacciones;
    private final Clock reloj;

    public CrearClinico(RepositorioUsuarios repositorioUsuarios,
                        RepositorioAuditoria repositorioAuditoria,
                        CodificadorContrasenas codificador,
                        EjecutorTransaccional transacciones,
                        Clock reloj) {
        this.repositorioUsuarios = repositorioUsuarios;
        this.repositorioAuditoria = repositorioAuditoria;
        this.codificador = codificador;
        this.transacciones = transacciones;
        this.reloj = reloj;
    }

    public DatosUsuario ejecutar(ComandoCrearClinico comando) {
        PoliticaContrasena.validar(comando.contrasenaInicial());
        String correo = Usuario.normalizarCorreo(comando.correo());
        Usuario.nuevoClinico(comando.nombreCompleto(), correo, "pendiente", comando.especialidad(), reloj.instant());
        String hash = codificador.codificar(comando.contrasenaInicial());

        return transacciones.ejecutar(() -> {
            if (repositorioUsuarios.existePorCorreo(correo)) {
                throw new ExcepcionCorreoYaRegistrado();
            }
            Usuario guardado = repositorioUsuarios.guardar(
                    Usuario.nuevoClinico(comando.nombreCompleto(), correo, hash, comando.especialidad(), reloj.instant())
            );
            repositorioAuditoria.registrar(new RegistroAuditoria(
                    comando.actorId(),
                    AccionAuditoria.CLINICO_CREADO,
                    String.valueOf(guardado.getId()),
                    reloj.instant()
            ));
            return DatosUsuario.desde(guardado);
        });
    }
}
