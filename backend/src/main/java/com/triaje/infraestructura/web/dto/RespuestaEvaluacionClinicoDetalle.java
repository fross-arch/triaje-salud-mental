package com.triaje.infraestructura.web.dto;

import com.triaje.aplicacion.dto.DetalleEvaluacionClinico;
import com.triaje.dominio.SeveridadAnsiedad;
import com.triaje.dominio.SeveridadDepresion;

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

    public static RespuestaEvaluacionClinicoDetalle desde(DetalleEvaluacionClinico detalle) {
        List<RespuestaItemClinico> items = detalle.respuestas().stream()
                .map(r -> new RespuestaItemClinico(
                        r.instrumento(),
                        r.indicePregunta(),
                        r.enunciadoPregunta(),
                        r.valorPuntaje(),
                        r.textoFrecuencia()
                ))
                .toList();

        return new RespuestaEvaluacionClinicoDetalle(
                detalle.evaluacionId(),
                detalle.nombrePaciente(),
                detalle.fechaFinalizacion(),
                detalle.puntajePhq9(),
                detalle.puntajeGad7(),
                detalle.severidadDepresion(),
                detalle.severidadAnsiedad(),
                detalle.riesgoCrisis(),
                detalle.resultadoMinimo(),
                items
        );
    }
}
