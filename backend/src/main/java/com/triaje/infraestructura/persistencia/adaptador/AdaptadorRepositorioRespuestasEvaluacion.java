package com.triaje.infraestructura.persistencia.adaptador;

import com.triaje.aplicacion.puerto.RepositorioRespuestasEvaluacion;
import com.triaje.dominio.modelo.InstrumentoCribado;
import com.triaje.dominio.modelo.RespuestaEvaluacion;
import com.triaje.infraestructura.persistencia.entidad.RespuestaEvaluacionEntidad;
import com.triaje.infraestructura.persistencia.repositorio.RespuestaEvaluacionRepositorioJpa;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class AdaptadorRepositorioRespuestasEvaluacion implements RepositorioRespuestasEvaluacion {

    private final RespuestaEvaluacionRepositorioJpa repositorio;

    public AdaptadorRepositorioRespuestasEvaluacion(RespuestaEvaluacionRepositorioJpa repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public RespuestaEvaluacion guardar(RespuestaEvaluacion respuesta) {
        RespuestaEvaluacionEntidad entidad = new RespuestaEvaluacionEntidad(
                respuesta.getId(),
                respuesta.getEvaluacionId(),
                respuesta.getInstrumento(),
                respuesta.getIndicePregunta(),
                respuesta.getValorPuntaje()
        );
        RespuestaEvaluacionEntidad guardada = repositorio.saveAndFlush(entidad);
        return aDominio(guardada);
    }

    @Override
    public Optional<RespuestaEvaluacion> buscarPorEvaluacionEInstrumentoEIndice(
            String evaluacionId,
            InstrumentoCribado instrumento,
            int indicePregunta
    ) {
        return repositorio.findByEvaluacionIdAndInstrumentoAndIndicePregunta(evaluacionId, instrumento, indicePregunta)
                .map(AdaptadorRepositorioRespuestasEvaluacion::aDominio);
    }

    @Override
    public List<RespuestaEvaluacion> buscarPorEvaluacionId(String evaluacionId) {
        return repositorio.findByEvaluacionIdOrderByInstrumentoAscIndicePreguntaAsc(evaluacionId).stream()
                .map(AdaptadorRepositorioRespuestasEvaluacion::aDominio)
                .toList();
    }

    private static RespuestaEvaluacion aDominio(RespuestaEvaluacionEntidad entidad) {
        return new RespuestaEvaluacion(
                entidad.getId(),
                entidad.getEvaluacionId(),
                entidad.getInstrumento(),
                entidad.getIndicePregunta(),
                entidad.getValorPuntaje()
        );
    }
}
