package com.triaje.aplicacion.dto;

import com.triaje.dominio.modelo.InstrumentoCribado;

public record ComandoRegistrarRespuesta(
        String evaluacionId,
        Long usuarioId,
        InstrumentoCribado instrumento,
        int indicePregunta,
        int valorPuntaje
) {
}
