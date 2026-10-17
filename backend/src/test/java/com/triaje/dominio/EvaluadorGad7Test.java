package com.triaje.dominio;

import com.triaje.dominio.excepcion.ExcepcionDominio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EvaluadorGad7Test {

    private EvaluadorGad7 evaluador;

    @BeforeEach
    void inicializar() {
        evaluador = new EvaluadorGad7();
    }

    @ParameterizedTest(name = "Puntaje {0} debe clasificarse como severidad {1}")
    @CsvSource({
            "0, MINIMA",
            "4, MINIMA",
            "5, LEVE",
            "9, LEVE",
            "10, MODERADA",
            "14, MODERADA",
            "15, SEVERA",
            "21, SEVERA"
    })
    @DisplayName("Debe clasificar correctamente todas las fronteras de severidad en GAD-7")
    void deberiaClasificarTodasLasFronterasDeSeveridadCorrectamente(int puntaje, SeveridadAnsiedad severidadEsperada) {
        SeveridadAnsiedad severidadObtenida = evaluador.clasificar(puntaje);
        assertEquals(severidadEsperada, severidadObtenida);
    }

    @ParameterizedTest(name = "Puntaje acumulado {0} debe evaluar como severidad {1}")
    @CsvSource({
            "0, MINIMA",
            "4, MINIMA",
            "5, LEVE",
            "9, LEVE",
            "10, MODERADA",
            "14, MODERADA",
            "15, SEVERA",
            "21, SEVERA"
    })
    @DisplayName("Debe calcular puntaje y evaluar severidad en todas las fronteras a partir de lista de respuestas")
    void deberiaEvaluarRespuestasEnTodasLasFronterasCorrectamente(int puntajeObjetivo, SeveridadAnsiedad severidadEsperada) {
        List<Integer> respuestas = generarRespuestasConPuntajeTotal(puntajeObjetivo);

        int puntajeCalculado = evaluador.calcularPuntaje(respuestas);
        SeveridadAnsiedad severidad = evaluador.evaluar(respuestas);

        assertEquals(puntajeObjetivo, puntajeCalculado);
        assertEquals(severidadEsperada, severidad);
    }

    @ParameterizedTest
    @ValueSource(ints = {-5, -1, 22, 30})
    @DisplayName("Debe lanzar ExcepcionDominio al clasificar puntajes fuera del rango 0-21")
    void deberiaLanzarExcepcionCuandoPuntajeEstaFueraDeRango(int puntajeInvalido) {
        assertThrows(ExcepcionDominio.class, () -> evaluador.clasificar(puntajeInvalido));
    }

    @Test
    @DisplayName("Debe lanzar ExcepcionDominio si la lista de respuestas es nula")
    void deberiaLanzarExcepcionCuandoListaDeRespuestasEsNula() {
        assertThrows(ExcepcionDominio.class, () -> evaluador.calcularPuntaje(null));
        assertThrows(ExcepcionDominio.class, () -> evaluador.evaluar(null));
    }

    @Test
    @DisplayName("Debe lanzar ExcepcionDominio si la lista tiene menos de 7 respuestas")
    void deberiaLanzarExcepcionCuandoHayMenosDeSieteRespuestas() {
        List<Integer> respuestas = Arrays.asList(0, 1, 2, 0, 1, 2); // 6 items
        assertThrows(ExcepcionDominio.class, () -> evaluador.calcularPuntaje(respuestas));
        assertThrows(ExcepcionDominio.class, () -> evaluador.evaluar(respuestas));
    }

    @Test
    @DisplayName("Debe lanzar ExcepcionDominio si la lista tiene mas de 7 respuestas")
    void deberiaLanzarExcepcionCuandoHayMasDeSieteRespuestas() {
        List<Integer> respuestas = Arrays.asList(0, 1, 2, 0, 1, 2, 3, 1); // 8 items
        assertThrows(ExcepcionDominio.class, () -> evaluador.calcularPuntaje(respuestas));
        assertThrows(ExcepcionDominio.class, () -> evaluador.evaluar(respuestas));
    }

    @Test
    @DisplayName("Debe lanzar ExcepcionDominio si algun item de la lista es nulo")
    void deberiaLanzarExcepcionCuandoUnItemEsNulo() {
        List<Integer> respuestas = Arrays.asList(0, 1, null, 1, 2, 0, 1);
        assertThrows(ExcepcionDominio.class, () -> evaluador.calcularPuntaje(respuestas));
        assertThrows(ExcepcionDominio.class, () -> evaluador.evaluar(respuestas));
    }

    @ParameterizedTest
    @ValueSource(ints = {-2, -1, 4, 5, 8})
    @DisplayName("Debe lanzar ExcepcionDominio si un item tiene un valor fuera del rango 0-3")
    void deberiaLanzarExcepcionCuandoUnItemTieneValorFueraDeRango(int valorInvalido) {
        List<Integer> respuestas = Arrays.asList(0, 1, 2, valorInvalido, 1, 0, 1);
        assertThrows(ExcepcionDominio.class, () -> evaluador.calcularPuntaje(respuestas));
        assertThrows(ExcepcionDominio.class, () -> evaluador.evaluar(respuestas));
    }

    private List<Integer> generarRespuestasConPuntajeTotal(int puntajeObjetivo) {
        List<Integer> lista = new ArrayList<>(Collections.nCopies(7, 0));
        int restante = puntajeObjetivo;
        for (int i = 0; i < 7; i++) {
            int asignar = Math.min(restante, 3);
            lista.set(i, asignar);
            restante -= asignar;
        }
        return lista;
    }
}
