package com.Cesde.triaje.Excepcion;

public class ExcepcionServicioReportesNoDisponible extends ExcepcionBase {
    public ExcepcionServicioReportesNoDisponible() {
        super("El servicio de generación de reportes no está disponible en este momento. Por favor, intente más tarde.");
    }

    public ExcepcionServicioReportesNoDisponible(String mensaje) {
        super(mensaje);
    }

    public ExcepcionServicioReportesNoDisponible(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
