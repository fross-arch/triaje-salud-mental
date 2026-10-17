package com.triaje.dominio.modelo;

import com.triaje.dominio.excepcion.ExcepcionDatoInvalido;

import java.util.Objects;

/**
 * Respuesta individual a una de las preguntas de un instrumento en una evaluacion.
 */
public final class RespuestaEvaluacion {

    private final Long id;
    private final String evaluacionId;
    private final InstrumentoCribado instrumento;
    private final int indicePregunta;
    private int valorPuntaje;

    public RespuestaEvaluacion(Long id, String evaluacionId, InstrumentoCribado instrumento,
                               int indicePregunta, int valorPuntaje) {
        if (evaluacionId == null || evaluacionId.isBlank()) {
            throw new ExcepcionDatoInvalido("El identificador de evaluación es obligatorio.");
        }
        this.id = id;
        this.evaluacionId = evaluacionId;
        this.instrumento = Objects.requireNonNull(instrumento, "El instrumento es obligatorio.");
        validarIndicePregunta(instrumento, indicePregunta);
        validarValorPuntaje(valorPuntaje);
        this.indicePregunta = indicePregunta;
        this.valorPuntaje = valorPuntaje;
    }

    public static RespuestaEvaluacion nueva(String evaluacionId, InstrumentoCribado instrumento,
                                            int indicePregunta, int valorPuntaje) {
        return new RespuestaEvaluacion(null, evaluacionId, instrumento, indicePregunta, valorPuntaje);
    }

    public void actualizarValor(int nuevoValor) {
        validarValorPuntaje(nuevoValor);
        this.valorPuntaje = nuevoValor;
    }

    private static void validarIndicePregunta(InstrumentoCribado instrumento, int indice) {
        int maximo = (instrumento == InstrumentoCribado.PHQ9) ? 9 : 7;
        if (indice < 1 || indice > maximo) {
            throw new ExcepcionDatoInvalido("El índice de pregunta para " + instrumento.name()
                    + " debe estar entre 1 y " + maximo + ".");
        }
    }

    private static void validarValorPuntaje(int puntaje) {
        if (puntaje < 0 || puntaje > 3) {
            throw new ExcepcionDatoInvalido("El valor del puntaje debe estar entre 0 y 3.");
        }
    }

    public Long getId() {
        return id;
    }

    public String getEvaluacionId() {
        return evaluacionId;
    }

    public InstrumentoCribado getInstrumento() {
        return instrumento;
    }

    public int getIndicePregunta() {
        return indicePregunta;
    }

    public int getValorPuntaje() {
        return valorPuntaje;
    }
}
