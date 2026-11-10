package com.Cesde.triaje.Modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "autorizaciones_acceso", uniqueConstraints = {
        @UniqueConstraint(name = "uk_autorizaciones_evaluacion_clinico", columnNames = {"evaluacion_id", "clinico_id"})
})
public class MAutorizacion {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evaluacion_id", nullable = false)
    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
    private MEvaluacion evaluacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paciente_id", nullable = false)
    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
    private MUsuario paciente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clinico_id", nullable = false)
    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
    private MUsuario clinico;

    @Column(name = "version_texto_autorizacion", nullable = false, length = 20)
    private String versionTextoAutorizacion = "1.0";

    @Column(name = "fecha_otorgamiento", nullable = false, updatable = false)
    private Instant fechaOtorgamiento;

    @Column(name = "fecha_revocacion")
    private Instant fechaRevocacion;

    @OneToMany(mappedBy = "autorizacion")
    private List<MCita> citas = new ArrayList<>();

    public MAutorizacion() {
    }

    public MAutorizacion(String id, MEvaluacion evaluacion, MUsuario paciente, MUsuario clinico,
                         String versionTextoAutorizacion, Instant fechaOtorgamiento, Instant fechaRevocacion) {
        this.id = id;
        this.evaluacion = evaluacion;
        this.paciente = paciente;
        this.clinico = clinico;
        this.versionTextoAutorizacion = versionTextoAutorizacion != null ? versionTextoAutorizacion : "1.0";
        this.fechaOtorgamiento = fechaOtorgamiento;
        this.fechaRevocacion = fechaRevocacion;
    }

    public MAutorizacion(MEvaluacion evaluacion, MUsuario paciente, MUsuario clinico, String versionTextoAutorizacion) {
        this(UUID.randomUUID().toString(), evaluacion, paciente, clinico, versionTextoAutorizacion, null, null);
    }

    @PrePersist
    public void alCrear() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        if (fechaOtorgamiento == null) {
            fechaOtorgamiento = Instant.now();
        }
    }

    public boolean estaVigente() {
        return this.fechaRevocacion == null;
    }

    public void revocar(Instant ahora) {
        this.fechaRevocacion = ahora;
    }

    public void reactivar(Instant ahora) {
        this.fechaOtorgamiento = ahora;
        this.fechaRevocacion = null;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public MEvaluacion getEvaluacion() {
        return evaluacion;
    }

    public void setEvaluacion(MEvaluacion evaluacion) {
        this.evaluacion = evaluacion;
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

    public String getVersionTextoAutorizacion() {
        return versionTextoAutorizacion;
    }

    public void setVersionTextoAutorizacion(String versionTextoAutorizacion) {
        this.versionTextoAutorizacion = versionTextoAutorizacion;
    }

    public Instant getFechaOtorgamiento() {
        return fechaOtorgamiento;
    }

    public void setFechaOtorgamiento(Instant fechaOtorgamiento) {
        this.fechaOtorgamiento = fechaOtorgamiento;
    }

    public Instant getFechaRevocacion() {
        return fechaRevocacion;
    }

    public void setFechaRevocacion(Instant fechaRevocacion) {
        this.fechaRevocacion = fechaRevocacion;
    }

    public List<MCita> getCitas() {
        return citas;
    }

    public void setCitas(List<MCita> citas) {
        this.citas = citas;
    }

    public String getEvaluacionId() {
        return evaluacion != null ? evaluacion.getId() : null;
    }

    public Long getPacienteId() {
        return paciente != null ? paciente.getId() : null;
    }

    public Long getClinicoId() {
        return clinico != null ? clinico.getId() : null;
    }
}
