package com.triaje.aplicacion.puerto;

import java.time.Duration;

public interface GeneradorTokensRenovacion {

    /**
     * Genera un valor aleatorio, opaco y criptográficamente seguro.
     */
    String generarValor();

    /**
     * Calcula el hash (hexadecimal) con el que se guarda el token.
     */
    String calcularHash(String valor);

    Duration duracion();
}
