package com.triaje.infraestructura.persistencia.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "tokens_renovacion")
public class TokenRenovacionEntidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "hash_token", nullable = false, unique = true, length = 64)
    private String hashToken;

    @Column(name = "fecha_creacion", nullable = false, updatable = false, columnDefinition = "TIMESTAMP")
    private Instant fechaCreacion;

    @Column(name = "fecha_expiracion", nullable = false, columnDefinition = "TIMESTAMP")
    private Instant fechaExpiracion;

    @Column(name = "fecha_revocacion", columnDefinition = "TIMESTAMP")
    private Instant fechaRevocacion;

    protected TokenRenovacionEntidad() {
    }

    public TokenRenovacionEntidad(Long id, Long usuarioId, String hashToken, Instant fechaCreacion,
                                  Instant fechaExpiracion, Instant fechaRevocacion) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.hashToken = hashToken;
        this.fechaCreacion = fechaCreacion;
        this.fechaExpiracion = fechaExpiracion;
        this.fechaRevocacion = fechaRevocacion;
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
