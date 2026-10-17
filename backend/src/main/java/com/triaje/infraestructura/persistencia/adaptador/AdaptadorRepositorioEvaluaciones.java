package com.triaje.infraestructura.persistencia.adaptador;

import com.triaje.aplicacion.puerto.RepositorioEvaluaciones;
import com.triaje.dominio.modelo.EstadoEvaluacion;
import com.triaje.dominio.modelo.Evaluacion;
import com.triaje.infraestructura.persistencia.entidad.EvaluacionEntidad;
import com.triaje.infraestructura.persistencia.repositorio.EvaluacionRepositorioJpa;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
public class AdaptadorRepositorioEvaluaciones implements RepositorioEvaluaciones {

    private final EvaluacionRepositorioJpa repositorio;

    public AdaptadorRepositorioEvaluaciones(EvaluacionRepositorioJpa repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public Evaluacion guardar(Evaluacion evaluacion) {
        EvaluacionEntidad entidad = new EvaluacionEntidad(
                evaluacion.getId(),
                evaluacion.getUsuarioId(),
                evaluacion.getEstado(),
                evaluacion.getPuntajePhq9(),
                evaluacion.getPuntajeGad7(),
                evaluacion.getSeveridadDepresion(),
                evaluacion.getSeveridadAnsiedad(),
                evaluacion.isRiesgoCrisis(),
                evaluacion.getFechaCreacion(),
                evaluacion.getFechaFinalizacion()
        );
        EvaluacionEntidad guardada = repositorio.saveAndFlush(entidad);
        return aDominio(guardada);
    }

    @Override
    public Optional<Evaluacion> buscarPorId(String id) {
        return repositorio.findById(id).map(AdaptadorRepositorioEvaluaciones::aDominio);
    }

    @Override
    public List<Evaluacion> buscarPorUsuarioIdOrdenadasPorFechaDescendente(Long usuarioId) {
        return repositorio.findByUsuarioIdOrderByFechaCreacionDesc(usuarioId).stream()
                .map(AdaptadorRepositorioEvaluaciones::aDominio)
                .toList();
    }

    @Override
    public boolean existePorUsuarioIdYEstado(Long usuarioId, EstadoEvaluacion estado) {
        return repositorio.existsByUsuarioIdAndEstado(usuarioId, estado);
    }

    @Override
    public List<Evaluacion> buscarEnCursoConAntiguedadMayorA(Instant fechaLimite) {
        return repositorio.findByEstadoAndFechaCreacionBefore(EstadoEvaluacion.EN_CURSO, fechaLimite).stream()
                .map(AdaptadorRepositorioEvaluaciones::aDominio)
                .toList();
    }

    private static Evaluacion aDominio(EvaluacionEntidad entidad) {
        return new Evaluacion(
                entidad.getId(),
                entidad.getUsuarioId(),
                entidad.getEstado(),
                entidad.getPuntajePhq9(),
                entidad.getPuntajeGad7(),
                entidad.getSeveridadDepresion(),
                entidad.getSeveridadAnsiedad(),
                entidad.isRiesgoCrisis(),
                entidad.getFechaCreacion(),
                entidad.getFechaFinalizacion()
        );
    }
}
