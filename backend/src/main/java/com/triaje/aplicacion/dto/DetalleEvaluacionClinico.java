package com.triaje.aplicacion.dto;

import com.triaje.dominio.SeveridadAnsiedad;
import com.triaje.dominio.SeveridadDepresion;

import java.time.Instant;
import java.util.List;

public record DetalleEvaluacionClinico(
        String evaluacionId,
        String nombrePaciente,
        Instant fechaFinalizacion,
        Integer puntajePhq9,
        Integer puntajeGad7,
        SeveridadDepresion severidadDepresion,
        SeveridadAnsiedad severidadAnsiedad,
        boolean riesgoCrisis,
        boolean resultadoMinimo,
        List<ItemRespuestaClinico> respuestas
) {
    public record ItemRespuestaClinico(
            String instrumento,
            int indicePregunta,
            String enunciadoPregunta,
            int valorPuntaje,
            String textoFrecuencia
    ) {
    }
}
