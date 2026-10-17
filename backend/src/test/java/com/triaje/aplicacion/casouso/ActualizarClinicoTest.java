package com.triaje.aplicacion.casouso;

import com.triaje.aplicacion.dto.ComandoActualizarClinico;
import com.triaje.aplicacion.dto.DatosUsuario;
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

class ActualizarClinicoTest {

    private RepositorioUsuarios repositorioUsuarios;
    private RepositorioAuditoria repositorioAuditoria;
    private CodificadorContrasenas codificador;
    private Clock reloj;
    private ActualizarClinico casoUso;

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

        casoUso = new ActualizarClinico(repositorioUsuarios, repositorioAuditoria, codificador, transaccional, reloj);
    }

    @Test
    @DisplayName("Actualiza exitosamente el clínico y genera registro de auditoría")
    void debeActualizarClinicoExitosamente() {
        Usuario clinicoExistente = new Usuario(
                10L,
                "Dr. Roberto Antiguo",
                "roberto@salud.org",
                "$2a$10$hashValido123456789012345678901234567890123456789012",
                RolUsuario.CLINICO,
                true,
                AHORA.minusSeconds(3600),
                "Psicología Clínica"
        );

        when(repositorioUsuarios.buscarPorId(10L)).thenReturn(Optional.of(clinicoExistente));
        when(repositorioUsuarios.guardar(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ComandoActualizarClinico comando = new ComandoActualizarClinico(
                1L,
                10L,
                "Dr. Roberto Modificado",
                "roberto.nuevo@salud.org",
                "Neuropsicología"
        );

        DatosUsuario resultado = casoUso.ejecutar(comando);

        assertNotNull(resultado);
        assertEquals("Dr. Roberto Modificado", resultado.nombreCompleto());
        assertEquals("roberto.nuevo@salud.org", resultado.correo());
        assertEquals("Neuropsicología", resultado.especialidad());
        assertEquals(RolUsuario.CLINICO, resultado.rol());

        ArgumentCaptor<RegistroAuditoria> captorAuditoria = ArgumentCaptor.forClass(RegistroAuditoria.class);
        verify(repositorioAuditoria).registrar(captorAuditoria.capture());
        assertEquals(1L, captorAuditoria.getValue().usuarioId());
        assertEquals(AccionAuditoria.CLINICO_ACTUALIZADO, captorAuditoria.getValue().accion());
        assertEquals("10", captorAuditoria.getValue().entidadId());
    }

    @Test
    @DisplayName("Lanza ExcepcionUsuarioNoEncontrado si el clínico no existe")
    void debeFallarSiClinicoNoExiste() {
        when(repositorioUsuarios.buscarPorId(99L)).thenReturn(Optional.empty());

        ComandoActualizarClinico comando = new ComandoActualizarClinico(
                1L,
                99L,
                "Dr. Inexistente",
                "inexistente@salud.org",
                "Psicología"
        );

        ExcepcionUsuarioNoEncontrado ex = assertThrows(ExcepcionUsuarioNoEncontrado.class,
                () -> casoUso.ejecutar(comando));
        assertEquals("El clinico no existe.", ex.getMessage());
        verify(repositorioUsuarios, never()).guardar(any());
        verify(repositorioAuditoria, never()).registrar(any());
    }

    @Test
    @DisplayName("Lanza ExcepcionUsuarioNoEncontrado si el usuario encontrado no tiene rol CLINICO")
    void debeFallarSiUsuarioNoEsClinico() {
        Usuario paciente = new Usuario(
                20L,
                "Paciente Juan",
                "juan@paciente.org",
                "$2a$10$hashValido123456789012345678901234567890123456789012",
                RolUsuario.PACIENTE,
                true,
                AHORA.minusSeconds(3600),
                null
        );

        when(repositorioUsuarios.buscarPorId(20L)).thenReturn(Optional.of(paciente));

        ComandoActualizarClinico comando = new ComandoActualizarClinico(
                1L,
                20L,
                "Paciente Modificado",
                "juan.nuevo@paciente.org",
                "Ninguna"
        );

        ExcepcionUsuarioNoEncontrado ex = assertThrows(ExcepcionUsuarioNoEncontrado.class,
                () -> casoUso.ejecutar(comando));
        assertEquals("El usuario especificado no es un clinico.", ex.getMessage());
        verify(repositorioUsuarios, never()).guardar(any());
        verify(repositorioAuditoria, never()).registrar(any());
    }

    @Test
    @DisplayName("Lanza ExcepcionCorreoYaRegistrado si el nuevo correo pertenece a otro usuario")
    void debeFallarSiCorreoYaEstaEnUso() {
        Usuario clinico = new Usuario(
                10L,
                "Dr. Roberto",
                "roberto@salud.org",
                "$2a$10$hashValido123456789012345678901234567890123456789012",
                RolUsuario.CLINICO,
                true,
                AHORA.minusSeconds(3600),
                "Psicología Clínica"
        );

        when(repositorioUsuarios.buscarPorId(10L)).thenReturn(Optional.of(clinico));
        when(repositorioUsuarios.existePorCorreo("ocupado@salud.org")).thenReturn(true);

        ComandoActualizarClinico comando = new ComandoActualizarClinico(
                1L,
                10L,
                "Dr. Roberto",
                "ocupado@salud.org",
                "Psicología Clínica"
        );

        assertThrows(ExcepcionCorreoYaRegistrado.class, () -> casoUso.ejecutar(comando));
        verify(repositorioUsuarios, never()).guardar(any());
    }

    @Test
    @DisplayName("Actualiza la contraseña del clínico codificándola con BCrypt si se proporciona válida")
    void debeActualizarContrasenaSiEsValida() {
        Usuario clinico = new Usuario(
                10L,
                "Dr. Roberto",
                "roberto@salud.org",
                "$2a$10$hashAnterior123456789012345678901234567890123456789012",
                RolUsuario.CLINICO,
                true,
                AHORA.minusSeconds(3600),
                "Psicología Clínica"
        );

        when(repositorioUsuarios.buscarPorId(10L)).thenReturn(Optional.of(clinico));
        when(codificador.codificar("NuevaClave2026!")).thenReturn("$2a$10$nuevoHash123456789012345678901234567890123456789012");
        when(repositorioUsuarios.guardar(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ComandoActualizarClinico comando = new ComandoActualizarClinico(
                1L,
                10L,
                "Dr. Roberto",
                "roberto@salud.org",
                "Psicología Clínica",
                "NuevaClave2026!"
        );

        DatosUsuario resultado = casoUso.ejecutar(comando);

        assertNotNull(resultado);
        verify(codificador).codificar("NuevaClave2026!");
        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(repositorioUsuarios).guardar(captor.capture());
        assertEquals("$2a$10$nuevoHash123456789012345678901234567890123456789012", captor.getValue().getContrasenaHash());
    }

    @Test
    @DisplayName("Lanza ExcepcionDatoInvalido si la nueva contraseña tiene menos de 10 caracteres")
    void debeFallarSiNuevaContrasenaTieneMenosDe10Caracteres() {
        Usuario clinico = new Usuario(
                10L,
                "Dr. Roberto",
                "roberto@salud.org",
                "$2a$10$hashAnterior123456789012345678901234567890123456789012",
                RolUsuario.CLINICO,
                true,
                AHORA.minusSeconds(3600),
                "Psicología Clínica"
        );

        when(repositorioUsuarios.buscarPorId(10L)).thenReturn(Optional.of(clinico));

        ComandoActualizarClinico comando = new ComandoActualizarClinico(
                1L,
                10L,
                "Dr. Roberto",
                "roberto@salud.org",
                "Psicología Clínica",
                "Corta123"
        );

        ExcepcionDatoInvalido ex = assertThrows(ExcepcionDatoInvalido.class, () -> casoUso.ejecutar(comando));
        assertEquals("La contraseña debe tener al menos 10 caracteres.", ex.getMessage());
        verify(repositorioUsuarios, never()).guardar(any());
    }
}
