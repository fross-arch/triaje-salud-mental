package com.triaje.infraestructura.tarea;

import com.triaje.aplicacion.puerto.CodificadorContrasenas;
import com.triaje.aplicacion.puerto.RepositorioUsuarios;
import com.triaje.dominio.RolUsuario;
import com.triaje.dominio.modelo.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.time.Clock;

/**
 * Inicializa el primer usuario con rol ADMINISTRADOR si no existe ninguno en la base de datos.
 * Las credenciales se leen de las variables de entorno ADMIN_CORREO y ADMIN_CONTRASENA_INICIAL.
 * Nunca se registran nombres, correos ni contraseñas en los logs.
 */
@Component
public class InicializadorAdministrador implements ApplicationRunner {

    private static final Logger REGISTRO = LoggerFactory.getLogger(InicializadorAdministrador.class);
    private static final int LONGITUD_MINIMA_CONTRASENA = 10;

    private final RepositorioUsuarios repositorioUsuarios;
    private final CodificadorContrasenas codificador;
    private final Environment ambiente;
    private final Clock reloj;

    public InicializadorAdministrador(RepositorioUsuarios repositorioUsuarios,
                                      CodificadorContrasenas codificador,
                                      Environment ambiente,
                                      Clock reloj) {
        this.repositorioUsuarios = repositorioUsuarios;
        this.codificador = codificador;
        this.ambiente = ambiente;
        this.reloj = reloj;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (repositorioUsuarios.existePorRol(RolUsuario.ADMINISTRADOR)) {
            return;
        }

        String correo = ambiente.getProperty("seguridad.admin.correo");
        if (correo == null || correo.isBlank()) {
            correo = ambiente.getProperty("triaje.seguridad.admin.correo");
        }
        if (correo == null || correo.isBlank()) {
            correo = ambiente.getProperty("ADMIN_CORREO");
        }

        String contrasena = ambiente.getProperty("seguridad.admin.contrasena-inicial");
        if (contrasena == null || contrasena.isBlank()) {
            contrasena = ambiente.getProperty("triaje.seguridad.admin.contrasena-inicial");
        }
        if (contrasena == null || contrasena.isBlank()) {
            contrasena = ambiente.getProperty("ADMIN_CONTRASENA_INICIAL");
        }

        if (correo == null || correo.isBlank() || contrasena == null || contrasena.isBlank()) {
            REGISTRO.warn("No se pudo crear el usuario administrador inicial: faltan las variables de entorno requeridas.");
            return;
        }

        if (contrasena.length() < LONGITUD_MINIMA_CONTRASENA) {
            REGISTRO.warn("No se pudo crear el usuario administrador inicial: la contrasena no cumple con el minimo de 10 caracteres.");
            return;
        }

        try {
            String correoNormalizado = Usuario.normalizarCorreo(correo);
            if (repositorioUsuarios.existePorCorreo(correoNormalizado)) {
                REGISTRO.warn("No se pudo crear el usuario administrador inicial: el correo configurado ya se encuentra registrado.");
                return;
            }

            String contrasenaHash = codificador.codificar(contrasena);
            Usuario nuevoAdmin = Usuario.nuevoAdministrador(
                    "Administrador del Sistema",
                    correoNormalizado,
                    contrasenaHash,
                    reloj.instant()
            );
            repositorioUsuarios.guardar(nuevoAdmin);
            REGISTRO.info("Usuario administrador inicial creado exitosamente.");
        } catch (Exception e) {
            REGISTRO.warn("No se pudo crear el usuario administrador inicial debido a datos no validos.");
        }
    }
}
