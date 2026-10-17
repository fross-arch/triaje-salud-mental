package com.triaje.infraestructura.persistencia.repositorio;

import com.triaje.dominio.EstadoCita;
import com.triaje.infraestructura.persistencia.entidad.CitaEntidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface CitaRepositorioJpa extends JpaRepository<CitaEntidad, String> {

    boolean existsByClinicoIdAndFechaHoraAndEstadoIn(Long clinicoId, Instant fechaHora, Collection<EstadoCita> estados);

    boolean existsByClinicoIdAndFechaHoraAndEstadoInAndIdNot(Long clinicoId, Instant fechaHora, Collection<EstadoCita> estados, String idExcluido);

    boolean existsByPacienteIdAndClinicoIdAndEstadoIn(Long pacienteId, Long clinicoId, Collection<EstadoCita> estados);

    boolean existsByPacienteIdAndEstadoIn(Long pacienteId, Collection<EstadoCita> estados);

    List<CitaEntidad> findByClinicoIdOrderByFechaHoraAsc(Long clinicoId);

    List<CitaEntidad> findByPacienteIdOrderByFechaHoraAsc(Long pacienteId);

    List<CitaEntidad> findByAutorizacionIdAndEstado(String autorizacionId, EstadoCita estado);

    List<CitaEntidad> findByEstadoAndFechaHoraBefore(EstadoCita estado, Instant limite);
}
