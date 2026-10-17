package com.triaje.dominio.excepcion;

/**
 * Se lanza cuando un usuario intenta acceder o modificar un recurso que no le pertenece.
 */
public class ExcepcionAccesoDenegado extends ExcepcionDominio {

    public ExcepcionAccesoDenegado(String mensaje) {
        super(mensaje);
    }
}
