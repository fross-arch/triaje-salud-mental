package com.triaje.infraestructura.configuracion;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Propiedades que definen los recursos de ayuda y lineas de atencion.
 * Se leen de la configuracion (application.yml) y nunca se cablean en codigo.
 */
@ConfigurationProperties(prefix = "triaje")
public record PropiedadesRecursosAyuda(List<RecursoAyuda> recursosAyuda) {

    public PropiedadesRecursosAyuda {
        if (recursosAyuda == null) {
            recursosAyuda = List.of();
        }
    }

    public record RecursoAyuda(
            String nombre,
            String telefono,
            String horario,
            String descripcion,
            String ambito
    ) {
    }
}
