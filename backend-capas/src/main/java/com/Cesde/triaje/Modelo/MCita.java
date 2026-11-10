package com.Cesde.triaje.Modelo;

import com.Cesde.triaje.Modelo.Enumeracion.EstadoCita;
import com.Cesde.triaje.Modelo.Enumeracion.ModalidadCita;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "citas", indexes = {
        @Index(name = "idx_citas_clinico_fecha", columnList = "clinico_id, fecha_hora")
})
public class MCita {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "autorizacion_id", nullable = false)
    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
    private MAutorizacion autorizacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paciente_id", nullable = false)
    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
    private MUsuario paciente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clinico_id", nullable = false)
    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
    private MUsuario clinico;

    @Column(name = "fecha_hora", nullable = false)
    private Instant fechaHora;

    @Enumerated(EnumType.STRING)
    @Column(name = "modalidad", nullable = false, length = 20)
    private ModalidadCita modalidad;

    @Column(name = "mensaje", length = 300)
    private String mensaje;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoCita estado = EstadoCita.PROPUESTA;

    @Column(name = "observaciones", length = 1000)
    private String observaciones;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private Instant fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private Instant fechaActualizacion;

    public MCita() {
    }

    public MCita(String id, MAutorizacion autorizacion, MUsuario paciente, MUsuario clinico, Instant fechaHora,
                 ModalidadCita modalidad, String mensaje, EstadoCita estado, String observaciones,
                 Instant fechaCreacion, Instant fechaActualizacion) {
        this.id = id;
        this.autorizacion = autorizacion;
        this.paciente = paciente;
        this.clinico = clinico;
        this.fechaHora = fechaHora;
        this.modalidad = modalidad;
        this.mensaje = mensaje;
        this.estado = estado;
        this.observaciones = observaciones;
        this.fechaCreacion = fechaCreacion;
        this.fechaActualizacion = fechaActualizacion;
    }

    public MCita(MAutorizacion autorizacion, MUsuario paciente, MUsuario clinico, Instant fechaHora,
                 ModalidadCita modalidad, String mensaje) {
        this(UUID.randomUUID().toString(), autorizacion, paciente, clinico, fechaHora, modalidad, mensaje,
                EstadoCita.PROPUESTA, null, null, null);
    }

    @PrePersist
    public void alCrear() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        Instant ahora = Instant.now();
        if (fechaCreacion == null) {
            fechaCreacion = ahora;
        }
        if (fechaActualizacion == null) {
            fechaActualizacion = ahora;
        }
    }

    @PreUpdate
    public void alActualizar() {
        fechaActualizacion = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public MAutorizacion getAutorizacion() {
        return autorizacion;
    }

    public void setAutorizacion(MAutorizacion autorizacion) {
        this.autorizacion = autorizacion;
    }

    public MUsuario getPaciente() {
        return paciente;
    }

    public void setPaciente(MUsuario paciente) {
        this.paciente = paciente;
    }

    public MUsuario getClinico() {
        return clinico;
    }

    public void setClinico(MUsuario clinico) {
        this.clinico = clinico;
    }

    public Instant getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(Instant fechaHora) {
        this.fechaHora = fechaHora;
    }

    public ModalidadCita getModalidad() {
        return modalidad;
    }

    public void setModalidad(ModalidadCita modalidad) {
        this.modalidad = modalidad;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public EstadoCita getEstado() {
        return estado;
    }

    public void setEstado(EstadoCita estado) {
        this.estado = estado;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public String getMotivoCancelacion() {
        return observaciones;
    }

    public void setMotivoCancelacion(String motivoCancelacion) {
        this.observaciones = motivoCancelacion;
    }

    public Instant getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(Instant fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public Instant getFechaActualizacion() {
        return fechaActualizacion;
    }

    public void setFechaActualizacion(Instant fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }
}
