package com.triaje.aplicacion.casouso;

import com.triaje.aplicacion.puerto.EjecutorTransaccional;
import com.triaje.aplicacion.puerto.RepositorioAuditoria;
import com.triaje.aplicacion.puerto.RepositorioAutorizaciones;
import com.triaje.aplicacion.puerto.RepositorioConsentimientos;
import com.triaje.dominio.Autorizacion;
import com.triaje.dominio.modelo.AccionAuditoria;
import com.triaje.dominio.modelo.Consentimiento;
import com.triaje.dominio.modelo.RegistroAuditoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.triaje.aplicacion.puerto.RepositorioCitas;
import com.triaje.aplicacion.puerto.RepositorioEvaluaciones;
import com.triaje.dominio.MaquinaEstadosCita;
import com.triaje.dominio.EstadoCita;

class RevocarConsentimientoTest {

    private RepositorioConsentimientos repositorioConsentimientos;
    private RepositorioAutorizaciones repositorioAutorizaciones;
    private RepositorioCitas repositorioCitas;
    private RepositorioEvaluaciones repositorioEvaluaciones;
    private RepositorioAuditoria repositorioAuditoria;
    private MaquinaEstadosCita maquinaEstadosCita;
    private Clock reloj;
    private RevocarConsentimiento casoUso;

    @BeforeEach
    void inicializar() {
        repositorioConsentimientos = mock(RepositorioConsentimientos.class);
        repositorioAutorizaciones = mock(RepositorioAutorizaciones.class);
        repositorioCitas = mock(RepositorioCitas.class);
        repositorioEvaluaciones = mock(RepositorioEvaluaciones.class);
        repositorioAuditoria = mock(RepositorioAuditoria.class);
        maquinaEstadosCita = new MaquinaEstadosCita();
        EjecutorTransaccional transaccional = new EjecutorTransaccional() {
            @Override
            public <T> T ejecutar(Supplier<T> operacion) {
                return operacion.get();
            }
        };
        reloj = Clock.fixed(Instant.parse("2026-10-13T10:00:00Z"), ZoneOffset.UTC);
        casoUso = new RevocarConsentimiento(repositorioConsentimientos, repositorioAutorizaciones,
                repositorioCitas, repositorioEvaluaciones, repositorioAuditoria, maquinaEstadosCita, transaccional, reloj);
    }

    @Test
    @DisplayName("Debe revocar consentimiento activo, revocar autorizaciones en cascada y registrar auditoria")
    void deberiaRevocarConsentimientoActivo() {
        Consentimiento consentimiento = new Consentimiento(
                5L, 1L, "1.0", "1.0", true, Instant.now(reloj), null
        );
        when(repositorioConsentimientos.buscarVigentePorUsuarioId(1L)).thenReturn(Optional.of(consentimiento));

        Autorizacion aut = Autorizacion.nueva("ev-1", 1L, 2L, "1.0", Instant.now(reloj));
        when(repositorioAutorizaciones.listarVigentesPorPacienteId(1L)).thenReturn(List.of(aut));

        boolean resultado = casoUso.ejecutar(1L);

        assertTrue(resultado);
        assertFalse(consentimiento.estaVigente());
        verify(repositorioConsentimientos).guardar(consentimiento);
        verify(repositorioAutorizaciones).revocarVigentesPorPacienteId(1L, Instant.now(reloj));

        ArgumentCaptor<RegistroAuditoria> captor = ArgumentCaptor.forClass(RegistroAuditoria.class);
        verify(repositorioAuditoria, org.mockito.Mockito.times(2)).registrar(captor.capture());
        List<RegistroAuditoria> eventos = captor.getAllValues();

        assertEquals(AccionAuditoria.CONSENTIMIENTO_REVOCADO, eventos.get(0).accion());
        assertEquals("5", eventos.get(0).entidadId());

        assertEquals(AccionAuditoria.AUTORIZACION_REVOCADA, eventos.get(1).accion());
        assertEquals("ev-1", eventos.get(1).entidadId());
    }

    @Test
    @DisplayName("Debe ser idempotente: si ya esta revocado no lanza error ni registra auditoria duplicada")
    void deberiaSerIdempotenteCuandoYaEstaRevocado() {
        when(repositorioConsentimientos.buscarVigentePorUsuarioId(1L)).thenReturn(Optional.empty());

        boolean resultado = casoUso.ejecutar(1L);

        assertFalse(resultado);
        verify(repositorioConsentimientos, never()).guardar(org.mockito.ArgumentMatchers.any(Consentimiento.class));
        verify(repositorioAutorizaciones, never()).revocarVigentesPorPacienteId(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any());
        verify(repositorioAuditoria, never()).registrar(org.mockito.ArgumentMatchers.any(RegistroAuditoria.class));
    }
}
