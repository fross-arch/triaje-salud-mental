package com.triaje.dominio.excepcion;

public class ExcepcionUsuarioNoEncontrado extends ExcepcionDominio {

    public ExcepcionUsuarioNoEncontrado() {
        super("El usuario no existe.");
    }

    public ExcepcionUsuarioNoEncontrado(String mensaje) {
        super(mensaje);
    }
}
