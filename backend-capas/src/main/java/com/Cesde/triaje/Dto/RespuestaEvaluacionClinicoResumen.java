package com.Cesde.triaje.Dto;

import com.Cesde.triaje.Modelo.Enumeracion.SeveridadAnsiedad;
import com.Cesde.triaje.Modelo.Enumeracion.SeveridadDepresion;

import java.time.Instant;

public record RespuestaEvaluacionClinicoResumen(
        String evaluacionId,
        String nombrePaciente,
        Instant fechaFinalizacion,
        SeveridadDepresion severidadDepresion,
        SeveridadAnsiedad severidadAnsiedad,
        boolean riesgoCrisis,
        boolean resultadoMinimo
) {
}
