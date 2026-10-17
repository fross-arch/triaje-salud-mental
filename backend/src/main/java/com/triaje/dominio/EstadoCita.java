package com.triaje.dominio;

/**
 * Estados posibles del ciclo de vida de una cita de cribado.
 * Gestionados por la MaquinaEstadosCita sin dependencias de frameworks.
 */
public enum EstadoCita {
    PROPUESTA,
    CONFIRMADA,
    RECHAZADA,
    CANCELADA,
    CUMPLIDA
}
