package com.triaje.aplicacion.casouso;

import com.triaje.aplicacion.dto.ResultadoSesion;
import com.triaje.aplicacion.puerto.GeneradorTokensAcceso;
import com.triaje.aplicacion.puerto.GeneradorTokensRenovacion;
import com.triaje.aplicacion.puerto.RepositorioTokensRenovacion;
import com.triaje.dominio.modelo.TokenRenovacion;
import com.triaje.dominio.modelo.Usuario;

import java.time.Clock;
import java.time.Instant;

/**
 * Emite el par token de acceso + token de renovación para un usuario.
 * Debe invocarse dentro de una transacción.
 */
public class EmisorSesion {

    private final GeneradorTokensAcceso generadorTokensAcceso;
    private final GeneradorTokensRenovacion generadorTokensRenovacion;
    private final RepositorioTokensRenovacion repositorioTokens;
    private final Clock reloj;

    public EmisorSesion(GeneradorTokensAcceso generadorTokensAcceso,
                        GeneradorTokensRenovacion generadorTokensRenovacion,
                        RepositorioTokensRenovacion repositorioTokens,
                        Clock reloj) {
        this.generadorTokensAcceso = generadorTokensAcceso;
        this.generadorTokensRenovacion = generadorTokensRenovacion;
        this.repositorioTokens = repositorioTokens;
        this.reloj = reloj;
    }

    public ResultadoSesion emitir(Usuario usuario) {
        Instant ahora = reloj.instant();
        String valorRenovacion = generadorTokensRenovacion.generarValor();
        TokenRenovacion token = TokenRenovacion.nuevo(
                usuario.getId(),
                generadorTokensRenovacion.calcularHash(valorRenovacion),
                ahora,
                ahora.plus(generadorTokensRenovacion.duracion()));
        repositorioTokens.guardar(token);

        GeneradorTokensAcceso.TokenAcceso acceso = generadorTokensAcceso.generar(usuario);
        return new ResultadoSesion(acceso.valor(), acceso.expiraEnSegundos(), valorRenovacion,
                generadorTokensRenovacion.duracion());
    }
}
