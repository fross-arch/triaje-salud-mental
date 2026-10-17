package com.triaje.infraestructura.persistencia.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "autorizaciones_acceso")
public class AutorizacionEntidad {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id", columnDefinition = "char(36)", length = 36, nullable = false)
    private String id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "evaluacion_id", columnDefinition = "char(36)", length = 36, nullable = false)
    private String evaluacionId;

    @Column(name = "paciente_id", nullable = false)
    private Long pacienteId;

    @Column(name = "clinico_id", nullable = false)
    private Long clinicoId;

    @Column(name = "version_texto_autorizacion", nullable = false, length = 20)
    private String versionTextoAutorizacion;

    @Column(name = "fecha_otorgamiento", nullable = false, columnDefinition = "TIMESTAMP")
    private Instant fechaOtorgamiento;

    @Column(name = "fecha_revocacion", columnDefinition = "TIMESTAMP")
    private Instant fechaRevocacion;

    protected AutorizacionEntidad() {
    }

    public AutorizacionEntidad(String id, String evaluacionId, Long pacienteId, Long clinicoId,
                              String versionTextoAutorizacion, Instant fechaOtorgamiento,
                              Instant fechaRevocacion) {
        this.id = id;
        this.evaluacionId = evaluacionId;
        this.pacienteId = pacienteId;
        this.clinicoId = clinicoId;
        this.versionTextoAutorizacion = versionTextoAutorizacion;
        this.fechaOtorgamiento = fechaOtorgamiento;
        this.fechaRevocacion = fechaRevocacion;
    }

    public String getId() {
        return id;
    }

    public String getEvaluacionId() {
        return evaluacionId;
    }

    public Long getPacienteId() {
        return pacienteId;
    }

    public Long getClinicoId() {
        return clinicoId;
    }

    public String getVersionTextoAutorizacion() {
        return versionTextoAutorizacion;
    }

    public Instant getFechaOtorgamiento() {
        return fechaOtorgamiento;
    }

    public Instant getFechaRevocacion() {
        return fechaRevocacion;
    }

    public void setFechaOtorgamiento(Instant fechaOtorgamiento) {
        this.fechaOtorgamiento = fechaOtorgamiento;
    }

    public void setFechaRevocacion(Instant fechaRevocacion) {
        this.fechaRevocacion = fechaRevocacion;
    }
}
