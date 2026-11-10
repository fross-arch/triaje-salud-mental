package com.Cesde.triaje.Dto;

import com.Cesde.triaje.Modelo.Enumeracion.SeveridadAnsiedad;
import com.Cesde.triaje.Modelo.Enumeracion.SeveridadDepresion;

import java.time.Instant;
import java.util.List;

public record RespuestaEvaluacionClinicoDetalle(
        String evaluacionId,
        String nombrePaciente,
        Instant fechaFinalizacion,
        Integer puntajePhq9,
        Integer puntajeGad7,
        SeveridadDepresion severidadDepresion,
        SeveridadAnsiedad severidadAnsiedad,
        boolean riesgoCrisis,
        boolean resultadoMinimo,
        List<RespuestaItemClinico> respuestas
) {
    public record RespuestaItemClinico(
            String instrumento,
            int indicePregunta,
            String enunciadoPregunta,
            int valorPuntaje,
            String textoFrecuencia
    ) {
    }
}
