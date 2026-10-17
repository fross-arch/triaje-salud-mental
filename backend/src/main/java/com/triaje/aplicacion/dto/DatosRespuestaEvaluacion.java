package com.triaje.aplicacion.dto;

import com.triaje.dominio.modelo.InstrumentoCribado;
import com.triaje.dominio.modelo.RespuestaEvaluacion;

public record DatosRespuestaEvaluacion(
        InstrumentoCribado instrumento,
        int indicePregunta,
        int valorPuntaje
) {
    public static DatosRespuestaEvaluacion desde(RespuestaEvaluacion respuesta) {
        return new DatosRespuestaEvaluacion(
                respuesta.getInstrumento(),
                respuesta.getIndicePregunta(),
                respuesta.getValorPuntaje()
        );
    }
}
