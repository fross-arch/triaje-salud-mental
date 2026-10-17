package com.triaje.aplicacion.dto;

import com.triaje.dominio.ModalidadCita;

import java.time.Instant;

public record ComandoProponerCita(
        String evaluacionId,
        Long clinicoId,
        Instant fechaHora,
        ModalidadCita modalidad,
        String mensaje
) {
}
