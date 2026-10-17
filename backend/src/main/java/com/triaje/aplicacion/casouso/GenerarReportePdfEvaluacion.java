package com.triaje.aplicacion.casouso;

import com.triaje.aplicacion.dto.DatosSolicitudReporte;
import com.triaje.aplicacion.dto.DatosSolicitudReporte.DatosItemRespuestaReporte;
import com.triaje.aplicacion.dto.DatosSolicitudReporte.DatosLineaAyudaReporte;
import com.triaje.aplicacion.puerto.ClienteReportes;
import com.triaje.aplicacion.puerto.RepositorioAuditoria;
import com.triaje.aplicacion.puerto.RepositorioEvaluaciones;
import com.triaje.aplicacion.puerto.RepositorioRespuestasEvaluacion;
import com.triaje.aplicacion.puerto.RepositorioUsuarios;
import com.triaje.dominio.ReglaResultadoMinimo;
import com.triaje.dominio.excepcion.ExcepcionEvaluacionIncompleta;
import com.triaje.dominio.excepcion.ExcepcionEvaluacionNoEncontrada;
import com.triaje.dominio.excepcion.ExcepcionUsuarioNoEncontrado;
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

/**
 * Caso de uso para compilar y generar el reporte PDF oficial de una evaluacion completada.
 */
public class GenerarReportePdfEvaluacion {

    private final RepositorioEvaluaciones repositorioEvaluaciones;
    private final RepositorioUsuarios repositorioUsuarios;
    private final RepositorioRespuestasEvaluacion repositorioRespuestas;
    private final RepositorioAuditoria repositorioAuditoria;
    private final ClienteReportes clienteReportes;
    private final ReglaResultadoMinimo reglaResultadoMinimo;
    private final List<DatosLineaAyudaReporte> lineasAyuda;
    private final Clock reloj;

    public GenerarReportePdfEvaluacion(RepositorioEvaluaciones repositorioEvaluaciones,
                                      RepositorioUsuarios repositorioUsuarios,
                                      RepositorioRespuestasEvaluacion repositorioRespuestas,
                                      RepositorioAuditoria repositorioAuditoria,
                                      ClienteReportes clienteReportes,
                                      ReglaResultadoMinimo reglaResultadoMinimo,
                                      List<DatosLineaAyudaReporte> lineasAyuda,
                                      Clock reloj) {
        this.repositorioEvaluaciones = Objects.requireNonNull(repositorioEvaluaciones);
        this.repositorioUsuarios = Objects.requireNonNull(repositorioUsuarios);
        this.repositorioRespuestas = Objects.requireNonNull(repositorioRespuestas);
        this.repositorioAuditoria = Objects.requireNonNull(repositorioAuditoria);
        this.clienteReportes = Objects.requireNonNull(clienteReportes);
        this.reglaResultadoMinimo = Objects.requireNonNull(reglaResultadoMinimo);
        this.lineasAyuda = lineasAyuda != null ? List.copyOf(lineasAyuda) : List.of();
        this.reloj = Objects.requireNonNull(reloj);
    }

    public byte[] ejecutar(String evaluacionId, Long usuarioId) {
        Evaluacion evaluacion = repositorioEvaluaciones.buscarPorId(evaluacionId)
                .orElseThrow(() -> new ExcepcionEvaluacionNoEncontrada("La evaluacion no existe o no tienes acceso a ella."));

        if (!evaluacion.getUsuarioId().equals(usuarioId)) {
            throw new ExcepcionEvaluacionNoEncontrada("La evaluacion no existe o no tienes acceso a ella.");
        }

        if (evaluacion.getEstado() != EstadoEvaluacion.COMPLETADA) {
            throw new ExcepcionEvaluacionIncompleta(
                    "Solo se puede descargar el reporte de una evaluacion en estado COMPLETADA. "
                            + "La evaluacion actual se encuentra en estado " + evaluacion.getEstado().name() + "."
            );
        }

        Usuario usuario = repositorioUsuarios.buscarPorId(usuarioId)
                .orElseThrow(() -> new ExcepcionUsuarioNoEncontrado("Usuario no encontrado"));

        List<RespuestaEvaluacion> respuestas = repositorioRespuestas.buscarPorEvaluacionId(evaluacionId);

        List<DatosItemRespuestaReporte> respuestasResumen = respuestas.stream()
                .sorted((r1, r2) -> {
                    int comp = r1.getInstrumento().compareTo(r2.getInstrumento());
                    if (comp != 0) {
                        return comp;
                    }
                    return Integer.compare(r1.getIndicePregunta(), r2.getIndicePregunta());
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
                usuario.getNombreCompleto(),
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

        repositorioAuditoria.registrar(new RegistroAuditoria(
                usuarioId,
                AccionAuditoria.PDF_DESCARGADO,
                evaluacionId,
                Instant.now(reloj)
        ));

        return pdfBytes;
    }
}
