package com.triaje.dominio.modelo;

import java.time.Instant;
import java.util.Objects;

/**
 * Token de renovación de sesión. Solo se conoce su hash; el valor en claro vive únicamente en la cookie del cliente.
 */
public final class TokenRenovacion {

    private final Long id;
    private final Long usuarioId;
    private final String hashToken;
    private final Instant fechaCreacion;
    private final Instant fechaExpiracion;
    private Instant fechaRevocacion;

    public TokenRenovacion(Long id, Long usuarioId, String hashToken, Instant fechaCreacion,
                           Instant fechaExpiracion, Instant fechaRevocacion) {
        this.id = id;
        this.usuarioId = Objects.requireNonNull(usuarioId, "El usuario del token es obligatorio");
        this.hashToken = Objects.requireNonNull(hashToken, "El hash del token es obligatorio");
        this.fechaCreacion = Objects.requireNonNull(fechaCreacion, "La fecha de creación es obligatoria");
        this.fechaExpiracion = Objects.requireNonNull(fechaExpiracion, "La fecha de expiración es obligatoria");
        if (!fechaExpiracion.isAfter(fechaCreacion)) {
            throw new IllegalArgumentException("La fecha de expiración debe ser posterior a la de creación");
        }
        this.fechaRevocacion = fechaRevocacion;
    }

    public static TokenRenovacion nuevo(Long usuarioId, String hashToken, Instant ahora, Instant fechaExpiracion) {
        return new TokenRenovacion(null, usuarioId, hashToken, ahora, fechaExpiracion, null);
    }

    public boolean estaRevocado() {
        return fechaRevocacion != null;
    }

    public boolean estaExpirado(Instant ahora) {
        return !ahora.isBefore(fechaExpiracion);
    }

    public boolean estaVigente(Instant ahora) {
        return !estaRevocado() && !estaExpirado(ahora);
    }

    public void revocar(Instant ahora) {
        if (fechaRevocacion == null) {
            fechaRevocacion = ahora;
        }
    }

    public Long getId() {
        return id;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public String getHashToken() {
        return hashToken;
    }

    public Instant getFechaCreacion() {
        return fechaCreacion;
    }

    public Instant getFechaExpiracion() {
        return fechaExpiracion;
    }

    public Instant getFechaRevocacion() {
        return fechaRevocacion;
    }
}
