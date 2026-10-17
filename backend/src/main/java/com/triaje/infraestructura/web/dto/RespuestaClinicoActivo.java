package com.triaje.infraestructura.web.dto;

import com.triaje.aplicacion.dto.DatosClinicoActivo;

public record RespuestaClinicoActivo(
        Long id,
        String nombreCompleto,
        String especialidad
) {
    public RespuestaClinicoActivo(Long id, String nombreCompleto) {
        this(id, nombreCompleto, null);
    }

    public static RespuestaClinicoActivo desde(DatosClinicoActivo datos) {
        return new RespuestaClinicoActivo(datos.id(), datos.nombreCompleto(), datos.especialidad());
    }
}
