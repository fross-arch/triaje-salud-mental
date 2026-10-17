package com.triaje.infraestructura.persistencia.adaptador;

import com.triaje.aplicacion.puerto.RepositorioTokensRenovacion;
import com.triaje.dominio.modelo.TokenRenovacion;
import com.triaje.infraestructura.persistencia.entidad.TokenRenovacionEntidad;
import com.triaje.infraestructura.persistencia.repositorio.TokenRenovacionRepositorioJpa;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

@Component
public class AdaptadorRepositorioTokensRenovacion implements RepositorioTokensRenovacion {

    private final TokenRenovacionRepositorioJpa repositorio;

    public AdaptadorRepositorioTokensRenovacion(TokenRenovacionRepositorioJpa repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public TokenRenovacion guardar(TokenRenovacion token) {
        TokenRenovacionEntidad guardada = repositorio.save(new TokenRenovacionEntidad(
                token.getId(), token.getUsuarioId(), token.getHashToken(), token.getFechaCreacion(),
                token.getFechaExpiracion(), token.getFechaRevocacion()));
        return aDominio(guardada);
    }

    @Override
    public Optional<TokenRenovacion> buscarPorHash(String hashToken) {
        return repositorio.findByHashToken(hashToken).map(AdaptadorRepositorioTokensRenovacion::aDominio);
    }

    @Override
    public void revocarTodosDelUsuario(Long usuarioId, Instant ahora) {
        repositorio.revocarTodosDelUsuario(usuarioId, ahora);
    }

    private static TokenRenovacion aDominio(TokenRenovacionEntidad entidad) {
        return new TokenRenovacion(entidad.getId(), entidad.getUsuarioId(), entidad.getHashToken(),
                entidad.getFechaCreacion(), entidad.getFechaExpiracion(), entidad.getFechaRevocacion());
    }
}
