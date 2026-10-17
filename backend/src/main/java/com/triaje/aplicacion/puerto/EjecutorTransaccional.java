package com.triaje.aplicacion.puerto;

import java.util.function.Supplier;

/**
 * Permite a los casos de uso ejecutar una operación dentro de una transacción
 * sin depender del framework de infraestructura.
 */
public interface EjecutorTransaccional {

    <T> T ejecutar(Supplier<T> operacion);

    default void ejecutarSinResultado(Runnable operacion) {
        ejecutar(() -> {
            operacion.run();
            return null;
        });
    }
}
