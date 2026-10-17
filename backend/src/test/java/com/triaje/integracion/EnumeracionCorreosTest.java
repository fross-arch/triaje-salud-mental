package com.triaje.integracion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EnumeracionCorreosTest extends PruebaIntegracionBase {

    @Test
    @DisplayName("Inicio de sesion con correo inexistente y con contrasena incorrecta devuelven el mismo codigo y el mismo mensaje")
    void debeDevolverMismoCodigoYMensajeParaCorreoInexistenteYContrasenaErronea() throws Exception {
        String correoExistente = correoUnico();
        registrar(correoExistente);

        // Caso 1: Correo existe, pero contrasena incorrecta
        MvcResult respuestaContrasenaMal = mockMvc.perform(post("/api/autenticacion/inicio-sesion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("correo", correoExistente, "contrasena", "ContrasenaEquivocada123"))))
                .andExpect(status().isUnauthorized())
                .andReturn();

        // Caso 2: Correo no existe
        MvcResult respuestaCorreoNoExiste = mockMvc.perform(post("/api/autenticacion/inicio-sesion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("correo", "no-existe-" + correoUnico(), "contrasena", CONTRASENA_VALIDA))))
                .andExpect(status().isUnauthorized())
                .andReturn();

        var json1 = leer(respuestaContrasenaMal);
        var json2 = leer(respuestaCorreoNoExiste);

        assertThat(json1.get("estado").asInt()).isEqualTo(json2.get("estado").asInt()).isEqualTo(401);
        assertThat(json1.get("error").asText()).isEqualTo(json2.get("error").asText()).isEqualTo("CREDENCIALES_INVALIDAS");
        assertThat(json1.get("mensaje").asText()).isEqualTo(json2.get("mensaje").asText());
    }

    @Test
    @DisplayName("Registrar con 'Usuario@Ejemplo.com ' y luego iniciar sesion con 'usuario@ejemplo.com' funciona correctamente")
    void debeNormalizarCorreoEnRegistroYEnInicioSesion() throws Exception {
        String correoBase = "usuario-" + System.currentTimeMillis() + "@ejemplo.com";
        String correoConMayusculasYEspacios = " " + correoBase.toUpperCase() + "  ";
        String correoNormalizado = correoBase.toLowerCase();

        // 1. Registro con mayusculas y espacios
        mockMvc.perform(post("/api/autenticacion/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nombreCompleto", "Usuario Ejemplo",
                                "correo", correoConMayusculasYEspacios,
                                "contrasena", CONTRASENA_VALIDA))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.correo").value(correoNormalizado));

        // 2. Inicio de sesion usando minusculas limpias
        mockMvc.perform(post("/api/autenticacion/inicio-sesion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "correo", correoNormalizado,
                                "contrasena", CONTRASENA_VALIDA))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenAcceso").isString());
    }
}
