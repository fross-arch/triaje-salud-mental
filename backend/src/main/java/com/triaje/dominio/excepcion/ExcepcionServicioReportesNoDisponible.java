package com.triaje.dominio.excepcion;

/**
 * Se lanza cuando el microservicio de generacion de reportes PDF no responde
 * o produce un error en la comunicacion.
 */
public class ExcepcionServicioReportesNoDisponible extends RuntimeException {

    public ExcepcionServicioReportesNoDisponible(String mensaje) {
        super(mensaje);
    }

    public ExcepcionServicioReportesNoDisponible(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
