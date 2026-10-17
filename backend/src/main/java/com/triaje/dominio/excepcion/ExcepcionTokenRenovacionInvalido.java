package com.triaje.dominio.excepcion;

public class ExcepcionTokenRenovacionInvalido extends ExcepcionDominio {

    public ExcepcionTokenRenovacionInvalido() {
        super("La sesión no es válida o ha expirado. Inicia sesión nuevamente.");
    }
}
