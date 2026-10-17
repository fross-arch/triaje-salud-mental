package com.triaje.dominio;

import com.triaje.dominio.excepcion.ExcepcionDatoInvalido;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidad pura de dominio que representa una autorizacion de acceso a una evaluacion
 * otorgada por un paciente hacia un clinico.
 * No depende de Spring ni de JPA.
 */
public final class Autorizacion {

    public static final String VERSION_TEXTO_PREDETERMINADA = "1.0";

    private final String id;
    private final String evaluacionId;
    private final Long pacienteId;
    private final Long clinicoId;
    private final String versionTextoAutorizacion;
    private final Instant fechaOtorgamiento;
    private final Instant fechaRevocacion;

    public Autorizacion(String id, String evaluacionId, Long pacienteId, Long clinicoId,
                        String versionTextoAutorizacion, Instant fechaOtorgamiento, Instant fechaRevocacion) {
        if (id == null || id.isBlank()) {
            throw new ExcepcionDatoInvalido("El identificador de la autorizacion es obligatorio.");
        }
        if (evaluacionId == null || evaluacionId.isBlank()) {
            throw new ExcepcionDatoInvalido("El identificador de la evaluacion es obligatorio.");
        }
        this.id = id;
        this.evaluacionId = evaluacionId;
        this.pacienteId = Objects.requireNonNull(pacienteId, "El identificador del paciente es obligatorio.");
        this.clinicoId = Objects.requireNonNull(clinicoId, "El identificador del clinico es obligatorio.");
        if (versionTextoAutorizacion == null || versionTextoAutorizacion.isBlank()) {
            throw new ExcepcionDatoInvalido("La version del texto de autorizacion es obligatoria.");
        }
        this.versionTextoAutorizacion = versionTextoAutorizacion;
        this.fechaOtorgamiento = Objects.requireNonNull(fechaOtorgamiento, "La fecha de otorgamiento es obligatoria.");
        this.fechaRevocacion = fechaRevocacion;
    }

    /**
     * Crea una nueva autorizacion vigente para una evaluacion y clinico.
     */
    public static Autorizacion nueva(String evaluacionId, Long pacienteId, Long clinicoId,
                                     String versionTextoAutorizacion, Instant fechaOtorgamiento) {
        return new Autorizacion(
                UUID.randomUUID().toString(),
                evaluacionId,
                pacienteId,
                clinicoId,
                versionTextoAutorizacion != null ? versionTextoAutorizacion : VERSION_TEXTO_PREDETERMINADA,
                fechaOtorgamiento,
                null
        );
    }

    /**
     * Devuelve true si la autorizacion se encuentra vigente (sin fecha de revocacion).
     */
    public boolean estaVigente() {
        return this.fechaRevocacion == null;
    }

    /**
     * Mutacion pura: produce una nueva autorizacion revocada con la fecha indicada.
     */
    public Autorizacion revocar(Instant fechaRevocacion) {
        Objects.requireNonNull(fechaRevocacion, "La fecha de revocacion es obligatoria.");
        return new Autorizacion(
                this.id,
                this.evaluacionId,
                this.pacienteId,
                this.clinicoId,
                this.versionTextoAutorizacion,
                this.fechaOtorgamiento,
                fechaRevocacion
        );
    }

    /**
     * Mutacion pura: produce una nueva autorizacion reactivada (vigente) actualizando la fecha de otorgamiento.
     */
    public Autorizacion otorgar(Instant nuevaFechaOtorgamiento) {
        Objects.requireNonNull(nuevaFechaOtorgamiento, "La fecha de otorgamiento es obligatoria.");
        return new Autorizacion(
                this.id,
                this.evaluacionId,
                this.pacienteId,
                this.clinicoId,
                this.versionTextoAutorizacion,
                nuevaFechaOtorgamiento,
                null
        );
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
}
