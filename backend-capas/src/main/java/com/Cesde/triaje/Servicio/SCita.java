package com.Cesde.triaje.Servicio;

import com.Cesde.triaje.Dto.RespuestaCitaClinico;
import com.Cesde.triaje.Dto.RespuestaCitaPaciente;
import com.Cesde.triaje.Dto.SolicitudCancelarCitaClinico;
import com.Cesde.triaje.Dto.SolicitudCumplirCita;
import com.Cesde.triaje.Dto.SolicitudPosponerCita;
import com.Cesde.triaje.Dto.SolicitudProponerCita;
import com.Cesde.triaje.Excepcion.ExcepcionCitaNoEncontrada;
import com.Cesde.triaje.Excepcion.ExcepcionDatoInvalido;
import com.Cesde.triaje.Excepcion.ExcepcionDominio;
import com.Cesde.triaje.Excepcion.ExcepcionEvaluacionNoEncontrada;
import com.Cesde.triaje.Mapper.ICitaMapper;
import com.Cesde.triaje.Modelo.Enumeracion.AccionAuditoria;
import com.Cesde.triaje.Modelo.Enumeracion.EstadoCita;
import com.Cesde.triaje.Modelo.Enumeracion.RolUsuario;
import com.Cesde.triaje.Modelo.MAutorizacion;
import com.Cesde.triaje.Modelo.MCita;
import com.Cesde.triaje.Modelo.MEvaluacion;
import com.Cesde.triaje.Modelo.MRegistroAuditoria;
import com.Cesde.triaje.Regla.MaquinaEstadosCita;
import com.Cesde.triaje.Repositorio.IAutorizacionRepositorio;
import com.Cesde.triaje.Repositorio.ICitaRepositorio;
import com.Cesde.triaje.Repositorio.IRegistroAuditoriaRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@Transactional
public class SCita {

    private final ICitaRepositorio citaRepositorio;
    private final IAutorizacionRepositorio autorizacionRepositorio;
    private final SAuditoria auditoriaServicio;
    private final ICitaMapper citaMapper;
    private final MaquinaEstadosCita maquinaEstados = new MaquinaEstadosCita();

    public SCita(
            ICitaRepositorio citaRepositorio,
            IAutorizacionRepositorio autorizacionRepositorio,
            SAuditoria auditoriaServicio,
            ICitaMapper citaMapper
    ) {
        this.citaRepositorio = citaRepositorio;
        this.autorizacionRepositorio = autorizacionRepositorio;
        this.auditoriaServicio = auditoriaServicio;
        this.citaMapper = citaMapper;
    }

    // ==========================================
    // MÉTODOS PARA EL PACIENTE
    // ==========================================

    @Transactional(readOnly = true)
    public List<RespuestaCitaPaciente> listarCitasPaciente(Long pacienteId) {
        return citaRepositorio.findByPacienteIdOrderByFechaHoraDesc(pacienteId).stream()
                .map(citaMapper::aRespuestaCitaPaciente)
                .toList();
    }

    public RespuestaCitaPaciente confirmarCita(String id, Long pacienteId) {
        MCita cita = citaRepositorio.findById(id).orElseThrow(ExcepcionCitaNoEncontrada::new);
        if (!cita.getPaciente().getId().equals(pacienteId)) {
            throw new ExcepcionCitaNoEncontrada();
        }

        EstadoCita nuevoEstado = maquinaEstados.confirmar(cita.getEstado(), RolUsuario.PACIENTE);
        cita.setEstado(nuevoEstado);
        MCita guardada = citaRepositorio.save(cita);
        auditoriaServicio.registrar(cita.getPaciente(), AccionAuditoria.CITA_CONFIRMADA, cita.getId());

        return citaMapper.aRespuestaCitaPaciente(guardada);
    }

    public RespuestaCitaPaciente rechazarCita(String id, Long pacienteId) {
        MCita cita = citaRepositorio.findById(id).orElseThrow(ExcepcionCitaNoEncontrada::new);
        if (!cita.getPaciente().getId().equals(pacienteId)) {
            throw new ExcepcionCitaNoEncontrada();
        }

        EstadoCita nuevoEstado = maquinaEstados.rechazar(cita.getEstado(), RolUsuario.PACIENTE);
        cita.setEstado(nuevoEstado);
        MCita guardada = citaRepositorio.save(cita);
        auditoriaServicio.registrar(cita.getPaciente(), AccionAuditoria.CITA_RECHAZADA, cita.getId());

        return citaMapper.aRespuestaCitaPaciente(guardada);
    }

    public RespuestaCitaPaciente cancelarCitaPaciente(String id, Long pacienteId) {
        MCita cita = citaRepositorio.findById(id).orElseThrow(ExcepcionCitaNoEncontrada::new);
        if (!cita.getPaciente().getId().equals(pacienteId)) {
            throw new ExcepcionCitaNoEncontrada();
        }

        EstadoCita nuevoEstado = maquinaEstados.cancelar(cita.getEstado(), RolUsuario.PACIENTE);
        cita.setEstado(nuevoEstado);
        MCita guardada = citaRepositorio.save(cita);
        auditoriaServicio.registrar(cita.getPaciente(), AccionAuditoria.CITA_CANCELADA, cita.getId());

        return citaMapper.aRespuestaCitaPaciente(guardada);
    }

    // ==========================================
    // MÉTODOS PARA EL CLÍNICO
    // ==========================================

    public RespuestaCitaClinico proponerCita(Long clinicoId, SolicitudProponerCita solicitud) {
        Instant ahora = Instant.now();
        if (solicitud.fechaHora() == null || !solicitud.fechaHora().isAfter(ahora)) {
            throw new ExcepcionDatoInvalido("La fecha y hora de la cita debe ser en el futuro.");
        }

        MAutorizacion autorizacion = autorizacionRepositorio
                .findByEvaluacionIdAndClinicoIdAndFechaRevocacionIsNull(solicitud.evaluacionId(), clinicoId)
                .orElseThrow(() -> new ExcepcionEvaluacionNoEncontrada("No existe una autorización vigente para esta evaluación y clínico."));

        MEvaluacion evaluacion = autorizacion.getEvaluacion();
        Long pacienteId = autorizacion.getPaciente().getId();

        // Verificar si ya existe cita activa entre este paciente y clínico
        if (citaRepositorio.existsByPacienteIdAndClinicoIdAndEstadoIn(
                pacienteId, clinicoId, List.of(EstadoCita.PROPUESTA, EstadoCita.CONFIRMADA))) {
            throw new ExcepcionDominio("Ya existe una cita activa (propuesta o confirmada) entre este paciente y clínico.");
        }

        // Verificar disponibilidad de horario para el clínico (bloque de 45 min)
        validarDisponibilidadClinico(clinicoId, solicitud.fechaHora(), null);

        EstadoCita estadoInicial = maquinaEstados.proponer(RolUsuario.CLINICO);

        MCita nueva = new MCita(
                autorizacion,
                autorizacion.getPaciente(),
                autorizacion.getClinico(),
                solicitud.fechaHora(),
                solicitud.modalidad(),
                solicitud.mensaje()
        );
        nueva.setEstado(estadoInicial);
        MCita guardada = citaRepositorio.save(nueva);

        auditoriaServicio.registrar(autorizacion.getClinico(), AccionAuditoria.CITA_PROPUESTA, guardada.getId());

        return citaMapper.aRespuestaCitaClinico(guardada);
    }

    @Transactional(readOnly = true)
    public List<RespuestaCitaClinico> listarCitasClinico(Long clinicoId) {
        return citaRepositorio.findByClinicoIdOrderByFechaHoraDesc(clinicoId).stream()
                .map(citaMapper::aRespuestaCitaClinico)
                .toList();
    }

    public RespuestaCitaClinico cumplirCita(String id, Long clinicoId, SolicitudCumplirCita solicitud) {
        MCita cita = citaRepositorio.findById(id).orElseThrow(ExcepcionCitaNoEncontrada::new);
        if (!cita.getClinico().getId().equals(clinicoId)) {
            throw new ExcepcionCitaNoEncontrada();
        }

        EstadoCita nuevoEstado = maquinaEstados.cumplir(cita.getEstado(), RolUsuario.CLINICO);
        cita.setEstado(nuevoEstado);
        cita.setObservaciones(solicitud.observaciones().trim());

        MCita guardada = citaRepositorio.save(cita);
        auditoriaServicio.registrar(cita.getClinico(), AccionAuditoria.CITA_CUMPLIDA, cita.getId());

        return citaMapper.aRespuestaCitaClinico(guardada);
    }

    public RespuestaCitaClinico cancelarCitaClinico(String id, Long clinicoId, SolicitudCancelarCitaClinico solicitud) {
        MCita cita = citaRepositorio.findById(id).orElseThrow(ExcepcionCitaNoEncontrada::new);
        if (!cita.getClinico().getId().equals(clinicoId)) {
            throw new ExcepcionCitaNoEncontrada();
        }

        String motivo = solicitud != null ? solicitud.motivoEfectivo() : null;
        if (motivo == null || motivo.isBlank()) {
            throw new ExcepcionDatoInvalido("El motivo u observaciones de cancelación es obligatorio para el clínico.");
        }

        EstadoCita nuevoEstado = maquinaEstados.cancelar(cita.getEstado(), RolUsuario.CLINICO);
        cita.setEstado(nuevoEstado);
        cita.setObservaciones(motivo.trim());

        MCita guardada = citaRepositorio.save(cita);
        auditoriaServicio.registrar(cita.getClinico(), AccionAuditoria.CITA_CANCELADA, cita.getId());

        return citaMapper.aRespuestaCitaClinico(guardada);
    }

    public RespuestaCitaClinico posponerCita(String id, Long clinicoId, SolicitudPosponerCita solicitud) {
        MCita cita = citaRepositorio.findById(id).orElseThrow(ExcepcionCitaNoEncontrada::new);
        if (!cita.getClinico().getId().equals(clinicoId)) {
            throw new ExcepcionCitaNoEncontrada();
        }

        if (cita.getEstado() != EstadoCita.PROPUESTA && cita.getEstado() != EstadoCita.CONFIRMADA) {
            throw new ExcepcionDominio("Solo se pueden posponer citas en estado PROPUESTA o CONFIRMADA.");
        }

        Instant ahora = Instant.now();
        if (solicitud.nuevaFechaHora() == null || !solicitud.nuevaFechaHora().isAfter(ahora)) {
            throw new ExcepcionDatoInvalido("La nueva fecha y hora de la cita debe ser en el futuro.");
        }

        validarDisponibilidadClinico(clinicoId, solicitud.nuevaFechaHora(), cita.getId());

        cita.setFechaHora(solicitud.nuevaFechaHora());
        if (solicitud.mensaje() != null) {
            cita.setMensaje(solicitud.mensaje().trim());
        }
        if (solicitud.observaciones() != null && !solicitud.observaciones().isBlank()) {
            String obsPrevias = cita.getObservaciones() != null ? cita.getObservaciones() + "\n" : "";
            cita.setObservaciones(obsPrevias + "[Pospuesta]: " + solicitud.observaciones().trim());
        }

        MCita guardada = citaRepositorio.save(cita);
        auditoriaServicio.registrar(cita.getClinico(), AccionAuditoria.CITA_POSPUESTA, cita.getId());

        return citaMapper.aRespuestaCitaClinico(guardada);
    }

    public int cancelarPropuestasVencidas(Instant ahora) {
        List<MCita> vencidas = citaRepositorio.findByEstadoAndFechaHoraBefore(EstadoCita.PROPUESTA, ahora);
        for (MCita c : vencidas) {
            c.setEstado(EstadoCita.CANCELADA);
            c.setObservaciones("Cancelada automáticamente por el sistema por expiración de fecha.");
            citaRepositorio.save(c);
        }
        return vencidas.size();
    }

    private void validarDisponibilidadClinico(Long clinicoId, Instant fechaHora, String citaIdExcluida) {
        boolean colision = citaIdExcluida != null
                ? citaRepositorio.existsByClinicoIdAndFechaHoraAndEstadoInAndIdNot(
                clinicoId, fechaHora, List.of(EstadoCita.PROPUESTA, EstadoCita.CONFIRMADA), citaIdExcluida)
                : citaRepositorio.existsByClinicoIdAndFechaHoraAndEstadoIn(
                clinicoId, fechaHora, List.of(EstadoCita.PROPUESTA, EstadoCita.CONFIRMADA));

        if (colision) {
            throw new ExcepcionDominio("El clínico ya tiene una cita agendada en esa misma fecha y hora.");
        }
    }
}
