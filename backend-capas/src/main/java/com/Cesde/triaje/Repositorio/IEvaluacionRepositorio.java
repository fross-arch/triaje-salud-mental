package com.Cesde.triaje.Repositorio;

import com.Cesde.triaje.Modelo.Enumeracion.EstadoEvaluacion;
import com.Cesde.triaje.Modelo.MEvaluacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface IEvaluacionRepositorio extends JpaRepository<MEvaluacion, String> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM MEvaluacion e WHERE e.usuario.id = :usuarioId")
    void deleteByUsuarioId(@Param("usuarioId") Long usuarioId);

    @Query("SELECT e FROM MEvaluacion e WHERE e.usuario.id = :usuarioId ORDER BY e.fechaCreacion DESC")
    List<MEvaluacion> findByUsuarioIdOrderByFechaCreacionDesc(@Param("usuarioId") Long usuarioId);

    @Query("SELECT COUNT(e) > 0 FROM MEvaluacion e WHERE e.usuario.id = :usuarioId AND e.estado = :estado")
    boolean existsByUsuarioIdAndEstado(
            @Param("usuarioId") Long usuarioId,
            @Param("estado") EstadoEvaluacion estado);

    List<MEvaluacion> findByEstadoAndFechaCreacionBefore(EstadoEvaluacion estado, Instant limite);

    List<IEvaluacionAnonimaProjection> findByEstadoOrderByFechaFinalizacionAsc(EstadoEvaluacion estado);

    List<IEvaluacionAnonimaProjection> findByEstadoAndFechaFinalizacionGreaterThanEqualOrderByFechaFinalizacionAsc(
            EstadoEvaluacion estado, Instant limiteFecha);

    @Query("SELECT COUNT(DISTINCT e.usuario.id) FROM MEvaluacion e WHERE e.estado = :estado")
    long countDistinctUsuariosPorEstado(@Param("estado") EstadoEvaluacion estado);
}

