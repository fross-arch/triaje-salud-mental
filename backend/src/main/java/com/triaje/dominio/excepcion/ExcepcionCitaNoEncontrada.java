package com.triaje.dominio.excepcion;

/**
 * Se lanza cuando la cita solicitada no existe o no pertenece al usuario autenticado.
 */
public class ExcepcionCitaNoEncontrada extends ExcepcionDominio {

    public ExcepcionCitaNoEncontrada(String mensaje) {
        super(mensaje);
    }
}
