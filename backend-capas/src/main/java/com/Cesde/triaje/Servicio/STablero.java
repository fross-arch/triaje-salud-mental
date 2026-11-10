package com.Cesde.triaje.Servicio;

import com.Cesde.triaje.Dto.RespuestaTablero;
import com.Cesde.triaje.Excepcion.ExcepcionDatoInvalido;
import com.Cesde.triaje.Modelo.Enumeracion.AccionAuditoria;
import com.Cesde.triaje.Modelo.Enumeracion.EstadoCita;
import com.Cesde.triaje.Modelo.Enumeracion.EstadoEvaluacion;
import com.Cesde.triaje.Modelo.Enumeracion.RolUsuario;
import com.Cesde.triaje.Repositorio.ICitaRepositorio;
import com.Cesde.triaje.Repositorio.IEvaluacionAnonimaProjection;
import com.Cesde.triaje.Repositorio.IEvaluacionRepositorio;
import com.Cesde.triaje.Repositorio.IEventoActividadProjection;
import com.Cesde.triaje.Repositorio.IFechaCreacionProjection;
import com.Cesde.triaje.Repositorio.IRegistroAuditoriaRepositorio;
import com.Cesde.triaje.Repositorio.IUsuarioRepositorio;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Transactional
public class STablero {

    private static final Logger REGISTRO = LoggerFactory.getLogger(STablero.class);
    private static final ZoneId ZONA_BOGOTA = ZoneId.of("America/Bogota");
    private static final DateTimeFormatter FORMATEADOR_FECHA = DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZONA_BOGOTA);

    private static final List<AccionAuditoria> ACCIONES_ACTIVIDAD = List.of(
            AccionAuditoria.EVALUACION_COMPLETADA,
            AccionAuditoria.CONSENTIMIENTO_ACEPTADO,
            AccionAuditoria.CONSENTIMIENTO_REVOCADO,
            AccionAuditoria.CUENTA_ELIMINADA,
            AccionAuditoria.AUTORIZACION_OTORGADA,
            AccionAuditoria.AUTORIZACION_REVOCADA,
            AccionAuditoria.CITA_PROPUESTA,
            AccionAuditoria.CITA_CONFIRMADA,
            AccionAuditoria.CITA_RECHAZADA,
            AccionAuditoria.CITA_CANCELADA,
            AccionAuditoria.CITA_CUMPLIDA,
            AccionAuditoria.CITA_POSPUESTA,
            AccionAuditoria.CLINICO_CREADO,
            AccionAuditoria.CLINICO_ACTIVADO,
            AccionAuditoria.CLINICO_DESACTIVADO,
            AccionAuditoria.CLINICO_ELIMINADO
    );

    private final IEvaluacionRepositorio evaluacionRepositorio;
    private final IRegistroAuditoriaRepositorio registroAuditoriaRepositorio;
    private final IUsuarioRepositorio usuarioRepositorio;
    private final ICitaRepositorio citaRepositorio;
    private final SReporte reporteServicio;
    private final SAuditoria auditoriaServicio;
    private final int minimoGrupo;
    private final boolean modoDemo;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Autowired
    public STablero(
            IEvaluacionRepositorio evaluacionRepositorio,
            IRegistroAuditoriaRepositorio registroAuditoriaRepositorio,
            IUsuarioRepositorio usuarioRepositorio,
            ICitaRepositorio citaRepositorio,
            SReporte reporteServicio,
            SAuditoria auditoriaServicio,
            @Value("${triaje.tablero.minimo-grupo:5}") int minimoGrupo,
            @Value("${triaje.tablero.modo-demo:false}") boolean modoDemo) {
        this.evaluacionRepositorio = Objects.requireNonNull(evaluacionRepositorio, "El repositorio de evaluación es obligatorio");
        this.registroAuditoriaRepositorio = registroAuditoriaRepositorio;
        this.usuarioRepositorio = usuarioRepositorio;
        this.citaRepositorio = citaRepositorio;
        this.reporteServicio = Objects.requireNonNull(reporteServicio, "El servicio de reporte es obligatorio");
        this.auditoriaServicio = Objects.requireNonNull(auditoriaServicio, "El servicio de auditoría es obligatorio");
        this.minimoGrupo = minimoGrupo > 0 ? minimoGrupo : 5;
        this.modoDemo = modoDemo;
    }

    public STablero(
            IEvaluacionRepositorio evaluacionRepositorio,
            SReporte reporteServicio,
            SAuditoria auditoriaServicio,
            int minimoGrupo,
            boolean modoDemo) {
        this(evaluacionRepositorio, null, null, null, reporteServicio, auditoriaServicio, minimoGrupo, modoDemo);
    }

    public record ArchivoExportado(String nombreArchivo, String tipoContenido, byte[] contenido) {
    }

    /**
     * Obtiene estadísticas agregadas del tablero para administradores.
     * Valida el parámetro de días, consulta únicamente proyecciones anónimas (sin ids ni usuarios),
     * registra la auditoría administrativa y remite los datos al microservicio Python.
     *
     * @param adminId Identificador del usuario administrador que realiza la consulta.
     * @param dias Rango de días hacia atrás (0 = histórico completo, 7, 30 o 90).
     * @return DTO con estadísticas agregadas y 8 gráficos en base64.
     */
    public RespuestaTablero obtenerTablero(Long adminId, int dias) {
        if (dias != 0 && dias != 7 && dias != 30 && dias != 90) {
            throw new ExcepcionDatoInvalido("El parámetro 'dias' solo permite los valores: 0, 7, 30 o 90.");
        }

        if (modoDemo) {
            REGISTRO.info("Generando tablero en MODO DEMOSTRACIÓN (datos sintéticos) para periodo de {} días", dias);
            Map<String, Object> peticion = new HashMap<>();
            peticion.put("demo", true);
            peticion.put("minimo_grupo", minimoGrupo);
            peticion.put("registros", List.of());
            peticion.put("eventos_actividad", List.of());

            RespuestaTablero respuesta = reporteServicio.generarTablero(peticion);
            if (adminId != null) {
                auditoriaServicio.registrar(adminId, AccionAuditoria.TABLERO_CONSULTADO, dias + " (MODO_DEMO)");
            }
            return respuesta;
        }

        List<IEvaluacionAnonimaProjection> proyecciones;
        List<IEventoActividadProjection> eventosProyecciones = List.of();
        List<IFechaCreacionProjection> fechasPacientes = List.of();

        if (dias > 0) {
            Instant fechaDesde = Instant.now().minus(Duration.ofDays(dias));
            proyecciones = evaluacionRepositorio.findByEstadoAndFechaFinalizacionGreaterThanEqualOrderByFechaFinalizacionAsc(
                    EstadoEvaluacion.COMPLETADA, fechaDesde
            );
            if (registroAuditoriaRepositorio != null) {
                eventosProyecciones = registroAuditoriaRepositorio.findActividadDesde(ACCIONES_ACTIVIDAD, fechaDesde);
            }
            if (usuarioRepositorio != null) {
                fechasPacientes = usuarioRepositorio.findFechasCreacionPorRolDesde(RolUsuario.PACIENTE, fechaDesde);
            }
        } else {
            proyecciones = evaluacionRepositorio.findByEstadoOrderByFechaFinalizacionAsc(
                    EstadoEvaluacion.COMPLETADA
            );
            if (registroAuditoriaRepositorio != null) {
                eventosProyecciones = registroAuditoriaRepositorio.findActividadHistorica(ACCIONES_ACTIVIDAD);
            }
            if (usuarioRepositorio != null) {
                fechasPacientes = usuarioRepositorio.findFechasCreacionPorRol(RolUsuario.PACIENTE);
            }
        }

        List<Map<String, Object>> registrosAnonimos = proyecciones.stream()
                .filter(p -> p.getFechaFinalizacion() != null)
                .map(p -> {
                    Map<String, Object> reg = new HashMap<>();
                    reg.put("fecha", FORMATEADOR_FECHA.format(p.getFechaFinalizacion()));
                    reg.put("puntaje_phq9", p.getPuntajePhq9() != null ? p.getPuntajePhq9() : 0);
                    reg.put("puntaje_gad7", p.getPuntajeGad7() != null ? p.getPuntajeGad7() : 0);
                    reg.put("severidad_depresion", p.getSeveridadDepresion() != null ? p.getSeveridadDepresion().name() : "MINIMA");
                    reg.put("severidad_ansiedad", p.getSeveridadAnsiedad() != null ? p.getSeveridadAnsiedad().name() : "MINIMA");
                    reg.put("riesgo_crisis", p.isRiesgoCrisis());
                    return reg;
                })
                .toList();

        List<Map<String, Object>> eventosAnonimos = new ArrayList<>();
        if (eventosProyecciones != null) {
            for (IEventoActividadProjection ev : eventosProyecciones) {
                if (ev.getFechaCreacion() != null && ev.getAccion() != null) {
                    eventosAnonimos.add(Map.of(
                            "fecha", FORMATEADOR_FECHA.format(ev.getFechaCreacion()),
                            "tipo", ev.getAccion().name()
                    ));
                }
            }
        }
        if (fechasPacientes != null) {
            for (IFechaCreacionProjection fp : fechasPacientes) {
                if (fp.getFechaCreacion() != null) {
                    eventosAnonimos.add(Map.of(
                            "fecha", FORMATEADOR_FECHA.format(fp.getFechaCreacion()),
                            "tipo", "REGISTRO_PACIENTE"
                    ));
                }
            }
        }

        long totalPacientesReg = usuarioRepositorio != null ? usuarioRepositorio.countByRol(RolUsuario.PACIENTE) : 0;
        long totalPacientesEval = evaluacionRepositorio.countDistinctUsuariosPorEstado(EstadoEvaluacion.COMPLETADA);
        long totalPacientesCita = citaRepositorio != null ? citaRepositorio.countDistinctPacientesPorEstado(EstadoCita.CONFIRMADA) : 0;

        REGISTRO.info("Generando tablero para periodo de {} días con {} evaluaciones anónimas y {} eventos de actividad",
                dias, registrosAnonimos.size(), eventosAnonimos.size());

        Map<String, Object> peticion = new HashMap<>();
        peticion.put("demo", false);
        peticion.put("registros", registrosAnonimos);
        peticion.put("eventos_actividad", eventosAnonimos);
        peticion.put("total_pacientes_registrados", (int) totalPacientesReg);
        peticion.put("total_pacientes_con_evaluacion", (int) totalPacientesEval);
        peticion.put("total_pacientes_con_cita_confirmada", (int) totalPacientesCita);
        peticion.put("minimo_grupo", minimoGrupo);

        RespuestaTablero respuesta = reporteServicio.generarTablero(peticion);
        if (adminId != null) {
            auditoriaServicio.registrar(adminId, AccionAuditoria.TABLERO_CONSULTADO, String.valueOf(dias));
        }
        return respuesta;
    }

    public RespuestaTablero obtenerTablero(int dias) {
        return obtenerTablero(null, dias);
    }

    /**
     * Exporta el tablero agregado en formato CSV o PDF sin identificadores personales.
     * Construye el documento a partir de la MISMA respuesta obtenida en pantalla.
     *
     * @param adminId Identificador del administrador.
     * @param dias Periodo de análisis (0, 7, 30 o 90).
     * @param formato Formato de salida ("csv" o "pdf").
     * @return ArchivoExportado con nombre, tipo de contenido y bytes.
     */
    public ArchivoExportado exportarTablero(Long adminId, int dias, String formato) {
        if (dias != 0 && dias != 7 && dias != 30 && dias != 90) {
            throw new ExcepcionDatoInvalido("El parámetro 'dias' solo permite los valores: 0, 7, 30 o 90.");
        }
        if (formato == null || (!formato.equalsIgnoreCase("csv") && !formato.equalsIgnoreCase("pdf"))) {
            throw new ExcepcionDatoInvalido("El parámetro 'formato' solo permite los valores: csv o pdf.");
        }

        String formatoNorm = formato.trim().toLowerCase();
        RespuestaTablero tablero = obtenerTablero(null, dias);

        LocalDate hoy = LocalDate.now(ZONA_BOGOTA);
        String prefijo = (modoDemo || tablero.esDemostracion()) ? "DEMO_" : "";
        String nombreArchivo = prefijo + "tablero_" + hoy + "_" + dias + "d." + formatoNorm;

        byte[] contenido;
        String tipoContenido;

        if ("csv".equals(formatoNorm)) {
            contenido = construirCsvTablero(tablero, dias);
            tipoContenido = "text/csv; charset=UTF-8";
        } else {
            Map<String, Object> peticionPdf = new HashMap<>();
            Map<String, Object> datosMap = objectMapper.convertValue(tablero, new TypeReference<>() {});
            peticionPdf.put("datos", datosMap);
            peticionPdf.put("dias", dias);
            peticionPdf.put("fecha_reporte", hoy.toString());

            contenido = reporteServicio.generarPdfTablero(peticionPdf);
            tipoContenido = "application/pdf";
        }

        String detalleAuditoria = formatoNorm.toUpperCase() + " (" + dias + "d)" + ((modoDemo || tablero.esDemostracion()) ? " (MODO_DEMO)" : "");
        if (adminId != null) {
            auditoriaServicio.registrar(adminId, AccionAuditoria.TABLERO_EXPORTADO, detalleAuditoria);
        }

        return new ArchivoExportado(nombreArchivo, tipoContenido, contenido);
    }

    private byte[] construirCsvTablero(RespuestaTablero tablero, int dias) {
        StringBuilder sb = new StringBuilder();
        sb.append('\uFEFF'); // BOM UTF-8

        String periodoStr = dias == 0 ? "Histórico completo" : ("Últimos " + dias + " días");
        sb.append(escaparCsv("INFORME ESTADÍSTICO AGREGADO DEL TABLERO")).append(';')
                .append(escaparCsv(periodoStr)).append("\r\n");

        if (tablero.esDemostracion() || modoDemo) {
            sb.append(escaparCsv("MARCA_DEMO")).append(';')
                    .append(escaparCsv("DATOS DE DEMOSTRACIÓN — Cifras sintéticas sin personas reales")).append("\r\n");
        }

        sb.append("\r\n");
        sb.append(escaparCsv("SECCIÓN")).append(';')
                .append(escaparCsv("1. INDICADORES DE CRIBADO CLÍNICO (PHQ-9 Y GAD-7)")).append("\r\n");
        sb.append(escaparCsv("Indicador")).append(';')
                .append(escaparCsv("Valor")).append("\r\n");

        RespuestaTablero.IndicadoresTablero ind = tablero.indicadores();
        if (ind != null) {
            sb.append(escaparCsv("Total evaluaciones completadas")).append(';')
                    .append(escaparCsv(String.valueOf(ind.totalEvaluaciones()))).append("\r\n");
            sb.append(escaparCsv("Media PHQ-9 (Depresión)")).append(';')
                    .append(escaparCsv(ind.mediaPhq9() != null ? String.valueOf(ind.mediaPhq9()) : "N/D")).append("\r\n");
            sb.append(escaparCsv("Mediana PHQ-9")).append(';')
                    .append(escaparCsv(ind.medianaPhq9() != null ? String.valueOf(ind.medianaPhq9()) : "N/D")).append("\r\n");
            sb.append(escaparCsv("Desviación estándar PHQ-9")).append(';')
                    .append(escaparCsv(ind.desviacionPhq9() != null ? String.valueOf(ind.desviacionPhq9()) : "N/D")).append("\r\n");
            sb.append(escaparCsv("Media GAD-7 (Ansiedad)")).append(';')
                    .append(escaparCsv(ind.mediaGad7() != null ? String.valueOf(ind.mediaGad7()) : "N/D")).append("\r\n");
            sb.append(escaparCsv("Mediana GAD-7")).append(';')
                    .append(escaparCsv(ind.medianaGad7() != null ? String.valueOf(ind.medianaGad7()) : "N/D")).append("\r\n");
            sb.append(escaparCsv("Desviación estándar GAD-7")).append(';')
                    .append(escaparCsv(ind.desviacionGad7() != null ? String.valueOf(ind.desviacionGad7()) : "N/D")).append("\r\n");
            sb.append(escaparCsv("Correlación Pearson PHQ-9 vs GAD-7")).append(';')
                    .append(escaparCsv(ind.correlacionPhq9Gad7() != null ? String.valueOf(ind.correlacionPhq9Gad7()) : "N/D")).append("\r\n");
            sb.append(escaparCsv("Casos con riesgo de crisis")).append(';')
                    .append(escaparCsv(String.valueOf(ind.totalCrisis()))).append("\r\n");
            sb.append(escaparCsv("Casos sin riesgo de crisis")).append(';')
                    .append(escaparCsv(ind.totalSinCrisis() != null ? String.valueOf(ind.totalSinCrisis()) : "0")).append("\r\n");
            sb.append(escaparCsv("Porcentaje de casos con riesgo de crisis")).append(';')
                    .append(escaparCsv(ind.porcentajeCrisis() != null ? (ind.porcentajeCrisis() + "%") : "N/D")).append("\r\n");
        } else {
            sb.append(escaparCsv("Estado")).append(';')
                    .append(escaparCsv("Datos insuficientes para agregación psicométrica (<" + tablero.minimoRequerido() + " casos)")).append("\r\n");
        }

        sb.append("\r\n");
        sb.append(escaparCsv("SECCIÓN")).append(';')
                .append(escaparCsv("2. ACTIVIDAD DEL SISTEMA")).append("\r\n");
        sb.append(escaparCsv("Indicador")).append(';')
                .append(escaparCsv("Valor")).append("\r\n");

        RespuestaTablero.ActividadTablero act = tablero.actividad();
        if (act != null && act.indicadores() != null) {
            var aInd = act.indicadores();
            sb.append(escaparCsv("Pacientes registrados (distintos)")).append(';')
                    .append(escaparCsv(String.valueOf(aInd.totalPacientesRegistrados()))).append("\r\n");
            sb.append(escaparCsv("Pacientes con al menos una evaluación completada")).append(';')
                    .append(escaparCsv(String.valueOf(aInd.totalPacientesConEvaluacion()))).append("\r\n");
            sb.append(escaparCsv("Pacientes con al menos una cita confirmada")).append(';')
                    .append(escaparCsv(String.valueOf(aInd.totalPacientesConCitaConfirmada()))).append("\r\n");
            sb.append(escaparCsv("Citas propuestas (por clínico)")).append(';')
                    .append(escaparCsv(String.valueOf(aInd.citasPropuestas()))).append("\r\n");
            sb.append(escaparCsv("Citas confirmadas (por paciente)")).append(';')
                    .append(escaparCsv(String.valueOf(aInd.citasConfirmadas()))).append("\r\n");
            sb.append(escaparCsv("Citas rechazadas (por paciente)")).append(';')
                    .append(escaparCsv(String.valueOf(aInd.citasRechazadas()))).append("\r\n");
            sb.append(escaparCsv("Citas cumplidas")).append(';')
                    .append(escaparCsv(String.valueOf(aInd.citasCumplidas()))).append("\r\n");
            sb.append(escaparCsv("Citas canceladas")).append(';')
                    .append(escaparCsv(String.valueOf(aInd.citasCanceladas()))).append("\r\n");
            sb.append(escaparCsv("Citas pospuestas")).append(';')
                    .append(escaparCsv(String.valueOf(aInd.citasPospuestas()))).append("\r\n");
            sb.append(escaparCsv("Altas de profesionales clínicos")).append(';')
                    .append(escaparCsv(String.valueOf(aInd.clinicosAltas()))).append("\r\n");
            sb.append(escaparCsv("Cuentas clínicas activadas")).append(';')
                    .append(escaparCsv(String.valueOf(aInd.clinicosActivados()))).append("\r\n");
            sb.append(escaparCsv("Cuentas clínicas desactivadas")).append(';')
                    .append(escaparCsv(String.valueOf(aInd.clinicosDesactivados()))).append("\r\n");
            sb.append(escaparCsv("Cuentas clínicas eliminadas")).append(';')
                    .append(escaparCsv(String.valueOf(aInd.clinicosEliminados()))).append("\r\n");
            sb.append(escaparCsv("Consentimientos aceptados")).append(';')
                    .append(escaparCsv(String.valueOf(aInd.consentimientosAceptados()))).append("\r\n");
            sb.append(escaparCsv("Consentimientos revocados")).append(';')
                    .append(escaparCsv(String.valueOf(aInd.consentimientosRevocados()))).append("\r\n");
            sb.append(escaparCsv("Autorizaciones de acceso otorgadas")).append(';')
                    .append(escaparCsv(String.valueOf(aInd.autorizacionesOtorgadas()))).append("\r\n");
            sb.append(escaparCsv("Autorizaciones de acceso revocadas")).append(';')
                    .append(escaparCsv(String.valueOf(aInd.autorizacionesRevocadas()))).append("\r\n");
            sb.append(escaparCsv("Cuentas de usuario eliminadas")).append(';')
                    .append(escaparCsv(String.valueOf(aInd.cuentasEliminadas()))).append("\r\n");
        }

        sb.append("\r\n");
        sb.append(escaparCsv("AVISO_PRIVACIDAD")).append(';')
                .append(escaparCsv("Datos protegidos bajo umbral mínimo de grupo N=" + tablero.minimoRequerido() + " y supresión complementaria. No contiene listas nominales.")).append("\r\n");

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String escaparCsv(String valor) {
        if (valor == null) {
            return "";
        }
        String texto = valor;
        if (!texto.isEmpty() && (texto.startsWith("=") || texto.startsWith("+") || texto.startsWith("-") || texto.startsWith("@"))) {
            texto = "'" + texto;
        }
        if (texto.contains(";") || texto.contains("\"") || texto.contains("\n") || texto.contains("\r")) {
            texto = "\"" + texto.replace("\"", "\"\"") + "\"";
        }
        return texto;
    }
}
