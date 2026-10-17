package com.triaje.dominio;

import com.triaje.dominio.excepcion.ExcepcionDominio;

import java.util.List;

/**
 * Evaluador del cuestionario PHQ-9 para cribado de depresion.
 * Consta de 9 items con opciones de respuesta entre 0 y 3, con un puntaje total entre 0 y 27.
 * Esta clase realiza exclusivamente cribado preliminar y no emite diagnosticos clinicos.
 */
public class EvaluadorPhq9 {

    public static final int CANTIDAD_ITEMS = 9;
    public static final int VALOR_MINIMO_ITEM = 0;
    public static final int VALOR_MAXIMO_ITEM = 3;
    public static final int PUNTAJE_MINIMO = 0;
    public static final int PUNTAJE_MAXIMO = 27;

    public EvaluadorPhq9() {
    }

    /**
     * Calcula el puntaje total sumando las respuestas validadas del cuestionario PHQ-9.
     *
     * @param respuestas lista con las 9 respuestas del cuestionario
     * @return puntaje total acumulado (entre 0 y 27)
     * @throws ExcepcionDominio si la lista es nula, no contiene 9 items o algun valor esta fuera del rango [0, 3]
     */
    public int calcularPuntaje(List<Integer> respuestas) {
        validarRespuestas(respuestas);
        int total = 0;
        for (Integer respuesta : respuestas) {
            total += respuesta;
        }
        return total;
    }

    /**
     * Clasifica un puntaje total segun los rangos de severidad estandarizados de PHQ-9.
     *
     * @param puntaje total acumulado entre 0 y 27
     * @return nivel de severidad observado
     * @throws ExcepcionDominio si el puntaje se encuentra fuera del rango valido [0, 27]
     */
    public SeveridadDepresion clasificar(int puntaje) {
        if (puntaje < PUNTAJE_MINIMO || puntaje > PUNTAJE_MAXIMO) {
            throw new ExcepcionDominio("El puntaje total del PHQ-9 debe estar entre " + PUNTAJE_MINIMO + " y " + PUNTAJE_MAXIMO + ".");
        }
        if (puntaje <= 4) {
            return SeveridadDepresion.MINIMA;
        }
        if (puntaje <= 9) {
            return SeveridadDepresion.LEVE;
        }
        if (puntaje <= 14) {
            return SeveridadDepresion.MODERADA;
        }
        if (puntaje <= 19) {
            return SeveridadDepresion.MODERADAMENTE_SEVERA;
        }
        return SeveridadDepresion.SEVERA;
    }

    /**
     * Evalua un conjunto de respuestas del cuestionario PHQ-9 y determina la severidad correspondiente.
     *
     * @param respuestas lista con las 9 respuestas
     * @return nivel de severidad resultante del cribado
     * @throws ExcepcionDominio si la lista o sus valores son invalidos
     */
    public SeveridadDepresion evaluar(List<Integer> respuestas) {
        int puntaje = calcularPuntaje(respuestas);
        return clasificar(puntaje);
    }

    private void validarRespuestas(List<Integer> respuestas) {
        if (respuestas == null) {
            throw new ExcepcionDominio("La lista de respuestas del PHQ-9 no puede ser nula.");
        }
        if (respuestas.size() != CANTIDAD_ITEMS) {
            throw new ExcepcionDominio("El cuestionario PHQ-9 debe contener exactamente " + CANTIDAD_ITEMS + " respuestas.");
        }
        for (int i = 0; i < respuestas.size(); i++) {
            Integer respuesta = respuestas.get(i);
            if (respuesta == null) {
                throw new ExcepcionDominio("La respuesta al item " + (i + 1) + " del PHQ-9 no puede ser nula.");
            }
            if (respuesta < VALOR_MINIMO_ITEM || respuesta > VALOR_MAXIMO_ITEM) {
                throw new ExcepcionDominio("El valor de la respuesta al item " + (i + 1) + " del PHQ-9 debe estar entre "
                        + VALOR_MINIMO_ITEM + " y " + VALOR_MAXIMO_ITEM + ".");
            }
        }
    }
}
