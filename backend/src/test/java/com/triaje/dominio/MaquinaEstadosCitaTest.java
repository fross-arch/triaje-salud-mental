package com.triaje.dominio;

import com.triaje.dominio.excepcion.ExcepcionDominio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MaquinaEstadosCitaTest {

    private MaquinaEstadosCita maquina;

    @BeforeEach
    void setUp() {
        maquina = new MaquinaEstadosCita();
    }

    @Test
    @DisplayName("Debe permitir que un clinico proponga una nueva cita")
    void debePermitirProponerCitaPorClinico() {
        EstadoCita resultado = maquina.proponer(RolUsuario.CLINICO);
        assertEquals(EstadoCita.PROPUESTA, resultado);

        EstadoCita transicionDirecta = maquina.transicionar(null, EstadoCita.PROPUESTA, RolUsuario.CLINICO);
        assertEquals(EstadoCita.PROPUESTA, transicionDirecta);
    }

    @Test
    @DisplayName("Debe rechazar que un paciente o administrador proponga una nueva cita")
    void debeRechazarPropuestaCitaPorNoClinico() {
        assertThrows(ExcepcionDominio.class, () -> maquina.proponer(RolUsuario.PACIENTE));
        assertThrows(ExcepcionDominio.class, () -> maquina.proponer(RolUsuario.ADMINISTRADOR));
        assertThrows(ExcepcionDominio.class, () -> maquina.proponer(null));
        assertThrows(ExcepcionDominio.class, () -> maquina.transicionar(null, EstadoCita.CONFIRMADA, RolUsuario.CLINICO));
    }

    @Test
    @DisplayName("Debe permitir que el paciente confirme una cita en estado PROPUESTA")
    void debePermitirConfirmarCitaPropuestaPorPaciente() {
        EstadoCita resultado = maquina.confirmar(EstadoCita.PROPUESTA, RolUsuario.PACIENTE);
        assertEquals(EstadoCita.CONFIRMADA, resultado);

        EstadoCita transicionDirecta = maquina.transicionar(EstadoCita.PROPUESTA, EstadoCita.CONFIRMADA, RolUsuario.PACIENTE);
        assertEquals(EstadoCita.CONFIRMADA, transicionDirecta);
    }

    @Test
    @DisplayName("Debe rechazar que un clinico o administrador confirme una cita propuesta")
    void debeRechazarConfirmarCitaPorNoPaciente() {
        assertThrows(ExcepcionDominio.class, () -> maquina.confirmar(EstadoCita.PROPUESTA, RolUsuario.CLINICO));
        assertThrows(ExcepcionDominio.class, () -> maquina.confirmar(EstadoCita.PROPUESTA, RolUsuario.ADMINISTRADOR));
    }

    @Test
    @DisplayName("Debe permitir que el paciente rechace una cita en estado PROPUESTA")
    void debePermitirRechazarCitaPropuestaPorPaciente() {
        EstadoCita resultado = maquina.rechazar(EstadoCita.PROPUESTA, RolUsuario.PACIENTE);
        assertEquals(EstadoCita.RECHAZADA, resultado);

        EstadoCita transicionDirecta = maquina.transicionar(EstadoCita.PROPUESTA, EstadoCita.RECHAZADA, RolUsuario.PACIENTE);
        assertEquals(EstadoCita.RECHAZADA, transicionDirecta);
    }

    @Test
    @DisplayName("Debe rechazar que un clinico o administrador rechace una cita propuesta")
    void debeRechazarRechazarCitaPorNoPaciente() {
        assertThrows(ExcepcionDominio.class, () -> maquina.rechazar(EstadoCita.PROPUESTA, RolUsuario.CLINICO));
        assertThrows(ExcepcionDominio.class, () -> maquina.rechazar(EstadoCita.PROPUESTA, RolUsuario.ADMINISTRADOR));
    }

    @Test
    @DisplayName("Debe permitir que el paciente, el clinico o el sistema cancelen una cita PROPUESTA")
    void debePermitirCancelarCitaPropuestaPorAmbos() {
        assertEquals(EstadoCita.CANCELADA, maquina.cancelar(EstadoCita.PROPUESTA, RolUsuario.PACIENTE));
        assertEquals(EstadoCita.CANCELADA, maquina.cancelar(EstadoCita.PROPUESTA, RolUsuario.CLINICO));
        assertEquals(EstadoCita.CANCELADA, maquina.cancelar(EstadoCita.PROPUESTA, null));
    }

    @Test
    @DisplayName("Debe permitir que el paciente, el clinico o el sistema cancelen una cita CONFIRMADA")
    void debePermitirCancelarCitaConfirmadaPorAmbos() {
        assertEquals(EstadoCita.CANCELADA, maquina.cancelar(EstadoCita.CONFIRMADA, RolUsuario.PACIENTE));
        assertEquals(EstadoCita.CANCELADA, maquina.cancelar(EstadoCita.CONFIRMADA, RolUsuario.CLINICO));
        assertEquals(EstadoCita.CANCELADA, maquina.cancelar(EstadoCita.CONFIRMADA, null));
    }

    @ParameterizedTest
    @EnumSource(value = EstadoCita.class)
    @DisplayName("No debe permitir ninguna transicion saliente desde el estado RECHAZADA")
    void noDebePermitirTransicionesDesdeRechazada(EstadoCita nuevoEstado) {
        assertThrows(ExcepcionDominio.class, () ->
                maquina.transicionar(EstadoCita.RECHAZADA, nuevoEstado, RolUsuario.PACIENTE));
        assertThrows(ExcepcionDominio.class, () ->
                maquina.transicionar(EstadoCita.RECHAZADA, nuevoEstado, RolUsuario.CLINICO));
    }

    @ParameterizedTest
    @EnumSource(value = EstadoCita.class)
    @DisplayName("No debe permitir ninguna transicion saliente desde el estado CANCELADA")
    void noDebePermitirTransicionesDesdeCancelada(EstadoCita nuevoEstado) {
        assertThrows(ExcepcionDominio.class, () ->
                maquina.transicionar(EstadoCita.CANCELADA, nuevoEstado, RolUsuario.PACIENTE));
        assertThrows(ExcepcionDominio.class, () ->
                maquina.transicionar(EstadoCita.CANCELADA, nuevoEstado, RolUsuario.CLINICO));
    }

    @Test
    @DisplayName("No debe permitir pasar de CONFIRMADA a CONFIRMADA, RECHAZADA o PROPUESTA")
    void noDebePermitirTransicionesInvalidasDesdeConfirmada() {
        assertThrows(ExcepcionDominio.class, () ->
                maquina.transicionar(EstadoCita.CONFIRMADA, EstadoCita.CONFIRMADA, RolUsuario.PACIENTE));
        assertThrows(ExcepcionDominio.class, () ->
                maquina.transicionar(EstadoCita.CONFIRMADA, EstadoCita.RECHAZADA, RolUsuario.PACIENTE));
        assertThrows(ExcepcionDominio.class, () ->
                maquina.transicionar(EstadoCita.CONFIRMADA, EstadoCita.PROPUESTA, RolUsuario.CLINICO));
    }

    @Test
    @DisplayName("No debe permitir pasar de PROPUESTA a PROPUESTA")
    void noDebePermitirPropuestaAPropuesta() {
        assertThrows(ExcepcionDominio.class, () ->
                maquina.transicionar(EstadoCita.PROPUESTA, EstadoCita.PROPUESTA, RolUsuario.PACIENTE));
        assertThrows(ExcepcionDominio.class, () ->
                maquina.transicionar(EstadoCita.PROPUESTA, EstadoCita.PROPUESTA, RolUsuario.CLINICO));
    }
}
