package com.triaje.dominio;

import com.triaje.dominio.excepcion.ExcepcionDominio;

import java.util.Objects;

/**
 * Gestor de transiciones validas del ciclo de vida de una cita de cribado.
 * Implementa las reglas puras de dominio sin dependencias de frameworks:
 * - De (nueva) a PROPUESTA (iniciada por clinico).
 * - De PROPUESTA a CONFIRMADA o RECHAZADA (exclusivo del paciente).
 * - De PROPUESTA o CONFIRMADA a CANCELADA (paciente o clinico, cada uno sobre sus citas).
 * - Cualquier otra transicion lanza ExcepcionDominio (mapeada a HTTP 409).
 */
public class MaquinaEstadosCita {

    public EstadoCita proponer(RolUsuario rolActor) {
        return transicionar(null, EstadoCita.PROPUESTA, rolActor);
    }

    public EstadoCita confirmar(EstadoCita estadoActual, RolUsuario rolActor) {
        return transicionar(estadoActual, EstadoCita.CONFIRMADA, rolActor);
    }

    public EstadoCita rechazar(EstadoCita estadoActual, RolUsuario rolActor) {
        return transicionar(estadoActual, EstadoCita.RECHAZADA, rolActor);
    }

    public EstadoCita cancelar(EstadoCita estadoActual, RolUsuario rolActor) {
        return transicionar(estadoActual, EstadoCita.CANCELADA, rolActor);
    }

    public EstadoCita cumplir(EstadoCita estadoActual, RolUsuario rolActor) {
        return transicionar(estadoActual, EstadoCita.CUMPLIDA, rolActor);
    }

    public EstadoCita transicionar(EstadoCita estadoActual, EstadoCita nuevoEstado, RolUsuario rolActor) {
        Objects.requireNonNull(nuevoEstado, "El nuevo estado no puede ser nulo.");

        // Transicion de cita nueva
        if (estadoActual == null) {
            if (nuevoEstado == EstadoCita.PROPUESTA && rolActor == RolUsuario.CLINICO) {
                return EstadoCita.PROPUESTA;
            }
            throw new ExcepcionDominio("Una nueva cita solo puede ser propuesta por un clinico.");
        }

        // Estados terminales
        if (estadoActual == EstadoCita.RECHAZADA) {
            throw new ExcepcionDominio("No se permiten transiciones desde el estado RECHAZADA.");
        }
        if (estadoActual == EstadoCita.CANCELADA) {
            throw new ExcepcionDominio("No se permiten transiciones desde el estado CANCELADA.");
        }
        if (estadoActual == EstadoCita.CUMPLIDA) {
            throw new ExcepcionDominio("No se permiten transiciones desde el estado CUMPLIDA.");
        }

        // Transiciones desde PROPUESTA
        if (estadoActual == EstadoCita.PROPUESTA) {
            if (nuevoEstado == EstadoCita.CONFIRMADA) {
                if (rolActor == RolUsuario.PACIENTE) {
                    return EstadoCita.CONFIRMADA;
                }
                throw new ExcepcionDominio("Solo el paciente puede confirmar una cita propuesta.");
            }
            if (nuevoEstado == EstadoCita.RECHAZADA) {
                if (rolActor == RolUsuario.PACIENTE) {
                    return EstadoCita.RECHAZADA;
                }
                throw new ExcepcionDominio("Solo el paciente puede rechazar una cita propuesta.");
            }
            if (nuevoEstado == EstadoCita.CANCELADA) {
                if (rolActor == RolUsuario.PACIENTE || rolActor == RolUsuario.CLINICO || rolActor == null) {
                    return EstadoCita.CANCELADA;
                }
                throw new ExcepcionDominio("El rol no tiene permisos para cancelar la cita propuesta.");
            }
            if (nuevoEstado == EstadoCita.CUMPLIDA) {
                if (rolActor == RolUsuario.CLINICO) {
                    return EstadoCita.CUMPLIDA;
                }
                throw new ExcepcionDominio("Solo el clinico puede cumplir una cita propuesta.");
            }
            throw new ExcepcionDominio("Transicion no valida desde PROPUESTA hacia " + nuevoEstado + ".");
        }

        // Transiciones desde CONFIRMADA
        if (estadoActual == EstadoCita.CONFIRMADA) {
            if (nuevoEstado == EstadoCita.CANCELADA) {
                if (rolActor == RolUsuario.PACIENTE || rolActor == RolUsuario.CLINICO || rolActor == null) {
                    return EstadoCita.CANCELADA;
                }
                throw new ExcepcionDominio("El rol no tiene permisos para cancelar la cita confirmada.");
            }
            if (nuevoEstado == EstadoCita.CUMPLIDA) {
                if (rolActor == RolUsuario.CLINICO) {
                    return EstadoCita.CUMPLIDA;
                }
                throw new ExcepcionDominio("Solo el clinico puede cumplir una cita confirmada.");
            }
            throw new ExcepcionDominio("Transicion no valida desde CONFIRMADA hacia " + nuevoEstado + ".");
        }

        throw new ExcepcionDominio("Transicion no permitida de " + estadoActual + " a " + nuevoEstado + ".");
    }
}
