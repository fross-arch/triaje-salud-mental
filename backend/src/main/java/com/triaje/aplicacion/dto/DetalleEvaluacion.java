package com.triaje.aplicacion.dto;

import java.util.List;

public record DetalleEvaluacion(
        DatosEvaluacion evaluacion,
        List<DatosRespuestaEvaluacion> respuestas
) {
}
