package com.triaje.aplicacion.casouso;

import com.triaje.aplicacion.dto.ComandoActualizarPaciente;
import com.triaje.aplicacion.dto.DatosPacienteAdministracion;
import com.triaje.aplicacion.puerto.CodificadorContrasenas;
import com.triaje.aplicacion.puerto.EjecutorTransaccional;
import com.triaje.aplicacion.puerto.RepositorioAuditoria;
import com.triaje.aplicacion.puerto.RepositorioUsuarios;
import com.triaje.dominio.RolUsuario;
import com.triaje.dominio.excepcion.ExcepcionCorreoYaRegistrado;
import com.triaje.dominio.excepcion.ExcepcionDatoInvalido;
import com.triaje.dominio.excepcion.ExcepcionUsuarioNoEncontrado;
import com.triaje.dominio.modelo.AccionAuditoria;
import com.triaje.dominio.modelo.RegistroAuditoria;
import com.triaje.dominio.modelo.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ActualizarPacienteTest {

    private RepositorioUsuarios repositorioUsuarios;
    private RepositorioAuditoria repositorioAuditoria;
    private CodificadorContrasenas codificador;
    private Clock reloj;
    private ActualizarPaciente casoUso;

    private static final Instant AHORA = Instant.parse("2026-10-16T12:00:00Z");

    @BeforeEach
    void inicializar() {
        repositorioUsuarios = mock(RepositorioUsuarios.class);
        repositorioAuditoria = mock(RepositorioAuditoria.class);
        codificador = mock(CodificadorContrasenas.class);
        reloj = Clock.fixed(AHORA, ZoneOffset.UTC);
        EjecutorTransaccional transaccional = new EjecutorTransaccional() {
            @Override
            public <T> T ejecutar(Supplier<T> operacion) {
                return operacion.get();
            }
        };

        casoUso = new ActualizarPaciente(repositorioUsuarios, repositorioAuditoria, codificador, transaccional, reloj);
    }

    @Test
    @DisplayName("Actualiza exitosamente el paciente y genera registro de auditoría PACIENTE_ACTUALIZADO")
    void debeActualizarPacienteExitosamente() {
        Usuario pacienteExistente = new Usuario(
                20L,
                "María Gómez Antiguo",
                "maria@ejemplo.com",
                "$2a$10$hashOriginal123456789012345678901234567890123456789012",
                RolUsuario.PACIENTE,
                true,
                AHORA.minusSeconds(7200),
                null
        );

        when(repositorioUsuarios.buscarPorId(20L)).thenReturn(Optional.of(pacienteExistente));
        when(repositorioUsuarios.guardar(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ComandoActualizarPaciente comando = new ComandoActualizarPaciente(
                1L,
                20L,
                "María Gómez Actualizada",
                "maria.nueva@ejemplo.com",
                null
        );

        DatosPacienteAdministracion resultado = casoUso.ejecutar(comando);

        assertNotNull(resultado);
        assertEquals(20L, resultado.id());
        assertEquals("María Gómez Actualizada", resultado.nombre());
        assertEquals("maria.nueva@ejemplo.com", resultado.correo());

        ArgumentCaptor<Usuario> captorUsuario = ArgumentCaptor.forClass(Usuario.class);
        verify(repositorioUsuarios).guardar(captorUsuario.capture());
        assertEquals("$2a$10$hashOriginal123456789012345678901234567890123456789012", captorUsuario.getValue().getContrasenaHash());

        ArgumentCaptor<RegistroAuditoria> captorAuditoria = ArgumentCaptor.forClass(RegistroAuditoria.class);
        verify(repositorioAuditoria).registrar(captorAuditoria.capture());
        assertEquals(1L, captorAuditoria.getValue().usuarioId());
        assertEquals(AccionAuditoria.PACIENTE_ACTUALIZADO, captorAuditoria.getValue().accion());
        assertEquals("20", captorAuditoria.getValue().entidadId());
    }

    @Test
    @DisplayName("Actualiza la contraseña del paciente con BCrypt si se suministra una nueva contraseña válida")
    void debeRestablecerContrasenaPacienteSiEsValida() {
        Usuario pacienteExistente = new Usuario(
                20L,
                "Carlos Paciente",
                "carlos@ejemplo.com",
                "$2a$10$hashOriginal123456789012345678901234567890123456789012",
                RolUsuario.PACIENTE,
                true,
                AHORA.minusSeconds(7200),
                null
        );

        when(repositorioUsuarios.buscarPorId(20L)).thenReturn(Optional.of(pacienteExistente));
        when(codificador.codificar("PacienteSeguro2026!")).thenReturn("$2a$10$nuevoHashBCrypt123456789012345678901234567890123456789012");
        when(repositorioUsuarios.guardar(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ComandoActualizarPaciente comando = new ComandoActualizarPaciente(
                1L,
                20L,
                "Carlos Paciente",
                "carlos@ejemplo.com",
                "PacienteSeguro2026!"
        );

        DatosPacienteAdministracion resultado = casoUso.ejecutar(comando);

        assertNotNull(resultado);
        verify(codificador).codificar("PacienteSeguro2026!");
        ArgumentCaptor<Usuario> captorUsuario = ArgumentCaptor.forClass(Usuario.class);
        verify(repositorioUsuarios).guardar(captorUsuario.capture());
        assertEquals("$2a$10$nuevoHashBCrypt123456789012345678901234567890123456789012", captorUsuario.getValue().getContrasenaHash());
    }

    @Test
    @DisplayName("Lanza ExcepcionDatoInvalido si la nueva contraseña tiene menos de 10 caracteres")
    void debeFallarSiContrasenaTieneMenosDe10Caracteres() {
        Usuario pacienteExistente = new Usuario(
                20L,
                "Carlos Paciente",
                "carlos@ejemplo.com",
                "$2a$10$hashOriginal123456789012345678901234567890123456789012",
                RolUsuario.PACIENTE,
                true,
                AHORA.minusSeconds(7200),
                null
        );

        when(repositorioUsuarios.buscarPorId(20L)).thenReturn(Optional.of(pacienteExistente));

        ComandoActualizarPaciente comando = new ComandoActualizarPaciente(
                1L,
                20L,
                "Carlos Paciente",
                "carlos@ejemplo.com",
                "Corta123"
        );

        ExcepcionDatoInvalido ex = assertThrows(ExcepcionDatoInvalido.class, () -> casoUso.ejecutar(comando));
        assertEquals("La contraseña debe tener al menos 10 caracteres.", ex.getMessage());
        verify(repositorioUsuarios, never()).guardar(any());
    }

    @Test
    @DisplayName("Lanza ExcepcionUsuarioNoEncontrado si el paciente no existe")
    void debeFallarSiPacienteNoExiste() {
        when(repositorioUsuarios.buscarPorId(99L)).thenReturn(Optional.empty());

        ComandoActualizarPaciente comando = new ComandoActualizarPaciente(
                1L,
                99L,
                "Paciente Fantasma",
                "fantasma@ejemplo.com",
                null
        );

        ExcepcionUsuarioNoEncontrado ex = assertThrows(ExcepcionUsuarioNoEncontrado.class,
                () -> casoUso.ejecutar(comando));
        assertEquals("El paciente no existe.", ex.getMessage());
        verify(repositorioUsuarios, never()).guardar(any());
    }

    @Test
    @DisplayName("Lanza ExcepcionUsuarioNoEncontrado si el usuario encontrado no tiene rol PACIENTE")
    void debeFallarSiUsuarioNoEsPaciente() {
        Usuario clinico = new Usuario(
                15L,
                "Dr. Roberto",
                "roberto@salud.org",
                "$2a$10$hashValido123456789012345678901234567890123456789012",
                RolUsuario.CLINICO,
                true,
                AHORA.minusSeconds(3600),
                "Psicología"
        );

        when(repositorioUsuarios.buscarPorId(15L)).thenReturn(Optional.of(clinico));

        ComandoActualizarPaciente comando = new ComandoActualizarPaciente(
                1L,
                15L,
                "Dr. Roberto Mod",
                "roberto.mod@salud.org",
                null
        );

        ExcepcionUsuarioNoEncontrado ex = assertThrows(ExcepcionUsuarioNoEncontrado.class,
                () -> casoUso.ejecutar(comando));
        assertEquals("El usuario especificado no es un paciente.", ex.getMessage());
        verify(repositorioUsuarios, never()).guardar(any());
    }

    @Test
    @DisplayName("Lanza ExcepcionCorreoYaRegistrado si el nuevo correo ya pertenece a otro usuario")
    void debeFallarSiCorreoYaEstaEnUso() {
        Usuario paciente = new Usuario(
                20L,
                "Carlos",
                "carlos@ejemplo.com",
                "$2a$10$hashOriginal123456789012345678901234567890123456789012",
                RolUsuario.PACIENTE,
                true,
                AHORA.minusSeconds(3600),
                null
        );

        when(repositorioUsuarios.buscarPorId(20L)).thenReturn(Optional.of(paciente));
        when(repositorioUsuarios.existePorCorreo("ocupado@ejemplo.com")).thenReturn(true);

        ComandoActualizarPaciente comando = new ComandoActualizarPaciente(
                1L,
                20L,
                "Carlos",
                "ocupado@ejemplo.com",
                null
        );

        assertThrows(ExcepcionCorreoYaRegistrado.class, () -> casoUso.ejecutar(comando));
        verify(repositorioUsuarios, never()).guardar(any());
    }
}
