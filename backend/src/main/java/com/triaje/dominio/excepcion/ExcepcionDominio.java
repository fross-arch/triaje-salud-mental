package com.triaje.dominio.excepcion;

/**
 * Excepción base de las reglas de negocio. No debe contener datos personales en su mensaje.
 */
public class ExcepcionDominio extends RuntimeException {

    public ExcepcionDominio(String mensaje) {
        super(mensaje);
    }
}
