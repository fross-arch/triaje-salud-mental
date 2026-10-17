package com.triaje.infraestructura.web.dto;

import com.triaje.aplicacion.dto.ResultadoRegistroRespuesta;
import com.triaje.dominio.modelo.InstrumentoCribado;

public record RespuestaRegistroItem(
        String evaluacionId,
        InstrumentoCribado instrumento,
        int indicePregunta,
        boolean riesgoCrisis
) {
    public static RespuestaRegistroItem desde(ResultadoRegistroRespuesta resultado) {
        return new RespuestaRegistroItem(
                resultado.evaluacionId(),
                resultado.instrumento(),
                resultado.indicePregunta(),
                resultado.riesgoCrisis()
        );
    }
}
