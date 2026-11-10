package com.Cesde.triaje.Excepcion;

public class ExcepcionTokenRenovacionInvalido extends ExcepcionBase {
    public ExcepcionTokenRenovacionInvalido() {
        super("El token de renovación es inválido o ha expirado.");
    }

    public ExcepcionTokenRenovacionInvalido(String mensaje) {
        super(mensaje);
    }
}
