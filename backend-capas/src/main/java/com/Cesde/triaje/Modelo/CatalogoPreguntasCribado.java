package com.Cesde.triaje.Modelo;

import com.Cesde.triaje.Modelo.Enumeracion.InstrumentoCribado;

import java.util.Map;

/**
 * Catálogo de enunciados de síntomas y etiquetas de frecuencia para el reporte y detalle clínico.
 */
public final class CatalogoPreguntasCribado {

    private static final Map<Integer, String> PREGUNTAS_PHQ9 = Map.of(
            1, "Poco interés o placer en hacer cosas",
            2, "Sentirse desanimado/a, deprimido/a o sin esperanza",
            3, "Problemas para dormir o para mantenerse dormido/a, o dormir demasiado",
            4, "Sentirse cansado/a o tener poca energía",
            5, "Poco apetito o comer en exceso",
            6, "Sentirse mal consigo mismo/a — o sentir que es un/a fracasado/a o que ha decepcionado a su familia o a usted mismo/a",
            7, "Dificultad para concentrarse en cosas tales como leer el periódico o ver televisión",
            8, "Moverse o hablar tan lentamente que otras personas lo han notado, o lo contrario: estar tan inquieto/a o intranquilo/a que se ha estado moviendo mucho más de lo habitual",
            9, "Pensamientos de que estaría mejor muerto/a o de hacerse daño de alguna manera"
    );

    private static final Map<Integer, String> PREGUNTAS_GAD7 = Map.of(
            1, "Sentirse nervioso/a, intranquilo/a o con los nervios de punta",
            2, "No poder parar o controlar las preocupaciones",
            3, "Preocuparse demasiado por diferentes cosas",
            4, "Dificultad para relajarse",
            5, "Estar tan inquieto/a que le cuesta permanecer quieto/a",
            6, "Ponerse molesto/a o irritable con facilidad",
            7, "Sentir miedo como si algo terrible fuera a pasar"
    );

    private static final Map<Integer, String> ETIQUETAS_FRECUENCIA = Map.of(
            0, "Ningún día",
            1, "Varios días",
            2, "Más de la mitad de los días",
            3, "Casi todos los días"
    );

    private CatalogoPreguntasCribado() {
    }

    public static String obtenerTextoPregunta(InstrumentoCribado instrumento, int indice) {
        if (instrumento == InstrumentoCribado.PHQ9) {
            return PREGUNTAS_PHQ9.getOrDefault(indice, "Ítem " + indice + " de PHQ-9");
        }
        return PREGUNTAS_GAD7.getOrDefault(indice, "Ítem " + indice + " de GAD-7");
    }

    public static String obtenerEtiquetaFrecuencia(int puntaje) {
        return ETIQUETAS_FRECUENCIA.getOrDefault(puntaje, "Reportado");
    }
}
