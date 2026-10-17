package com.triaje.infraestructura.web.dto;

/**
 * Token de acceso devuelto en el cuerpo. El token de renovación viaja solo en la cookie httpOnly.
 */
public record RespuestaToken(String tokenAcceso, String tipo, long expiraEnSegundos) {

    public static RespuestaToken bearer(String tokenAcceso, long expiraEnSegundos) {
        return new RespuestaToken(tokenAcceso, "Bearer", expiraEnSegundos);
    }

    @Override
    public String toString() {
        return "RespuestaToken{token oculto}";
    }
}
