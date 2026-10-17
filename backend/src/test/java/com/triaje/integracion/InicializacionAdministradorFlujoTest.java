package com.triaje.integracion;

import com.fasterxml.jackson.databind.JsonNode;
import com.triaje.dominio.RolUsuario;
import com.triaje.infraestructura.seguridad.ProveedorTokensJwt;
import com.triaje.infraestructura.tarea.InicializadorAdministrador;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InicializacionAdministradorFlujoTest extends PruebaIntegracionBase {

    @Autowired
    private InicializadorAdministrador inicializadorAdministrador;

    @Autowired
    private ProveedorTokensJwt proveedorTokensJwt;

    @Test
    @DisplayName("Debe inicializar el administrador por defecto, permitir inicio de sesion con JWT rol ADMINISTRADOR y consultar /api/usuarios/yo")
    void debeInicializarAdministradorYPermitirInicioSesionYConsultaPerfil() throws Exception {
        // Ejecutar inicializador (la base de datos se limpia en @BeforeEach de PruebaIntegracionBase)
        inicializadorAdministrador.run(null);

        // 1. Verificar inicio de sesion con las credenciales por defecto configuradas
        MvcResult resLogin = mockMvc.perform(post("/api/autenticacion/inicio-sesion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "correo", "admin@triaje.local",
                                "contrasena", "AdminSeguro2026*"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenAcceso").isString())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andReturn();

        String token = tokenAcceso(resLogin);
        assertThat(token).isNotBlank();

        // 2. Verificar que el token JWT contiene el claim rol="ADMINISTRADOR"
        String[] partesJwt = token.split("\\.");
        assertThat(partesJwt).hasSizeGreaterThanOrEqualTo(2);
        String payloadJson = new String(Base64.getUrlDecoder().decode(partesJwt[1]), StandardCharsets.UTF_8);
        JsonNode payload = mapeador.readTree(payloadJson);
        assertThat(payload.has("rol")).isTrue();
        assertThat(payload.get("rol").asText()).isEqualTo("ADMINISTRADOR");

        var autenticadoOpt = proveedorTokensJwt.validar(token);
        assertThat(autenticadoOpt).isPresent();
        assertThat(autenticadoOpt.get().rol()).isEqualTo(RolUsuario.ADMINISTRADOR);

        // 3. Consultar GET /api/usuarios/yo devolviendo nombreCompleto y rol de administrador
        mockMvc.perform(get("/api/usuarios/yo")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreCompleto").value("Administrador del Sistema"))
                .andExpect(jsonPath("$.rol").value("ADMINISTRADOR"))
                .andExpect(jsonPath("$.correo").value("admin@triaje.local"));

        // 4. Idempotencia: ejecutar nuevamente el inicializador no debe duplicar ni fallar
        inicializadorAdministrador.run(null);

        Integer cantidadAdmins = jdbc.queryForObject(
                "SELECT COUNT(*) FROM usuarios WHERE rol = 'ADMINISTRADOR'", Integer.class);
        assertThat(cantidadAdmins).isEqualTo(1);
    }
}
