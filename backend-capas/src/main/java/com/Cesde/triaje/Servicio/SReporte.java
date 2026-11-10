package com.Cesde.triaje.Servicio;

import com.Cesde.triaje.Configuracion.PropiedadesRecursosAyuda;
import com.Cesde.triaje.Excepcion.ExcepcionServicioReportesNoDisponible;
import com.Cesde.triaje.Modelo.CatalogoPreguntasCribado;
import com.Cesde.triaje.Modelo.Enumeracion.InstrumentoCribado;
import com.Cesde.triaje.Modelo.MEvaluacion;
import com.Cesde.triaje.Modelo.MRespuestaEvaluacion;
import com.Cesde.triaje.Modelo.MUsuario;
import com.Cesde.triaje.Regla.ReglaResultadoMinimo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class SReporte {

    private static final Logger REGISTRO = LoggerFactory.getLogger(SReporte.class);
    private static final String CABECERA_CLAVE_INTERNA = "X-Clave-Interna";

    private final RestClient clienteRest;
    private final String claveInterna;
    private final PropiedadesRecursosAyuda propiedadesRecursos;
    private final ReglaResultadoMinimo reglaResultadoMinimo = new ReglaResultadoMinimo();

    public SReporte(
            RestClient.Builder constructorRestClient,
            @Value("${triaje.reportes.url:${reportes.url:${TRIAJE_REPORTES_URL:http://127.0.0.1:8000}}}") String urlReportes,
            @Value("${triaje.reportes.clave-interna:${reportes.clave-interna:${CLAVE_INTERNA:}}}") String claveInterna,
            @Value("${triaje.reportes.tiempo-espera-conexion-segundos:5}") int tiempoEsperaConexionSegundos,
            @Value("${triaje.reportes.tiempo-espera-lectura-segundos:15}") int tiempoEsperaLecturaSegundos,
            PropiedadesRecursosAyuda propiedadesRecursos) {

        String url = (urlReportes != null && !urlReportes.isBlank()) ? urlReportes : "http://127.0.0.1:8000";
        this.claveInterna = claveInterna != null ? claveInterna : "";
        this.propiedadesRecursos = Objects.requireNonNull(propiedadesRecursos, "Las propiedades de recursos son obligatorias");

        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(tiempoEsperaConexionSegundos > 0 ? tiempoEsperaConexionSegundos : 5));
        fabrica.setReadTimeout(Duration.ofSeconds(tiempoEsperaLecturaSegundos > 0 ? tiempoEsperaLecturaSegundos : 15));

        this.clienteRest = constructorRestClient
                .baseUrl(url)
                .requestFactory(fabrica)
                .build();
    }

    public byte[] generarReportePdf(MEvaluacion evaluacion, MUsuario paciente, List<MRespuestaEvaluacion> respuestas) {
        try {
            boolean resultadoMinimo = reglaResultadoMinimo.evaluar(
                    evaluacion.getSeveridadDepresion(),
                    evaluacion.getSeveridadAnsiedad(),
                    evaluacion.isRiesgoCrisis()
            );

            Instant fechaReferencia = evaluacion.getFechaFinalizacion() != null
                    ? evaluacion.getFechaFinalizacion()
                    : evaluacion.getFechaCreacion();

            String fechaEvaluacion = DateTimeFormatter.ISO_LOCAL_DATE
                    .withZone(ZoneOffset.UTC)
                    .format(fechaReferencia);

            List<Map<String, Object>> lineasAyuda = propiedadesRecursos.getRecursosAyuda().stream()
                    .map(r -> Map.<String, Object>of(
                            "nombre", r.getNombre(),
                            "telefono", r.getTelefono(),
                            "horario", r.getHorario(),
                            "descripcion", r.getDescripcion()
                    ))
                    .toList();

            List<Map<String, Object>> respuestasResumen = respuestas.stream()
                    .sorted((r1, r2) -> {
                        int comp = r1.getInstrumento().compareTo(r2.getInstrumento());
                        return comp != 0 ? comp : Integer.compare(r1.getIndicePregunta(), r2.getIndicePregunta());
                    })
                    .map(r -> Map.<String, Object>of(
                            "instrumento", r.getInstrumento() == InstrumentoCribado.PHQ9 ? "PHQ-9" : "GAD-7",
                            "item", r.getIndicePregunta(),
                            "frecuencia", CatalogoPreguntasCribado.obtenerEtiquetaFrecuencia(r.getValorPuntaje())
                    ))
                    .toList();

            Map<String, Object> peticion = new HashMap<>();
            peticion.put("nombre_paciente", paciente.getNombreCompleto());
            peticion.put("fecha_evaluacion", fechaEvaluacion);
            peticion.put("puntaje_phq9", evaluacion.getPuntajePhq9() != null ? evaluacion.getPuntajePhq9() : 0);
            peticion.put("puntaje_gad7", evaluacion.getPuntajeGad7() != null ? evaluacion.getPuntajeGad7() : 0);
            peticion.put("severidad_depresion", evaluacion.getSeveridadDepresion().name());
            peticion.put("severidad_ansiedad", evaluacion.getSeveridadAnsiedad().name());
            peticion.put("riesgo_crisis", evaluacion.isRiesgoCrisis());
            peticion.put("resultado_minimo", resultadoMinimo);
            peticion.put("lineas_ayuda", lineasAyuda);
            peticion.put("respuestas_resumen", respuestasResumen);

            byte[] resultado = clienteRest.post()
                    .uri("/api/reportes/generar")
                    .header(CABECERA_CLAVE_INTERNA, claveInterna)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_PDF)
                    .body(peticion)
                    .retrieve()
                    .body(byte[].class);

            if (resultado == null || resultado.length == 0) {
                REGISTRO.error("El microservicio de reportes devolvió un cuerpo vacío");
                throw new ExcepcionServicioReportesNoDisponible(
                        "El servicio de generación de reportes no está disponible en este momento. Por favor, intente más tarde."
                );
            }
            return resultado;
        } catch (RestClientException e) {
            REGISTRO.error("Error al comunicar con el microservicio de reportes ({})", e.getClass().getSimpleName());
            throw new ExcepcionServicioReportesNoDisponible(
                    "El servicio de generación de reportes no está disponible en este momento. Por favor, intente más tarde.",
                    e
            );
        }
    }

    public com.Cesde.triaje.Dto.RespuestaTablero generarTablero(Map<String, Object> peticion) {
        try {
            com.Cesde.triaje.Dto.RespuestaTablero resultado = clienteRest.post()
                    .uri("/tablero")
                    .header(CABECERA_CLAVE_INTERNA, claveInterna)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(peticion)
                    .retrieve()
                    .body(com.Cesde.triaje.Dto.RespuestaTablero.class);

            if (resultado == null) {
                REGISTRO.error("El microservicio de reportes devolvió una respuesta nula para el tablero");
                throw new ExcepcionServicioReportesNoDisponible(
                        "El servicio de generación de reportes no está disponible en este momento. Por favor, intente más tarde."
                );
            }
            return resultado;
        } catch (RestClientException e) {
            REGISTRO.error("Error al comunicar con el microservicio de reportes para el tablero ({})", e.getClass().getSimpleName());
            throw new ExcepcionServicioReportesNoDisponible(
                    "El servicio de generación de reportes no está disponible en este momento. Por favor, intente más tarde.",
                    e
            );
        }
    }

    public byte[] generarPdfTablero(Map<String, Object> peticion) {
        try {
            byte[] resultado = clienteRest.post()
                    .uri("/tablero/exportar-pdf")
                    .header(CABECERA_CLAVE_INTERNA, claveInterna)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_PDF)
                    .body(peticion)
                    .retrieve()
                    .body(byte[].class);

            if (resultado == null || resultado.length == 0) {
                REGISTRO.error("El microservicio de reportes devolvió un PDF vacío para el tablero");
                throw new ExcepcionServicioReportesNoDisponible(
                        "El servicio de generación de reportes no está disponible en este momento. Por favor, intente más tarde."
                );
            }
            return resultado;
        } catch (RestClientException e) {
            REGISTRO.error("Error al comunicar con el microservicio de reportes para exportar PDF del tablero ({})", e.getClass().getSimpleName());
            throw new ExcepcionServicioReportesNoDisponible(
                    "El servicio de generación de reportes no está disponible en este momento. Por favor, intente más tarde.",
                    e
            );
        }
    }
}

