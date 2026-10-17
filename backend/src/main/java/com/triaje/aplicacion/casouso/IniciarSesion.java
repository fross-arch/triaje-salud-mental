package com.triaje.aplicacion.casouso;

import com.triaje.aplicacion.dto.ResultadoSesion;
import com.triaje.aplicacion.puerto.CodificadorContrasenas;
import com.triaje.aplicacion.puerto.EjecutorTransaccional;
import com.triaje.aplicacion.puerto.RepositorioAuditoria;
import com.triaje.aplicacion.puerto.RepositorioUsuarios;
import com.triaje.dominio.excepcion.ExcepcionCredencialesInvalidas;
import com.triaje.dominio.excepcion.ExcepcionDatoInvalido;
import com.triaje.dominio.modelo.AccionAuditoria;
import com.triaje.dominio.modelo.RegistroAuditoria;
import com.triaje.dominio.modelo.Usuario;

import java.time.Clock;
import java.util.Optional;

public class IniciarSesion {

    private final RepositorioUsuarios repositorioUsuarios;
    private final RepositorioAuditoria repositorioAuditoria;
    private final CodificadorContrasenas codificador;
    private final EmisorSesion emisorSesion;
    private final EjecutorTransaccional transacciones;
    private final Clock reloj;

    public IniciarSesion(RepositorioUsuarios repositorioUsuarios, RepositorioAuditoria repositorioAuditoria,
                         CodificadorContrasenas codificador, EmisorSesion emisorSesion,
                         EjecutorTransaccional transacciones, Clock reloj) {
        this.repositorioUsuarios = repositorioUsuarios;
        this.repositorioAuditoria = repositorioAuditoria;
        this.codificador = codificador;
        this.emisorSesion = emisorSesion;
        this.transacciones = transacciones;
        this.reloj = reloj;
    }

    // Hash BCrypt sintetico valido para igualar el tiempo de respuesta frente a correos inexistentes
    private static final String HASH_FICTICIO = "$2a$12$e80yVvU.yQkLp7gq6vMv.uKzVb19DkJZ9b8gL/a1PZ3n78k9wE2.u";

    public ResultadoSesion ejecutar(String correo, String contrasena) {
        if (contrasena == null || contrasena.isEmpty()) {
            throw new ExcepcionCredencialesInvalidas();
        }
        String correoNormalizado;
        try {
            correoNormalizado = Usuario.normalizarCorreo(correo);
        } catch (ExcepcionDatoInvalido e) {
            codificador.coincide(contrasena, HASH_FICTICIO);
            throw new ExcepcionCredencialesInvalidas();
        }

        Optional<Usuario> encontrado = repositorioUsuarios.buscarPorCorreo(correoNormalizado);
        if (encontrado.isEmpty()) {
            // Se ejecuta la comprobacion de BCrypt contra un hash ficticio para no revelar existencia por tiempo
            codificador.coincide(contrasena, HASH_FICTICIO);
            throw new ExcepcionCredencialesInvalidas();
        }
        Usuario usuario = encontrado.get();
        if (!codificador.coincide(contrasena, usuario.getContrasenaHash())) {
            throw new ExcepcionCredencialesInvalidas();
        }
        if (!usuario.isActivo()) {
            throw new ExcepcionCredencialesInvalidas();
        }

        return transacciones.ejecutar(() -> {
            ResultadoSesion resultado = emisorSesion.emitir(usuario);
            repositorioAuditoria.registrar(new RegistroAuditoria(
                    usuario.getId(), AccionAuditoria.INICIO_SESION, null, reloj.instant()));
            return resultado;
        });
    }
}
