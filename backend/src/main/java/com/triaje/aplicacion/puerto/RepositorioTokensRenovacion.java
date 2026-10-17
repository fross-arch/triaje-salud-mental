package com.triaje.aplicacion.puerto;

import com.triaje.dominio.modelo.TokenRenovacion;

import java.time.Instant;
import java.util.Optional;

public interface RepositorioTokensRenovacion {

    TokenRenovacion guardar(TokenRenovacion token);

    Optional<TokenRenovacion> buscarPorHash(String hashToken);

    /**
     * Revoca todos los tokens vigentes de un usuario (por ejemplo, al detectar reutilización).
     */
    void revocarTodosDelUsuario(Long usuarioId, Instant ahora);
}
