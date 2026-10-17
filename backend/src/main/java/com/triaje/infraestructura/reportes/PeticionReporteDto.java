package com.triaje.infraestructura.reportes;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.triaje.aplicacion.dto.DatosSolicitudReporte;

import java.util.List;

/**
 * Formato de transferencia JSON para enviar la solicitud al microservicio FastAPI.
 */
public record PeticionReporteDto(
        @JsonProperty("nombre_paciente") String nombrePaciente,
        @JsonProperty("fecha_evaluacion") String fechaEvaluacion,
        @JsonProperty("puntaje_phq9") int puntajePhq9,
        @JsonProperty("puntaje_gad7") int puntajeGad7,
        @JsonProperty("severidad_depresion") String severidadDepresion,
        @JsonProperty("severidad_ansiedad") String severidadAnsiedad,
        @JsonProperty("riesgo_crisis") boolean riesgoCrisis,
        @JsonProperty("resultado_minimo") boolean resultadoMinimo,
        @JsonProperty("lineas_ayuda") List<PeticionLineaAyudaDto> lineasAyuda,
        @JsonProperty("respuestas_resumen") List<PeticionItemRespuestaDto> respuestasResumen
) {
    public record PeticionLineaAyudaDto(
            @JsonProperty("nombre") String nombre,
            @JsonProperty("telefono") String telefono,
            @JsonProperty("horario") String horario,
            @JsonProperty("descripcion") String descripcion
    ) {
    }

    public record PeticionItemRespuestaDto(
            @JsonProperty("instrumento") String instrumento,
            @JsonProperty("item") int item,
            @JsonProperty("frecuencia") String frecuencia
    ) {
    }

    public static PeticionReporteDto desde(DatosSolicitudReporte datos) {
        List<PeticionLineaAyudaDto> lineas = datos.lineasAyuda().stream()
                .map(l -> new PeticionLineaAyudaDto(l.nombre(), l.telefono(), l.horario(), l.descripcion()))
                .toList();

        List<PeticionItemRespuestaDto> respuestas = datos.respuestasResumen().stream()
                .map(r -> new PeticionItemRespuestaDto(r.instrumento(), r.item(), r.frecuencia()))
                .toList();

        return new PeticionReporteDto(
                datos.nombrePaciente(),
                datos.fechaEvaluacion(),
                datos.puntajePhq9(),
                datos.puntajeGad7(),
                datos.severidadDepresion().name(),
                datos.severidadAnsiedad().name(),
                datos.riesgoCrisis(),
                datos.resultadoMinimo(),
                lineas,
                respuestas
        );
    }
}
