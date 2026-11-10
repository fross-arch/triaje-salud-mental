package com.Cesde.triaje.Dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO inmutable (Record) con los resultados agregados y anonimizados del tablero administrativo.
 */
public record RespuestaTablero(
        @JsonProperty("datos_insuficientes") boolean datosInsuficientes,
        @JsonProperty("es_demostracion") boolean esDemostracion,
        String mensaje,
        @JsonProperty("total_evaluaciones") int totalEvaluaciones,
        @JsonProperty("minimo_requerido") int minimoRequerido,
        IndicadoresTablero indicadores,
        GraficosTablero graficos,
        ActividadTablero actividad
) {

    public RespuestaTablero(boolean datosInsuficientes, boolean esDemostracion, String mensaje, int totalEvaluaciones, int minimoRequerido, IndicadoresTablero indicadores, GraficosTablero graficos) {
        this(datosInsuficientes, esDemostracion, mensaje, totalEvaluaciones, minimoRequerido, indicadores, graficos, null);
    }

    public RespuestaTablero(boolean datosInsuficientes, String mensaje, int totalEvaluaciones, int minimoRequerido, IndicadoresTablero indicadores, GraficosTablero graficos) {
        this(datosInsuficientes, false, mensaje, totalEvaluaciones, minimoRequerido, indicadores, graficos, null);
    }

    public record IndicadoresTablero(
            @JsonProperty("total_evaluaciones") int totalEvaluaciones,
            @JsonProperty("media_phq9") Double mediaPhq9,
            @JsonProperty("mediana_phq9") Double medianaPhq9,
            @JsonProperty("desviacion_phq9") Double desviacionPhq9,
            @JsonProperty("media_gad7") Double mediaGad7,
            @JsonProperty("mediana_gad7") Double medianaGad7,
            @JsonProperty("desviacion_gad7") Double desviacionGad7,
            @JsonProperty("correlacion_phq9_gad7") Double correlacionPhq9Gad7,
            @JsonProperty("total_crisis") Object totalCrisis,
            @JsonProperty("total_sin_crisis") Object totalSinCrisis,
            @JsonProperty("porcentaje_crisis") Object porcentajeCrisis,
            @JsonProperty("minimo_grupo") Integer minimoGrupo
    ) {
        public IndicadoresTablero(int totalEvaluaciones, Double mediaPhq9, Double medianaPhq9, Double desviacionPhq9, Double mediaGad7, Double medianaGad7, Double desviacionGad7, Double correlacionPhq9Gad7, Object totalCrisis, Object porcentajeCrisis, Integer minimoGrupo) {
            this(totalEvaluaciones, mediaPhq9, medianaPhq9, desviacionPhq9, mediaGad7, medianaGad7, desviacionGad7, correlacionPhq9Gad7, totalCrisis, null, porcentajeCrisis, minimoGrupo);
        }
    }

    public record GraficosTablero(
            @JsonProperty("severidad_phq9") String severidadPhq9,
            @JsonProperty("severidad_gad7") String severidadGad7,
            @JsonProperty("evaluaciones_semana") String evaluacionesSemana,
            @JsonProperty("phq9_vs_gad7") String phq9VsGad7
    ) {
    }

    public record ActividadTablero(
            IndicadoresActividad indicadores,
            GraficosActividad graficos
    ) {
        public record IndicadoresActividad(
                @JsonProperty("total_pacientes_registrados") Object totalPacientesRegistrados,
                @JsonProperty("total_pacientes_con_evaluacion") Object totalPacientesConEvaluacion,
                @JsonProperty("total_pacientes_con_cita_confirmada") Object totalPacientesConCitaConfirmada,
                @JsonProperty("citas_propuestas") Object citasPropuestas,
                @JsonProperty("citas_confirmadas") Object citasConfirmadas,
                @JsonProperty("citas_rechazadas") Object citasRechazadas,
                @JsonProperty("citas_cumplidas") Object citasCumplidas,
                @JsonProperty("citas_canceladas") Object citasCanceladas,
                @JsonProperty("citas_pospuestas") Object citasPospuestas,
                @JsonProperty("clinicos_altas") Integer clinicosAltas,
                @JsonProperty("clinicos_activados") Integer clinicosActivados,
                @JsonProperty("clinicos_desactivados") Integer clinicosDesactivados,
                @JsonProperty("clinicos_eliminados") Integer clinicosEliminados,
                @JsonProperty("consentimientos_aceptados") Object consentimientosAceptados,
                @JsonProperty("consentimientos_revocados") Object consentimientosRevocados,
                @JsonProperty("autorizaciones_otorgadas") Object autorizacionesOtorgadas,
                @JsonProperty("autorizaciones_revocadas") Object autorizacionesRevocadas,
                @JsonProperty("cuentas_eliminadas") Object cuentasEliminadas
        ) {
        }

        public record GraficosActividad(
                @JsonProperty("actividad_semanal_pacientes") String actividadSemanalPacientes,
                @JsonProperty("estado_citas") String estadoCitas,
                @JsonProperty("gestion_clinicos") String gestionClinicos,
                @JsonProperty("comparativa_pacientes") String comparativaPacientes
        ) {
        }
    }
}
