package com.Cesde.triaje.Tarea;

import com.Cesde.triaje.Modelo.Enumeracion.RolUsuario;
import com.Cesde.triaje.Modelo.MUsuario;
import com.Cesde.triaje.Repositorio.IUsuarioRepositorio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class InicializadorAdministrador implements ApplicationRunner {

    private static final Logger REGISTRO = LoggerFactory.getLogger(InicializadorAdministrador.class);
    private static final int LONGITUD_MINIMA_CONTRASENA = 10;

    private final IUsuarioRepositorio usuarioRepositorio;
    private final PasswordEncoder codificador;
    private final Environment ambiente;

    public InicializadorAdministrador(
            IUsuarioRepositorio usuarioRepositorio,
            PasswordEncoder codificador,
            Environment ambiente) {
        this.usuarioRepositorio = usuarioRepositorio;
        this.codificador = codificador;
        this.ambiente = ambiente;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (usuarioRepositorio.existsByRol(RolUsuario.ADMINISTRADOR)) {
            return;
        }

        String correo = obtenerPropiedad("ADMIN_CORREO");
        if (correo == null || correo.isBlank()) {
            correo = ambiente.getProperty("seguridad.admin.correo");
        }
        if (correo == null || correo.isBlank()) {
            correo = ambiente.getProperty("triaje.seguridad.admin.correo");
        }

        String contrasena = obtenerPropiedad("ADMIN_CONTRASENA_INICIAL");
        if (contrasena == null || contrasena.isBlank()) {
            contrasena = ambiente.getProperty("seguridad.admin.contrasena-inicial");
        }
        if (contrasena == null || contrasena.isBlank()) {
            contrasena = ambiente.getProperty("triaje.seguridad.admin.contrasena-inicial");
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
            String correoNormalizado = correo.trim().toLowerCase(Locale.ROOT);
            if (usuarioRepositorio.existsByCorreo(correoNormalizado)) {
                REGISTRO.warn("No se pudo crear el usuario administrador inicial: el correo configurado ya se encuentra registrado.");
                return;
            }

            MUsuario nuevoAdmin = new MUsuario(
                    "Administrador del Sistema",
                    correoNormalizado,
                    codificador.encode(contrasena),
                    RolUsuario.ADMINISTRADOR
            );
            nuevoAdmin.setActivo(true);
            usuarioRepositorio.save(nuevoAdmin);
            REGISTRO.info("Usuario administrador inicial creado exitosamente.");
        } catch (Exception e) {
            REGISTRO.warn("No se pudo crear el usuario administrador inicial debido a datos no validos.");
        }
    }

    private String obtenerPropiedad(String clave) {
        String valor = ambiente.getProperty(clave);
        if (valor != null && !valor.isBlank()) {
            return valor;
        }
        try {
            java.nio.file.Path rutaEnv = java.nio.file.Path.of(".env");
            if (!java.nio.file.Files.exists(rutaEnv)) {
                rutaEnv = java.nio.file.Path.of("../.env");
            }
            if (java.nio.file.Files.exists(rutaEnv)) {
                for (String linea : java.nio.file.Files.readAllLines(rutaEnv)) {
                    linea = linea.trim();
                    if (linea.startsWith(clave + "=")) {
                        return linea.substring(clave.length() + 1).trim();
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
