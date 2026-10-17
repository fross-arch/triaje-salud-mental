package com.triaje.aplicacion.puerto;

import com.triaje.dominio.Cita;
import com.triaje.dominio.EstadoCita;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RepositorioCitas {

    Cita guardar(Cita cita);

    Optional<Cita> buscarPorId(String id);

    boolean existePorClinicoYFechaHoraYEstados(Long clinicoId, Instant fechaHora, Collection<EstadoCita> estados);

    boolean existePorClinicoYFechaHoraYEstadosYIdDistinto(Long clinicoId, Instant fechaHora, Collection<EstadoCita> estados, String idExcluido);

    boolean existePorPacienteYClinicoYEstados(Long pacienteId, Long clinicoId, Collection<EstadoCita> estados);

    boolean existePorPacienteYEstados(Long pacienteId, Collection<EstadoCita> estados);

    List<Cita> listarPorClinicoId(Long clinicoId);

    List<Cita> listarPorPacienteId(Long pacienteId);

    List<Cita> listarPorAutorizacionIdYEstado(String autorizacionId, EstadoCita estado);

    List<Cita> listarPropuestasConFechaHoraMenorA(Instant limite);
}
