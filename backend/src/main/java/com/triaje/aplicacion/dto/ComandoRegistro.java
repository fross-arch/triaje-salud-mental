package com.triaje.aplicacion.dto;

public record ComandoRegistro(String nombreCompleto, String correo, String contrasena) {

    @Override
    public String toString() {
        return "ComandoRegistro{datos ocultos}";
    }
}
