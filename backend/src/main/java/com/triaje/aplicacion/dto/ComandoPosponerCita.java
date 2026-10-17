package com.triaje.aplicacion.dto;

import java.time.Instant;

public record ComandoPosponerCita(
        String citaId,
        Long clinicoId,
        Instant nuevaFechaHora,
        String observaciones
) {
}
