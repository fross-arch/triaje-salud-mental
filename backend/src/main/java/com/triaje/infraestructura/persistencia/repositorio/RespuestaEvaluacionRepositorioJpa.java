package com.triaje.infraestructura.persistencia.repositorio;

import com.triaje.dominio.modelo.InstrumentoCribado;
import com.triaje.infraestructura.persistencia.entidad.RespuestaEvaluacionEntidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RespuestaEvaluacionRepositorioJpa extends JpaRepository<RespuestaEvaluacionEntidad, Long> {

    Optional<RespuestaEvaluacionEntidad> findByEvaluacionIdAndInstrumentoAndIndicePregunta(
            String evaluacionId,
            InstrumentoCribado instrumento,
            int indicePregunta
    );

    List<RespuestaEvaluacionEntidad> findByEvaluacionIdOrderByInstrumentoAscIndicePreguntaAsc(String evaluacionId);
}
