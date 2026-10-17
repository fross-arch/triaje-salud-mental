package com.triaje.aplicacion.dto;

import com.triaje.dominio.SeveridadAnsiedad;
import com.triaje.dominio.SeveridadDepresion;

import java.time.Instant;

public record ResumenEvaluacionClinico(
        String evaluacionId,
        String nombrePaciente,
        Instant fechaFinalizacion,
        SeveridadDepresion severidadDepresion,
        SeveridadAnsiedad severidadAnsiedad,
        boolean riesgoCrisis,
        boolean resultadoMinimo
) {
}
