package com.triaje.aplicacion.casouso;

import com.triaje.aplicacion.dto.DatosAutorizacion;
import com.triaje.aplicacion.puerto.RepositorioAutorizaciones;
import com.triaje.aplicacion.puerto.RepositorioEvaluaciones;
import com.triaje.dominio.excepcion.ExcepcionEvaluacionNoEncontrada;
import com.triaje.dominio.modelo.Evaluacion;

import java.util.List;
import java.util.Objects;

/**
 * Caso de uso: el paciente lista las autorizaciones asociadas a su evaluacion y su estado.
 */
public class ListarAutorizacionesEvaluacion {

    private final RepositorioAutorizaciones repositorioAutorizaciones;
    private final RepositorioEvaluaciones repositorioEvaluaciones;

    public ListarAutorizacionesEvaluacion(RepositorioAutorizaciones repositorioAutorizaciones,
                                         RepositorioEvaluaciones repositorioEvaluaciones) {
        this.repositorioAutorizaciones = Objects.requireNonNull(repositorioAutorizaciones);
        this.repositorioEvaluaciones = Objects.requireNonNull(repositorioEvaluaciones);
    }

    public List<DatosAutorizacion> ejecutar(String evaluacionId, Long pacienteId) {
        Evaluacion evaluacion = repositorioEvaluaciones.buscarPorId(evaluacionId)
                .orElseThrow(() -> new ExcepcionEvaluacionNoEncontrada("La evaluacion no existe o no tienes acceso a ella."));

        if (!evaluacion.getUsuarioId().equals(pacienteId)) {
            throw new ExcepcionEvaluacionNoEncontrada("La evaluacion no existe o no tienes acceso a ella.");
        }

        return repositorioAutorizaciones.listarPorEvaluacionId(evaluacionId).stream()
                .map(DatosAutorizacion::desde)
                .toList();
    }
}
