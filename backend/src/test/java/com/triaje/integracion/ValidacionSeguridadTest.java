package com.triaje.integracion;

import com.triaje.TriajeAplicacion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ValidacionSeguridadTest extends PruebaIntegracionBase {

    @Test
    @DisplayName("Contrasena de mas de 72 bytes en UTF-8 es rechazada con mensaje claro en espanol")
    void debeRechazarContrasenaDeMasDe72Bytes() throws Exception {
        // Generamos una contrasena de mas de 72 bytes UTF-8 (73 caracteres ASCII)
        String contrasenaLarga = "A".repeat(73);

        mockMvc.perform(post("/api/autenticacion/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nombreCompleto", "Usuario Prueba",
                                "correo", correoUnico(),
                                "contrasena", contrasenaLarga))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estado").value(400))
                .andExpect(jsonPath("$.error").value("SOLICITUD_INVALIDA"));
    }

    @Test
    @DisplayName("Contrasena menor a 10 caracteres es rechazada con mensaje explicito")
    void debeRechazarContrasenaCorta() throws Exception {
        mockMvc.perform(post("/api/autenticacion/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nombreCompleto", "Usuario Corto",
                                "correo", correoUnico(),
                                "contrasena", "123456789"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles[?(@.campo == 'contrasena')].mensaje")
                        .value("La contraseña debe tener al menos 10 caracteres"));
    }

    @Test
    @DisplayName("La aplicacion no arranca con JWT_SECRETO ausente o menor de 32 bytes")
    void debeFallarArranqueSinSecretoOSecretoDebil() {
        // Caso 1: Secreto menor de 32 bytes
        assertThatThrownBy(() -> {
            var propiedades = new com.triaje.infraestructura.configuracion.PropiedadesSeguridad(
                    new com.triaje.infraestructura.configuracion.PropiedadesSeguridad.Jwt("corto-menor-a-32-bytes", "triaje", java.time.Duration.ofMinutes(15)),
                    new com.triaje.infraestructura.configuracion.PropiedadesSeguridad.Renovacion(java.time.Duration.ofDays(7), "token", "/api", false),
                    new com.triaje.infraestructura.configuracion.PropiedadesSeguridad.Cors(java.util.List.of()),
                    new com.triaje.infraestructura.configuracion.PropiedadesSeguridad.LimiteIntentos(20, 5, java.time.Duration.ofMinutes(1)),
                    12
            );
            new com.triaje.infraestructura.seguridad.ProveedorTokensJwt(propiedades, java.time.Clock.systemUTC());
        })
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Error de configuracion de seguridad: La propiedad JWT_SECRETO es obligatoria y debe contener al menos 32 bytes.");

        // Caso 2: Secreto ausente / vacio
        assertThatThrownBy(() -> {
            var propiedades = new com.triaje.infraestructura.configuracion.PropiedadesSeguridad(
                    new com.triaje.infraestructura.configuracion.PropiedadesSeguridad.Jwt("", "triaje", java.time.Duration.ofMinutes(15)),
                    new com.triaje.infraestructura.configuracion.PropiedadesSeguridad.Renovacion(java.time.Duration.ofDays(7), "token", "/api", false),
                    new com.triaje.infraestructura.configuracion.PropiedadesSeguridad.Cors(java.util.List.of()),
                    new com.triaje.infraestructura.configuracion.PropiedadesSeguridad.LimiteIntentos(20, 5, java.time.Duration.ofMinutes(1)),
                    12
            );
            new com.triaje.infraestructura.seguridad.ProveedorTokensJwt(propiedades, java.time.Clock.systemUTC());
        })
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Error de configuracion de seguridad: La propiedad JWT_SECRETO es obligatoria y debe contener al menos 32 bytes.");
    }
}
