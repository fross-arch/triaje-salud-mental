package com.Cesde.triaje.Excepcion;

public class ExcepcionBase extends RuntimeException {
    public ExcepcionBase(String mensaje) {
        super(mensaje);
    }

    public ExcepcionBase(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
