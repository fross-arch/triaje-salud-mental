package com.triaje.dominio;

import com.triaje.dominio.excepcion.ExcepcionDominio;

import java.util.List;
import java.util.Objects;

/**
 * Regla de negocio que determina si un cribado arroja un resultado minimo.
 * Devuelve true UNICAMENTE si la severidad de depresion es MINIMA, la severidad de ansiedad es MINIMA
 * y no existe riesgo de crisis (ReglaCrisis es false).
 * Si hay riesgo de crisis, nunca es resultado minimo, aun cuando los puntajes totales sean bajos.
 */
public class ReglaResultadoMinimo {

    private final ReglaCrisis reglaCrisis;

    public ReglaResultadoMinimo() {
        this(new ReglaCrisis());
    }

    public ReglaResultadoMinimo(ReglaCrisis reglaCrisis) {
        this.reglaCrisis = Objects.requireNonNull(reglaCrisis, "La regla de crisis no puede ser nula");
    }

    /**
     * Evalua si el resultado es minimo dadas las severidades obtenidas y la presencia de riesgo de crisis.
     *
     * @param severidadDepresion nivel de severidad de depresion (PHQ-9)
     * @param severidadAnsiedad  nivel de severidad de ansiedad (GAD-7)
     * @param riesgoCrisis       indicador booleano de si existe riesgo de crisis
     * @return true solo si ambas severidades son MINIMA y riesgoCrisis es false; false en caso contrario
     * @throws ExcepcionDominio si alguna severidad es nula
     */
    public boolean evaluar(SeveridadDepresion severidadDepresion, SeveridadAnsiedad severidadAnsiedad, boolean riesgoCrisis) {
        if (severidadDepresion == null) {
            throw new ExcepcionDominio("La severidad de depresion no puede ser nula.");
        }
        if (severidadAnsiedad == null) {
            throw new ExcepcionDominio("La severidad de ansiedad no puede ser nula.");
        }

        if (riesgoCrisis) {
            return false;
        }

        return severidadDepresion == SeveridadDepresion.MINIMA && severidadAnsiedad == SeveridadAnsiedad.MINIMA;
    }

    /**
     * Evalua si el resultado es minimo calculando el riesgo de crisis a partir de las respuestas del PHQ-9.
     *
     * @param severidadDepresion nivel de severidad de depresion
     * @param severidadAnsiedad  nivel de severidad de ansiedad
     * @param respuestasPhq9     lista con las 9 respuestas del cuestionario PHQ-9
     * @return true solo si ambas severidades son MINIMA y el item 9 no indica crisis
     * @throws ExcepcionDominio si alguna severidad o la lista de respuestas es invalida
     */
    public boolean evaluar(SeveridadDepresion severidadDepresion, SeveridadAnsiedad severidadAnsiedad, List<Integer> respuestasPhq9) {
        boolean riesgoCrisis = this.reglaCrisis.evaluar(respuestasPhq9);
        return evaluar(severidadDepresion, severidadAnsiedad, riesgoCrisis);
    }
}
