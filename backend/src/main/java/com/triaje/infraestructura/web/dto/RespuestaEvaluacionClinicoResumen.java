package com.triaje.infraestructura.web.dto;

import com.triaje.aplicacion.dto.ResumenEvaluacionClinico;
import com.triaje.dominio.SeveridadAnsiedad;
import com.triaje.dominio.SeveridadDepresion;

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
    public static RespuestaEvaluacionClinicoResumen desde(ResumenEvaluacionClinico datos) {
        return new RespuestaEvaluacionClinicoResumen(
                datos.evaluacionId(),
                datos.nombrePaciente(),
                datos.fechaFinalizacion(),
                datos.severidadDepresion(),
                datos.severidadAnsiedad(),
                datos.riesgoCrisis(),
                datos.resultadoMinimo()
        );
    }
}
