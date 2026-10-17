package com.triaje.infraestructura.persistencia.entidad;

import com.triaje.dominio.EstadoCita;
import com.triaje.dominio.ModalidadCita;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "citas")
public class CitaEntidad {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id", columnDefinition = "char(36)", length = 36, nullable = false)
    private String id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "autorizacion_id", columnDefinition = "char(36)", length = 36, nullable = false)
    private String autorizacionId;

    @Column(name = "paciente_id", nullable = false)
    private Long pacienteId;

    @Column(name = "clinico_id", nullable = false)
    private Long clinicoId;

    @Column(name = "fecha_hora", nullable = false, columnDefinition = "TIMESTAMP")
    private Instant fechaHora;

    @Enumerated(EnumType.STRING)
    @Column(name = "modalidad", nullable = false, columnDefinition = "enum('PRESENCIAL','VIRTUAL')")
    private ModalidadCita modalidad;

    @Column(name = "mensaje", length = 300)
    private String mensaje;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, columnDefinition = "enum('PROPUESTA','CONFIRMADA','RECHAZADA','CANCELADA','CUMPLIDA')")
    private EstadoCita estado;

    @Column(name = "observaciones", length = 1000)
    private String observaciones;

    @Column(name = "fecha_creacion", nullable = false, columnDefinition = "TIMESTAMP")
    private Instant fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false, columnDefinition = "TIMESTAMP")
    private Instant fechaActualizacion;

    protected CitaEntidad() {
    }

    public CitaEntidad(String id, String autorizacionId, Long pacienteId, Long clinicoId,
                       Instant fechaHora, ModalidadCita modalidad, String mensaje,
                       EstadoCita estado, String observaciones, Instant fechaCreacion, Instant fechaActualizacion) {
        this.id = id;
        this.autorizacionId = autorizacionId;
        this.pacienteId = pacienteId;
        this.clinicoId = clinicoId;
        this.fechaHora = fechaHora;
        this.modalidad = modalidad;
        this.mensaje = mensaje;
        this.estado = estado;
        this.observaciones = observaciones;
        this.fechaCreacion = fechaCreacion;
        this.fechaActualizacion = fechaActualizacion;
    }

    public CitaEntidad(String id, String autorizacionId, Long pacienteId, Long clinicoId,
                       Instant fechaHora, ModalidadCita modalidad, String mensaje,
                       EstadoCita estado, Instant fechaCreacion, Instant fechaActualizacion) {
        this(id, autorizacionId, pacienteId, clinicoId, fechaHora, modalidad, mensaje, estado, null, fechaCreacion, fechaActualizacion);
    }

    public String getId() {
        return id;
    }

    public String getAutorizacionId() {
        return autorizacionId;
    }

    public Long getPacienteId() {
        return pacienteId;
    }

    public Long getClinicoId() {
        return clinicoId;
    }

    public Instant getFechaHora() {
        return fechaHora;
    }

    public ModalidadCita getModalidad() {
        return modalidad;
    }

    public String getMensaje() {
        return mensaje;
    }

    public EstadoCita getEstado() {
        return estado;
    }

    public Instant getFechaCreacion() {
        return fechaCreacion;
    }

    public Instant getFechaActualizacion() {
        return fechaActualizacion;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public void setFechaHora(Instant fechaHora) {
        this.fechaHora = fechaHora;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public void setEstado(EstadoCita estado) {
        this.estado = estado;
    }

    public void setFechaActualizacion(Instant fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }
}
