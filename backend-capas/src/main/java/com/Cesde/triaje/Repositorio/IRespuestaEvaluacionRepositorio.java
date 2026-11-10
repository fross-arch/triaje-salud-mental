package com.Cesde.triaje.Repositorio;

import com.Cesde.triaje.Modelo.Enumeracion.InstrumentoCribado;
import com.Cesde.triaje.Modelo.MRespuestaEvaluacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IRespuestaEvaluacionRepositorio extends JpaRepository<MRespuestaEvaluacion, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM MRespuestaEvaluacion r WHERE r.evaluacion.id IN (SELECT e.id FROM MEvaluacion e WHERE e.usuario.id = :usuarioId)")
    void deleteByUsuarioId(@Param("usuarioId") Long usuarioId);

    @Query("SELECT r FROM MRespuestaEvaluacion r WHERE r.evaluacion.id = :evaluacionId")
    List<MRespuestaEvaluacion> findByEvaluacionId(@Param("evaluacionId") String evaluacionId);

    @Query("SELECT r FROM MRespuestaEvaluacion r WHERE r.evaluacion.id = :evaluacionId ORDER BY r.instrumento ASC, r.indicePregunta ASC")
    List<MRespuestaEvaluacion> findByEvaluacionIdOrderByInstrumentoAscIndicePreguntaAsc(@Param("evaluacionId") String evaluacionId);

    @Query("SELECT r FROM MRespuestaEvaluacion r WHERE r.evaluacion.id = :evaluacionId AND r.instrumento = :instrumento AND r.indicePregunta = :indicePregunta")
    Optional<MRespuestaEvaluacion> findByEvaluacionIdAndInstrumentoAndIndicePregunta(
            @Param("evaluacionId") String evaluacionId,
            @Param("instrumento") InstrumentoCribado instrumento,
            @Param("indicePregunta") int indicePregunta);
}
