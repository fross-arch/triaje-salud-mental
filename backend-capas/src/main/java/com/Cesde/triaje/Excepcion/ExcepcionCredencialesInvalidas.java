package com.Cesde.triaje.Excepcion;

public class ExcepcionCredencialesInvalidas extends ExcepcionBase {
    public ExcepcionCredencialesInvalidas() {
        super("Credenciales inválidas.");
    }

    public ExcepcionCredencialesInvalidas(String mensaje) {
        super(mensaje);
    }
}
