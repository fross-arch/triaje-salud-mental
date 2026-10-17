package com.triaje.infraestructura.persistencia.adaptador;

import com.triaje.aplicacion.puerto.RepositorioCitas;
import com.triaje.dominio.Cita;
import com.triaje.dominio.EstadoCita;
import com.triaje.infraestructura.persistencia.entidad.CitaEntidad;
import com.triaje.infraestructura.persistencia.repositorio.CitaRepositorioJpa;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
public class AdaptadorRepositorioCitas implements RepositorioCitas {

    private final CitaRepositorioJpa repositorio;

    public AdaptadorRepositorioCitas(CitaRepositorioJpa repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public Cita guardar(Cita cita) {
        CitaEntidad entidad = new CitaEntidad(
                cita.getId(),
                cita.getAutorizacionId(),
                cita.getPacienteId(),
                cita.getClinicoId(),
                cita.getFechaHora(),
                cita.getModalidad(),
                cita.getMensaje(),
                cita.getEstado(),
                cita.getObservaciones(),
                cita.getFechaCreacion(),
                cita.getFechaActualizacion()
        );
        CitaEntidad guardada = repositorio.saveAndFlush(entidad);
        return aDominio(guardada);
    }

    @Override
    public Optional<Cita> buscarPorId(String id) {
        return repositorio.findById(id).map(AdaptadorRepositorioCitas::aDominio);
    }

    @Override
    public boolean existePorClinicoYFechaHoraYEstados(Long clinicoId, Instant fechaHora, Collection<EstadoCita> estados) {
        return repositorio.existsByClinicoIdAndFechaHoraAndEstadoIn(clinicoId, fechaHora, estados);
    }

    @Override
    public boolean existePorClinicoYFechaHoraYEstadosYIdDistinto(Long clinicoId, Instant fechaHora, Collection<EstadoCita> estados, String idExcluido) {
        return repositorio.existsByClinicoIdAndFechaHoraAndEstadoInAndIdNot(clinicoId, fechaHora, estados, idExcluido);
    }

    @Override
    public boolean existePorPacienteYClinicoYEstados(Long pacienteId, Long clinicoId, Collection<EstadoCita> estados) {
        return repositorio.existsByPacienteIdAndClinicoIdAndEstadoIn(pacienteId, clinicoId, estados);
    }

    @Override
    public boolean existePorPacienteYEstados(Long pacienteId, Collection<EstadoCita> estados) {
        return repositorio.existsByPacienteIdAndEstadoIn(pacienteId, estados);
    }

    @Override
    public List<Cita> listarPorClinicoId(Long clinicoId) {
        return repositorio.findByClinicoIdOrderByFechaHoraAsc(clinicoId).stream()
                .map(AdaptadorRepositorioCitas::aDominio)
                .toList();
    }

    @Override
    public List<Cita> listarPorPacienteId(Long pacienteId) {
        return repositorio.findByPacienteIdOrderByFechaHoraAsc(pacienteId).stream()
                .map(AdaptadorRepositorioCitas::aDominio)
                .toList();
    }

    @Override
    public List<Cita> listarPorAutorizacionIdYEstado(String autorizacionId, EstadoCita estado) {
        return repositorio.findByAutorizacionIdAndEstado(autorizacionId, estado).stream()
                .map(AdaptadorRepositorioCitas::aDominio)
                .toList();
    }

    @Override
    public List<Cita> listarPropuestasConFechaHoraMenorA(Instant limite) {
        return repositorio.findByEstadoAndFechaHoraBefore(EstadoCita.PROPUESTA, limite).stream()
                .map(AdaptadorRepositorioCitas::aDominio)
                .toList();
    }

    private static Cita aDominio(CitaEntidad entidad) {
        return new Cita(
                entidad.getId(),
                entidad.getAutorizacionId(),
                entidad.getPacienteId(),
                entidad.getClinicoId(),
                entidad.getFechaHora(),
                entidad.getModalidad(),
                entidad.getMensaje(),
                entidad.getEstado(),
                entidad.getObservaciones(),
                entidad.getFechaCreacion(),
                entidad.getFechaActualizacion()
        );
    }
}
