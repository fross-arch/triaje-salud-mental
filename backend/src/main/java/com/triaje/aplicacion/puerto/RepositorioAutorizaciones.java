package com.triaje.aplicacion.puerto;

import com.triaje.dominio.Autorizacion;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface RepositorioAutorizaciones {

    Autorizacion guardar(Autorizacion autorizacion);

    Optional<Autorizacion> buscarPorEvaluacionYClinico(String evaluacionId, Long clinicoId);

    Optional<Autorizacion> buscarVigentePorEvaluacionYClinico(String evaluacionId, Long clinicoId);

    List<Autorizacion> listarPorEvaluacionId(String evaluacionId);

    List<Autorizacion> listarVigentesPorClinicoId(Long clinicoId);

    List<Autorizacion> listarVigentesPorPacienteId(Long pacienteId);

    void revocarVigentesPorPacienteId(Long pacienteId, Instant fechaRevocacion);
}
