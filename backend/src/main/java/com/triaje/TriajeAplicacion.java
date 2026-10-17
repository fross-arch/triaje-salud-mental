package com.triaje;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Punto de entrada del backend del sistema de cribado preliminar (no diagnostica).
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class TriajeAplicacion {

    public static void main(String[] args) {
        SpringApplication.run(TriajeAplicacion.class, args);
    }
}
