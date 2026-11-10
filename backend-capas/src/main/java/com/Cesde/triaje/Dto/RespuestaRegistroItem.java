package com.Cesde.triaje.Dto;

import com.Cesde.triaje.Modelo.Enumeracion.InstrumentoCribado;

public record RespuestaRegistroItem(
        String evaluacionId,
        InstrumentoCribado instrumento,
        int indicePregunta,
        boolean riesgoCrisis
) {
}
