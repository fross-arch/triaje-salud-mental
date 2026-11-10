package com.Cesde.triaje.Dto;

import com.Cesde.triaje.Modelo.Enumeracion.InstrumentoCribado;

public record RespuestaItemDetalle(
        InstrumentoCribado instrumento,
        int indicePregunta,
        int valorPuntaje
) {
}
