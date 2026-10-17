package com.triaje.aplicacion.dto;

import com.triaje.dominio.excepcion.ExcepcionDatoInvalido;

import java.util.Objects;

public record ComandoOtorgarAutorizacion(
        String evaluacionId,
        Long pacienteId,
        Long clinicoId,
        boolean confirmaAutorizacion
) {
    public ComandoOtorgarAutorizacion {
        if (evaluacionId == null || evaluacionId.isBlank()) {
            throw new ExcepcionDatoInvalido("El identificador de la evaluacion es obligatorio.");
        }
        Objects.requireNonNull(pacienteId, "El identificador del paciente es obligatorio.");
        Objects.requireNonNull(clinicoId, "El identificador del clinico es obligatorio.");
        if (!confirmaAutorizacion) {
            throw new ExcepcionDatoInvalido("Debes confirmar la autorizacion para otorgar el acceso.");
        }
    }
}
