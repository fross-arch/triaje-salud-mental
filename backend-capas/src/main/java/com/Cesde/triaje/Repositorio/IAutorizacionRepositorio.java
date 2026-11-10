package com.Cesde.triaje.Repositorio;

import com.Cesde.triaje.Modelo.MAutorizacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface IAutorizacionRepositorio extends JpaRepository<MAutorizacion, String> {

    @Query("SELECT a FROM MAutorizacion a WHERE a.evaluacion.id = :evaluacionId AND a.clinico.id = :clinicoId")
    Optional<MAutorizacion> findByEvaluacionIdAndClinicoId(
            @Param("evaluacionId") String evaluacionId,
            @Param("clinicoId") Long clinicoId);

    @Query("SELECT a FROM MAutorizacion a WHERE a.evaluacion.id = :evaluacionId AND a.clinico.id = :clinicoId AND a.fechaRevocacion IS NULL")
    Optional<MAutorizacion> findByEvaluacionIdAndClinicoIdAndFechaRevocacionIsNull(
            @Param("evaluacionId") String evaluacionId,
            @Param("clinicoId") Long clinicoId);

    @Query("SELECT a FROM MAutorizacion a WHERE a.evaluacion.id = :evaluacionId")
    List<MAutorizacion> findByEvaluacionId(@Param("evaluacionId") String evaluacionId);

    @Query("SELECT a FROM MAutorizacion a WHERE a.evaluacion.id = :evaluacionId ORDER BY a.fechaOtorgamiento DESC")
    List<MAutorizacion> findByEvaluacionIdOrderByFechaOtorgamientoDesc(@Param("evaluacionId") String evaluacionId);

    @Query("SELECT a FROM MAutorizacion a WHERE a.paciente.id = :pacienteId AND a.fechaRevocacion IS NULL")
    List<MAutorizacion> findByPacienteIdAndFechaRevocacionIsNull(@Param("pacienteId") Long pacienteId);

    @Query("SELECT a FROM MAutorizacion a WHERE a.clinico.id = :clinicoId AND a.fechaRevocacion IS NULL")
    List<MAutorizacion> findByClinicoIdAndFechaRevocacionIsNull(@Param("clinicoId") Long clinicoId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE MAutorizacion a SET a.fechaRevocacion = :fecha WHERE a.paciente.id = :pacienteId AND a.fechaRevocacion IS NULL")
    int revocarTodasPorPacienteId(@Param("pacienteId") Long pacienteId, @Param("fecha") Instant fecha);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM MAutorizacion a WHERE a.paciente.id = :pacienteId")
    void deleteByPacienteId(@Param("pacienteId") Long pacienteId);

    @Query("SELECT COUNT(a) > 0 FROM MAutorizacion a WHERE a.clinico.id = :clinicoId")
    boolean existsByClinicoId(@Param("clinicoId") Long clinicoId);
}
