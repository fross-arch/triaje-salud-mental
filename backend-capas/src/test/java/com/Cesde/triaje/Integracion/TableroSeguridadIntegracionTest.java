package com.Cesde.triaje.Integracion;

import com.Cesde.triaje.Dto.RespuestaTablero;
import com.Cesde.triaje.Modelo.Enumeracion.RolUsuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TableroSeguridadIntegracionTest extends PruebaIntegracionBase {

    @org.springframework.beans.factory.annotation.Autowired
    private com.Cesde.triaje.Repositorio.IRegistroAuditoriaRepositorio registroAuditoriaRepositorio;

    @Test

    @DisplayName("Un PACIENTE autenticado recibe 403 FORBIDDEN al intentar acceder a /api/administracion/tablero")
    void pacienteRecibe403EnTablero() throws Exception {
        String correoPaciente = correoUnico();
        registrar(correoPaciente);
        String tokenPaciente = tokenAcceso(iniciarSesion(correoPaciente));

        mockMvc.perform(get("/api/administracion/tablero")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenPaciente)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACCESO_DENEGADO"));
    }

    @Test
    @DisplayName("Un CLÍNICO autenticado recibe 403 FORBIDDEN al intentar acceder a /api/administracion/tablero")
    void clinicoRecibe403EnTablero() throws Exception {
        String correoClinico = correoUnico();
        crearUsuarioDirecto(correoClinico, "Dra. Clinica Test", RolUsuario.CLINICO, true, "Psicologia");
        String tokenClinico = tokenAcceso(iniciarSesion(correoClinico));

        mockMvc.perform(get("/api/administracion/tablero")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenClinico)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACCESO_DENEGADO"));
    }

    @Test
    @DisplayName("Una petición no autenticada recibe 401 UNAUTHORIZED en /api/administracion/tablero")
    void solicitudSinTokenRecibe401EnTablero() throws Exception {
        mockMvc.perform(get("/api/administracion/tablero"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("NO_AUTENTICADO"));
    }

    @Test
    @DisplayName("Un ADMINISTRADOR recibe 200 OK y el payload hacia Python no contiene id, nombre ni correo")
    @SuppressWarnings("unchecked")
    void administradorRecibe200YPayloadEsAnonimo() throws Exception {
        String correoAdmin = correoUnico();
        crearUsuarioDirecto(correoAdmin, "Admin Central", RolUsuario.ADMINISTRADOR, true);
        String tokenAdmin = tokenAcceso(iniciarSesion(correoAdmin));

        RespuestaTablero respuestaSimulada = new RespuestaTablero(
                false,
                null,
                15,
                5,
                new RespuestaTablero.IndicadoresTablero(15, 8.2, 8.0, 2.1, 6.5, 6.0, 1.9, 0.72, 0, 0.0, 5),
                new RespuestaTablero.GraficosTablero("img_phq9", "img_gad7", "img_semanas", "img_matriz")
        );

        when(reporteServicio.generarTablero(any())).thenReturn(respuestaSimulada);

        MvcResult resultado = mockMvc.perform(get("/api/administracion/tablero?dias=30")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos_insuficientes").value(false))
                .andExpect(jsonPath("$.total_evaluaciones").value(15))
                .andExpect(jsonPath("$.indicadores.media_phq9").value(8.2))
                .andExpect(jsonPath("$.graficos.severidad_phq9").value("img_phq9"))
                .andReturn();

        // Validar que el payload recibido por el servicio de reportes Python NO contiene campos de identidad
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(reporteServicio).generarTablero(captor.capture());

        Map<String, Object> payloadHaciaPython = captor.getValue();
        assertThat(payloadHaciaPython).containsKey("registros");
        assertThat(payloadHaciaPython).containsKey("minimo_grupo");
        assertEquals(5, payloadHaciaPython.get("minimo_grupo"));

        List<Map<String, Object>> registros = (List<Map<String, Object>>) payloadHaciaPython.get("registros");
        for (Map<String, Object> reg : registros) {
            assertFalse(reg.containsKey("id"), "El payload hacia Python no debe contener id");
            assertFalse(reg.containsKey("nombre"), "El payload hacia Python no debe contener nombre");
            assertFalse(reg.containsKey("correo"), "El payload hacia Python no debe contener correo");
            assertFalse(reg.containsKey("usuario"), "El payload hacia Python no debe contener usuario");
            assertFalse(reg.containsKey("usuarioId"), "El payload hacia Python no debe contener usuarioId");
        }
    }

    @Test
    @DisplayName("Un PACIENTE autenticado recibe 403 FORBIDDEN al intentar exportar el tablero")
    void pacienteRecibe403EnExportar() throws Exception {
        String correoPaciente = correoUnico();
        registrar(correoPaciente);
        String tokenPaciente = tokenAcceso(iniciarSesion(correoPaciente));

        mockMvc.perform(get("/api/administracion/tablero/exportar?dias=30&formato=csv")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenPaciente)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACCESO_DENEGADO"));
    }

    @Test
    @DisplayName("Un CLÍNICO autenticado recibe 403 FORBIDDEN al intentar exportar el tablero")
    void clinicoRecibe403EnExportar() throws Exception {
        String correoClinico = correoUnico();
        crearUsuarioDirecto(correoClinico, "Dra. Clinica Test", RolUsuario.CLINICO, true, "Psicologia");
        String tokenClinico = tokenAcceso(iniciarSesion(correoClinico));

        mockMvc.perform(get("/api/administracion/tablero/exportar?dias=30&formato=csv")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenClinico)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACCESO_DENEGADO"));
    }

    @Test
    @DisplayName("Una petición sin token recibe 401 UNAUTHORIZED al intentar exportar el tablero")
    void solicitudSinTokenRecibe401EnExportar() throws Exception {
        mockMvc.perform(get("/api/administracion/tablero/exportar?dias=30&formato=csv"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("NO_AUTENTICADO"));
    }

    @Test
    @DisplayName("Un formato inválido responde 400 BAD_REQUEST (ExcepcionDatoInvalido)")
    void formatoInvalidoResponde400() throws Exception {
        String correoAdmin = correoUnico();
        crearUsuarioDirecto(correoAdmin, "Admin Central", RolUsuario.ADMINISTRADOR, true);
        String tokenAdmin = tokenAcceso(iniciarSesion(correoAdmin));

        mockMvc.perform(get("/api/administracion/tablero/exportar?dias=30&formato=xml")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Un ADMINISTRADOR exporta CSV agregado exitosamente: UTF-8 BOM, sin identificadores y con auditoría TABLERO_EXPORTADO")
    void administradorExportaCsvYRegistraAuditoria() throws Exception {
        String correoAdmin = correoUnico();
        crearUsuarioDirecto(correoAdmin, "Admin Central", RolUsuario.ADMINISTRADOR, true);
        String tokenAdmin = tokenAcceso(iniciarSesion(correoAdmin));

        RespuestaTablero respuestaSimulada = new RespuestaTablero(
                false,
                null,
                15,
                5,
                new RespuestaTablero.IndicadoresTablero(15, 8.2, 8.0, 2.1, 6.5, 6.0, 1.9, 0.72, 0, 0.0, 5),
                new RespuestaTablero.GraficosTablero("img_phq9", "img_gad7", "img_semanas", "img_matriz")
        );
        when(reporteServicio.generarTablero(any())).thenReturn(respuestaSimulada);

        MvcResult resultado = mockMvc.perform(get("/api/administracion/tablero/exportar?dias=30&formato=csv")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin)))
                .andExpect(status().isOk())
                .andReturn();

        byte[] contenidoBytes = resultado.getResponse().getContentAsByteArray();
        assertThat(contenidoBytes).isNotEmpty();

        // Verificar BOM UTF-8 (0xEF, 0xBB, 0xBF)
        assertThat(contenidoBytes[0]).isEqualTo((byte) 0xEF);
        assertThat(contenidoBytes[1]).isEqualTo((byte) 0xBB);
        assertThat(contenidoBytes[2]).isEqualTo((byte) 0xBF);

        String contenidoStr = new String(contenidoBytes, java.nio.charset.StandardCharsets.UTF_8);
        assertThat(contenidoStr).contains(";");
        assertThat(contenidoStr).contains("INFORME ESTADÍSTICO AGREGADO DEL TABLERO");
        assertThat(contenidoStr).doesNotContain(correoAdmin);
        assertThat(contenidoStr).doesNotContain("Admin Central");

        // Verificar registro de auditoría TABLERO_EXPORTADO
        var registrosAuditoria = registroAuditoriaRepositorio.findAll();
        boolean auditado = registrosAuditoria.stream()
                .anyMatch(r -> r.getAccion() == com.Cesde.triaje.Modelo.Enumeracion.AccionAuditoria.TABLERO_EXPORTADO);
        assertThat(auditado).isTrue();
    }
}

