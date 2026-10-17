package com.triaje.dominio.excepcion;

/**
 * Se lanza cuando la evaluacion solicitada no existe o no pertenece al usuario autenticado.
 */
public class ExcepcionEvaluacionNoEncontrada extends ExcepcionDominio {

    public ExcepcionEvaluacionNoEncontrada(String mensaje) {
        super(mensaje);
    }
}
