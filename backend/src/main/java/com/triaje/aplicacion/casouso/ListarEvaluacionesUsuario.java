package com.triaje.aplicacion.casouso;

import com.triaje.aplicacion.dto.DatosEvaluacion;
import com.triaje.aplicacion.puerto.RepositorioEvaluaciones;
import com.triaje.dominio.ReglaResultadoMinimo;
import com.triaje.dominio.modelo.EstadoEvaluacion;
import com.triaje.dominio.modelo.Evaluacion;

import java.util.List;
import java.util.Objects;

/**
 * Consulta el historial de evaluaciones del usuario autenticado ordenadas de forma descendente por fecha.
 */
public class ListarEvaluacionesUsuario {

    private final RepositorioEvaluaciones repositorioEvaluaciones;
    private final ReglaResultadoMinimo reglaResultadoMinimo;

    public ListarEvaluacionesUsuario(RepositorioEvaluaciones repositorioEvaluaciones,
                                     ReglaResultadoMinimo reglaResultadoMinimo) {
        this.repositorioEvaluaciones = Objects.requireNonNull(repositorioEvaluaciones);
        this.reglaResultadoMinimo = Objects.requireNonNull(reglaResultadoMinimo);
    }

    public List<DatosEvaluacion> ejecutar(Long usuarioId) {
        List<Evaluacion> evaluaciones = repositorioEvaluaciones.buscarPorUsuarioIdOrdenadasPorFechaDescendente(usuarioId);
        return evaluaciones.stream()
                .map(e -> {
                    Boolean resultadoMinimo = null;
                    if (e.getEstado() == EstadoEvaluacion.COMPLETADA
                            && e.getSeveridadDepresion() != null
                            && e.getSeveridadAnsiedad() != null) {
                        resultadoMinimo = reglaResultadoMinimo.evaluar(
                                e.getSeveridadDepresion(),
                                e.getSeveridadAnsiedad(),
                                e.isRiesgoCrisis()
                        );
                    }
                    return DatosEvaluacion.desde(e, resultadoMinimo);
                })
                .toList();
    }
}
