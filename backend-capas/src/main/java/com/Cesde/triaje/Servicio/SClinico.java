package com.Cesde.triaje.Servicio;

import com.Cesde.triaje.Dto.RespuestaClinicoActivo;
import com.Cesde.triaje.Dto.RespuestaEvaluacionClinicoDetalle;
import com.Cesde.triaje.Dto.RespuestaEvaluacionClinicoDetalle.RespuestaItemClinico;
import com.Cesde.triaje.Dto.RespuestaEvaluacionClinicoResumen;
import com.Cesde.triaje.Excepcion.ExcepcionEvaluacionNoEncontrada;
import com.Cesde.triaje.Modelo.CatalogoPreguntasCribado;
import com.Cesde.triaje.Modelo.Enumeracion.AccionAuditoria;
import com.Cesde.triaje.Modelo.Enumeracion.RolUsuario;
import com.Cesde.triaje.Modelo.MAutorizacion;
import com.Cesde.triaje.Modelo.MEvaluacion;
import com.Cesde.triaje.Modelo.MRegistroAuditoria;
import com.Cesde.triaje.Modelo.MRespuestaEvaluacion;
import com.Cesde.triaje.Modelo.MUsuario;
import com.Cesde.triaje.Regla.PoliticaAccesoClinico;
import com.Cesde.triaje.Regla.ReglaResultadoMinimo;
import com.Cesde.triaje.Repositorio.IAutorizacionRepositorio;
import com.Cesde.triaje.Repositorio.IEvaluacionRepositorio;
import com.Cesde.triaje.Repositorio.IRespuestaEvaluacionRepositorio;
import com.Cesde.triaje.Repositorio.IUsuarioRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class SClinico {

    private final IUsuarioRepositorio usuarioRepositorio;
    private final IAutorizacionRepositorio autorizacionRepositorio;
    private final IEvaluacionRepositorio evaluacionRepositorio;
    private final IRespuestaEvaluacionRepositorio respuestaRepositorio;
    private final SAuditoria auditoriaServicio;
    private final SReporte reporteServicio;

    private final PoliticaAccesoClinico politicaAcceso = new PoliticaAccesoClinico();
    private final ReglaResultadoMinimo reglaResultadoMinimo = new ReglaResultadoMinimo();

    public SClinico(
            IUsuarioRepositorio usuarioRepositorio,
            IAutorizacionRepositorio autorizacionRepositorio,
            IEvaluacionRepositorio evaluacionRepositorio,
            IRespuestaEvaluacionRepositorio respuestaRepositorio,
            SAuditoria auditoriaServicio,
            SReporte reporteServicio) {
        this.usuarioRepositorio = usuarioRepositorio;
        this.autorizacionRepositorio = autorizacionRepositorio;
        this.evaluacionRepositorio = evaluacionRepositorio;
        this.respuestaRepositorio = respuestaRepositorio;
        this.auditoriaServicio = auditoriaServicio;
        this.reporteServicio = reporteServicio;
    }

    @Transactional(readOnly = true)
    public List<RespuestaClinicoActivo> listarActivos() {
        return usuarioRepositorio.findByRolAndActivoTrue(RolUsuario.CLINICO).stream()
                .map(u -> new RespuestaClinicoActivo(u.getId(), u.getNombreCompleto(), u.getEspecialidad()))
                .toList();
    }

    @Transactional
    public List<RespuestaEvaluacionClinicoResumen> listarEvaluaciones(Long clinicoId) {
        List<MAutorizacion> autorizacionesVigentes = autorizacionRepositorio.findByClinicoIdAndFechaRevocacionIsNull(clinicoId);
        List<RespuestaEvaluacionClinicoResumen> resultado = new ArrayList<>();

        for (MAutorizacion autorizacion : autorizacionesVigentes) {
            Optional<MEvaluacion> evaluacionOpt = evaluacionRepositorio.findById(autorizacion.getEvaluacionId());
            if (evaluacionOpt.isEmpty()) {
                continue;
            }
            MEvaluacion evaluacion = evaluacionOpt.get();
            Optional<MUsuario> pacienteOpt = usuarioRepositorio.findById(evaluacion.getUsuarioId());
            String nombrePaciente = pacienteOpt.map(MUsuario::getNombreCompleto).orElse("Paciente");

            boolean resultadoMinimo = reglaResultadoMinimo.evaluar(
                    evaluacion.getSeveridadDepresion(),
                    evaluacion.getSeveridadAnsiedad(),
                    evaluacion.isRiesgoCrisis()
            );

            resultado.add(new RespuestaEvaluacionClinicoResumen(
                    evaluacion.getId(),
                    nombrePaciente,
                    evaluacion.getFechaFinalizacion(),
                    evaluacion.getSeveridadDepresion(),
                    evaluacion.getSeveridadAnsiedad(),
                    evaluacion.isRiesgoCrisis(),
                    resultadoMinimo
            ));
        }

        Comparator<RespuestaEvaluacionClinicoResumen> comparador = Comparator
                .comparing(RespuestaEvaluacionClinicoResumen::riesgoCrisis, Comparator.reverseOrder())
                .thenComparing(RespuestaEvaluacionClinicoResumen::fechaFinalizacion, Comparator.nullsLast(Comparator.reverseOrder()));

        resultado.sort(comparador);

        auditoriaServicio.registrar(clinicoId, AccionAuditoria.ACCESO_CLINICO_LISTADO, null);

        return resultado;
    }

    @Transactional
    public RespuestaEvaluacionClinicoDetalle obtenerDetalle(String evaluacionId, Long clinicoId) {
        Optional<MAutorizacion> autorizacionOpt = autorizacionRepositorio
                .findByEvaluacionIdAndClinicoIdAndFechaRevocacionIsNull(evaluacionId, clinicoId);

        if (!politicaAcceso.permiteLectura(autorizacionOpt, clinicoId, evaluacionId)) {
            throw new ExcepcionEvaluacionNoEncontrada("La evaluación no existe o no tienes acceso a ella.");
        }

        MEvaluacion evaluacion = evaluacionRepositorio.findById(evaluacionId)
                .orElseThrow(() -> new ExcepcionEvaluacionNoEncontrada("La evaluación no existe o no tienes acceso a ella."));

        MUsuario paciente = usuarioRepositorio.findById(evaluacion.getUsuarioId())
                .orElseThrow(() -> new ExcepcionEvaluacionNoEncontrada("La evaluación no existe o no tienes acceso a ella."));

        List<MRespuestaEvaluacion> respuestas = respuestaRepositorio.findByEvaluacionId(evaluacionId);

        List<RespuestaItemClinico> items = respuestas.stream()
                .sorted((r1, r2) -> {
                    int c = r1.getInstrumento().compareTo(r2.getInstrumento());
                    return c != 0 ? c : Integer.compare(r1.getIndicePregunta(), r2.getIndicePregunta());
                })
                .map(r -> new RespuestaItemClinico(
                        r.getInstrumento().name(),
                        r.getIndicePregunta(),
                        CatalogoPreguntasCribado.obtenerTextoPregunta(r.getInstrumento(), r.getIndicePregunta()),
                        r.getValorPuntaje(),
                        CatalogoPreguntasCribado.obtenerEtiquetaFrecuencia(r.getValorPuntaje())
                ))
                .toList();

        boolean resultadoMinimo = reglaResultadoMinimo.evaluar(
                evaluacion.getSeveridadDepresion(),
                evaluacion.getSeveridadAnsiedad(),
                evaluacion.isRiesgoCrisis()
        );

        auditoriaServicio.registrar(clinicoId, AccionAuditoria.ACCESO_CLINICO_DETALLE, evaluacionId);

        return new RespuestaEvaluacionClinicoDetalle(
                evaluacion.getId(),
                paciente.getNombreCompleto(),
                evaluacion.getFechaFinalizacion(),
                evaluacion.getPuntajePhq9(),
                evaluacion.getPuntajeGad7(),
                evaluacion.getSeveridadDepresion(),
                evaluacion.getSeveridadAnsiedad(),
                evaluacion.isRiesgoCrisis(),
                resultadoMinimo,
                items
        );
    }

    @Transactional
    public byte[] generarPdf(String evaluacionId, Long clinicoId) {
        Optional<MAutorizacion> autorizacionOpt = autorizacionRepositorio
                .findByEvaluacionIdAndClinicoIdAndFechaRevocacionIsNull(evaluacionId, clinicoId);

        if (!politicaAcceso.permiteLectura(autorizacionOpt, clinicoId, evaluacionId)) {
            throw new ExcepcionEvaluacionNoEncontrada("La evaluación no existe o no tienes acceso a ella.");
        }

        MEvaluacion evaluacion = evaluacionRepositorio.findById(evaluacionId)
                .orElseThrow(() -> new ExcepcionEvaluacionNoEncontrada("La evaluación no existe o no tienes acceso a ella."));

        MUsuario paciente = usuarioRepositorio.findById(evaluacion.getUsuarioId())
                .orElseThrow(() -> new ExcepcionEvaluacionNoEncontrada("La evaluación no existe o no tienes acceso a ella."));

        List<MRespuestaEvaluacion> respuestas = respuestaRepositorio.findByEvaluacionId(evaluacionId);

        auditoriaServicio.registrar(clinicoId, AccionAuditoria.PDF_DESCARGADO_CLINICO, evaluacionId);

        return reporteServicio.generarReportePdf(evaluacion, paciente, respuestas);
    }
}
