package com.Cesde.triaje.Excepcion;

public class ExcepcionEvaluacionNoEncontrada extends ExcepcionBase {
    public ExcepcionEvaluacionNoEncontrada() {
        super("La evaluación no existe o no tienes acceso a ella.");
    }

    public ExcepcionEvaluacionNoEncontrada(String mensaje) {
        super(mensaje);
    }
}
