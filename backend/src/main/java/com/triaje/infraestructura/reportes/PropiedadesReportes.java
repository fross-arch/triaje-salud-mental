package com.triaje.infraestructura.reportes;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades de conexion al microservicio interno de reportes PDF.
 */
@ConfigurationProperties(prefix = "triaje.reportes")
public record PropiedadesReportes(
        String url,
        String claveInterna,
        int tiempoEsperaConexionSegundos,
        int tiempoEsperaLecturaSegundos
) {
    public PropiedadesReportes {
        if (url == null || url.isBlank()) {
            url = "http://localhost:8000";
        }
        if (claveInterna == null) {
            claveInterna = "";
        }
        if (tiempoEsperaConexionSegundos <= 0) {
            tiempoEsperaConexionSegundos = 5;
        }
        if (tiempoEsperaLecturaSegundos <= 0) {
            tiempoEsperaLecturaSegundos = 15;
        }
    }
}
