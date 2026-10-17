package com.triaje.infraestructura.persistencia.adaptador;

import com.triaje.aplicacion.puerto.RepositorioAutorizaciones;
import com.triaje.dominio.Autorizacion;
import com.triaje.infraestructura.persistencia.entidad.AutorizacionEntidad;
import com.triaje.infraestructura.persistencia.repositorio.AutorizacionRepositorioJpa;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
public class AdaptadorRepositorioAutorizaciones implements RepositorioAutorizaciones {

    private final AutorizacionRepositorioJpa repositorio;

    public AdaptadorRepositorioAutorizaciones(AutorizacionRepositorioJpa repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public Autorizacion guardar(Autorizacion autorizacion) {
        AutorizacionEntidad entidad = new AutorizacionEntidad(
                autorizacion.getId(),
                autorizacion.getEvaluacionId(),
                autorizacion.getPacienteId(),
                autorizacion.getClinicoId(),
                autorizacion.getVersionTextoAutorizacion(),
                autorizacion.getFechaOtorgamiento(),
                autorizacion.getFechaRevocacion()
        );
        AutorizacionEntidad guardada = repositorio.saveAndFlush(entidad);
        return aDominio(guardada);
    }

    @Override
    public Optional<Autorizacion> buscarPorEvaluacionYClinico(String evaluacionId, Long clinicoId) {
        return repositorio.findByEvaluacionIdAndClinicoId(evaluacionId, clinicoId)
                .map(AdaptadorRepositorioAutorizaciones::aDominio);
    }

    @Override
    public Optional<Autorizacion> buscarVigentePorEvaluacionYClinico(String evaluacionId, Long clinicoId) {
        return repositorio.findByEvaluacionIdAndClinicoIdAndFechaRevocacionIsNull(evaluacionId, clinicoId)
                .map(AdaptadorRepositorioAutorizaciones::aDominio);
    }

    @Override
    public List<Autorizacion> listarPorEvaluacionId(String evaluacionId) {
        return repositorio.findByEvaluacionIdOrderByFechaOtorgamientoDesc(evaluacionId).stream()
                .map(AdaptadorRepositorioAutorizaciones::aDominio)
                .toList();
    }

    @Override
    public List<Autorizacion> listarVigentesPorClinicoId(Long clinicoId) {
        return repositorio.findByClinicoIdAndFechaRevocacionIsNull(clinicoId).stream()
                .map(AdaptadorRepositorioAutorizaciones::aDominio)
                .toList();
    }

    @Override
    public List<Autorizacion> listarVigentesPorPacienteId(Long pacienteId) {
        return repositorio.findByPacienteIdAndFechaRevocacionIsNull(pacienteId).stream()
                .map(AdaptadorRepositorioAutorizaciones::aDominio)
                .toList();
    }

    @Override
    public void revocarVigentesPorPacienteId(Long pacienteId, Instant fechaRevocacion) {
        repositorio.revocarTodasPorPacienteId(pacienteId, fechaRevocacion);
    }

    private static Autorizacion aDominio(AutorizacionEntidad entidad) {
        return new Autorizacion(
                entidad.getId(),
                entidad.getEvaluacionId(),
                entidad.getPacienteId(),
                entidad.getClinicoId(),
                entidad.getVersionTextoAutorizacion(),
                entidad.getFechaOtorgamiento(),
                entidad.getFechaRevocacion()
        );
    }
}
