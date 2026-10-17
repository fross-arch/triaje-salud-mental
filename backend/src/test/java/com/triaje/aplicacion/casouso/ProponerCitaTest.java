package com.triaje.aplicacion.casouso;

import com.triaje.aplicacion.dto.ComandoProponerCita;
import com.triaje.aplicacion.dto.DatosCitaClinico;
import com.triaje.aplicacion.puerto.EjecutorTransaccional;
import com.triaje.aplicacion.puerto.RepositorioAuditoria;
import com.triaje.aplicacion.puerto.RepositorioAutorizaciones;
import com.triaje.aplicacion.puerto.RepositorioCitas;
import com.triaje.aplicacion.puerto.RepositorioUsuarios;
import com.triaje.dominio.Autorizacion;
import com.triaje.dominio.Cita;
import com.triaje.dominio.EstadoCita;
import com.triaje.dominio.MaquinaEstadosCita;
import com.triaje.dominio.ModalidadCita;
import com.triaje.dominio.excepcion.ExcepcionDatoInvalido;
import com.triaje.dominio.excepcion.ExcepcionDominio;
import com.triaje.dominio.excepcion.ExcepcionEvaluacionNoEncontrada;
import com.triaje.dominio.modelo.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProponerCitaTest {

    private RepositorioCitas repositorioCitas;
    private RepositorioAutorizaciones repositorioAutorizaciones;
    private RepositorioUsuarios repositorioUsuarios;
    private RepositorioAuditoria repositorioAuditoria;
    private MaquinaEstadosCita maquinaEstadosCita;
    private Clock reloj;
    private ProponerCita casoUso;

    private final Instant AHORA = Instant.parse("2026-10-20T10:00:00Z");

    @BeforeEach
    void setUp() {
        repositorioCitas = mock(RepositorioCitas.class);
        repositorioAutorizaciones = mock(RepositorioAutorizaciones.class);
        repositorioUsuarios = mock(RepositorioUsuarios.class);
        repositorioAuditoria = mock(RepositorioAuditoria.class);
        maquinaEstadosCita = new MaquinaEstadosCita();
        EjecutorTransaccional transaccional = new EjecutorTransaccional() {
            @Override
            public <T> T ejecutar(Supplier<T> operacion) {
                return operacion.get();
            }
        };
        reloj = Clock.fixed(AHORA, ZoneOffset.UTC);
        casoUso = new ProponerCita(repositorioCitas, repositorioAutorizaciones, repositorioUsuarios,
                repositorioAuditoria, maquinaEstadosCita, transaccional, reloj);
    }

    @Test
    @DisplayName("Debe fallar si la fechaHora es pasada o igual a la actual")
    void debeFallarSiFechaHoraEsPasada() {
        ComandoProponerCita comandoPasado = new ComandoProponerCita(
                "ev-1", 10L, AHORA.minus(1, ChronoUnit.HOURS), ModalidadCita.PRESENCIAL, "Mensaje"
        );
        assertThrows(ExcepcionDatoInvalido.class, () -> casoUso.ejecutar(comandoPasado));

        ComandoProponerCita comandoIgual = new ComandoProponerCita(
                "ev-1", 10L, AHORA, ModalidadCita.PRESENCIAL, "Mensaje"
        );
        assertThrows(ExcepcionDatoInvalido.class, () -> casoUso.ejecutar(comandoIgual));
    }

    @Test
    @DisplayName("Debe fallar con 404 si no existe autorizacion vigente")
    void debeFallarSiNoExisteAutorizacionVigente() {
        Instant fechaFutura = AHORA.plus(2, ChronoUnit.DAYS);
        ComandoProponerCita comando = new ComandoProponerCita(
                "ev-1", 10L, fechaFutura, ModalidadCita.PRESENCIAL, "Mensaje"
        );

        when(repositorioAutorizaciones.buscarVigentePorEvaluacionYClinico("ev-1", 10L))
                .thenReturn(Optional.empty());

        assertThrows(ExcepcionEvaluacionNoEncontrada.class, () -> casoUso.ejecutar(comando));
    }

    @Test
    @DisplayName("Debe fallar con 409 Conflict si el clinico ya tiene cita en la misma fecha y hora exacta")
    void debeFallarSiExisteSolapamiento() {
        Instant fechaFutura = AHORA.plus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        ComandoProponerCita comando = new ComandoProponerCita(
                "ev-1", 10L, fechaFutura, ModalidadCita.PRESENCIAL, "Mensaje"
        );

        Autorizacion aut = Autorizacion.nueva("ev-1", 20L, 10L, "1.0", AHORA);
        when(repositorioAutorizaciones.buscarVigentePorEvaluacionYClinico("ev-1", 10L))
                .thenReturn(Optional.of(aut));

        when(repositorioCitas.existePorClinicoYFechaHoraYEstados(eq(10L), eq(fechaFutura), any()))
                .thenReturn(true);

        assertThrows(ExcepcionDominio.class, () -> casoUso.ejecutar(comando));
    }

    @Test
    @DisplayName("Debe proponer cita exitosamente y registrar auditoria")
    void debeProponerCitaExitosamente() {
        Instant fechaFutura = AHORA.plus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        ComandoProponerCita comando = new ComandoProponerCita(
                "ev-1", 10L, fechaFutura, ModalidadCita.VIRTUAL, "Cita de valoracion"
        );

        Autorizacion aut = Autorizacion.nueva("ev-1", 20L, 10L, "1.0", AHORA);
        when(repositorioAutorizaciones.buscarVigentePorEvaluacionYClinico("ev-1", 10L))
                .thenReturn(Optional.of(aut));

        when(repositorioCitas.existePorClinicoYFechaHoraYEstados(eq(10L), eq(fechaFutura), any()))
                .thenReturn(false);

        when(repositorioCitas.guardar(any(Cita.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario paciente = new Usuario(20L, "Ana Paciente", "ana@ejemplo.com", "hash", com.triaje.dominio.RolUsuario.PACIENTE, true, AHORA);
        when(repositorioUsuarios.buscarPorId(20L)).thenReturn(Optional.of(paciente));

        DatosCitaClinico resultado = casoUso.ejecutar(comando);

        assertNotNull(resultado);
        assertEquals(EstadoCita.PROPUESTA, resultado.estado());
        assertEquals(ModalidadCita.VIRTUAL, resultado.modalidad());
        assertEquals("Ana Paciente", resultado.nombrePaciente());
        verify(repositorioAuditoria).registrar(any());
    }
}
