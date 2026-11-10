package com.Cesde.triaje.Regla;

import com.Cesde.triaje.Excepcion.ExcepcionDominio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReglaCrisisTest {

    private ReglaCrisis reglaCrisis;

    @BeforeEach
    void inicializar() {
        reglaCrisis = new ReglaCrisis();
    }

    @ParameterizedTest(name = "Item 9 con valor {0} debe resultar en riesgo={1}")
    @CsvSource({
            "0, false",
            "1, true",
            "2, true",
            "3, true"
    })
    @DisplayName("Debe evaluar correctamente el riesgo de crisis segun el valor directo del item 9")
    void deberiaEvaluarRiesgoCrisisSegunValorDirectoItem9(int valorItem9, boolean esperado) {
        boolean resultado = reglaCrisis.evaluarItem9(valorItem9);
        assertEquals(esperado, resultado);
    }

    @ParameterizedTest(name = "Lista con item 9={0} debe resultar en riesgo={1}")
    @CsvSource({
            "0, false",
            "1, true",
            "2, true",
            "3, true"
    })
    @DisplayName("Debe evaluar correctamente el riesgo de crisis a partir de la lista completa de PHQ-9")
    void deberiaEvaluarRiesgoCrisisSegunListaPhq9(int valorItem9, boolean esperado) {
        List<Integer> respuestas = Arrays.asList(0, 0, 0, 0, 0, 0, 0, 0, valorItem9);
        boolean resultado = reglaCrisis.evaluar(respuestas);
        assertEquals(esperado, resultado);
    }

    @ParameterizedTest
    @ValueSource(ints = {-3, -1, 4, 10})
    @DisplayName("Debe lanzar ExcepcionDominio si el valor de item 9 esta fuera del rango 0-3")
    void deberiaLanzarExcepcionCuandoItem9EstaFueraDeRango(int valorInvalido) {
        assertThrows(ExcepcionDominio.class, () -> reglaCrisis.evaluarItem9(valorInvalido));
    }

    @Test
    @DisplayName("Debe lanzar ExcepcionDominio si la lista de respuestas es nula")
    void deberiaLanzarExcepcionCuandoListaPhq9EsNula() {
        assertThrows(ExcepcionDominio.class, () -> reglaCrisis.evaluar(null));
    }

    @Test
    @DisplayName("Debe lanzar ExcepcionDominio si la lista no contiene exactamente 9 respuestas")
    void deberiaLanzarExcepcionCuandoCantidadDeItemsEsInvalida() {
        List<Integer> ochoItems = Arrays.asList(0, 0, 0, 0, 0, 0, 0, 1);
        List<Integer> diezItems = Arrays.asList(0, 0, 0, 0, 0, 0, 0, 0, 1, 0);

        assertThrows(ExcepcionDominio.class, () -> reglaCrisis.evaluar(ochoItems));
        assertThrows(ExcepcionDominio.class, () -> reglaCrisis.evaluar(diezItems));
    }

    @Test
    @DisplayName("Debe lanzar ExcepcionDominio si algun item en la lista es nulo")
    void deberiaLanzarExcepcionCuandoAlgúnItemEnListaEsNulo() {
        List<Integer> respuestas = Arrays.asList(0, 0, null, 0, 0, 0, 0, 0, 1);
        assertThrows(ExcepcionDominio.class, () -> reglaCrisis.evaluar(respuestas));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 4})
    @DisplayName("Debe lanzar ExcepcionDominio si algun item en la lista tiene un valor fuera del rango 0-3")
    void deberiaLanzarExcepcionCuandoItemEnListaEstaFueraDeRango(int valorInvalido) {
        List<Integer> respuestas = Arrays.asList(0, valorInvalido, 0, 0, 0, 0, 0, 0, 0);
        assertThrows(ExcepcionDominio.class, () -> reglaCrisis.evaluar(respuestas));
    }
}
