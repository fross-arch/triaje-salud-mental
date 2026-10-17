package com.triaje.dominio;

import com.triaje.dominio.excepcion.ExcepcionDominio;

import java.util.List;

/**
 * Regla de negocio para deteccion de riesgo de crisis en el cribado.
 * Se activa (retorna true) si el item 9 del PHQ-9 (pensamientos de autolesion o ideacion) es mayor o igual a 1.
 * Si el item 9 es 0, retorna false.
 */
public class ReglaCrisis {

    private static final int INDICE_ITEM_CRISIS = 8; // En base 0, el noveno item corresponde al indice 8

    public ReglaCrisis() {
    }

    /**
     * Evalua si existe riesgo de crisis a partir de la lista completa de respuestas del PHQ-9.
     *
     * @param respuestasPhq9 lista con las 9 respuestas del cuestionario PHQ-9
     * @return true si el item 9 es >= 1, false si es 0
     * @throws ExcepcionDominio si la lista es nula, no contiene 9 items o algun valor esta fuera del rango [0, 3]
     */
    public boolean evaluar(List<Integer> respuestasPhq9) {
        if (respuestasPhq9 == null) {
            throw new ExcepcionDominio("La lista de respuestas del PHQ-9 no puede ser nula.");
        }
        if (respuestasPhq9.size() != EvaluadorPhq9.CANTIDAD_ITEMS) {
            throw new ExcepcionDominio("El cuestionario PHQ-9 debe contener exactamente "
                    + EvaluadorPhq9.CANTIDAD_ITEMS + " respuestas.");
        }
        for (int i = 0; i < respuestasPhq9.size(); i++) {
            Integer respuesta = respuestasPhq9.get(i);
            if (respuesta == null) {
                throw new ExcepcionDominio("La respuesta al item " + (i + 1) + " del PHQ-9 no puede ser nula.");
            }
            if (respuesta < EvaluadorPhq9.VALOR_MINIMO_ITEM || respuesta > EvaluadorPhq9.VALOR_MAXIMO_ITEM) {
                throw new ExcepcionDominio("El valor de la respuesta al item " + (i + 1) + " del PHQ-9 debe estar entre "
                        + EvaluadorPhq9.VALOR_MINIMO_ITEM + " y " + EvaluadorPhq9.VALOR_MAXIMO_ITEM + ".");
            }
        }
        int valorItem9 = respuestasPhq9.get(INDICE_ITEM_CRISIS);
        return evaluarItem9(valorItem9);
    }

    /**
     * Evalua si un valor individual del item 9 indica riesgo de crisis.
     *
     * @param item9 valor de la respuesta al noveno item (entre 0 y 3)
     * @return true si item9 >= 1, false si item9 == 0
     * @throws ExcepcionDominio si el valor esta fuera del rango permitido [0, 3]
     */
    public boolean evaluarItem9(int item9) {
        if (item9 < EvaluadorPhq9.VALOR_MINIMO_ITEM || item9 > EvaluadorPhq9.VALOR_MAXIMO_ITEM) {
            throw new ExcepcionDominio("El valor del item 9 del PHQ-9 debe estar entre "
                    + EvaluadorPhq9.VALOR_MINIMO_ITEM + " y " + EvaluadorPhq9.VALOR_MAXIMO_ITEM + ".");
        }
        return item9 >= 1;
    }
}
