package com.triaje.integracion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AutenticacionFlujoTest extends PruebaIntegracionBase {

    @Test
    @DisplayName("Debe registrar un usuario exitosamente con codigo 201")
    void debeRegistrarUsuarioExitosamente() throws Exception {
        String correo = correoUnico();

        mockMvc.perform(post("/api/autenticacion/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nombreCompleto", "Camila Gomez",
                                "correo", correo,
                                "contrasena", CONTRASENA_VALIDA))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nombreCompleto").value("Camila Gomez"))
                .andExpect(jsonPath("$.correo").value(correo))
                .andExpect(jsonPath("$.rol").value("PACIENTE"));
    }

    @Test
    @DisplayName("Debe rechazar el registro con 400 si la contrasena tiene menos de 10 caracteres")
    void debeRechazarRegistroConContrasenaCorta() throws Exception {
        mockMvc.perform(post("/api/autenticacion/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nombreCompleto", "Carlos Diaz",
                                "correo", correoUnico(),
                                "contrasena", "corta123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estado").value(400))
                .andExpect(jsonPath("$.error").value("SOLICITUD_INVALIDA"))
                .andExpect(jsonPath("$.detalles[0].campo").value("contrasena"));
    }

    @Test
    @DisplayName("Debe rechazar el registro con 409 si el correo ya esta registrado")
    void debeRechazarRegistroConCorreoDuplicado() throws Exception {
        String correo = correoUnico();
        registrar(correo);

        mockMvc.perform(post("/api/autenticacion/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nombreCompleto", "Otro Usuario",
                                "correo", correo,
                                "contrasena", CONTRASENA_VALIDA))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.estado").value(409))
                .andExpect(jsonPath("$.error").value("CORREO_YA_REGISTRADO"));
    }

    @Test
    @DisplayName("Debe iniciar sesion devolviendo JWT en cuerpo, cookie httpOnly SameSite=Strict y registrar auditoria")
    void debeIniciarSesionCorrectamenteYAuditar() throws Exception {
        String correo = correoUnico();
        long usuarioId = registrar(correo);

        MvcResult resultado = mockMvc.perform(post("/api/autenticacion/inicio-sesion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("correo", correo, "contrasena", CONTRASENA_VALIDA))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenAcceso").isString())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.expiraEnSegundos").value(900))
                .andExpect(header().exists(HttpHeaders.SET_COOKIE))
                .andReturn();

        String encabezadoSetCookie = encabezadoCookie(resultado);
        assertThat(encabezadoSetCookie).contains(NOMBRE_COOKIE);
        assertThat(encabezadoSetCookie).containsIgnoringCase("HttpOnly");
        assertThat(encabezadoSetCookie).containsIgnoringCase("SameSite=Strict");

        List<Map<String, Object>> filas = jdbc.queryForList(
                "SELECT accion, usuario_id FROM registro_auditoria WHERE usuario_id = ?", usuarioId);
        assertThat(filas).isNotEmpty();
        assertThat(filas.stream().anyMatch(f -> "INICIO_SESION".equals(f.get("accion")))).isTrue();
    }

    @Test
    @DisplayName("Debe responder 401 si las credenciales son incorrectas")
    void debeResponder401ConCredencialesIncorrectas() throws Exception {
        String correo = correoUnico();
        registrar(correo);

        mockMvc.perform(post("/api/autenticacion/inicio-sesion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("correo", correo, "contrasena", "ClaveErronea999"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.estado").value(401))
                .andExpect(jsonPath("$.error").value("CREDENCIALES_INVALIDAS"));
    }

    @Test
    @DisplayName("Debe responder 401 sin token y 200 con Bearer en endpoint protegido")
    void debeProtegerEndpointSegunToken() throws Exception {
        String correo = correoUnico();
        registrar(correo);

        mockMvc.perform(get("/api/usuarios/yo"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.estado").value(401))
                .andExpect(jsonPath("$.error").value("NO_AUTENTICADO"));

        MvcResult login = iniciarSesion(correo);
        String token = tokenAcceso(login);

        mockMvc.perform(get("/api/usuarios/yo")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value(correo));
    }

    @Test
    @DisplayName("Debe rotar el token de renovacion al llamar a /renovar e invalidar el anterior")
    void debeRotarTokenRenovacion() throws Exception {
        String correo = correoUnico();
        registrar(correo);

        MvcResult login = iniciarSesion(correo);
        var cookieOriginal = cookieRenovacion(login);
        assertThat(cookieOriginal).isNotNull();

        MvcResult renovado = mockMvc.perform(post("/api/autenticacion/renovar")
                        .cookie(cookieOriginal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenAcceso").isString())
                .andExpect(header().exists(HttpHeaders.SET_COOKIE))
                .andReturn();

        var nuevaCookie = cookieRenovacion(renovado);
        assertThat(nuevaCookie).isNotNull();
        assertThat(nuevaCookie.getValue()).isNotEqualTo(cookieOriginal.getValue());

        // El token anterior ahora debe ser rechazado
        mockMvc.perform(post("/api/autenticacion/renovar")
                        .cookie(cookieOriginal))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("SESION_INVALIDA"));
    }

    @Test
    @DisplayName("Debe cerrar sesion revocando la cookie y limpiando la cabecera Set-Cookie")
    void debeCerrarSesionCorrectamente() throws Exception {
        String correo = correoUnico();
        registrar(correo);

        MvcResult login = iniciarSesion(correo);
        var cookie = cookieRenovacion(login);

        MvcResult cierre = mockMvc.perform(post("/api/autenticacion/cierre-sesion")
                        .cookie(cookie))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")))
                .andReturn();

        assertThat(encabezadoCookie(cierre)).contains(NOMBRE_COOKIE);

        // La cookie cerrada ya no permite renovar
        mockMvc.perform(post("/api/autenticacion/renovar")
                        .cookie(cookie))
                .andExpect(status().isUnauthorized());
    }
}
