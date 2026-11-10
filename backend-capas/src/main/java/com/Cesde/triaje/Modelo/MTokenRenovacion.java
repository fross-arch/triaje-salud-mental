package com.Cesde.triaje.Modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(name = "tokens_renovacion", uniqueConstraints = {
        @UniqueConstraint(name = "uk_tokens_hash", columnNames = {"hash_token"})
})
public class MTokenRenovacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
    private MUsuario usuario;

    @Column(name = "hash_token", nullable = false, length = 64)
    private String hashToken;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private Instant fechaCreacion;

    @Column(name = "fecha_expiracion", nullable = false)
    private Instant fechaExpiracion;

    @Column(name = "fecha_revocacion")
    private Instant fechaRevocacion;

    public MTokenRenovacion() {
    }

    public MTokenRenovacion(Long id, MUsuario usuario, String hashToken, Instant fechaCreacion,
                            Instant fechaExpiracion, Instant fechaRevocacion) {
        this.id = id;
        this.usuario = usuario;
        this.hashToken = hashToken;
        this.fechaCreacion = fechaCreacion;
        this.fechaExpiracion = fechaExpiracion;
        this.fechaRevocacion = fechaRevocacion;
    }

    public MTokenRenovacion(MUsuario usuario, String hashToken, Instant fechaExpiracion) {
        this(null, usuario, hashToken, null, fechaExpiracion, null);
    }

    @PrePersist
    public void alCrear() {
        if (fechaCreacion == null) {
            fechaCreacion = Instant.now();
        }
    }

    public boolean estaActivo(Instant ahora) {
        return fechaRevocacion == null && fechaExpiracion.isAfter(ahora);
    }

    public boolean estaRevocado() {
        return fechaRevocacion != null;
    }

    public boolean estaExpirado(Instant ahora) {
        return !fechaExpiracion.isAfter(ahora);
    }

    public void revocar(Instant ahora) {
        this.fechaRevocacion = ahora;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public MUsuario getUsuario() {
        return usuario;
    }

    public void setUsuario(MUsuario usuario) {
        this.usuario = usuario;
    }

    public String getHashToken() {
        return hashToken;
    }

    public void setHashToken(String hashToken) {
        this.hashToken = hashToken;
    }

    public Instant getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(Instant fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public Instant getFechaExpiracion() {
        return fechaExpiracion;
    }

    public void setFechaExpiracion(Instant fechaExpiracion) {
        this.fechaExpiracion = fechaExpiracion;
    }

    public Instant getFechaRevocacion() {
        return fechaRevocacion;
    }

    public void setFechaRevocacion(Instant fechaRevocacion) {
        this.fechaRevocacion = fechaRevocacion;
    }
}
