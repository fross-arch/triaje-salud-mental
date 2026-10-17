package com.triaje.aplicacion.dto;

import com.triaje.dominio.SeveridadAnsiedad;
import com.triaje.dominio.SeveridadDepresion;

import java.util.List;

/**
 * Datos consolidados necesarios para compilar y generar el reporte PDF.
 */
public record DatosSolicitudReporte(
        String nombrePaciente,
        String fechaEvaluacion,
        int puntajePhq9,
        int puntajeGad7,
        SeveridadDepresion severidadDepresion,
        SeveridadAnsiedad severidadAnsiedad,
        boolean riesgoCrisis,
        boolean resultadoMinimo,
        List<DatosLineaAyudaReporte> lineasAyuda,
        List<DatosItemRespuestaReporte> respuestasResumen
) {
    public DatosSolicitudReporte {
        if (lineasAyuda == null) {
            lineasAyuda = List.of();
        }
        if (respuestasResumen == null) {
            respuestasResumen = List.of();
        }
    }

    public record DatosLineaAyudaReporte(
            String nombre,
            String telefono,
            String horario,
            String descripcion
    ) {
    }

    public record DatosItemRespuestaReporte(
            String instrumento,
            int item,
            String frecuencia
    ) {
    }
}
