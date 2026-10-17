package com.triaje.aplicacion.casouso;

import com.triaje.aplicacion.puerto.EjecutorTransaccional;
import com.triaje.aplicacion.puerto.GeneradorTokensRenovacion;
import com.triaje.aplicacion.puerto.RepositorioTokensRenovacion;

import java.time.Clock;

/**
 * Cierra la sesión revocando el token de renovación. Es idempotente: sin token o con un token
 * desconocido no falla.
 */
public class CerrarSesion {

    private final RepositorioTokensRenovacion repositorioTokens;
    private final GeneradorTokensRenovacion generadorTokensRenovacion;
    private final EjecutorTransaccional transacciones;
    private final Clock reloj;

    public CerrarSesion(RepositorioTokensRenovacion repositorioTokens,
                        GeneradorTokensRenovacion generadorTokensRenovacion,
                        EjecutorTransaccional transacciones, Clock reloj) {
        this.repositorioTokens = repositorioTokens;
        this.generadorTokensRenovacion = generadorTokensRenovacion;
        this.transacciones = transacciones;
        this.reloj = reloj;
    }

    public void ejecutar(String valorToken) {
        if (valorToken == null || valorToken.isBlank()) {
            return;
        }
        String hash = generadorTokensRenovacion.calcularHash(valorToken);
        transacciones.ejecutarSinResultado(() -> repositorioTokens.buscarPorHash(hash).ifPresent(token -> {
            if (!token.estaRevocado()) {
                token.revocar(reloj.instant());
                repositorioTokens.guardar(token);
            }
        }));
    }
}
