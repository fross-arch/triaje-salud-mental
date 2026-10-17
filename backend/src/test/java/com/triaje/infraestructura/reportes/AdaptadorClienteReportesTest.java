package com.triaje.infraestructura.reportes;

import com.triaje.aplicacion.dto.DatosSolicitudReporte;
import com.triaje.aplicacion.dto.DatosSolicitudReporte.DatosItemRespuestaReporte;
import com.triaje.aplicacion.dto.DatosSolicitudReporte.DatosLineaAyudaReporte;
import com.triaje.dominio.SeveridadAnsiedad;
import com.triaje.dominio.SeveridadDepresion;
import com.triaje.dominio.excepcion.ExcepcionServicioReportesNoDisponible;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AdaptadorClienteReportesTest {

    private static final String CLAVE_SECRETA_PRUEBA = "clave_interna_secreta_reportes_123";
    private static final String URL_BASE = "http://localhost:8000";

    private MockRestServiceServer servidorSimulado;
    private AdaptadorClienteReportes adaptador;

    @BeforeEach
    void inicializar() {
        RestClient.Builder constructor = RestClient.builder().baseUrl(URL_BASE);
        servidorSimulado = MockRestServiceServer.bindTo(constructor).build();
        RestClient restClient = constructor.build();

        PropiedadesReportes propiedades = new PropiedadesReportes(
                URL_BASE,
                CLAVE_SECRETA_PRUEBA,
                2,
                5
        );
        adaptador = new AdaptadorClienteReportes(restClient, propiedades);
    }

    @Test
    @DisplayName("Debe enviar cabecera X-Clave-Interna y cuerpo JSON correcto con 16 respuestas y retornar bytes del PDF")
    void debeEnviarCabeceraClaveInternaYCuerpoJsonCorrecto() {
        List<DatosItemRespuestaReporte> dieciseisRespuestas = List.of(
                new DatosItemRespuestaReporte("PHQ-9", 1, "Ningún día"),
                new DatosItemRespuestaReporte("PHQ-9", 2, "Varios días"),
                new DatosItemRespuestaReporte("PHQ-9", 3, "Más de la mitad de los días"),
                new DatosItemRespuestaReporte("PHQ-9", 4, "Casi todos los días"),
                new DatosItemRespuestaReporte("PHQ-9", 5, "Ningún día"),
                new DatosItemRespuestaReporte("PHQ-9", 6, "Varios días"),
                new DatosItemRespuestaReporte("PHQ-9", 7, "Más de la mitad de los días"),
                new DatosItemRespuestaReporte("PHQ-9", 8, "Casi todos los días"),
                new DatosItemRespuestaReporte("PHQ-9", 9, "Ningún día"),
                new DatosItemRespuestaReporte("GAD-7", 1, "Ningún día"),
                new DatosItemRespuestaReporte("GAD-7", 2, "Varios días"),
                new DatosItemRespuestaReporte("GAD-7", 3, "Más de la mitad de los días"),
                new DatosItemRespuestaReporte("GAD-7", 4, "Casi todos los días"),
                new DatosItemRespuestaReporte("GAD-7", 5, "Ningún día"),
                new DatosItemRespuestaReporte("GAD-7", 6, "Varios días"),
                new DatosItemRespuestaReporte("GAD-7", 7, "Más de la mitad de los días")
        );

        DatosSolicitudReporte datos = new DatosSolicitudReporte(
                "Persona de Prueba",
                "2026-10-13",
                12,
                8,
                SeveridadDepresion.MODERADA,
                SeveridadAnsiedad.LEVE,
                false,
                false,
                List.of(new DatosLineaAyudaReporte("Linea 106", "106", "24/7", "Apoyo")),
                dieciseisRespuestas
        );

        byte[] pdfEsperado = "%PDF-1.4 prueba binaria de pdf".getBytes();

        servidorSimulado.expect(requestTo(URL_BASE + "/api/reportes/generar"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Clave-Interna", CLAVE_SECRETA_PRUEBA))
                .andExpect(jsonPath("$.nombre_paciente").value("Persona de Prueba"))
                .andExpect(jsonPath("$.fecha_evaluacion").value("2026-10-13"))
                .andExpect(jsonPath("$.puntaje_phq9").value(12))
                .andExpect(jsonPath("$.puntaje_gad7").value(8))
                .andExpect(jsonPath("$.severidad_depresion").value("MODERADA"))
                .andExpect(jsonPath("$.severidad_ansiedad").value("LEVE"))
                .andExpect(jsonPath("$.riesgo_crisis").value(false))
                .andExpect(jsonPath("$.resultado_minimo").value(false))
                .andExpect(jsonPath("$.lineas_ayuda[0].nombre").value("Linea 106"))
                .andExpect(jsonPath("$.respuestas_resumen", hasSize(16)))
                .andExpect(jsonPath("$.respuestas_resumen[0].instrumento").value("PHQ-9"))
                .andExpect(jsonPath("$.respuestas_resumen[0].item").value(1))
                .andExpect(jsonPath("$.respuestas_resumen[0].frecuencia").value("Ningún día"))
                .andExpect(jsonPath("$.respuestas_resumen[8].instrumento").value("PHQ-9"))
                .andExpect(jsonPath("$.respuestas_resumen[8].item").value(9))
                .andExpect(jsonPath("$.respuestas_resumen[8].frecuencia").value("Ningún día"))
                .andExpect(jsonPath("$.respuestas_resumen[9].instrumento").value("GAD-7"))
                .andExpect(jsonPath("$.respuestas_resumen[9].item").value(1))
                .andExpect(jsonPath("$.respuestas_resumen[9].frecuencia").value("Ningún día"))
                .andExpect(jsonPath("$.respuestas_resumen[15].instrumento").value("GAD-7"))
                .andExpect(jsonPath("$.respuestas_resumen[15].item").value(7))
                .andExpect(jsonPath("$.respuestas_resumen[15].frecuencia").value("Más de la mitad de los días"))
                .andRespond(withSuccess(pdfEsperado, MediaType.APPLICATION_PDF));

        byte[] pdfResultado = adaptador.generarReportePdf(datos);

        assertThat(pdfResultado).isEqualTo(pdfEsperado);
        servidorSimulado.verify();
    }

    @Test
    @DisplayName("Debe lanzar ExcepcionServicioReportesNoDisponible cuando el microservicio responde error 500")
    void debeLanzarExcepcionCuandoMicroservicioRespondeError500() {
        DatosSolicitudReporte datos = new DatosSolicitudReporte(
                "Persona de Prueba",
                "2026-10-13",
                5,
                5,
                SeveridadDepresion.LEVE,
                SeveridadAnsiedad.LEVE,
                false,
                false,
                List.of(),
                List.of()
        );

        servidorSimulado.expect(requestTo(URL_BASE + "/api/reportes/generar"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Clave-Interna", CLAVE_SECRETA_PRUEBA))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> adaptador.generarReportePdf(datos))
                .isInstanceOf(ExcepcionServicioReportesNoDisponible.class)
                .hasMessageContaining("El servicio de generación de reportes no está disponible en este momento");

        servidorSimulado.verify();
    }

    @Test
    @DisplayName("Debe lanzar ExcepcionServicioReportesNoDisponible cuando la respuesta esta vacia")
    void debeLanzarExcepcionCuandoRespuestaEstaVacia() {
        DatosSolicitudReporte datos = new DatosSolicitudReporte(
                "Persona de Prueba",
                "2026-10-13",
                0,
                0,
                SeveridadDepresion.MINIMA,
                SeveridadAnsiedad.MINIMA,
                false,
                true,
                List.of(),
                List.of()
        );

        servidorSimulado.expect(requestTo(URL_BASE + "/api/reportes/generar"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(new byte[0], MediaType.APPLICATION_PDF));

        assertThatThrownBy(() -> adaptador.generarReportePdf(datos))
                .isInstanceOf(ExcepcionServicioReportesNoDisponible.class);

        servidorSimulado.verify();
    }
}
