package com.triaje.aplicacion.dto;

import com.triaje.dominio.modelo.InstrumentoCribado;

public record ResultadoRegistroRespuesta(
        String evaluacionId,
        InstrumentoCribado instrumento,
        int indicePregunta,
        boolean riesgoCrisis
) {
}
