package com.triaje.integracion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LimitacionIntentosTest extends PruebaIntegracionBase {

    @Test
    @DisplayName("Debe responder 429 Too Many Requests tras superar el limite de 5 intentos por correo")
    void debeBloquearTrasSuperarLimiteIntentos() throws Exception {
        String correo = correoUnico();

        // 5 intentos permitidos (fallan con 401 Credenciales Invalidas)
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/autenticacion/inicio-sesion")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("correo", correo, "contrasena", "ClaveErronea123"))))
                    .andExpect(status().isUnauthorized());
        }

        // El 6to intento debe responder 429 Too Many Requests
        mockMvc.perform(post("/api/autenticacion/inicio-sesion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("correo", correo, "contrasena", "ClaveErronea123"))))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.estado").value(429))
                .andExpect(jsonPath("$.error").value("DEMASIADOS_INTENTOS"))
                .andExpect(header().exists("Retry-After"));
    }
}
