package com.Cesde.triaje.Dto;

import java.util.List;

public record RespuestaEvaluacionDetalle(
        RespuestaEvaluacionResumen evaluacion,
        List<RespuestaItemDetalle> respuestas
) {
}
