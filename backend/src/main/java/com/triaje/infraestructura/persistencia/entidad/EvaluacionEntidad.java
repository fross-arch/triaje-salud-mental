package com.triaje.infraestructura.persistencia.entidad;

import com.triaje.dominio.SeveridadAnsiedad;
import com.triaje.dominio.SeveridadDepresion;
import com.triaje.dominio.modelo.EstadoEvaluacion;
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
@Table(name = "evaluaciones")
public class EvaluacionEntidad {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id", columnDefinition = "char(36)", length = 36, nullable = false)
    private String id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, columnDefinition = "enum('EN_CURSO','COMPLETADA','ABANDONADA')")
    private EstadoEvaluacion estado;

    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "puntaje_phq9", columnDefinition = "tinyint")
    private Integer puntajePhq9;

    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "puntaje_gad7", columnDefinition = "tinyint")
    private Integer puntajeGad7;

    @Enumerated(EnumType.STRING)
    @Column(name = "severidad_depresion", columnDefinition = "enum('MINIMA','LEVE','MODERADA','MODERADAMENTE_SEVERA','SEVERA')")
    private SeveridadDepresion severidadDepresion;

    @Enumerated(EnumType.STRING)
    @Column(name = "severidad_ansiedad", columnDefinition = "enum('MINIMA','LEVE','MODERADA','SEVERA')")
    private SeveridadAnsiedad severidadAnsiedad;

    @Column(name = "riesgo_crisis", nullable = false)
    private boolean riesgoCrisis;

    @Column(name = "fecha_creacion", nullable = false, updatable = false, columnDefinition = "TIMESTAMP")
    private Instant fechaCreacion;

    @Column(name = "fecha_finalizacion", columnDefinition = "TIMESTAMP")
    private Instant fechaFinalizacion;

    protected EvaluacionEntidad() {
    }

    public EvaluacionEntidad(String id, Long usuarioId, EstadoEvaluacion estado, Integer puntajePhq9,
                             Integer puntajeGad7, SeveridadDepresion severidadDepresion,
                             SeveridadAnsiedad severidadAnsiedad, boolean riesgoCrisis,
                             Instant fechaCreacion, Instant fechaFinalizacion) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.estado = estado;
        this.puntajePhq9 = puntajePhq9;
        this.puntajeGad7 = puntajeGad7;
        this.severidadDepresion = severidadDepresion;
        this.severidadAnsiedad = severidadAnsiedad;
        this.riesgoCrisis = riesgoCrisis;
        this.fechaCreacion = fechaCreacion;
        this.fechaFinalizacion = fechaFinalizacion;
    }

    public String getId() {
        return id;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public EstadoEvaluacion getEstado() {
        return estado;
    }

    public Integer getPuntajePhq9() {
        return puntajePhq9;
    }

    public Integer getPuntajeGad7() {
        return puntajeGad7;
    }

    public SeveridadDepresion getSeveridadDepresion() {
        return severidadDepresion;
    }

    public SeveridadAnsiedad getSeveridadAnsiedad() {
        return severidadAnsiedad;
    }

    public boolean isRiesgoCrisis() {
        return riesgoCrisis;
    }

    public Instant getFechaCreacion() {
        return fechaCreacion;
    }

    public Instant getFechaFinalizacion() {
        return fechaFinalizacion;
    }

    public void setEstado(EstadoEvaluacion estado) {
        this.estado = estado;
    }

    public void setPuntajePhq9(Integer puntajePhq9) {
        this.puntajePhq9 = puntajePhq9;
    }

    public void setPuntajeGad7(Integer puntajeGad7) {
        this.puntajeGad7 = puntajeGad7;
    }

    public void setSeveridadDepresion(SeveridadDepresion severidadDepresion) {
        this.severidadDepresion = severidadDepresion;
    }

    public void setSeveridadAnsiedad(SeveridadAnsiedad severidadAnsiedad) {
        this.severidadAnsiedad = severidadAnsiedad;
    }

    public void setRiesgoCrisis(boolean riesgoCrisis) {
        this.riesgoCrisis = riesgoCrisis;
    }

    public void setFechaFinalizacion(Instant fechaFinalizacion) {
        this.fechaFinalizacion = fechaFinalizacion;
    }
}
