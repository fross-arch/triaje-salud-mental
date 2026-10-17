package com.triaje.dominio.excepcion;

/**
 * Un dato de entrada no cumple las reglas del dominio (por ejemplo, contraseña demasiado corta).
 */
public class ExcepcionDatoInvalido extends ExcepcionDominio {

    public ExcepcionDatoInvalido(String mensaje) {
        super(mensaje);
    }
}
