package com.triaje.infraestructura.tarea;

import com.triaje.aplicacion.puerto.CodificadorContrasenas;
import com.triaje.aplicacion.puerto.RepositorioUsuarios;
import com.triaje.dominio.RolUsuario;
import com.triaje.dominio.modelo.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.env.Environment;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InicializadorAdministradorTest {

    @Mock
    private RepositorioUsuarios repositorioUsuarios;

    @Mock
    private CodificadorContrasenas codificador;

    @Mock
    private Environment ambiente;

    private final Clock reloj = Clock.fixed(Instant.parse("2026-10-15T12:00:00Z"), ZoneOffset.UTC);

    private InicializadorAdministrador inicializador;

    @BeforeEach
    void setUp() {
        inicializador = new InicializadorAdministrador(repositorioUsuarios, codificador, ambiente, reloj);
    }

    @Test
    @DisplayName("No debe crear administrador si ya existe uno en la base de datos")
    void noDebeCrearSiYaExisteAdministrador() {
        when(repositorioUsuarios.existePorRol(RolUsuario.ADMINISTRADOR)).thenReturn(true);

        inicializador.run(null);

        verify(repositorioUsuarios, never()).guardar(any());
    }

    @Test
    @DisplayName("No debe crear administrador si faltan las variables de entorno")
    void noDebeCrearSiFaltanVariablesEntorno() {
        when(repositorioUsuarios.existePorRol(RolUsuario.ADMINISTRADOR)).thenReturn(false);
        when(ambiente.getProperty("ADMIN_CORREO")).thenReturn(null);
        when(ambiente.getProperty("ADMIN_CONTRASENA_INICIAL")).thenReturn(null);

        inicializador.run(null);

        verify(repositorioUsuarios, never()).guardar(any());
    }

    @Test
    @DisplayName("No debe crear administrador si la contrasena tiene menos de 10 caracteres")
    void noDebeCrearSiContrasenaCorta() {
        when(repositorioUsuarios.existePorRol(RolUsuario.ADMINISTRADOR)).thenReturn(false);
        when(ambiente.getProperty("ADMIN_CORREO")).thenReturn("admin@ejemplo.com");
        when(ambiente.getProperty("ADMIN_CONTRASENA_INICIAL")).thenReturn("corta123");

        inicializador.run(null);

        verify(repositorioUsuarios, never()).guardar(any());
    }

    @Test
    @DisplayName("Debe crear administrador si no existe y las variables son validas")
    void debeCrearAdministradorConVariablesValidas() {
        when(repositorioUsuarios.existePorRol(RolUsuario.ADMINISTRADOR)).thenReturn(false);
        when(ambiente.getProperty("ADMIN_CORREO")).thenReturn("admin@ejemplo.com");
        when(ambiente.getProperty("ADMIN_CONTRASENA_INICIAL")).thenReturn("AdminSeguro2026++");
        when(repositorioUsuarios.existePorCorreo("admin@ejemplo.com")).thenReturn(false);
        when(codificador.codificar("AdminSeguro2026++")).thenReturn("hash_admin");

        inicializador.run(null);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(repositorioUsuarios).guardar(captor.capture());

        Usuario guardado = captor.getValue();
        assertThat(guardado.getRol()).isEqualTo(RolUsuario.ADMINISTRADOR);
        assertThat(guardado.getCorreo()).isEqualTo("admin@ejemplo.com");
        assertThat(guardado.isActivo()).isTrue();
    }

    @Test
    @DisplayName("Debe crear administrador usando las propiedades de configuracion seguridad.admin")
    void debeCrearAdministradorConPropiedadesConfiguradas() {
        when(repositorioUsuarios.existePorRol(RolUsuario.ADMINISTRADOR)).thenReturn(false);
        when(ambiente.getProperty("seguridad.admin.correo")).thenReturn("admin@triaje.local");
        when(ambiente.getProperty("seguridad.admin.contrasena-inicial")).thenReturn("AdminSeguro2026*");
        when(repositorioUsuarios.existePorCorreo("admin@triaje.local")).thenReturn(false);
        when(codificador.codificar("AdminSeguro2026*")).thenReturn("hash_admin_local");

        inicializador.run(null);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(repositorioUsuarios).guardar(captor.capture());

        Usuario guardado = captor.getValue();
        assertThat(guardado.getRol()).isEqualTo(RolUsuario.ADMINISTRADOR);
        assertThat(guardado.getCorreo()).isEqualTo("admin@triaje.local");
        assertThat(guardado.getNombreCompleto()).isEqualTo("Administrador del Sistema");
        assertThat(guardado.isActivo()).isTrue();
    }

    @Test
    @DisplayName("No debe crear administrador si la contrasena configurada en seguridad.admin tiene menos de 10 caracteres")
    void noDebeCrearSiContrasenaConfiguradaEsCorta() {
        when(repositorioUsuarios.existePorRol(RolUsuario.ADMINISTRADOR)).thenReturn(false);
        when(ambiente.getProperty("seguridad.admin.correo")).thenReturn("admin@triaje.local");
        when(ambiente.getProperty("seguridad.admin.contrasena-inicial")).thenReturn("corta123");

        inicializador.run(null);

        verify(repositorioUsuarios, never()).guardar(any());
    }
}
