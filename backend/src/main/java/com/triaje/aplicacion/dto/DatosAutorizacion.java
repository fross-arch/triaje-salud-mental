package com.triaje.aplicacion.dto;

import com.triaje.dominio.Autorizacion;

import java.time.Instant;

public record DatosAutorizacion(
        String id,
        String evaluacionId,
        Long pacienteId,
        Long clinicoId,
        String versionTextoAutorizacion,
        Instant fechaOtorgamiento,
        Instant fechaRevocacion,
        boolean vigente,
        String estado
) {
    public static DatosAutorizacion desde(Autorizacion autorizacion) {
        return new DatosAutorizacion(
                autorizacion.getId(),
                autorizacion.getEvaluacionId(),
                autorizacion.getPacienteId(),
                autorizacion.getClinicoId(),
                autorizacion.getVersionTextoAutorizacion(),
                autorizacion.getFechaOtorgamiento(),
                autorizacion.getFechaRevocacion(),
                autorizacion.estaVigente(),
                autorizacion.estaVigente() ? "VIGENTE" : "REVOCADA"
        );
    }
}
