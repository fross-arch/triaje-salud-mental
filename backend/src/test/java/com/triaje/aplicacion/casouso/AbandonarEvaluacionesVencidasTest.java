package com.triaje.aplicacion.casouso;

import com.triaje.aplicacion.puerto.EjecutorTransaccional;
import com.triaje.aplicacion.puerto.RepositorioEvaluaciones;
import com.triaje.dominio.modelo.EstadoEvaluacion;
import com.triaje.dominio.modelo.Evaluacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.Supplier;

class AbandonarEvaluacionesVencidasTest {

    private RepositorioEvaluaciones repositorioEvaluaciones;
    private Clock reloj;
    private AbandonarEvaluacionesVencidas casoUso;

    @BeforeEach
    void inicializar() {
        repositorioEvaluaciones = mock(RepositorioEvaluaciones.class);
        EjecutorTransaccional transaccional = new EjecutorTransaccional() {
            @Override
            public <T> T ejecutar(Supplier<T> operacion) {
                return operacion.get();
            }
        };
        reloj = Clock.fixed(Instant.parse("2026-10-13T10:00:00Z"), ZoneOffset.UTC);
        casoUso = new AbandonarEvaluacionesVencidas(repositorioEvaluaciones, transaccional, reloj);
    }

    @Test
    @DisplayName("Debe marcar como ABANDONADA las evaluaciones con mas de 24 horas preservando riesgoCrisis")
    void deberiaAbandonarEvaluacionesVencidasPreservandoCrisis() {
        Instant pasado = Instant.now(reloj).minus(25, ChronoUnit.HOURS);
        Evaluacion vencidaConCrisis = Evaluacion.nueva("eval-vencida", 1L, pasado);
        vencidaConCrisis.marcarRiesgoCrisis();

        when(repositorioEvaluaciones.buscarEnCursoConAntiguedadMayorA(any(Instant.class)))
                .thenReturn(List.of(vencidaConCrisis));

        int cantidad = casoUso.ejecutar();

        assertEquals(1, cantidad);
        assertEquals(EstadoEvaluacion.ABANDONADA, vencidaConCrisis.getEstado());
        assertTrue(vencidaConCrisis.isRiesgoCrisis());
        verify(repositorioEvaluaciones).guardar(vencidaConCrisis);
    }
}
