package com.Cesde.triaje.Excepcion;

public class ExcepcionUsuarioNoEncontrado extends ExcepcionBase {
    public ExcepcionUsuarioNoEncontrado() {
        super("El usuario no existe.");
    }

    public ExcepcionUsuarioNoEncontrado(String mensaje) {
        super(mensaje);
    }
}
