package com.triaje.integracion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AislamientoIpLimitadorTest extends PruebaIntegracionBase {

    @Test
    @DisplayName("Un atacante desde otra IP no bloquea el inicio de sesion de un usuario legitimo")
    void atacanteDesdeOtraIpNoBloqueaUsuarioLegitimo() throws Exception {
        String correoVictima = correoUnico();
        registrar(correoVictima);

        String ipAtacante = "198.51.100.25";
        String ipLegitima = "203.0.113.10";

        // El atacante agota sus 5 intentos fallidos para el correo de la victima desde ipAtacante
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/autenticacion/inicio-sesion")
                            .with(request -> {
                                request.setRemoteAddr(ipAtacante);
                                return request;
                            })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("correo", correoVictima, "contrasena", "ClaveErronea123"))))
                    .andExpect(status().isUnauthorized());
        }

        // El 6to intento del atacante desde su misma IP es bloqueado con 429
        mockMvc.perform(post("/api/autenticacion/inicio-sesion")
                        .with(request -> {
                            request.setRemoteAddr(ipAtacante);
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("correo", correoVictima, "contrasena", "ClaveErronea123"))))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("DEMASIADOS_INTENTOS"));

        // Sin embargo, el usuario legítimo desde su propia IP (ipLegitima) DEBE poder iniciar sesion normalmente
        mockMvc.perform(post("/api/autenticacion/inicio-sesion")
                        .with(request -> {
                            request.setRemoteAddr(ipLegitima);
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("correo", correoVictima, "contrasena", CONTRASENA_VALIDA))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenAcceso").isString());
    }
}
