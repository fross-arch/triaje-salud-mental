package com.triaje.aplicacion.casouso;

import com.triaje.aplicacion.dto.DatosEvaluacion;
import com.triaje.aplicacion.puerto.EjecutorTransaccional;
import com.triaje.aplicacion.puerto.RepositorioAuditoria;
import com.triaje.aplicacion.puerto.RepositorioEvaluaciones;
import com.triaje.aplicacion.puerto.RepositorioRespuestasEvaluacion;
import com.triaje.dominio.EvaluadorGad7;
import com.triaje.dominio.EvaluadorPhq9;
import com.triaje.dominio.ReglaCrisis;
import com.triaje.dominio.ReglaResultadoMinimo;
import com.triaje.dominio.SeveridadAnsiedad;
import com.triaje.dominio.SeveridadDepresion;
import com.triaje.dominio.excepcion.ExcepcionDominio;
import com.triaje.dominio.excepcion.ExcepcionEvaluacionNoEncontrada;
import com.triaje.dominio.modelo.AccionAuditoria;
import com.triaje.dominio.modelo.EstadoEvaluacion;
import com.triaje.dominio.modelo.Evaluacion;
import com.triaje.dominio.modelo.InstrumentoCribado;
import com.triaje.dominio.modelo.RegistroAuditoria;
import com.triaje.dominio.modelo.RespuestaEvaluacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.Supplier;

class FinalizarEvaluacionTest {

    private RepositorioEvaluaciones repositorioEvaluaciones;
    private RepositorioRespuestasEvaluacion repositorioRespuestas;
    private RepositorioAuditoria repositorioAuditoria;
    private EvaluadorPhq9 evaluadorPhq9;
    private EvaluadorGad7 evaluadorGad7;
    private ReglaResultadoMinimo reglaResultadoMinimo;
    private Clock reloj;
    private FinalizarEvaluacion casoUso;

    @BeforeEach
    void inicializar() {
        repositorioEvaluaciones = mock(RepositorioEvaluaciones.class);
        repositorioRespuestas = mock(RepositorioRespuestasEvaluacion.class);
        repositorioAuditoria = mock(RepositorioAuditoria.class);
        evaluadorPhq9 = new EvaluadorPhq9();
        evaluadorGad7 = new EvaluadorGad7();
        ReglaCrisis reglaCrisis = new ReglaCrisis();
        reglaResultadoMinimo = new ReglaResultadoMinimo(reglaCrisis);
        EjecutorTransaccional transaccional = new EjecutorTransaccional() {
            @Override
            public <T> T ejecutar(Supplier<T> operacion) {
                return operacion.get();
            }
        };
        reloj = Clock.fixed(Instant.parse("2026-10-13T10:00:00Z"), ZoneOffset.UTC);

        casoUso = new FinalizarEvaluacion(
                repositorioEvaluaciones,
                repositorioRespuestas,
                repositorioAuditoria,
                evaluadorPhq9,
                evaluadorGad7,
                reglaCrisis,
                reglaResultadoMinimo,
                transaccional,
                reloj
        );
    }

    @Test
    @DisplayName("Debe rechazar la finalizacion si faltan respuestas")
    void deberiaRechazarSiFaltanRespuestas() {
        Evaluacion evaluacion = Evaluacion.nueva("eval-1", 1L, Instant.now(reloj));
        when(repositorioEvaluaciones.buscarPorId("eval-1")).thenReturn(Optional.of(evaluacion));

        // Solo 8 respuestas de PHQ-9
        List<RespuestaEvaluacion> incompletas = new ArrayList<>();
        for (int i = 1; i <= 8; i++) {
            incompletas.add(new RespuestaEvaluacion((long) i, "eval-1", InstrumentoCribado.PHQ9, i, 0));
        }
        when(repositorioRespuestas.buscarPorEvaluacionId("eval-1")).thenReturn(incompletas);

        assertThrows(ExcepcionDominio.class, () -> casoUso.ejecutar("eval-1", 1L));
    }

    @Test
    @DisplayName("Debe finalizar evaluacion completa con resultado minimo true si ambas son MINIMA y sin crisis")
    void deberiaFinalizarConResultadoMinimoTrue() {
        Evaluacion evaluacion = Evaluacion.nueva("eval-1", 1L, Instant.now(reloj));
        when(repositorioEvaluaciones.buscarPorId("eval-1")).thenReturn(Optional.of(evaluacion));
        when(repositorioEvaluaciones.guardar(any(Evaluacion.class))).thenAnswer(inv -> inv.getArgument(0));

        List<RespuestaEvaluacion> todas = new ArrayList<>();
        // 9 de PHQ-9 en 0 (total 0, MINIMA, item 9 = 0)
        for (int i = 1; i <= 9; i++) {
            todas.add(new RespuestaEvaluacion((long) i, "eval-1", InstrumentoCribado.PHQ9, i, 0));
        }
        // 7 de GAD-7 en 0 (total 0, MINIMA)
        for (int i = 1; i <= 7; i++) {
            todas.add(new RespuestaEvaluacion((long) (10 + i), "eval-1", InstrumentoCribado.GAD7, i, 0));
        }
        when(repositorioRespuestas.buscarPorEvaluacionId("eval-1")).thenReturn(todas);

        DatosEvaluacion resultado = casoUso.ejecutar("eval-1", 1L);

        assertEquals(EstadoEvaluacion.COMPLETADA, resultado.estado());
        assertEquals(0, resultado.puntajePhq9());
        assertEquals(0, resultado.puntajeGad7());
        assertEquals(SeveridadDepresion.MINIMA, resultado.severidadDepresion());
        assertEquals(SeveridadAnsiedad.MINIMA, resultado.severidadAnsiedad());
        assertFalse(resultado.riesgoCrisis());
        assertTrue(resultado.resultadoMinimo());

        ArgumentCaptor<RegistroAuditoria> captor = ArgumentCaptor.forClass(RegistroAuditoria.class);
        verify(repositorioAuditoria).registrar(captor.capture());
        assertEquals(AccionAuditoria.EVALUACION_COMPLETADA, captor.getValue().accion());
        assertEquals("eval-1", captor.getValue().entidadId());
    }

    @Test
    @DisplayName("Debe calcular resultadoMinimo en false si hay crisis aunque puntajes sean minimos")
    void deberiaCalcularResultadoMinimoFalseSiHayCrisis() {
        Evaluacion evaluacion = Evaluacion.nueva("eval-1", 1L, Instant.now(reloj));
        evaluacion.marcarRiesgoCrisis();
        when(repositorioEvaluaciones.buscarPorId("eval-1")).thenReturn(Optional.of(evaluacion));
        when(repositorioEvaluaciones.guardar(any(Evaluacion.class))).thenAnswer(inv -> inv.getArgument(0));

        List<RespuestaEvaluacion> todas = new ArrayList<>();
        for (int i = 1; i <= 8; i++) {
            todas.add(new RespuestaEvaluacion((long) i, "eval-1", InstrumentoCribado.PHQ9, i, 0));
        }
        todas.add(new RespuestaEvaluacion(9L, "eval-1", InstrumentoCribado.PHQ9, 9, 1)); // crisis
        for (int i = 1; i <= 7; i++) {
            todas.add(new RespuestaEvaluacion((long) (10 + i), "eval-1", InstrumentoCribado.GAD7, i, 0));
        }
        when(repositorioRespuestas.buscarPorEvaluacionId("eval-1")).thenReturn(todas);

        DatosEvaluacion resultado = casoUso.ejecutar("eval-1", 1L);

        assertTrue(resultado.riesgoCrisis());
        assertFalse(resultado.resultadoMinimo());
    }

    @Test
    @DisplayName("Debe rechazar si el usuario que finaliza no es el dueno")
    void deberiaRechazarSiUsuarioNoEsDueno() {
        Evaluacion evaluacion = Evaluacion.nueva("eval-1", 1L, Instant.now(reloj));
        when(repositorioEvaluaciones.buscarPorId("eval-1")).thenReturn(Optional.of(evaluacion));

        assertThrows(ExcepcionEvaluacionNoEncontrada.class, () -> casoUso.ejecutar("eval-1", 2L));
    }
}
