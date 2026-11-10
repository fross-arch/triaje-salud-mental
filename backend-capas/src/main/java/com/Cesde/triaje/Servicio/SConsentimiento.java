package com.Cesde.triaje.Servicio;

import com.Cesde.triaje.Dto.RespuestaConsentimiento;
import com.Cesde.triaje.Dto.RespuestaConsentimientoVigente;
import com.Cesde.triaje.Dto.SolicitudConsentimiento;
import com.Cesde.triaje.Excepcion.ExcepcionDatoInvalido;
import com.Cesde.triaje.Excepcion.ExcepcionDominio;
import com.Cesde.triaje.Excepcion.ExcepcionUsuarioNoEncontrado;
import com.Cesde.triaje.Mapper.IConsentimientoMapper;
import com.Cesde.triaje.Modelo.Enumeracion.AccionAuditoria;
import com.Cesde.triaje.Modelo.Enumeracion.EstadoCita;
import com.Cesde.triaje.Modelo.Enumeracion.EstadoEvaluacion;
import com.Cesde.triaje.Modelo.MAutorizacion;
import com.Cesde.triaje.Modelo.MCita;
import com.Cesde.triaje.Modelo.MConsentimiento;
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

@Service
@Transactional
public class SConsentimiento {

    private final IConsentimientoRepositorio consentimientoRepositorio;
    private final IUsuarioRepositorio usuarioRepositorio;
    private final IEvaluacionRepositorio evaluacionRepositorio;
    private final IAutorizacionRepositorio autorizacionRepositorio;
    private final ICitaRepositorio citaRepositorio;
    private final SAuditoria auditoriaServicio;
    private final IConsentimientoMapper consentimientoMapper;

    public SConsentimiento(
            IConsentimientoRepositorio consentimientoRepositorio,
            IUsuarioRepositorio usuarioRepositorio,
            IEvaluacionRepositorio evaluacionRepositorio,
            IAutorizacionRepositorio autorizacionRepositorio,
            ICitaRepositorio citaRepositorio,
            SAuditoria auditoriaServicio,
            IConsentimientoMapper consentimientoMapper
    ) {
        this.consentimientoRepositorio = consentimientoRepositorio;
        this.usuarioRepositorio = usuarioRepositorio;
        this.evaluacionRepositorio = evaluacionRepositorio;
        this.autorizacionRepositorio = autorizacionRepositorio;
        this.citaRepositorio = citaRepositorio;
        this.auditoriaServicio = auditoriaServicio;
        this.consentimientoMapper = consentimientoMapper;
    }

    public RespuestaConsentimiento registrarConsentimiento(Long usuarioId, SolicitudConsentimiento solicitud) {
        if (!Boolean.TRUE.equals(solicitud.confirmaMayoriaEdad())) {
            throw new ExcepcionDatoInvalido("Debes confirmar la mayoría de edad para otorgar consentimiento.");
        }

        MUsuario usuario = usuarioRepositorio.findById(usuarioId)
                .orElseThrow(ExcepcionUsuarioNoEncontrado::new);

        Instant ahora = Instant.now();
        List<MConsentimiento> previos = consentimientoRepositorio.findByUsuarioId(usuarioId);
        for (MConsentimiento previo : previos) {
            if (previo.getFechaRevocacion() == null) {
                previo.setFechaRevocacion(ahora);
                consentimientoRepositorio.save(previo);
            }
        }

        MConsentimiento nuevo = new MConsentimiento(usuario, true);
        MConsentimiento guardado = consentimientoRepositorio.save(nuevo);

        auditoriaServicio.registrar(usuario, AccionAuditoria.CONSENTIMIENTO_ACEPTADO, String.valueOf(guardado.getId()));
        return consentimientoMapper.aRespuestaConsentimiento(guardado);
    }

    @Transactional(readOnly = true)
    public RespuestaConsentimientoVigente consultarVigente(Long usuarioId) {
        return consentimientoRepositorio.findFirstByUsuarioIdAndFechaRevocacionIsNullOrderByFechaAceptacionDesc(usuarioId)
                .map(consentimientoMapper::aRespuestaConsentimientoVigente)
                .orElseGet(RespuestaConsentimientoVigente::inactivo);
    }

    public void revocar(Long usuarioId) {
        if (evaluacionRepositorio.existsByUsuarioIdAndEstado(usuarioId, EstadoEvaluacion.EN_CURSO)) {
            throw new ExcepcionDominio("Hay una evaluación en curso; debes finalizarla o cancelarla antes de revocar el consentimiento");
        }

        if (citaRepositorio.existsByPacienteIdAndEstadoIn(usuarioId, List.of(EstadoCita.PROPUESTA, EstadoCita.CONFIRMADA))) {
            throw new ExcepcionDominio("No puedes revocar el consentimiento mientras tengas una cita activa programada o propuesta. Debes completarla o cancelarla desde 'Mis Citas'.");
        }

        List<MConsentimiento> consentimientos = consentimientoRepositorio.findByUsuarioId(usuarioId);
        boolean algunoRevocado = false;
        Instant ahora = Instant.now();
        for (MConsentimiento consentimiento : consentimientos) {
            if (consentimiento.getFechaRevocacion() == null) {
                consentimiento.setFechaRevocacion(ahora);
                consentimientoRepositorio.save(consentimiento);
                algunoRevocado = true;
                auditoriaServicio.registrar(consentimiento.getUsuario(), AccionAuditoria.CONSENTIMIENTO_REVOCADO, String.valueOf(consentimiento.getId()));
            }
        }

        if (!algunoRevocado) {
            return;
        }

        // Revocar autorizaciones vigentes del paciente
        List<MAutorizacion> autorizaciones = autorizacionRepositorio.findByPacienteIdAndFechaRevocacionIsNull(usuarioId);
        for (MAutorizacion autorizacion : autorizaciones) {
            autorizacion.revocar(ahora);
            autorizacionRepositorio.save(autorizacion);

            auditoriaServicio.registrar(usuarioId, AccionAuditoria.AUTORIZACION_REVOCADA, autorizacion.getEvaluacion().getId());
        }
    }
}
