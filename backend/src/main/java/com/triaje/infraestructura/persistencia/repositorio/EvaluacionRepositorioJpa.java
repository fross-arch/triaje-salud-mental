package com.triaje.infraestructura.persistencia.repositorio;

import com.triaje.dominio.modelo.EstadoEvaluacion;
import com.triaje.infraestructura.persistencia.entidad.EvaluacionEntidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface EvaluacionRepositorioJpa extends JpaRepository<EvaluacionEntidad, String> {

    List<EvaluacionEntidad> findByUsuarioIdOrderByFechaCreacionDesc(Long usuarioId);

    boolean existsByUsuarioIdAndEstado(Long usuarioId, EstadoEvaluacion estado);

    List<EvaluacionEntidad> findByEstadoAndFechaCreacionBefore(EstadoEvaluacion estado, Instant fechaLimite);
}
