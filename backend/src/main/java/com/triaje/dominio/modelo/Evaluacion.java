package com.triaje.dominio.modelo;

import com.triaje.dominio.SeveridadAnsiedad;
import com.triaje.dominio.SeveridadDepresion;
import com.triaje.dominio.excepcion.ExcepcionDatoInvalido;
import com.triaje.dominio.excepcion.ExcepcionDominio;

import java.time.Instant;
import java.util.Objects;

/**
 * Entidad de dominio que representa una sesion de cribado preliminar.
 */
public final class Evaluacion {

    private final String id;
    private final Long usuarioId;
    private EstadoEvaluacion estado;
    private Integer puntajePhq9;
    private Integer puntajeGad7;
    private SeveridadDepresion severidadDepresion;
    private SeveridadAnsiedad severidadAnsiedad;
    private boolean riesgoCrisis;
    private final Instant fechaCreacion;
    private Instant fechaFinalizacion;

    public Evaluacion(String id, Long usuarioId, EstadoEvaluacion estado, Integer puntajePhq9,
                      Integer puntajeGad7, SeveridadDepresion severidadDepresion,
                      SeveridadAnsiedad severidadAnsiedad, boolean riesgoCrisis,
                      Instant fechaCreacion, Instant fechaFinalizacion) {
        if (id == null || id.isBlank()) {
            throw new ExcepcionDatoInvalido("El identificador de evaluación es obligatorio.");
        }
        this.id = id;
        this.usuarioId = Objects.requireNonNull(usuarioId, "El identificador de usuario es obligatorio.");
        this.estado = Objects.requireNonNull(estado, "El estado de la evaluación es obligatorio.");
        this.puntajePhq9 = puntajePhq9;
        this.puntajeGad7 = puntajeGad7;
        this.severidadDepresion = severidadDepresion;
        this.severidadAnsiedad = severidadAnsiedad;
        this.riesgoCrisis = riesgoCrisis;
        this.fechaCreacion = Objects.requireNonNull(fechaCreacion, "La fecha de creación es obligatoria.");
        this.fechaFinalizacion = fechaFinalizacion;
    }

    public static Evaluacion nueva(String id, Long usuarioId, Instant fechaCreacion) {
        return new Evaluacion(
                id,
                usuarioId,
                EstadoEvaluacion.EN_CURSO,
                null,
                null,
                null,
                null,
                false,
                fechaCreacion,
                null
        );
    }

    public void marcarRiesgoCrisis() {
        this.riesgoCrisis = true;
    }

    public void finalizar(int puntajePhq9, int puntajeGad7, SeveridadDepresion severidadDepresion,
                          SeveridadAnsiedad severidadAnsiedad, Instant fechaFinalizacion) {
        if (this.estado != EstadoEvaluacion.EN_CURSO) {
            throw new ExcepcionDominio("Solo se pueden finalizar evaluaciones en estado EN_CURSO.");
        }
        this.puntajePhq9 = puntajePhq9;
        this.puntajeGad7 = puntajeGad7;
        this.severidadDepresion = Objects.requireNonNull(severidadDepresion, "La severidad de depresión es obligatoria.");
        this.severidadAnsiedad = Objects.requireNonNull(severidadAnsiedad, "La severidad de ansiedad es obligatoria.");
        this.fechaFinalizacion = Objects.requireNonNull(fechaFinalizacion, "La fecha de finalización es obligatoria.");
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
}
