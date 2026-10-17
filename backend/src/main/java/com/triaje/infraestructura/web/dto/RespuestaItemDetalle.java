package com.triaje.infraestructura.web.dto;

import com.triaje.aplicacion.dto.DatosRespuestaEvaluacion;
import com.triaje.dominio.modelo.InstrumentoCribado;

public record RespuestaItemDetalle(
        InstrumentoCribado instrumento,
        int indicePregunta,
        int valorPuntaje
) {
    public static RespuestaItemDetalle desde(DatosRespuestaEvaluacion datos) {
        return new RespuestaItemDetalle(
                datos.instrumento(),
                datos.indicePregunta(),
                datos.valorPuntaje()
        );
    }
}
