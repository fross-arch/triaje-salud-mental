package com.triaje.aplicacion.casouso;

import com.triaje.aplicacion.dto.ResultadoSesion;
import com.triaje.aplicacion.puerto.EjecutorTransaccional;
import com.triaje.aplicacion.puerto.GeneradorTokensRenovacion;
import com.triaje.aplicacion.puerto.RepositorioTokensRenovacion;
import com.triaje.aplicacion.puerto.RepositorioUsuarios;
import com.triaje.dominio.excepcion.ExcepcionTokenRenovacionInvalido;
import com.triaje.dominio.modelo.TokenRenovacion;
import com.triaje.dominio.modelo.Usuario;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

/**
 * Renueva la sesión rotando el token de renovación: el token usado queda revocado y se emite uno nuevo.
 * Si se presenta un token ya revocado (posible robo), se revocan todos los tokens del usuario.
 */
public class RenovarSesion {

    private final RepositorioTokensRenovacion repositorioTokens;
    private final RepositorioUsuarios repositorioUsuarios;
    private final GeneradorTokensRenovacion generadorTokensRenovacion;
    private final EmisorSesion emisorSesion;
    private final EjecutorTransaccional transacciones;
    private final Clock reloj;

    public RenovarSesion(RepositorioTokensRenovacion repositorioTokens, RepositorioUsuarios repositorioUsuarios,
                         GeneradorTokensRenovacion generadorTokensRenovacion, EmisorSesion emisorSesion,
                         EjecutorTransaccional transacciones, Clock reloj) {
        this.repositorioTokens = repositorioTokens;
        this.repositorioUsuarios = repositorioUsuarios;
        this.generadorTokensRenovacion = generadorTokensRenovacion;
        this.emisorSesion = emisorSesion;
        this.transacciones = transacciones;
        this.reloj = reloj;
    }

    public ResultadoSesion ejecutar(String valorToken) {
        if (valorToken == null || valorToken.isBlank()) {
            throw new ExcepcionTokenRenovacionInvalido();
        }
        String hash = generadorTokensRenovacion.calcularHash(valorToken);

        // La revocación por reutilización debe confirmarse aunque la renovación falle,
        // por eso la transacción devuelve Optional vacío en lugar de lanzar la excepción dentro.
        Optional<ResultadoSesion> resultado = transacciones.ejecutar(() -> renovar(hash));
        return resultado.orElseThrow(ExcepcionTokenRenovacionInvalido::new);
    }

    private Optional<ResultadoSesion> renovar(String hash) {
        Instant ahora = reloj.instant();
        Optional<TokenRenovacion> encontrado = repositorioTokens.buscarPorHash(hash);
        if (encontrado.isEmpty()) {
            return Optional.empty();
        }
        TokenRenovacion token = encontrado.get();
        if (token.estaRevocado()) {
            repositorioTokens.revocarTodosDelUsuario(token.getUsuarioId(), ahora);
            return Optional.empty();
        }
        if (token.estaExpirado(ahora)) {
            return Optional.empty();
        }
        Optional<Usuario> usuario = repositorioUsuarios.buscarPorId(token.getUsuarioId());
        if (usuario.isEmpty() || !usuario.get().isActivo()) {
            return Optional.empty();
        }
        token.revocar(ahora);
        repositorioTokens.guardar(token);
        return Optional.of(emisorSesion.emitir(usuario.get()));
    }
}
