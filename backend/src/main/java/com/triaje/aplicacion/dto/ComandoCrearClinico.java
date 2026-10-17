package com.triaje.aplicacion.dto;

public record ComandoCrearClinico(Long actorId, String nombreCompleto, String correo, String contrasenaInicial, String especialidad) {

    public ComandoCrearClinico(Long actorId, String nombreCompleto, String correo, String contrasenaInicial) {
        this(actorId, nombreCompleto, correo, contrasenaInicial, null);
    }

    @Override
    public String toString() {
        return "ComandoCrearClinico{datos ocultos}";
    }
}
