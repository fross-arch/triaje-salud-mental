package com.Cesde.triaje.Modelo;

import com.Cesde.triaje.Modelo.Enumeracion.AccionAuditoria;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "registro_auditoria")
public class MRegistroAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(name = "accion", nullable = false, length = 50)
    private AccionAuditoria accion;

    @Column(name = "entidad_id", length = 36)
    private String entidadId;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private Instant fechaCreacion;

    public MRegistroAuditoria() {
    }

    public MRegistroAuditoria(Long id, Long usuarioId, AccionAuditoria accion, String entidadId, Instant fechaCreacion) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.accion = accion;
        this.entidadId = entidadId;
        this.fechaCreacion = fechaCreacion;
    }

    public MRegistroAuditoria(Long usuarioId, AccionAuditoria accion, String entidadId) {
        this(null, usuarioId, accion, entidadId, Instant.now());
    }

    public MRegistroAuditoria(Long usuarioId, AccionAuditoria accion, String entidadId, Instant fechaCreacion) {
        this(null, usuarioId, accion, entidadId, fechaCreacion != null ? fechaCreacion : Instant.now());
    }

    public MRegistroAuditoria(MUsuario usuario, AccionAuditoria accion, String entidadId) {
        this(usuario != null ? usuario.getId() : null, accion, entidadId);
    }

    @PrePersist
    public void alCrear() {
        if (fechaCreacion == null) {
            fechaCreacion = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public AccionAuditoria getAccion() {
        return accion;
    }

    public void setAccion(AccionAuditoria accion) {
        this.accion = accion;
    }

    public String getEntidadId() {
        return entidadId;
    }

    public void setEntidadId(String entidadId) {
        this.entidadId = entidadId;
    }

    public Instant getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(Instant fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }
}
