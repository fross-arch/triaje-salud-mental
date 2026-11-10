package com.Cesde.triaje.Repositorio;

import com.Cesde.triaje.Modelo.Enumeracion.EstadoCita;
import com.Cesde.triaje.Modelo.MCita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

@Repository
public interface ICitaRepositorio extends JpaRepository<MCita, String> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM MCita c WHERE c.paciente.id = :pacienteId")
    void deleteByPacienteId(@Param("pacienteId") Long pacienteId);

    @Query("SELECT c FROM MCita c WHERE c.paciente.id = :pacienteId ORDER BY c.fechaHora DESC")
    List<MCita> findByPacienteIdOrderByFechaHoraDesc(@Param("pacienteId") Long pacienteId);

    @Query("SELECT c FROM MCita c WHERE c.paciente.id = :pacienteId ORDER BY c.fechaHora ASC")
    List<MCita> findByPacienteIdOrderByFechaHoraAsc(@Param("pacienteId") Long pacienteId);

    @Query("SELECT c FROM MCita c WHERE c.clinico.id = :clinicoId ORDER BY c.fechaHora DESC")
    List<MCita> findByClinicoIdOrderByFechaHoraDesc(@Param("clinicoId") Long clinicoId);

    @Query("SELECT c FROM MCita c WHERE c.clinico.id = :clinicoId ORDER BY c.fechaHora ASC")
    List<MCita> findByClinicoIdOrderByFechaHoraAsc(@Param("clinicoId") Long clinicoId);

    @Query("SELECT c FROM MCita c WHERE c.autorizacion.id = :autorizacionId AND c.estado = :estado")
    List<MCita> findByAutorizacionIdAndEstado(
            @Param("autorizacionId") String autorizacionId,
            @Param("estado") EstadoCita estado);

    @Query("SELECT COUNT(c) > 0 FROM MCita c WHERE c.paciente.id = :pacienteId AND c.estado IN :estados")
    boolean existsByPacienteIdAndEstadoIn(
            @Param("pacienteId") Long pacienteId,
            @Param("estados") Collection<EstadoCita> estados);

    @Query("SELECT COUNT(c) > 0 FROM MCita c WHERE c.clinico.id = :clinicoId AND c.fechaHora = :fechaHora AND c.estado IN :estados")
    boolean existsByClinicoIdAndFechaHoraAndEstadoIn(
            @Param("clinicoId") Long clinicoId,
            @Param("fechaHora") Instant fechaHora,
            @Param("estados") Collection<EstadoCita> estados);

    @Query("SELECT COUNT(c) > 0 FROM MCita c WHERE c.clinico.id = :clinicoId AND c.fechaHora = :fechaHora AND c.estado IN :estados AND c.id <> :idExcluido")
    boolean existsByClinicoIdAndFechaHoraAndEstadoInAndIdNot(
            @Param("clinicoId") Long clinicoId,
            @Param("fechaHora") Instant fechaHora,
            @Param("estados") Collection<EstadoCita> estados,
            @Param("idExcluido") String idExcluido);

    @Query("SELECT COUNT(c) > 0 FROM MCita c WHERE c.paciente.id = :pacienteId AND c.clinico.id = :clinicoId AND c.estado IN :estados")
    boolean existsByPacienteIdAndClinicoIdAndEstadoIn(
            @Param("pacienteId") Long pacienteId,
            @Param("clinicoId") Long clinicoId,
            @Param("estados") Collection<EstadoCita> estados);

    List<MCita> findByEstadoAndFechaHoraBefore(EstadoCita estado, Instant limite);

    @Query("SELECT COUNT(DISTINCT c.paciente.id) FROM MCita c WHERE c.estado = :estado")
    long countDistinctPacientesPorEstado(@Param("estado") EstadoCita estado);

    @Query("SELECT COUNT(c) > 0 FROM MCita c WHERE c.clinico.id = :clinicoId")
    boolean existsByClinicoId(@Param("clinicoId") Long clinicoId);
}

