package com.triaje.aplicacion.casouso;

import com.triaje.aplicacion.dto.DatosSolicitudReporte;
import com.triaje.aplicacion.dto.DatosSolicitudReporte.DatosItemRespuestaReporte;
import com.triaje.aplicacion.dto.DatosSolicitudReporte.DatosLineaAyudaReporte;
import com.triaje.aplicacion.puerto.ClienteReportes;
import com.triaje.aplicacion.puerto.EjecutorTransaccional;
import com.triaje.aplicacion.puerto.RepositorioAuditoria;
import com.triaje.aplicacion.puerto.RepositorioAutorizaciones;
import com.triaje.aplicacion.puerto.RepositorioEvaluaciones;
import com.triaje.aplicacion.puerto.RepositorioRespuestasEvaluacion;
import com.triaje.aplicacion.puerto.RepositorioUsuarios;
import com.triaje.dominio.Autorizacion;
import com.triaje.dominio.PoliticaAccesoClinico;
import com.triaje.dominio.ReglaResultadoMinimo;
import com.triaje.dominio.excepcion.ExcepcionEvaluacionIncompleta;
import com.triaje.dominio.excepcion.ExcepcionEvaluacionNoEncontrada;
import com.triaje.dominio.modelo.AccionAuditoria;
import com.triaje.dominio.modelo.CatalogoPreguntasCribado;
import com.triaje.dominio.modelo.EstadoEvaluacion;
import com.triaje.dominio.modelo.Evaluacion;
import com.triaje.dominio.modelo.InstrumentoCribado;
import com.triaje.dominio.modelo.RegistroAuditoria;
import com.triaje.dominio.modelo.RespuestaEvaluacion;
import com.triaje.dominio.modelo.Usuario;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Caso de uso: el clinico descarga el PDF de una evaluacion autorizada.
 * Verifica estrictamente en la misma transaccion que exista autorizacion vigente;
 * si no existe, responde con 404 (ExcepcionEvaluacionNoEncontrada).
 * Registra en auditoria el evento PDF_DESCARGADO_CLINICO.
 */
public class DescargarReportePdfClinico {

    private final RepositorioAutorizaciones repositorioAutorizaciones;
    private final RepositorioEvaluaciones repositorioEvaluaciones;
    private final RepositorioUsuarios repositorioUsuarios;
    private final RepositorioRespuestasEvaluacion repositorioRespuestas;
    private final RepositorioAuditoria repositorioAuditoria;
    private final ClienteReportes clienteReportes;
    private final PoliticaAccesoClinico politicaAccesoClinico;
    private final ReglaResultadoMinimo reglaResultadoMinimo;
    private final List<DatosLineaAyudaReporte> lineasAyuda;
    private final EjecutorTransaccional transacciones;
    private final Clock reloj;

    public DescargarReportePdfClinico(RepositorioAutorizaciones repositorioAutorizaciones,
                                      RepositorioEvaluaciones repositorioEvaluaciones,
                                      RepositorioUsuarios repositorioUsuarios,
                                      RepositorioRespuestasEvaluacion repositorioRespuestas,
                                      RepositorioAuditoria repositorioAuditoria,
                                      ClienteReportes clienteReportes,
                                      PoliticaAccesoClinico politicaAccesoClinico,
                                      ReglaResultadoMinimo reglaResultadoMinimo,
                                      List<DatosLineaAyudaReporte> lineasAyuda,
                                      EjecutorTransaccional transacciones,
                                      Clock reloj) {
        this.repositorioAutorizaciones = Objects.requireNonNull(repositorioAutorizaciones);
        this.repositorioEvaluaciones = Objects.requireNonNull(repositorioEvaluaciones);
        this.repositorioUsuarios = Objects.requireNonNull(repositorioUsuarios);
        this.repositorioRespuestas = Objects.requireNonNull(repositorioRespuestas);
        this.repositorioAuditoria = Objects.requireNonNull(repositorioAuditoria);
        this.clienteReportes = Objects.requireNonNull(clienteReportes);
        this.politicaAccesoClinico = Objects.requireNonNull(politicaAccesoClinico);
        this.reglaResultadoMinimo = Objects.requireNonNull(reglaResultadoMinimo);
        this.lineasAyuda = lineasAyuda != null ? List.copyOf(lineasAyuda) : List.of();
        this.transacciones = Objects.requireNonNull(transacciones);
        this.reloj = Objects.requireNonNull(reloj);
    }

    public byte[] ejecutar(String evaluacionId, Long clinicoId) {
        return transacciones.ejecutar(() -> {
            Optional<Autorizacion> autorizacionOpt = repositorioAutorizaciones
                    .buscarVigentePorEvaluacionYClinico(evaluacionId, clinicoId);

            if (!politicaAccesoClinico.permiteLectura(autorizacionOpt, clinicoId, evaluacionId)) {
                throw new ExcepcionEvaluacionNoEncontrada("La evaluacion no existe o no tienes acceso a ella.");
            }

            Evaluacion evaluacion = repositorioEvaluaciones.buscarPorId(evaluacionId)
                    .orElseThrow(() -> new ExcepcionEvaluacionNoEncontrada("La evaluacion no existe o no tienes acceso a ella."));

            if (evaluacion.getEstado() != EstadoEvaluacion.COMPLETADA) {
                throw new ExcepcionEvaluacionIncompleta(
                        "Solo se puede descargar el reporte de una evaluacion en estado COMPLETADA."
                );
            }

            Usuario paciente = repositorioUsuarios.buscarPorId(evaluacion.getUsuarioId())
                    .orElseThrow(() -> new ExcepcionEvaluacionNoEncontrada("La evaluacion no existe o no tienes acceso a ella."));

            List<RespuestaEvaluacion> respuestas = repositorioRespuestas.buscarPorEvaluacionId(evaluacionId);

            List<DatosItemRespuestaReporte> respuestasResumen = respuestas.stream()
                    .sorted((r1, r2) -> {
                        int comp = r1.getInstrumento().compareTo(r2.getInstrumento());
                        return comp != 0 ? comp : Integer.compare(r1.getIndicePregunta(), r2.getIndicePregunta());
                    })
                    .map(r -> new DatosItemRespuestaReporte(
                            r.getInstrumento() == InstrumentoCribado.PHQ9 ? "PHQ-9" : "GAD-7",
                            r.getIndicePregunta(),
                            CatalogoPreguntasCribado.obtenerEtiquetaFrecuencia(r.getValorPuntaje())
                    ))
                    .toList();

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

            DatosSolicitudReporte solicitud = new DatosSolicitudReporte(
                    paciente.getNombreCompleto(),
                    fechaEvaluacion,
                    evaluacion.getPuntajePhq9() != null ? evaluacion.getPuntajePhq9() : 0,
                    evaluacion.getPuntajeGad7() != null ? evaluacion.getPuntajeGad7() : 0,
                    evaluacion.getSeveridadDepresion(),
                    evaluacion.getSeveridadAnsiedad(),
                    evaluacion.isRiesgoCrisis(),
                    resultadoMinimo,
                    lineasAyuda,
                    respuestasResumen
            );

            byte[] pdfBytes = clienteReportes.generarReportePdf(solicitud);

            Instant ahora = Instant.now(reloj);
            repositorioAuditoria.registrar(new RegistroAuditoria(
                    clinicoId,
                    AccionAuditoria.PDF_DESCARGADO_CLINICO,
                    evaluacionId,
                    ahora
            ));

            return pdfBytes;
        });
    }
}
