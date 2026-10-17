package com.triaje.dominio.excepcion;

public class ExcepcionCredencialesInvalidas extends ExcepcionDominio {

    public ExcepcionCredencialesInvalidas() {
        super("Correo o contraseña incorrectos.");
    }
}
