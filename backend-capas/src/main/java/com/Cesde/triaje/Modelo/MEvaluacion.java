package com.Cesde.triaje.Modelo;

import com.Cesde.triaje.Modelo.Enumeracion.EstadoEvaluacion;
import com.Cesde.triaje.Modelo.Enumeracion.SeveridadAnsiedad;
import com.Cesde.triaje.Modelo.Enumeracion.SeveridadDepresion;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "evaluaciones")
public class MEvaluacion {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
    private MUsuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoEvaluacion estado = EstadoEvaluacion.EN_CURSO;

    @Column(name = "puntaje_phq9")
    private Integer puntajePhq9;

    @Column(name = "puntaje_gad7")
    private Integer puntajeGad7;

    @Enumerated(EnumType.STRING)
    @Column(name = "severidad_depresion", length = 30)
    private SeveridadDepresion severidadDepresion;

    @Enumerated(EnumType.STRING)
    @Column(name = "severidad_ansiedad", length = 30)
    private SeveridadAnsiedad severidadAnsiedad;

    @Column(name = "riesgo_crisis", nullable = false)
    private boolean riesgoCrisis = false;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private Instant fechaCreacion;

    @Column(name = "fecha_finalizacion")
    private Instant fechaFinalizacion;

    @OneToMany(mappedBy = "evaluacion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MRespuestaEvaluacion> respuestas = new ArrayList<>();

    @OneToMany(mappedBy = "evaluacion")
    private List<MAutorizacion> autorizaciones = new ArrayList<>();

    public MEvaluacion() {
    }

    public MEvaluacion(String id, MUsuario usuario, EstadoEvaluacion estado, Integer puntajePhq9,
                       Integer puntajeGad7, SeveridadDepresion severidadDepresion,
                       SeveridadAnsiedad severidadAnsiedad, boolean riesgoCrisis,
                       Instant fechaCreacion, Instant fechaFinalizacion) {
        this.id = id;
        this.usuario = usuario;
        this.estado = estado;
        this.puntajePhq9 = puntajePhq9;
        this.puntajeGad7 = puntajeGad7;
        this.severidadDepresion = severidadDepresion;
        this.severidadAnsiedad = severidadAnsiedad;
        this.riesgoCrisis = riesgoCrisis;
        this.fechaCreacion = fechaCreacion;
        this.fechaFinalizacion = fechaFinalizacion;
    }

    public MEvaluacion(MUsuario usuario) {
        this(UUID.randomUUID().toString(), usuario, EstadoEvaluacion.EN_CURSO, null, null, null, null, false, null, null);
    }

    @PrePersist
    public void alCrear() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        if (fechaCreacion == null) {
            fechaCreacion = Instant.now();
        }
    }

    public void marcarRiesgoCrisis() {
        this.riesgoCrisis = true;
    }

    public void finalizar(int puntajePhq9, int puntajeGad7, SeveridadDepresion severidadDepresion,
                          SeveridadAnsiedad severidadAnsiedad, Instant fechaFinalizacion) {
        this.puntajePhq9 = puntajePhq9;
        this.puntajeGad7 = puntajeGad7;
        this.severidadDepresion = severidadDepresion;
        this.severidadAnsiedad = severidadAnsiedad;
        this.fechaFinalizacion = fechaFinalizacion;
        this.estado = EstadoEvaluacion.COMPLETADA;
    }

    public void marcarAbandonada() {
        if (this.estado == EstadoEvaluacion.EN_CURSO) {
            this.estado = EstadoEvaluacion.ABANDONADA;
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public MUsuario getUsuario() {
        return usuario;
    }

    public void setUsuario(MUsuario usuario) {
        this.usuario = usuario;
    }

    public EstadoEvaluacion getEstado() {
        return estado;
    }

    public void setEstado(EstadoEvaluacion estado) {
        this.estado = estado;
    }

    public Integer getPuntajePhq9() {
        return puntajePhq9;
    }

    public void setPuntajePhq9(Integer puntajePhq9) {
        this.puntajePhq9 = puntajePhq9;
    }

    public Integer getPuntajeGad7() {
        return puntajeGad7;
    }

    public void setPuntajeGad7(Integer puntajeGad7) {
        this.puntajeGad7 = puntajeGad7;
    }

    public SeveridadDepresion getSeveridadDepresion() {
        return severidadDepresion;
    }

    public void setSeveridadDepresion(SeveridadDepresion severidadDepresion) {
        this.severidadDepresion = severidadDepresion;
    }

    public SeveridadAnsiedad getSeveridadAnsiedad() {
        return severidadAnsiedad;
    }

    public void setSeveridadAnsiedad(SeveridadAnsiedad severidadAnsiedad) {
        this.severidadAnsiedad = severidadAnsiedad;
    }

    public boolean isRiesgoCrisis() {
        return riesgoCrisis;
    }

    public void setRiesgoCrisis(boolean riesgoCrisis) {
        this.riesgoCrisis = riesgoCrisis;
    }

    public Instant getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(Instant fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public Instant getFechaFinalizacion() {
        return fechaFinalizacion;
    }

    public void setFechaFinalizacion(Instant fechaFinalizacion) {
        this.fechaFinalizacion = fechaFinalizacion;
    }

    public Long getUsuarioId() {
        return usuario != null ? usuario.getId() : null;
    }

    public List<MRespuestaEvaluacion> getRespuestas() {
        return respuestas;
    }

    public void setRespuestas(List<MRespuestaEvaluacion> respuestas) {
        this.respuestas = respuestas;
    }

    public List<MAutorizacion> getAutorizaciones() {
        return autorizaciones;
    }

    public void setAutorizaciones(List<MAutorizacion> autorizaciones) {
        this.autorizaciones = autorizaciones;
    }
}
