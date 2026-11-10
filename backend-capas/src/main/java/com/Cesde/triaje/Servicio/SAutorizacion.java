package com.Cesde.triaje.Servicio;

import com.Cesde.triaje.Dto.RespuestaAutorizacion;
import com.Cesde.triaje.Dto.SolicitudOtorgarAutorizacion;
import com.Cesde.triaje.Excepcion.ExcepcionDatoInvalido;
import com.Cesde.triaje.Excepcion.ExcepcionDominio;
import com.Cesde.triaje.Excepcion.ExcepcionEvaluacionIncompleta;
import com.Cesde.triaje.Excepcion.ExcepcionEvaluacionNoEncontrada;
import com.Cesde.triaje.Excepcion.ExcepcionUsuarioNoEncontrado;
import com.Cesde.triaje.Mapper.IAutorizacionMapper;
import com.Cesde.triaje.Modelo.Enumeracion.AccionAuditoria;
import com.Cesde.triaje.Modelo.Enumeracion.EstadoCita;
import com.Cesde.triaje.Modelo.Enumeracion.EstadoEvaluacion;
import com.Cesde.triaje.Modelo.Enumeracion.RolUsuario;
import com.Cesde.triaje.Modelo.MAutorizacion;
import com.Cesde.triaje.Modelo.MCita;
import com.Cesde.triaje.Modelo.MEvaluacion;
import com.Cesde.triaje.Modelo.MRegistroAuditoria;
import com.Cesde.triaje.Modelo.MUsuario;
import com.Cesde.triaje.Repositorio.IAutorizacionRepositorio;
import com.Cesde.triaje.Repositorio.ICitaRepositorio;
import com.Cesde.triaje.Repositorio.IConsentimientoRepositorio;
import com.Cesde.triaje.Repositorio.IEvaluacionRepositorio;
import com.Cesde.triaje.Repositorio.IRegistroAuditoriaRepositorio;
import com.Cesde.triaje.Repositorio.IUsuarioRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class SAutorizacion {

    private final IEvaluacionRepositorio evaluacionRepositorio;
    private final IConsentimientoRepositorio consentimientoRepositorio;
    private final IAutorizacionRepositorio autorizacionRepositorio;
    private final ICitaRepositorio citaRepositorio;
    private final IUsuarioRepositorio usuarioRepositorio;
    private final SAuditoria auditoriaServicio;
    private final IAutorizacionMapper autorizacionMapper;

    public SAutorizacion(
            IEvaluacionRepositorio evaluacionRepositorio,
            IConsentimientoRepositorio consentimientoRepositorio,
            IAutorizacionRepositorio autorizacionRepositorio,
            ICitaRepositorio citaRepositorio,
            IUsuarioRepositorio usuarioRepositorio,
            SAuditoria auditoriaServicio,
            IAutorizacionMapper autorizacionMapper
    ) {
        this.evaluacionRepositorio = evaluacionRepositorio;
        this.consentimientoRepositorio = consentimientoRepositorio;
        this.autorizacionRepositorio = autorizacionRepositorio;
        this.citaRepositorio = citaRepositorio;
        this.usuarioRepositorio = usuarioRepositorio;
        this.auditoriaServicio = auditoriaServicio;
        this.autorizacionMapper = autorizacionMapper;
    }

    public RespuestaAutorizacion otorgar(String evaluacionId, Long usuarioId, SolicitudOtorgarAutorizacion solicitud) {
        MEvaluacion evaluacion = evaluacionRepositorio.findById(evaluacionId)
                .orElseThrow(ExcepcionEvaluacionNoEncontrada::new);

        if (!evaluacion.getUsuario().getId().equals(usuarioId)) {
            throw new ExcepcionEvaluacionNoEncontrada();
        }

        if (evaluacion.getEstado() != EstadoEvaluacion.COMPLETADA) {
            throw new ExcepcionEvaluacionIncompleta("Solo se pueden autorizar evaluaciones en estado COMPLETADA.");
        }

        boolean consentimientoVigente = consentimientoRepositorio.findByUsuarioIdAndFechaRevocacionIsNull(usuarioId).isPresent();
        if (!consentimientoVigente) {
            throw new ExcepcionDominio("El consentimiento informado debe estar vigente para otorgar autorizaciones.");
        }

        MUsuario clinico = usuarioRepositorio.findById(solicitud.clinicoId())
                .orElseThrow(() -> new ExcepcionUsuarioNoEncontrado("El clinico no existe."));

        if (clinico.getRol() != RolUsuario.CLINICO || !clinico.isActivo()) {
            throw new ExcepcionDatoInvalido("El profesional de la salud debe tener rol CLINICO y encontrarse activo.");
        }

        Optional<MAutorizacion> previa = autorizacionRepositorio.findByEvaluacionIdAndClinicoId(evaluacionId, clinico.getId());
        if (previa.isPresent()) {
            MAutorizacion autorizacionExistente = previa.get();
            if (autorizacionExistente.estaVigente()) {
                return autorizacionMapper.aRespuestaAutorizacion(autorizacionExistente);
            }
            autorizacionExistente.reactivar(Instant.now());
            MAutorizacion guardada = autorizacionRepositorio.save(autorizacionExistente);
            auditoriaServicio.registrar(evaluacion.getUsuario(), AccionAuditoria.AUTORIZACION_OTORGADA, evaluacionId);
            return autorizacionMapper.aRespuestaAutorizacion(guardada);
        }

        MAutorizacion nueva = new MAutorizacion(
                evaluacion,
                evaluacion.getUsuario(),
                clinico,
                "1.0"
        );
        MAutorizacion guardada = autorizacionRepositorio.save(nueva);
        auditoriaServicio.registrar(evaluacion.getUsuario(), AccionAuditoria.AUTORIZACION_OTORGADA, evaluacionId);

        return autorizacionMapper.aRespuestaAutorizacion(guardada);
    }

    @Transactional(readOnly = true)
    public List<RespuestaAutorizacion> listarPorEvaluacion(String evaluacionId, Long usuarioId) {
        MEvaluacion evaluacion = evaluacionRepositorio.findById(evaluacionId)
                .orElseThrow(ExcepcionEvaluacionNoEncontrada::new);

        if (!evaluacion.getUsuario().getId().equals(usuarioId)) {
            throw new ExcepcionEvaluacionNoEncontrada();
        }

        return autorizacionRepositorio.findByEvaluacionIdOrderByFechaOtorgamientoDesc(evaluacionId).stream()
                .map(autorizacionMapper::aRespuestaAutorizacion)
                .toList();
    }

    public RespuestaAutorizacion revocar(String evaluacionId, Long usuarioId, Long clinicoId) {
        MEvaluacion evaluacion = evaluacionRepositorio.findById(evaluacionId)
                .orElseThrow(ExcepcionEvaluacionNoEncontrada::new);

        if (!evaluacion.getUsuario().getId().equals(usuarioId)) {
            throw new ExcepcionEvaluacionNoEncontrada();
        }

        MAutorizacion autorizacion = autorizacionRepositorio.findByEvaluacionIdAndClinicoId(evaluacionId, clinicoId)
                .orElseThrow(ExcepcionEvaluacionNoEncontrada::new);

        Instant ahora = Instant.now();
        autorizacion.revocar(ahora);
        MAutorizacion guardada = autorizacionRepositorio.save(autorizacion);

        auditoriaServicio.registrar(evaluacion.getUsuario(), AccionAuditoria.AUTORIZACION_REVOCADA, evaluacionId);

        // Cancelar citas propuestas asociadas
        List<MCita> citasPropuestas = citaRepositorio.findByAutorizacionIdAndEstado(autorizacion.getId(), EstadoCita.PROPUESTA);
        for (MCita cita : citasPropuestas) {
            cita.setEstado(EstadoCita.CANCELADA);
            citaRepositorio.save(cita);
            auditoriaServicio.registrar(evaluacion.getUsuario(), AccionAuditoria.CITA_CANCELADA, cita.getId());
        }

        return autorizacionMapper.aRespuestaAutorizacion(guardada);
    }
}
