package com.triaje.infraestructura.persistencia.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "registro_auditoria")
public class RegistroAuditoriaEntidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "accion", nullable = false, length = 50)
    private String accion;

    @Column(name = "entidad_id", length = 36, columnDefinition = "varchar(36)")
    private String entidadId;

    @Column(name = "fecha_creacion", nullable = false, updatable = false, columnDefinition = "TIMESTAMP")
    private Instant fechaCreacion;

    protected RegistroAuditoriaEntidad() {
    }

    public RegistroAuditoriaEntidad(Long usuarioId, String accion, String entidadId, Instant fechaCreacion) {
        this.usuarioId = usuarioId;
        this.accion = accion;
        this.entidadId = entidadId;
        this.fechaCreacion = fechaCreacion;
    }

    public Long getId() {
        return id;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public String getAccion() {
        return accion;
    }

    public String getEntidadId() {
        return entidadId;
    }

    public Instant getFechaCreacion() {
        return fechaCreacion;
    }
}
