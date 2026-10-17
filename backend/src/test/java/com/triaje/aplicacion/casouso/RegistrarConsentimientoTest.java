package com.triaje.aplicacion.casouso;

import com.triaje.aplicacion.dto.ComandoConsentimiento;
import com.triaje.aplicacion.dto.DatosConsentimiento;
import com.triaje.aplicacion.puerto.EjecutorTransaccional;
import com.triaje.aplicacion.puerto.RepositorioAuditoria;
import com.triaje.aplicacion.puerto.RepositorioConsentimientos;
import com.triaje.dominio.excepcion.ExcepcionDatoInvalido;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.Supplier;

class RegistrarConsentimientoTest {

    private RepositorioConsentimientos repositorioConsentimientos;
    private RepositorioAuditoria repositorioAuditoria;
    private Clock reloj;
    private RegistrarConsentimiento casoUso;

    @BeforeEach
    void inicializar() {
        repositorioConsentimientos = mock(RepositorioConsentimientos.class);
        repositorioAuditoria = mock(RepositorioAuditoria.class);
        EjecutorTransaccional transaccional = new EjecutorTransaccional() {
            @Override
            public <T> T ejecutar(Supplier<T> operacion) {
                return operacion.get();
            }
        };
        reloj = Clock.fixed(Instant.parse("2026-10-13T10:00:00Z"), ZoneOffset.UTC);
        casoUso = new RegistrarConsentimiento(repositorioConsentimientos, repositorioAuditoria, transaccional, reloj);
    }

    @Test
    @DisplayName("Debe registrar consentimiento exitosamente y registrar auditoria")
    void deberiaRegistrarConsentimientoYAuditoria() {
        ComandoConsentimiento comando = new ComandoConsentimiento(1L, true);

        when(repositorioConsentimientos.buscarVigentePorUsuarioId(1L)).thenReturn(Optional.empty());
        when(repositorioConsentimientos.guardar(any(Consentimiento.class))).thenAnswer(inv -> {
            Consentimiento c = inv.getArgument(0);
            return new Consentimiento(10L, c.getUsuarioId(), c.getVersionConsentimiento(),
                    c.getVersionPolitica(), c.isConfirmaMayoriaEdad(), c.getFechaAceptacion(), c.getFechaRevocacion());
        });

        DatosConsentimiento resultado = casoUso.ejecutar(comando);

        assertNotNull(resultado);
        assertEquals(10L, resultado.id());
        assertTrue(resultado.vigente());
        assertEquals("1.0", resultado.versionConsentimiento());

        ArgumentCaptor<RegistroAuditoria> captorAuditoria = ArgumentCaptor.forClass(RegistroAuditoria.class);
        verify(repositorioAuditoria).registrar(captorAuditoria.capture());
        assertEquals(AccionAuditoria.CONSENTIMIENTO_ACEPTADO, captorAuditoria.getValue().accion());
        assertEquals("10", captorAuditoria.getValue().entidadId());
    }

    @Test
    @DisplayName("Debe lanzar excepcion si no se confirma mayoria de edad")
    void deberiaLanzarExcepcionSinMayoriaDeEdad() {
        ComandoConsentimiento comando = new ComandoConsentimiento(1L, false);
        assertThrows(ExcepcionDatoInvalido.class, () -> casoUso.ejecutar(comando));
    }
}
