package com.Cesde.triaje.Integracion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReutilizacionTokenRenovacionTest extends PruebaIntegracionBase {

    @Test
    @DisplayName("Reutilizar un token de renovacion ya revocado debe revocar todas las sesiones del usuario")
    void debeRevocarTodasLasSesionesAlDetectarReutilizacion() throws Exception {
        String correo = correoUnico();
        long usuarioId = registrar(correo);

        // 1. Sesion inicial A
        MvcResult loginA = iniciarSesion(correo);
        var cookieA = cookieRenovacion(loginA);
        assertThat(cookieA).isNotNull();

        // 2. Sesion adicional B (por ejemplo, desde otro dispositivo)
        MvcResult loginB = iniciarSesion(correo);
        var cookieB = cookieRenovacion(loginB);
        assertThat(cookieB).isNotNull();

        // 3. Renovar con exito la sesion A -> rota cookieA (cookieA queda revocada)
        MvcResult renovadoA = mockMvc.perform(post("/api/autenticacion/renovar")
                        .cookie(cookieA))
                .andExpect(status().isOk())
                .andReturn();
        var cookieANueva = cookieRenovacion(renovadoA);

        // 4. Atacante o cliente desactualizado intenta usar la cookieA ya revocada
        mockMvc.perform(post("/api/autenticacion/renovar")
                        .cookie(cookieA))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("SESION_INVALIDA"));

        // 5. Verificacion: La renovacion de la sesion B y la sesion A nueva tambien deben fallar
        // porque la reutilizacion anula todas las sesiones activas del usuario
        mockMvc.perform(post("/api/autenticacion/renovar")
                        .cookie(cookieB))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("SESION_INVALIDA"));

        mockMvc.perform(post("/api/autenticacion/renovar")
                        .cookie(cookieANueva))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("SESION_INVALIDA"));

        // Comprobacion en BD: todos los tokens del usuario deben estar con fecha_revocacion no nula
        Integer tokensNoRevocados = jdbc.queryForObject(
                "SELECT COUNT(*) FROM tokens_renovacion WHERE usuario_id = ? AND fecha_revocacion IS NULL",
                Integer.class, usuarioId);
        assertThat(tokensNoRevocados).isZero();
    }
}
