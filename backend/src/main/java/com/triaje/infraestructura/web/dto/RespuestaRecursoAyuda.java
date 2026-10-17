package com.triaje.infraestructura.web.dto;

import com.triaje.infraestructura.configuracion.PropiedadesRecursosAyuda.RecursoAyuda;

public record RespuestaRecursoAyuda(
        String nombre,
        String telefono,
        String horario,
        String descripcion,
        String ambito
) {
    public static RespuestaRecursoAyuda desde(RecursoAyuda recurso) {
        return new RespuestaRecursoAyuda(
                recurso.nombre(),
                recurso.telefono(),
                recurso.horario(),
                recurso.descripcion(),
                recurso.ambito()
        );
    }
}
