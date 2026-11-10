package com.Cesde.triaje.Excepcion;

public class ExcepcionCitaNoEncontrada extends ExcepcionBase {
    public ExcepcionCitaNoEncontrada() {
        super("La cita solicitada no existe.");
    }

    public ExcepcionCitaNoEncontrada(String mensaje) {
        super(mensaje);
    }
}
