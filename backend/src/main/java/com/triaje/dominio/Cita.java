package com.triaje.dominio;

import com.triaje.dominio.excepcion.ExcepcionDatoInvalido;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidad pura de dominio que representa una cita de cribado entre un clinico y un paciente.
 * No depende de Spring, JPA ni librerias de infraestructura.
 */
public final class Cita {

    private final String id;
    private final String autorizacionId;
    private final Long pacienteId;
    private final Long clinicoId;
    private final Instant fechaHora;
    private final ModalidadCita modalidad;
    private final String mensaje;
    private final EstadoCita estado;
    private final String observaciones;
    private final Instant fechaCreacion;
    private final Instant fechaActualizacion;

    public Cita(String id, String autorizacionId, Long pacienteId, Long clinicoId,
                Instant fechaHora, ModalidadCita modalidad, String mensaje,
                EstadoCita estado, String observaciones, Instant fechaCreacion, Instant fechaActualizacion) {
        if (id == null || id.isBlank()) {
            throw new ExcepcionDatoInvalido("El identificador de la cita es obligatorio.");
        }
        if (autorizacionId == null || autorizacionId.isBlank()) {
            throw new ExcepcionDatoInvalido("El identificador de la autorizacion es obligatorio.");
        }
        this.id = id;
        this.autorizacionId = autorizacionId;
        this.pacienteId = Objects.requireNonNull(pacienteId, "El identificador del paciente es obligatorio.");
        this.clinicoId = Objects.requireNonNull(clinicoId, "El identificador del clinico es obligatorio.");
        this.fechaHora = Objects.requireNonNull(fechaHora, "La fecha y hora de la cita es obligatoria.");
        this.modalidad = Objects.requireNonNull(modalidad, "La modalidad de la cita es obligatoria.");
        if (mensaje != null && mensaje.length() > 300) {
            throw new ExcepcionDatoInvalido("El mensaje de la cita no puede exceder los 300 caracteres.");
        }
        this.mensaje = mensaje;
        this.estado = Objects.requireNonNull(estado, "El estado de la cita es obligatorio.");
        if (observaciones != null && observaciones.length() > 1000) {
            throw new ExcepcionDatoInvalido("Las observaciones no pueden superar los 1000 caracteres.");
        }
        this.observaciones = observaciones;
        this.fechaCreacion = Objects.requireNonNull(fechaCreacion, "La fecha de creacion es obligatoria.");
        this.fechaActualizacion = Objects.requireNonNull(fechaActualizacion, "La fecha de actualizacion es obligatoria.");
    }

    public Cita(String id, String autorizacionId, Long pacienteId, Long clinicoId,
                Instant fechaHora, ModalidadCita modalidad, String mensaje,
                EstadoCita estado, Instant fechaCreacion, Instant fechaActualizacion) {
        this(id, autorizacionId, pacienteId, clinicoId, fechaHora, modalidad, mensaje, estado, null, fechaCreacion, fechaActualizacion);
    }

    /**
     * Crea una nueva cita en estado PROPUESTA iniciada por un clinico.
     */
    public static Cita proponer(String autorizacionId, Long pacienteId, Long clinicoId,
                                Instant fechaHora, ModalidadCita modalidad, String mensaje,
                                Instant ahora, MaquinaEstadosCita maquina) {
        EstadoCita estadoInicial = maquina.proponer(RolUsuario.CLINICO);
        return new Cita(
                UUID.randomUUID().toString(),
                autorizacionId,
                pacienteId,
                clinicoId,
                fechaHora,
                modalidad,
                mensaje,
                estadoInicial,
                null,
                ahora,
                ahora
        );
    }

    /**
     * Transiciona la cita a CONFIRMADA (accion exclusiva del paciente).
     */
    public Cita confirmar(Instant ahora, MaquinaEstadosCita maquina) {
        EstadoCita nuevoEstado = maquina.confirmar(this.estado, RolUsuario.PACIENTE);
        return new Cita(
                this.id,
                this.autorizacionId,
                this.pacienteId,
                this.clinicoId,
                this.fechaHora,
                this.modalidad,
                this.mensaje,
                nuevoEstado,
                this.observaciones,
                this.fechaCreacion,
                ahora
        );
    }

    /**
     * Transiciona la cita a RECHAZADA (accion exclusiva del paciente).
     */
    public Cita rechazar(Instant ahora, MaquinaEstadosCita maquina) {
        EstadoCita nuevoEstado = maquina.rechazar(this.estado, RolUsuario.PACIENTE);
        return new Cita(
                this.id,
                this.autorizacionId,
                this.pacienteId,
                this.clinicoId,
                this.fechaHora,
                this.modalidad,
                this.mensaje,
                nuevoEstado,
                this.observaciones,
                this.fechaCreacion,
                ahora
        );
    }

    /**
     * Marca la cita como CUMPLIDA registrando observaciones de la sesion (exclusivo del clinico).
     */
    public Cita cumplir(String observaciones, Instant ahora, MaquinaEstadosCita maquina) {
        if (observaciones == null || observaciones.isBlank()) {
            throw new ExcepcionDatoInvalido("Las observaciones clinicas son obligatorias para cumplir la cita.");
        }
        if (observaciones.length() > 1000) {
            throw new ExcepcionDatoInvalido("Las observaciones no pueden superar los 1000 caracteres.");
        }
        EstadoCita nuevoEstado = maquina.cumplir(this.estado, RolUsuario.CLINICO);
        return new Cita(
                this.id,
                this.autorizacionId,
                this.pacienteId,
                this.clinicoId,
                this.fechaHora,
                this.modalidad,
                this.mensaje,
                nuevoEstado,
                observaciones.trim(),
                this.fechaCreacion,
                ahora
        );
    }

    /**
     * Pospone la fecha y hora de la cita (permitido en PROPUESTA o CONFIRMADA).
     */
    public Cita posponer(Instant nuevaFechaHora, String observaciones, Instant ahora, MaquinaEstadosCita maquina) {
        if (this.estado != EstadoCita.PROPUESTA && this.estado != EstadoCita.CONFIRMADA) {
            throw new com.triaje.dominio.excepcion.ExcepcionDominio("Solo se pueden posponer citas en estado PROPUESTA o CONFIRMADA.");
        }
        if (nuevaFechaHora == null || !nuevaFechaHora.isAfter(ahora)) {
            throw new ExcepcionDatoInvalido("La nueva fecha y hora debe ser futura.");
        }
        if (observaciones != null && observaciones.length() > 1000) {
            throw new ExcepcionDatoInvalido("Las observaciones no pueden superar los 1000 caracteres.");
        }
        String nuevasObservaciones = (observaciones != null && !observaciones.isBlank()) ? observaciones.trim() : this.observaciones;
        return new Cita(
                this.id,
                this.autorizacionId,
                this.pacienteId,
                this.clinicoId,
                nuevaFechaHora,
                this.modalidad,
                this.mensaje,
                this.estado,
                nuevasObservaciones,
                this.fechaCreacion,
                ahora
        );
    }

    /**
     * Cancela la cita registrando motivo u observaciones si es requerida por el actor.
     */
    public Cita cancelar(RolUsuario rolActor, String observaciones, Instant ahora, MaquinaEstadosCita maquina) {
        if (rolActor == RolUsuario.CLINICO && (observaciones == null || observaciones.isBlank())) {
            throw new ExcepcionDatoInvalido("El motivo u observaciones de cancelacion son obligatorios para el clinico.");
        }
        if (observaciones != null && observaciones.length() > 1000) {
            throw new ExcepcionDatoInvalido("Las observaciones no pueden superar los 1000 caracteres.");
        }
        EstadoCita nuevoEstado = maquina.cancelar(this.estado, rolActor);
        return new Cita(
                this.id,
                this.autorizacionId,
                this.pacienteId,
                this.clinicoId,
                this.fechaHora,
                this.modalidad,
                this.mensaje,
                nuevoEstado,
                observaciones != null ? observaciones.trim() : this.observaciones,
                this.fechaCreacion,
                ahora
        );
    }

    /**
     * Cancela la cita por parte del actor (paciente o clinico) o sistema sin observaciones adicionales.
     */
    public Cita cancelar(RolUsuario rolActor, Instant ahora, MaquinaEstadosCita maquina) {
        return cancelar(rolActor, null, ahora, maquina);
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

    public String getObservaciones() {
        return observaciones;
    }

    public Instant getFechaCreacion() {
        return fechaCreacion;
    }

    public Instant getFechaActualizacion() {
        return fechaActualizacion;
    }
}
