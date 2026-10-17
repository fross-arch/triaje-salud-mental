package com.triaje.dominio.excepcion;

/**
 * Se lanza cuando se intenta descargar el reporte de una evaluacion
 * que aun no se encuentra en estado COMPLETADA.
 */
public class ExcepcionEvaluacionIncompleta extends ExcepcionDominio {

    public ExcepcionEvaluacionIncompleta(String mensaje) {
        super(mensaje);
    }
}
