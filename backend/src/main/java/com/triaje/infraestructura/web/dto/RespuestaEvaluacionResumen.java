package com.triaje.infraestructura.web.dto;

import com.triaje.aplicacion.dto.DatosEvaluacion;
import com.triaje.dominio.SeveridadAnsiedad;
import com.triaje.dominio.SeveridadDepresion;
import com.triaje.dominio.modelo.EstadoEvaluacion;

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
    public static RespuestaEvaluacionResumen desde(DatosEvaluacion datos) {
        return new RespuestaEvaluacionResumen(
                datos.id(),
                datos.estado(),
                datos.puntajePhq9(),
                datos.puntajeGad7(),
                datos.severidadDepresion(),
                datos.severidadAnsiedad(),
                datos.riesgoCrisis(),
                datos.resultadoMinimo(),
                datos.fechaCreacion(),
                datos.fechaFinalizacion()
        );
    }
}
