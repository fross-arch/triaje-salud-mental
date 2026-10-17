package com.triaje.aplicacion.casouso;

import com.triaje.aplicacion.puerto.EjecutorTransaccional;
import com.triaje.aplicacion.puerto.RepositorioEvaluaciones;
import com.triaje.dominio.modelo.Evaluacion;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

/**
 * Identifica y marca como ABANDONADA aquellas evaluaciones en estado EN_CURSO
 * cuya fecha de creacion supere las 24 horas. Preserva intacto el indicador riesgo_crisis.
 */
public class AbandonarEvaluacionesVencidas {

    public static final long HORAS_LIMITE_ABANDONO = 24L;

    private final RepositorioEvaluaciones repositorioEvaluaciones;
    private final EjecutorTransaccional transacciones;
    private final Clock reloj;

    public AbandonarEvaluacionesVencidas(RepositorioEvaluaciones repositorioEvaluaciones,
                                         EjecutorTransaccional transacciones,
                                         Clock reloj) {
        this.repositorioEvaluaciones = Objects.requireNonNull(repositorioEvaluaciones);
        this.transacciones = Objects.requireNonNull(transacciones);
        this.reloj = Objects.requireNonNull(reloj);
    }

    public int ejecutar() {
        return transacciones.ejecutar(() -> {
            Instant limite = Instant.now(reloj).minus(HORAS_LIMITE_ABANDONO, ChronoUnit.HOURS);
            List<Evaluacion> vencidas = repositorioEvaluaciones.buscarEnCursoConAntiguedadMayorA(limite);

            for (Evaluacion evaluacion : vencidas) {
                evaluacion.marcarAbandonada();
                repositorioEvaluaciones.guardar(evaluacion);
            }

            return vencidas.size();
        });
    }
}
