package com.triaje.aplicacion.puerto;

import com.triaje.aplicacion.dto.DatosSolicitudReporte;

/**
 * Puerto hexagonal para la generacion del reporte PDF de la evaluacion.
 */
public interface ClienteReportes {

    byte[] generarReportePdf(DatosSolicitudReporte datos);
}
