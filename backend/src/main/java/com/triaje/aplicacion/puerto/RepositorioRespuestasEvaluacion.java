package com.triaje.aplicacion.puerto;

import com.triaje.dominio.modelo.InstrumentoCribado;
import com.triaje.dominio.modelo.RespuestaEvaluacion;

import java.util.List;
import java.util.Optional;

public interface RepositorioRespuestasEvaluacion {

    RespuestaEvaluacion guardar(RespuestaEvaluacion respuesta);

    Optional<RespuestaEvaluacion> buscarPorEvaluacionEInstrumentoEIndice(
            String evaluacionId,
            InstrumentoCribado instrumento,
            int indicePregunta
    );

    List<RespuestaEvaluacion> buscarPorEvaluacionId(String evaluacionId);
}
