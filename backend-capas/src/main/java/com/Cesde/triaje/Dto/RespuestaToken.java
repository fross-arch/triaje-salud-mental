package com.Cesde.triaje.Dto;

public record RespuestaToken(String tokenAcceso, String tipo, long expiraEnSegundos) {

    public static RespuestaToken bearer(String tokenAcceso, long expiraEnSegundos) {
        return new RespuestaToken(tokenAcceso, "Bearer", expiraEnSegundos);
    }
}
