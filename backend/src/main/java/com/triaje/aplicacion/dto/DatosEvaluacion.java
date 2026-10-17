package com.triaje.aplicacion.dto;

import com.triaje.dominio.SeveridadAnsiedad;
import com.triaje.dominio.SeveridadDepresion;
import com.triaje.dominio.modelo.EstadoEvaluacion;
import com.triaje.dominio.modelo.Evaluacion;

import java.time.Instant;

public record DatosEvaluacion(
        String id,
        Long usuarioId,
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
    public static DatosEvaluacion desde(Evaluacion evaluacion, Boolean resultadoMinimo) {
        return new DatosEvaluacion(
                evaluacion.getId(),
                evaluacion.getUsuarioId(),
                evaluacion.getEstado(),
                evaluacion.getPuntajePhq9(),
                evaluacion.getPuntajeGad7(),
                evaluacion.getSeveridadDepresion(),
                evaluacion.getSeveridadAnsiedad(),
                evaluacion.isRiesgoCrisis(),
                resultadoMinimo,
                evaluacion.getFechaCreacion(),
                evaluacion.getFechaFinalizacion()
        );
    }
}
