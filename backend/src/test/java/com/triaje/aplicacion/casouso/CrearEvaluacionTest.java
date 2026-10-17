package com.triaje.aplicacion.casouso;

import com.triaje.aplicacion.dto.DatosEvaluacion;
import com.triaje.aplicacion.puerto.EjecutorTransaccional;
import com.triaje.aplicacion.puerto.RepositorioAutorizaciones;
import com.triaje.aplicacion.puerto.RepositorioCitas;
import com.triaje.aplicacion.puerto.RepositorioConsentimientos;
import com.triaje.aplicacion.puerto.RepositorioEvaluaciones;
import com.triaje.dominio.excepcion.ExcepcionDominio;
import com.triaje.dominio.modelo.Consentimiento;
import com.triaje.dominio.modelo.EstadoEvaluacion;
import com.triaje.dominio.modelo.Evaluacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.function.Supplier;

class CrearEvaluacionTest {

    private RepositorioEvaluaciones repositorioEvaluaciones;
    private RepositorioConsentimientos repositorioConsentimientos;
    private RepositorioCitas repositorioCitas;
    private RepositorioAutorizaciones repositorioAutorizaciones;
    private Clock reloj;
    private CrearEvaluacion casoUso;

    @BeforeEach
    void inicializar() {
        repositorioEvaluaciones = mock(RepositorioEvaluaciones.class);
        repositorioConsentimientos = mock(RepositorioConsentimientos.class);
        repositorioCitas = mock(RepositorioCitas.class);
        repositorioAutorizaciones = mock(RepositorioAutorizaciones.class);
        EjecutorTransaccional transaccional = new EjecutorTransaccional() {
            @Override
            public <T> T ejecutar(Supplier<T> operacion) {
                return operacion.get();
            }
        };
        reloj = Clock.fixed(Instant.parse("2026-10-13T10:00:00Z"), ZoneOffset.UTC);
        casoUso = new CrearEvaluacion(repositorioEvaluaciones, repositorioConsentimientos,
                repositorioCitas, repositorioAutorizaciones, transaccional, reloj);

        when(repositorioCitas.existePorPacienteYEstados(anyLong(), anyCollection())).thenReturn(false);
        when(repositorioAutorizaciones.listarVigentesPorPacienteId(anyLong())).thenReturn(List.of());
    }

    @Test
    @DisplayName("Debe lanzar ExcepcionDominio si el usuario no tiene consentimiento vigente")
    void deberiaRechazarSinConsentimientoVigente() {
        when(repositorioConsentimientos.buscarVigentePorUsuarioId(1L)).thenReturn(Optional.empty());

        assertThrows(ExcepcionDominio.class, () -> casoUso.ejecutar(1L));
    }

    @Test
    @DisplayName("Debe crear evaluacion en estado EN_CURSO si tiene consentimiento vigente")
    void deberiaCrearEvaluacionConConsentimientoVigente() {
        Consentimiento consentimiento = Consentimiento.nuevo(1L, Instant.now(reloj));
        when(repositorioConsentimientos.buscarVigentePorUsuarioId(1L)).thenReturn(Optional.of(consentimiento));
        when(repositorioEvaluaciones.guardar(any(Evaluacion.class))).thenAnswer(inv -> inv.getArgument(0));

        DatosEvaluacion resultado = casoUso.ejecutar(1L);

        assertNotNull(resultado);
        assertEquals(EstadoEvaluacion.EN_CURSO, resultado.estado());
        assertFalse(resultado.riesgoCrisis());
    }
}
