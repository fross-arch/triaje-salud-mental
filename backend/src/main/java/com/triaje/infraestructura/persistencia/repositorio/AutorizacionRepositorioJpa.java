package com.triaje.infraestructura.persistencia.repositorio;

import com.triaje.infraestructura.persistencia.entidad.AutorizacionEntidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AutorizacionRepositorioJpa extends JpaRepository<AutorizacionEntidad, String> {

    Optional<AutorizacionEntidad> findByEvaluacionIdAndClinicoId(String evaluacionId, Long clinicoId);

    Optional<AutorizacionEntidad> findByEvaluacionIdAndClinicoIdAndFechaRevocacionIsNull(String evaluacionId, Long clinicoId);

    List<AutorizacionEntidad> findByEvaluacionIdOrderByFechaOtorgamientoDesc(String evaluacionId);

    List<AutorizacionEntidad> findByClinicoIdAndFechaRevocacionIsNull(Long clinicoId);

    List<AutorizacionEntidad> findByPacienteIdAndFechaRevocacionIsNull(Long pacienteId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update AutorizacionEntidad a set a.fechaRevocacion = :fecha where a.pacienteId = :pacienteId and a.fechaRevocacion is null")
    int revocarTodasPorPacienteId(@Param("pacienteId") Long pacienteId, @Param("fecha") Instant fecha);
}
