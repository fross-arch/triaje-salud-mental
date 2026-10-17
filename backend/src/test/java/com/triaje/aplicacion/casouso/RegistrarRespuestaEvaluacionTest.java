package com.triaje.aplicacion.casouso;

import com.triaje.aplicacion.dto.ComandoRegistrarRespuesta;
import com.triaje.aplicacion.dto.ResultadoRegistroRespuesta;
import com.triaje.aplicacion.puerto.EjecutorTransaccional;
import com.triaje.aplicacion.puerto.RepositorioEvaluaciones;
import com.triaje.aplicacion.puerto.RepositorioRespuestasEvaluacion;
import com.triaje.dominio.excepcion.ExcepcionDominio;
import com.triaje.dominio.excepcion.ExcepcionEvaluacionNoEncontrada;
import com.triaje.dominio.modelo.EstadoEvaluacion;
import com.triaje.dominio.modelo.Evaluacion;
import com.triaje.dominio.modelo.InstrumentoCribado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.Supplier;

class RegistrarRespuestaEvaluacionTest {

    private RepositorioEvaluaciones repositorioEvaluaciones;
    private RepositorioRespuestasEvaluacion repositorioRespuestas;
    private RegistrarRespuestaEvaluacion casoUso;

    @BeforeEach
    void inicializar() {
        repositorioEvaluaciones = mock(RepositorioEvaluaciones.class);
        repositorioRespuestas = mock(RepositorioRespuestasEvaluacion.class);
        EjecutorTransaccional transaccional = new EjecutorTransaccional() {
            @Override
            public <T> T ejecutar(Supplier<T> operacion) {
                return operacion.get();
            }
        };
        casoUso = new RegistrarRespuestaEvaluacion(repositorioEvaluaciones, repositorioRespuestas, transaccional);
    }

    @Test
    @DisplayName("Debe activar riesgo_crisis de inmediato si item 9 de PHQ-9 es >= 1")
    void deberiaActivarRiesgoCrisisDeInmediatoConItem9Positivo() {
        Evaluacion evaluacion = Evaluacion.nueva("eval-1", 1L, Instant.now());
        when(repositorioEvaluaciones.buscarPorId("eval-1")).thenReturn(Optional.of(evaluacion));
        when(repositorioRespuestas.buscarPorEvaluacionEInstrumentoEIndice("eval-1", InstrumentoCribado.PHQ9, 9))
                .thenReturn(Optional.empty());

        ComandoRegistrarRespuesta comando = new ComandoRegistrarRespuesta(
                "eval-1", 1L, InstrumentoCribado.PHQ9, 9, 1
        );

        ResultadoRegistroRespuesta resultado = casoUso.ejecutar(comando);

        assertTrue(resultado.riesgoCrisis());
        assertTrue(evaluacion.isRiesgoCrisis());
        verify(repositorioEvaluaciones).guardar(evaluacion);
    }

    @Test
    @DisplayName("No debe activar riesgo_crisis si item 9 de PHQ-9 es 0")
    void noDeberiaActivarRiesgoCrisisConItem9EnCero() {
        Evaluacion evaluacion = Evaluacion.nueva("eval-1", 1L, Instant.now());
        when(repositorioEvaluaciones.buscarPorId("eval-1")).thenReturn(Optional.of(evaluacion));
        when(repositorioRespuestas.buscarPorEvaluacionEInstrumentoEIndice("eval-1", InstrumentoCribado.PHQ9, 9))
                .thenReturn(Optional.empty());

        ComandoRegistrarRespuesta comando = new ComandoRegistrarRespuesta(
                "eval-1", 1L, InstrumentoCribado.PHQ9, 9, 0
        );

        ResultadoRegistroRespuesta resultado = casoUso.ejecutar(comando);

        assertFalse(resultado.riesgoCrisis());
        assertFalse(evaluacion.isRiesgoCrisis());
    }

    @Test
    @DisplayName("Debe rechazar con ExcepcionEvaluacionNoEncontrada si el usuario no es el propietario")
    void deberiaRechazarSiUsuarioNoEsPropietario() {
        Evaluacion evaluacion = Evaluacion.nueva("eval-1", 1L, Instant.now());
        when(repositorioEvaluaciones.buscarPorId("eval-1")).thenReturn(Optional.of(evaluacion));

        ComandoRegistrarRespuesta comando = new ComandoRegistrarRespuesta(
                "eval-1", 2L, InstrumentoCribado.PHQ9, 1, 0
        );

        assertThrows(ExcepcionEvaluacionNoEncontrada.class, () -> casoUso.ejecutar(comando));
    }

    @Test
    @DisplayName("Debe rechazar si la evaluacion esta en estado COMPLETADA")
    void deberiaRechazarSiEvaluacionEstaCompletada() {
        Evaluacion evaluacion = Evaluacion.nueva("eval-1", 1L, Instant.now());
        evaluacion.finalizar(0, 0, com.triaje.dominio.SeveridadDepresion.MINIMA,
                com.triaje.dominio.SeveridadAnsiedad.MINIMA, Instant.now());
        when(repositorioEvaluaciones.buscarPorId("eval-1")).thenReturn(Optional.of(evaluacion));

        ComandoRegistrarRespuesta comando = new ComandoRegistrarRespuesta(
                "eval-1", 1L, InstrumentoCribado.PHQ9, 1, 0
        );

        assertThrows(ExcepcionDominio.class, () -> casoUso.ejecutar(comando));
    }

    @Test
    @DisplayName("Debe mantener el pestillo (latch) de riesgo_crisis si el usuario envia valor 1 y luego 0")
    void deberiaMantenerPestilloDeCrisisSiUsuarioEnviaValor1YPosteriormente0() {
        Evaluacion evaluacion = Evaluacion.nueva("eval-1", 1L, Instant.now());
        when(repositorioEvaluaciones.buscarPorId("eval-1")).thenReturn(Optional.of(evaluacion));
        when(repositorioRespuestas.buscarPorEvaluacionEInstrumentoEIndice("eval-1", InstrumentoCribado.PHQ9, 9))
                .thenReturn(Optional.empty());

        // 1. Envia valor 1 -> se activa crisis
        ComandoRegistrarRespuesta comandoConCrisis = new ComandoRegistrarRespuesta(
                "eval-1", 1L, InstrumentoCribado.PHQ9, 9, 1
        );
        ResultadoRegistroRespuesta res1 = casoUso.ejecutar(comandoConCrisis);
        assertTrue(res1.riesgoCrisis());
        assertTrue(evaluacion.isRiesgoCrisis());

        // 2. Retrocede y envia valor 0 -> se debe mantener el pestillo en true
        ComandoRegistrarRespuesta comandoReversion = new ComandoRegistrarRespuesta(
                "eval-1", 1L, InstrumentoCribado.PHQ9, 9, 0
        );
        ResultadoRegistroRespuesta res2 = casoUso.ejecutar(comandoReversion);
        assertTrue(res2.riesgoCrisis(), "El riesgo de crisis debe mantenerse activo por criterio de seguridad clínica");
        assertTrue(evaluacion.isRiesgoCrisis());
    }
}
