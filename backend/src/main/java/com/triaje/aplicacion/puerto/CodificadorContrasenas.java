package com.triaje.aplicacion.puerto;

public interface CodificadorContrasenas {

    String codificar(String contrasenaPlana);

    boolean coincide(String contrasenaPlana, String contrasenaHash);
}
