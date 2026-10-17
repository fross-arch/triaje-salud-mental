package com.triaje.infraestructura.web.dto;

import com.triaje.aplicacion.dto.DetalleEvaluacion;

import java.util.List;

public record RespuestaEvaluacionDetalle(
        RespuestaEvaluacionResumen evaluacion,
        List<RespuestaItemDetalle> respuestas
) {
    public static RespuestaEvaluacionDetalle desde(DetalleEvaluacion detalle) {
        return new RespuestaEvaluacionDetalle(
                RespuestaEvaluacionResumen.desde(detalle.evaluacion()),
                detalle.respuestas().stream().map(RespuestaItemDetalle::desde).toList()
        );
    }
}
