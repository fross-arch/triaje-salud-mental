package com.Cesde.triaje.Excepcion;

public class ExcepcionDemasiadosIntentos extends ExcepcionBase {
    private final long segundosEspera;

    public ExcepcionDemasiadosIntentos(long segundosEspera) {
        super("Demasiados intentos de inicio de sesión. Espera un momento e inténtalo de nuevo.");
        this.segundosEspera = segundosEspera;
    }

    public long getSegundosEspera() {
        return segundosEspera;
    }
}
