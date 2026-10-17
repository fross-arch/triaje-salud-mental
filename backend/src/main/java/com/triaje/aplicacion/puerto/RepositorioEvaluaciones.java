package com.triaje.aplicacion.puerto;

import com.triaje.dominio.modelo.Evaluacion;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface RepositorioEvaluaciones {

    Evaluacion guardar(Evaluacion evaluacion);

    Optional<Evaluacion> buscarPorId(String id);

    List<Evaluacion> buscarPorUsuarioIdOrdenadasPorFechaDescendente(Long usuarioId);

    boolean existePorUsuarioIdYEstado(Long usuarioId, com.triaje.dominio.modelo.EstadoEvaluacion estado);

    List<Evaluacion> buscarEnCursoConAntiguedadMayorA(Instant fechaLimite);
}
