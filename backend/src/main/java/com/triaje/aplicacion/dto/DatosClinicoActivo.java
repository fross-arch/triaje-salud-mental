package com.triaje.aplicacion.dto;

public record DatosClinicoActivo(
        Long id,
        String nombreCompleto,
        String especialidad
) {
    public DatosClinicoActivo(Long id, String nombreCompleto) {
        this(id, nombreCompleto, null);
    }
}
