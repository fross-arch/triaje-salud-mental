package com.triaje.aplicacion.dto;

import java.time.Duration;

/**
 * Resultado de abrir o renovar una sesión.
 *
 * @param tokenAcceso         JWT de acceso (se devuelve en el cuerpo)
 * @param expiraEnSegundos    vigencia del token de acceso
 * @param tokenRenovacion     valor en claro del token de renovación (solo viaja en la cookie)
 * @param duracionRenovacion  vigencia del token de renovación
 */
public record ResultadoSesion(String tokenAcceso, long expiraEnSegundos, String tokenRenovacion,
                              Duration duracionRenovacion) {

    @Override
    public String toString() {
        return "ResultadoSesion{tokens ocultos}";
    }
}
