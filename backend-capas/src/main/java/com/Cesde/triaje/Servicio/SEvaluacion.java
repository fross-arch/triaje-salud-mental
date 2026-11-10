package com.Cesde.triaje.Servicio;

import com.Cesde.triaje.Dto.RespuestaEvaluacionDetalle;
import com.Cesde.triaje.Dto.RespuestaEvaluacionResumen;
import com.Cesde.triaje.Dto.RespuestaRegistroItem;
import com.Cesde.triaje.Dto.SolicitudRespuesta;
import com.Cesde.triaje.Excepcion.ExcepcionAccesoDenegado;
import com.Cesde.triaje.Excepcion.ExcepcionDominio;
import com.Cesde.triaje.Excepcion.ExcepcionEvaluacionIncompleta;
import com.Cesde.triaje.Excepcion.ExcepcionEvaluacionNoEncontrada;
import com.Cesde.triaje.Excepcion.ExcepcionUsuarioNoEncontrado;
import com.Cesde.triaje.Mapper.IEvaluacionMapper;
import com.Cesde.triaje.Mapper.IRespuestaEvaluacionMapper;
import com.Cesde.triaje.Modelo.Enumeracion.AccionAuditoria;
import com.Cesde.triaje.Modelo.Enumeracion.EstadoCita;
import com.Cesde.triaje.Modelo.Enumeracion.EstadoEvaluacion;
import com.Cesde.triaje.Modelo.Enumeracion.InstrumentoCribado;
import com.Cesde.triaje.Modelo.Enumeracion.SeveridadAnsiedad;
import com.Cesde.triaje.Modelo.Enumeracion.SeveridadDepresion;
import com.Cesde.triaje.Modelo.MAutorizacion;
import com.Cesde.triaje.Modelo.MCita;
import com.Cesde.triaje.Modelo.MEvaluacion;
import com.Cesde.triaje.Modelo.MRegistroAuditoria;
import com.Cesde.triaje.Modelo.MRespuestaEvaluacion;
import com.Cesde.triaje.Modelo.MUsuario;
import com.Cesde.triaje.Regla.EvaluadorGad7;
import com.Cesde.triaje.Regla.EvaluadorPhq9;
import com.Cesde.triaje.Regla.ReglaCrisis;
import com.Cesde.triaje.Regla.ReglaResultadoMinimo;
import com.Cesde.triaje.Repositorio.IAutorizacionRepositorio;
import com.Cesde.triaje.Repositorio.ICitaRepositorio;
import com.Cesde.triaje.Repositorio.IConsentimientoRepositorio;
import com.Cesde.triaje.Repositorio.IEvaluacionRepositorio;
import com.Cesde.triaje.Repositorio.IRespuestaEvaluacionRepositorio;
import com.Cesde.triaje.Repositorio.IUsuarioRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class SEvaluacion {

    private final IEvaluacionRepositorio evaluacionRepositorio;
    private final IRespuestaEvaluacionRepositorio respuestaRepositorio;
    private final IConsentimientoRepositorio consentimientoRepositorio;
    private final IAutorizacionRepositorio autorizacionRepositorio;
    private final ICitaRepositorio citaRepositorio;
    private final IUsuarioRepositorio usuarioRepositorio;
    private final SAuditoria auditoriaServicio;
    private final SReporte reporteServicio;
    private final IEvaluacionMapper evaluacionMapper;
    private final IRespuestaEvaluacionMapper respuestaEvaluacionMapper;

    private final EvaluadorPhq9 evaluadorPhq9 = new EvaluadorPhq9();
    private final EvaluadorGad7 evaluadorGad7 = new EvaluadorGad7();
    private final ReglaCrisis reglaCrisis = new ReglaCrisis();
    private final ReglaResultadoMinimo reglaResultadoMinimo = new ReglaResultadoMinimo(reglaCrisis);

    public SEvaluacion(
            IEvaluacionRepositorio evaluacionRepositorio,
            IRespuestaEvaluacionRepositorio respuestaRepositorio,
            IConsentimientoRepositorio consentimientoRepositorio,
            IAutorizacionRepositorio autorizacionRepositorio,
            ICitaRepositorio citaRepositorio,
            IUsuarioRepositorio usuarioRepositorio,
            SAuditoria auditoriaServicio,
            SReporte reporteServicio,
            IEvaluacionMapper evaluacionMapper,
            IRespuestaEvaluacionMapper respuestaEvaluacionMapper
    ) {
        this.evaluacionRepositorio = evaluacionRepositorio;
        this.respuestaRepositorio = respuestaRepositorio;
        this.consentimientoRepositorio = consentimientoRepositorio;
        this.autorizacionRepositorio = autorizacionRepositorio;
        this.citaRepositorio = citaRepositorio;
        this.usuarioRepositorio = usuarioRepositorio;
        this.auditoriaServicio = auditoriaServicio;
        this.reporteServicio = reporteServicio;
        this.evaluacionMapper = evaluacionMapper;
        this.respuestaEvaluacionMapper = respuestaEvaluacionMapper;
    }

    public RespuestaEvaluacionResumen crearEvaluacion(Long usuarioId) {
        boolean tieneConsentimiento = consentimientoRepositorio.findByUsuarioIdAndFechaRevocacionIsNull(usuarioId).isPresent();
        if (!tieneConsentimiento) {
            throw new ExcepcionAccesoDenegado("Debes aceptar el consentimiento informado antes de iniciar una evaluación.");
        }

        List<MEvaluacion> evaluacionesUsuario = evaluacionRepositorio.findByUsuarioIdOrderByFechaCreacionDesc(usuarioId);

        // a y b) Si ya existe una evaluación EN_CURSO, devolverla. Si hay varias, conservar la más reciente y marcar las demás como ABANDONADA.
        List<MEvaluacion> enCursoList = evaluacionesUsuario.stream()
                .filter(e -> e.getEstado() == EstadoEvaluacion.EN_CURSO)
                .toList();

        if (!enCursoList.isEmpty()) {
            MEvaluacion masReciente = enCursoList.get(0);
            if (enCursoList.size() > 1) {
                for (int i = 1; i < enCursoList.size(); i++) {
                    MEvaluacion vieja = enCursoList.get(i);
                    vieja.setEstado(EstadoEvaluacion.ABANDONADA);
                    evaluacionRepositorio.save(vieja);
                }
            }
            return evaluacionMapper.aRespuestaEvaluacionResumen(masReciente);
        }

        // Si no hay evaluación en curso, aplican las validaciones para una nueva evaluación:
        if (citaRepositorio.existsByPacienteIdAndEstadoIn(usuarioId, List.of(EstadoCita.PROPUESTA, EstadoCita.CONFIRMADA))) {
            throw new ExcepcionDominio("No puedes iniciar una nueva evaluación mientras tengas una cita activa pendiente de atención.");
        }

        List<MAutorizacion> autorizacionesVigentes = autorizacionRepositorio.findByPacienteIdAndFechaRevocacionIsNull(usuarioId);
        for (MAutorizacion autorizacion : autorizacionesVigentes) {
            List<MCita> cumplidas = citaRepositorio.findByAutorizacionIdAndEstado(autorizacion.getId(), EstadoCita.CUMPLIDA);
            if (cumplidas.isEmpty()) {
                throw new ExcepcionDominio("No puedes iniciar una nueva evaluación mientras tengas una evaluación compartida en espera de atención clínica.");
            }
        }

        // c y d) Regla C de espera: 30 días entre evaluaciones completadas
        Optional<MEvaluacion> ultimaCompletadaOpt = evaluacionesUsuario.stream()
                .filter(e -> e.getEstado() == EstadoEvaluacion.COMPLETADA)
                .findFirst();

        if (ultimaCompletadaOpt.isPresent()) {
            MEvaluacion ultimaCompletada = ultimaCompletadaOpt.get();
            Instant fechaFin = ultimaCompletada.getFechaFinalizacion() != null
                    ? ultimaCompletada.getFechaFinalizacion()
                    : ultimaCompletada.getFechaCreacion();

            Instant fechaDisponible = fechaFin.plus(30, ChronoUnit.DAYS);
            Instant ahora = Instant.now();

            if (ahora.isBefore(fechaDisponible)) {
                DateTimeFormatter formateador = DateTimeFormatter.ofPattern("dd/MM/yyyy")
                        .withZone(ZoneId.systemDefault());
                String fechaLegible = formateador.format(fechaDisponible);
                throw new ExcepcionDominio(
                        "Debes esperar 30 días entre evaluaciones completadas. Podrás realizar una nueva evaluación a partir del " + fechaLegible + "."
                );
            }
        }

        MUsuario usuario = usuarioRepositorio.findById(usuarioId)
                .orElseThrow(ExcepcionUsuarioNoEncontrado::new);

        MEvaluacion nueva = new MEvaluacion(usuario);
        MEvaluacion guardada = evaluacionRepositorio.save(nueva);
        return evaluacionMapper.aRespuestaEvaluacionResumen(guardada);
    }

    public RespuestaRegistroItem registrarRespuesta(String evaluacionId, Long usuarioId, SolicitudRespuesta solicitud) {
        MEvaluacion evaluacion = evaluacionRepositorio.findById(evaluacionId)
                .orElseThrow(ExcepcionEvaluacionNoEncontrada::new);

        if (!evaluacion.getUsuario().getId().equals(usuarioId)) {
            throw new ExcepcionEvaluacionNoEncontrada();
        }

        if (evaluacion.getEstado() == EstadoEvaluacion.COMPLETADA || evaluacion.getEstado() == EstadoEvaluacion.ABANDONADA) {
            throw new ExcepcionDominio("No se pueden modificar respuestas en una evaluación cerrada o no disponible (estado: "
                    + evaluacion.getEstado() + ").");
        }

        Optional<MRespuestaEvaluacion> previaOpt = respuestaRepositorio
                .findByEvaluacionIdAndInstrumentoAndIndicePregunta(
                        evaluacionId, solicitud.instrumento(), solicitud.indicePregunta());

        if (previaOpt.isPresent()) {
            MRespuestaEvaluacion previa = previaOpt.get();
            previa.setValorPuntaje(solicitud.valorPuntaje());
            respuestaRepositorio.save(previa);
        } else {
            MRespuestaEvaluacion nueva = new MRespuestaEvaluacion(
                    evaluacion, solicitud.instrumento(), solicitud.indicePregunta(), solicitud.valorPuntaje());
            respuestaRepositorio.save(nueva);
        }

        // Detección inmediata de crisis en item 9 de PHQ-9 (pestillo/latch)
        if (solicitud.instrumento() == InstrumentoCribado.PHQ9
                && solicitud.indicePregunta() == 9
                && solicitud.valorPuntaje() >= 1) {
            evaluacion.marcarRiesgoCrisis();
            evaluacionRepositorio.save(evaluacion);
        }

        return new RespuestaRegistroItem(
                evaluacion.getId(),
                solicitud.instrumento(),
                solicitud.indicePregunta(),
                evaluacion.isRiesgoCrisis()
        );
    }

    public RespuestaEvaluacionResumen finalizarEvaluacion(String evaluacionId, Long usuarioId) {
        MEvaluacion evaluacion = evaluacionRepositorio.findById(evaluacionId)
                .orElseThrow(ExcepcionEvaluacionNoEncontrada::new);

        if (!evaluacion.getUsuario().getId().equals(usuarioId)) {
            throw new ExcepcionEvaluacionNoEncontrada();
        }

        if (evaluacion.getEstado() != EstadoEvaluacion.EN_CURSO) {
            throw new ExcepcionDominio("Solo se pueden finalizar evaluaciones en estado EN_CURSO.");
        }

        List<MRespuestaEvaluacion> respuestas = respuestaRepositorio.findByEvaluacionId(evaluacionId);

        Map<Integer, Integer> phq9Map = respuestas.stream()
                .filter(r -> r.getInstrumento() == InstrumentoCribado.PHQ9)
                .collect(Collectors.toMap(MRespuestaEvaluacion::getIndicePregunta, MRespuestaEvaluacion::getValorPuntaje, (a, b) -> b));

        Map<Integer, Integer> gad7Map = respuestas.stream()
                .filter(r -> r.getInstrumento() == InstrumentoCribado.GAD7)
                .collect(Collectors.toMap(MRespuestaEvaluacion::getIndicePregunta, MRespuestaEvaluacion::getValorPuntaje, (a, b) -> b));

        if (phq9Map.size() != 9 || gad7Map.size() != 7) {
            throw new ExcepcionEvaluacionIncompleta("La evaluación está incompleta: deben responderse las 9 preguntas de PHQ-9 y las 7 de GAD-7 antes de finalizar.");
        }

        List<Integer> listaPhq9 = new ArrayList<>(9);
        for (int i = 1; i <= 9; i++) {
            Integer valor = phq9Map.get(i);
            if (valor == null) {
                throw new ExcepcionEvaluacionIncompleta("Falta la respuesta a la pregunta " + i + " de PHQ-9.");
            }
            listaPhq9.add(valor);
        }

        List<Integer> listaGad7 = new ArrayList<>(7);
        for (int i = 1; i <= 7; i++) {
            Integer valor = gad7Map.get(i);
            if (valor == null) {
                throw new ExcepcionEvaluacionIncompleta("Falta la respuesta a la pregunta " + i + " de GAD-7.");
            }
            listaGad7.add(valor);
        }

        int puntajePhq9 = evaluadorPhq9.calcularPuntaje(listaPhq9);
        SeveridadDepresion severidadDepresion = evaluadorPhq9.clasificar(puntajePhq9);

        int puntajeGad7 = evaluadorGad7.calcularPuntaje(listaGad7);
        SeveridadAnsiedad severidadAnsiedad = evaluadorGad7.clasificar(puntajeGad7);

        boolean riesgoCrisis = reglaCrisis.evaluar(listaPhq9);
        if (evaluacion.isRiesgoCrisis()) {
            riesgoCrisis = true; // Preserva latch si ya fue activado previamente
        }

        Instant ahora = Instant.now();
        evaluacion.finalizar(puntajePhq9, puntajeGad7, severidadDepresion, severidadAnsiedad, ahora);
        if (riesgoCrisis) {
            evaluacion.marcarRiesgoCrisis();
        }

        MEvaluacion guardada = evaluacionRepositorio.save(evaluacion);
        auditoriaServicio.registrar(evaluacion.getUsuario(), AccionAuditoria.EVALUACION_COMPLETADA, guardada.getId());

        return evaluacionMapper.aRespuestaEvaluacionResumen(guardada);
    }

    @Transactional(readOnly = true)
    public List<RespuestaEvaluacionResumen> listarEvaluaciones(Long usuarioId) {
        return evaluacionRepositorio.findByUsuarioIdOrderByFechaCreacionDesc(usuarioId).stream()
                .map(evaluacionMapper::aRespuestaEvaluacionResumen)
                .toList();
    }

    @Transactional(readOnly = true)
    public RespuestaEvaluacionDetalle obtenerDetalle(String evaluacionId, Long usuarioId) {
        MEvaluacion evaluacion = evaluacionRepositorio.findById(evaluacionId)
                .orElseThrow(ExcepcionEvaluacionNoEncontrada::new);

        if (!evaluacion.getUsuario().getId().equals(usuarioId)) {
            throw new ExcepcionEvaluacionNoEncontrada();
        }

        List<MRespuestaEvaluacion> respuestas = respuestaRepositorio.findByEvaluacionIdOrderByInstrumentoAscIndicePreguntaAsc(evaluacionId);
        return evaluacionMapper.aRespuestaEvaluacionDetalle(evaluacion, respuestas);
    }

    @Transactional
    public byte[] generarPdf(String evaluacionId, Long usuarioId) {
        MEvaluacion evaluacion = evaluacionRepositorio.findById(evaluacionId)
                .orElseThrow(ExcepcionEvaluacionNoEncontrada::new);

        if (!evaluacion.getUsuario().getId().equals(usuarioId)) {
            throw new ExcepcionEvaluacionNoEncontrada();
        }

        List<MRespuestaEvaluacion> respuestas = respuestaRepositorio.findByEvaluacionIdOrderByInstrumentoAscIndicePreguntaAsc(evaluacionId);
        auditoriaServicio.registrar(evaluacion.getUsuario(), AccionAuditoria.PDF_DESCARGADO, evaluacion.getId());
        return reporteServicio.generarReportePdf(evaluacion, evaluacion.getUsuario(), respuestas);
    }
}
