package com.Cesde.triaje.Regla;

import com.Cesde.triaje.Excepcion.ExcepcionDominio;
import com.Cesde.triaje.Modelo.Enumeracion.SeveridadAnsiedad;
import com.Cesde.triaje.Modelo.Enumeracion.SeveridadDepresion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReglaResultadoMinimoTest {

    private ReglaResultadoMinimo regla;

    @BeforeEach
    void inicializar() {
        regla = new ReglaResultadoMinimo();
    }

    @ParameterizedTest(name = "Depresion {0} + Ansiedad {1} con crisis={2} debe dar resultadoMinimo={3}")
    @CsvSource({
            "MINIMA, MINIMA, false, true",
            "MINIMA, MINIMA, true, false",
            "LEVE, MINIMA, false, false",
            "MINIMA, LEVE, false, false",
            "LEVE, MINIMA, true, false",
            "MINIMA, LEVE, true, false",
            "MODERADA, MINIMA, false, false",
            "MINIMA, MODERADA, false, false",
            "MODERADAMENTE_SEVERA, MINIMA, false, false",
            "SEVERA, SEVERA, false, false",
            "SEVERA, SEVERA, true, false"
    })
    @DisplayName("Debe evaluar correctamente combinaciones de severidad y riesgo de crisis")
    void deberiaEvaluarCombinacionesDeSeveridadYRiesgoCrisis(
            SeveridadDepresion depresion,
            SeveridadAnsiedad ansiedad,
            boolean riesgoCrisis,
            boolean esperado) {
        boolean resultado = regla.evaluar(depresion, ansiedad, riesgoCrisis);
        assertEquals(esperado, resultado);
    }

    @Test
    @DisplayName("Debe retornar true cuando ambas severidades son MINIMA y la lista de PHQ-9 tiene item 9 igual a 0")
    void deberiaRetornarTrueCuandoAmbasSonMinimaYSinCrisisSegunRespuestasPhq9() {
        List<Integer> respuestasPhq9SinCrisis = Arrays.asList(0, 0, 0, 0, 0, 0, 0, 0, 0);

        boolean resultado = regla.evaluar(SeveridadDepresion.MINIMA, SeveridadAnsiedad.MINIMA, respuestasPhq9SinCrisis);

        assertEquals(true, resultado);
    }

    @Test
    @DisplayName("Debe retornar false aunque ambas severidades sean MINIMA si la lista de PHQ-9 tiene item 9 >= 1")
    void deberiaRetornarFalseCuandoHayCrisisAunquePuntajesSeanMinimos() {
        // En este caso, el item 9 es 1 (riesgo de crisis activo), aunque el puntaje acumulado seria 1 (MINIMA)
        List<Integer> respuestasPhq9ConCrisis = Arrays.asList(0, 0, 0, 0, 0, 0, 0, 0, 1);

        boolean resultado = regla.evaluar(SeveridadDepresion.MINIMA, SeveridadAnsiedad.MINIMA, respuestasPhq9ConCrisis);

        assertEquals(false, resultado);
    }

    @Test
    @DisplayName("Debe lanzar ExcepcionDominio si la severidad de depresion es nula")
    void deberiaLanzarExcepcionCuandoSeveridadDepresionEsNula() {
        assertThrows(ExcepcionDominio.class, () -> regla.evaluar(null, SeveridadAnsiedad.MINIMA, false));
    }

    @Test
    @DisplayName("Debe lanzar ExcepcionDominio si la severidad de ansiedad es nula")
    void deberiaLanzarExcepcionCuandoSeveridadAnsiedadEsNula() {
        assertThrows(ExcepcionDominio.class, () -> regla.evaluar(SeveridadDepresion.MINIMA, null, false));
    }
}
