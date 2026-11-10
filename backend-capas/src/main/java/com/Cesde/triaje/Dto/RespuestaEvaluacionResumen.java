package com.Cesde.triaje.Dto;

import com.Cesde.triaje.Modelo.Enumeracion.EstadoEvaluacion;
import com.Cesde.triaje.Modelo.Enumeracion.SeveridadAnsiedad;
import com.Cesde.triaje.Modelo.Enumeracion.SeveridadDepresion;

import java.time.Instant;

public record RespuestaEvaluacionResumen(
        String id,
        EstadoEvaluacion estado,
        Integer puntajePhq9,
        Integer puntajeGad7,
        SeveridadDepresion severidadDepresion,
        SeveridadAnsiedad severidadAnsiedad,
        boolean riesgoCrisis,
        Boolean resultadoMinimo,
        Instant fechaCreacion,
        Instant fechaFinalizacion
) {
}
