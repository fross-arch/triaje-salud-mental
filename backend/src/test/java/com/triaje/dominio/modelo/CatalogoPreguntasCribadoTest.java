package com.triaje.dominio.modelo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogoPreguntasCribadoTest {

    @ParameterizedTest
    @CsvSource({
            "0, 'Ningún día'",
            "1, 'Varios días'",
            "2, 'Más de la mitad de los días'",
            "3, 'Casi todos los días'"
    })
    @DisplayName("Debe retornar la etiqueta de frecuencia con el texto exacto de la escala clinica")
    void debeRetornarEtiquetaFrecuenciaCorrecta(int puntaje, String etiquetaEsperada) {
        String etiqueta = CatalogoPreguntasCribado.obtenerEtiquetaFrecuencia(puntaje);
        assertThat(etiqueta).isEqualTo(etiquetaEsperada);
    }

    @Test
    @DisplayName("Debe retornar Reportado para puntajes fuera del rango 0 a 3")
    void debeRetornarReportadoParaPuntajesFueraDeRango() {
        assertThat(CatalogoPreguntasCribado.obtenerEtiquetaFrecuencia(-1)).isEqualTo("Reportado");
        assertThat(CatalogoPreguntasCribado.obtenerEtiquetaFrecuencia(4)).isEqualTo("Reportado");
    }
}
