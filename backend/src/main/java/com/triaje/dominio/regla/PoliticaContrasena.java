package com.triaje.dominio.regla;

import com.triaje.dominio.excepcion.ExcepcionDatoInvalido;

import java.nio.charset.StandardCharsets;

/**
 * Reglas de la contraseña: mínimo 10 caracteres y máximo 72 bytes (límite real de BCrypt).
 */
public final class PoliticaContrasena {

    public static final int LONGITUD_MINIMA = 10;
    public static final int BYTES_MAXIMOS = 72;

    private PoliticaContrasena() {
    }

    public static void validar(String contrasena) {
        if (contrasena == null || contrasena.isBlank()) {
            throw new ExcepcionDatoInvalido("La contraseña es obligatoria.");
        }
        if (contrasena.codePointCount(0, contrasena.length()) < LONGITUD_MINIMA) {
            throw new ExcepcionDatoInvalido("La contraseña debe tener al menos " + LONGITUD_MINIMA + " caracteres.");
        }
        if (contrasena.getBytes(StandardCharsets.UTF_8).length > BYTES_MAXIMOS) {
            throw new ExcepcionDatoInvalido("La contraseña es demasiado larga (máximo " + BYTES_MAXIMOS + " bytes).");
        }
    }
}
