package com.triaje.infraestructura.web.controlador;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.triaje.aplicacion.casouso.CrearEvaluacion;
import com.triaje.aplicacion.casouso.FinalizarEvaluacion;
import com.triaje.aplicacion.casouso.GenerarReportePdfEvaluacion;
import com.triaje.aplicacion.casouso.ListarAutorizacionesEvaluacion;
import com.triaje.aplicacion.casouso.ListarEvaluacionesUsuario;
import com.triaje.aplicacion.casouso.ObtenerDetalleEvaluacion;
import com.triaje.aplicacion.casouso.OtorgarAutorizacionAcceso;
import com.triaje.aplicacion.casouso.RegistrarRespuestaEvaluacion;
import com.triaje.aplicacion.casouso.RevocarAutorizacionAcceso;
import com.triaje.aplicacion.dto.ComandoRegistrarRespuesta;
import com.triaje.aplicacion.dto.DatosEvaluacion;
import com.triaje.aplicacion.dto.DatosRespuestaEvaluacion;
import com.triaje.aplicacion.dto.DetalleEvaluacion;
import com.triaje.aplicacion.dto.ResultadoRegistroRespuesta;
import com.triaje.dominio.SeveridadAnsiedad;
import com.triaje.dominio.SeveridadDepresion;
import com.triaje.dominio.excepcion.ExcepcionEvaluacionIncompleta;
import com.triaje.dominio.excepcion.ExcepcionEvaluacionNoEncontrada;
import com.triaje.dominio.excepcion.ExcepcionServicioReportesNoDisponible;
import com.triaje.dominio.modelo.EstadoEvaluacion;
import com.triaje.dominio.modelo.InstrumentoCribado;
import com.triaje.dominio.modelo.Rol;
import com.triaje.infraestructura.seguridad.GestorCookieRenovacion;
import com.triaje.infraestructura.seguridad.UsuarioAutenticado;
import com.triaje.infraestructura.web.error.FabricaRespuestaError;
import com.triaje.infraestructura.web.error.ManejadorGlobalExcepciones;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ControladorTriajeTest {

    private MockMvc mockMvc;
    private CrearEvaluacion crearEvaluacion;
    private RegistrarRespuestaEvaluacion registrarRespuesta;
    private FinalizarEvaluacion finalizarEvaluacion;
    private ListarEvaluacionesUsuario listarEvaluaciones;
    private ObtenerDetalleEvaluacion obtenerDetalleEvaluacion;
    private GenerarReportePdfEvaluacion generarReportePdfEvaluacion;
    private ObjectMapper objectMapper;

    @BeforeEach
    void inicializar() {
        crearEvaluacion = mock(CrearEvaluacion.class);
        registrarRespuesta = mock(RegistrarRespuestaEvaluacion.class);
        finalizarEvaluacion = mock(FinalizarEvaluacion.class);
        listarEvaluaciones = mock(ListarEvaluacionesUsuario.class);
        obtenerDetalleEvaluacion = mock(ObtenerDetalleEvaluacion.class);
        generarReportePdfEvaluacion = mock(GenerarReportePdfEvaluacion.class);
        objectMapper = new ObjectMapper();

        ControladorTriaje controlador = new ControladorTriaje(
                crearEvaluacion,
                registrarRespuesta,
                finalizarEvaluacion,
                listarEvaluaciones,
                obtenerDetalleEvaluacion,
                generarReportePdfEvaluacion,
                mock(OtorgarAutorizacionAcceso.class),
                mock(ListarAutorizacionesEvaluacion.class),
                mock(RevocarAutorizacionAcceso.class)
        );

        HandlerMethodArgumentResolver resolvedorUsuario = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.getParameterType().equals(UsuarioAutenticado.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return new UsuarioAutenticado(1L, Rol.PACIENTE);
            }
        };

        FabricaRespuestaError fabrica = new FabricaRespuestaError(objectMapper, Clock.systemUTC());
        ManejadorGlobalExcepciones manejadorExcepciones = new ManejadorGlobalExcepciones(fabrica, mock(GestorCookieRenovacion.class));

        mockMvc = MockMvcBuilders.standaloneSetup(controlador)
                .setCustomArgumentResolvers(resolvedorUsuario)
                .setControllerAdvice(manejadorExcepciones)
                .build();
    }

    @Test
    @DisplayName("Debe crear evaluacion y retornar 201 Created")
    void debeCrearEvaluacion() throws Exception {
        DatosEvaluacion datos = new DatosEvaluacion(
                "eval-123", 1L, EstadoEvaluacion.EN_CURSO, null, null, null, null, false, null, Instant.now(), null
        );
        when(crearEvaluacion.ejecutar(1L)).thenReturn(datos);

        mockMvc.perform(post("/api/triaje/evaluaciones"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("eval-123"))
                .andExpect(jsonPath("$.estado").value("EN_CURSO"));
    }

    @Test
    @DisplayName("Debe guardar respuesta de item y retornar riesgoCrisis")
    void debeGuardarRespuestaItem() throws Exception {
        ResultadoRegistroRespuesta resultado = new ResultadoRegistroRespuesta(
                "eval-123", InstrumentoCribado.PHQ9, 9, true
        );
        when(registrarRespuesta.ejecutar(any(ComandoRegistrarRespuesta.class))).thenReturn(resultado);

        mockMvc.perform(put("/api/triaje/evaluaciones/eval-123/respuestas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "instrumento", "PHQ9",
                                "indicePregunta", 9,
                                "valorPuntaje", 1
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evaluacionId").value("eval-123"))
                .andExpect(jsonPath("$.instrumento").value("PHQ9"))
                .andExpect(jsonPath("$.indicePregunta").value(9))
                .andExpect(jsonPath("$.riesgoCrisis").value(true));
    }

    @Test
    @DisplayName("Debe finalizar evaluacion y devolver puntajes y severidades")
    void debeFinalizarEvaluacion() throws Exception {
        DatosEvaluacion datos = new DatosEvaluacion(
                "eval-123", 1L, EstadoEvaluacion.COMPLETADA, 0, 0,
                SeveridadDepresion.MINIMA, SeveridadAnsiedad.MINIMA, false, true, Instant.now(), Instant.now()
        );
        when(finalizarEvaluacion.ejecutar("eval-123", 1L)).thenReturn(datos);

        mockMvc.perform(post("/api/triaje/evaluaciones/eval-123/finalizar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("COMPLETADA"))
                .andExpect(jsonPath("$.severidadDepresion").value("MINIMA"))
                .andExpect(jsonPath("$.severidadAnsiedad").value("MINIMA"))
                .andExpect(jsonPath("$.resultadoMinimo").value(true));
    }

    @Test
    @DisplayName("Debe obtener detalle de evaluacion con respuestas")
    void debeObtenerDetalleEvaluacion() throws Exception {
        DatosEvaluacion datos = new DatosEvaluacion(
                "eval-123", 1L, EstadoEvaluacion.COMPLETADA, 0, 0,
                SeveridadDepresion.MINIMA, SeveridadAnsiedad.MINIMA, false, true, Instant.now(), Instant.now()
        );
        List<DatosRespuestaEvaluacion> respuestas = List.of(
                new DatosRespuestaEvaluacion(InstrumentoCribado.PHQ9, 1, 0),
                new DatosRespuestaEvaluacion(InstrumentoCribado.GAD7, 1, 0)
        );
        DetalleEvaluacion detalle = new DetalleEvaluacion(datos, respuestas);
        when(obtenerDetalleEvaluacion.ejecutar("eval-123", 1L)).thenReturn(detalle);

        mockMvc.perform(get("/api/triaje/evaluaciones/eval-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evaluacion.id").value("eval-123"))
                .andExpect(jsonPath("$.respuestas", hasSize(2)));
    }

    @Test
    @DisplayName("Debe descargar PDF con Content-Type y Content-Disposition correctos")
    void debeDescargarPdfConCabecerasCorrectas() throws Exception {
        byte[] pdfBytes = "%PDF-1.4 contenido de prueba".getBytes();
        when(generarReportePdfEvaluacion.ejecutar("eval-123", 1L)).thenReturn(pdfBytes);

        mockMvc.perform(get("/api/triaje/evaluaciones/eval-123/pdf"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"reporte-cribado.pdf\""))
                .andExpect(content().bytes(pdfBytes));
    }

    @Test
    @DisplayName("Debe retornar 400 Bad Request cuando la evaluacion no esta completada")
    void debeRetornar400CuandoEvaluacionEstaIncompleta() throws Exception {
        when(generarReportePdfEvaluacion.ejecutar("eval-123", 1L))
                .thenThrow(new ExcepcionEvaluacionIncompleta("Solo se puede descargar el reporte de una evaluacion en estado COMPLETADA."));

        mockMvc.perform(get("/api/triaje/evaluaciones/eval-123/pdf"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("EVALUACION_INCOMPLETA"))
                .andExpect(jsonPath("$.mensaje").value("Solo se puede descargar el reporte de una evaluacion en estado COMPLETADA."));
    }

    @Test
    @DisplayName("Debe retornar 404 Not Found cuando la evaluacion no existe o no pertenece al usuario")
    void debeRetornar404CuandoEvaluacionNoExiste() throws Exception {
        when(generarReportePdfEvaluacion.ejecutar("eval-123", 1L))
                .thenThrow(new ExcepcionEvaluacionNoEncontrada("La evaluación no existe o no tienes acceso a ella."));

        mockMvc.perform(get("/api/triaje/evaluaciones/eval-123/pdf"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("EVALUACION_NO_ENCONTRADA"));
    }

    @Test
    @DisplayName("Debe retornar 503 Service Unavailable cuando el microservicio de reportes no responde")
    void debeRetornar503CuandoServicioReportesNoEstaDisponible() throws Exception {
        when(generarReportePdfEvaluacion.ejecutar("eval-123", 1L))
                .thenThrow(new ExcepcionServicioReportesNoDisponible("El servicio de generación de reportes no está disponible en este momento. Por favor, intente más tarde."));

        mockMvc.perform(get("/api/triaje/evaluaciones/eval-123/pdf"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("SERVICIO_NO_DISPONIBLE"))
                .andExpect(jsonPath("$.mensaje").value("El servicio de generación de reportes no está disponible en este momento. Por favor, intente más tarde."));
    }
}

