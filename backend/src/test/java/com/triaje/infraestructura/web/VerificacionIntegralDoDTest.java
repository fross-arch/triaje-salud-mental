package com.triaje.infraestructura.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.triaje.aplicacion.casouso.AbandonarEvaluacionesVencidas;
import com.triaje.integracion.PruebaIntegracionBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Suite integral de pruebas de integracion automatizadas para la Definicion de Terminado (DoD)
 * del Sprint 6, verificando los puntos criticos de seguridad, protocolo de crisis y persistencia.
 */
class VerificacionIntegralDoDTest extends PruebaIntegracionBase {

    @Autowired(required = false)
    private AbandonarEvaluacionesVencidas casoUsoAbandonar;

    @Test
    @DisplayName("DoD 1: Debe bloquear la creacion de evaluacion si el consentimiento fue revocado (400 o 403 con mensaje explicativo)")
    void debeBloquearEvaluacionSiConsentimientoFueRevocado() throws Exception {
        String correo = correoUnico();
        registrar(correo);
        String token = tokenAcceso(iniciarSesion(correo));

        // 1. Registrar consentimiento
        mockMvc.perform(post("/api/consentimientos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("confirmaMayoriaEdad", true))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.vigente").value(true));

        // 2. Revocar consentimiento
        mockMvc.perform(post("/api/consentimientos/revocar")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());

        // 3. Comprobar que POST /api/triaje/evaluaciones retorne 400 o 403 con mensaje explicativo
        MvcResult resultado = mockMvc.perform(post("/api/triaje/evaluaciones")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andReturn();

        int estado = resultado.getResponse().getStatus();
        assertTrue(estado == 400 || estado == 403,
                "Se esperaba codigo HTTP 400 o 403 pero se obtuvo: " + estado);

        JsonNode cuerpoError = leer(resultado);
        String mensaje = cuerpoError.path("mensaje").asText();
        assertNotNull(mensaje, "El cuerpo de error debe incluir el campo mensaje");
        assertFalse(mensaje.isBlank(), "El mensaje de error no debe estar vacio");
        assertThat(mensaje.toLowerCase()).contains("consentimiento");
    }

    @Test
    @DisplayName("DoD 2: Debe impedir acceso a evaluacion y reporte PDF de otro usuario (403 Forbidden o 404 Not Found)")
    void debeImpedirAccesoAEvaluacionDeOtroUsuario() throws Exception {
        // Autenticar Usuario A y crear su evaluacion
        String correoA = correoUnico();
        registrar(correoA);
        String tokenA = tokenAcceso(iniciarSesion(correoA));

        mockMvc.perform(post("/api/consentimientos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("confirmaMayoriaEdad", true))))
                .andExpect(status().isCreated());

        MvcResult resCrearA = mockMvc.perform(post("/api/triaje/evaluaciones")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenA)))
                .andExpect(status().isCreated())
                .andReturn();
        String idEvaluacionUsuarioA = leer(resCrearA).get("id").asText();

        // Autenticar Usuario B
        String correoB = correoUnico();
        registrar(correoB);
        String tokenB = tokenAcceso(iniciarSesion(correoB));

        // Intentar consultar detalle de evaluacion de Usuario A
        MvcResult resDetalle = mockMvc.perform(get("/api/triaje/evaluaciones/" + idEvaluacionUsuarioA)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenB)))
                .andReturn();
        int estadoDetalle = resDetalle.getResponse().getStatus();
        assertTrue(estadoDetalle == 403 || estadoDetalle == 404,
                "La consulta de evaluacion ajena debe retornar 403 o 404 pero fue: " + estadoDetalle);

        // Intentar descargar reporte PDF de evaluacion de Usuario A
        MvcResult resPdf = mockMvc.perform(get("/api/triaje/evaluaciones/" + idEvaluacionUsuarioA + "/pdf")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenB)))
                .andReturn();
        int estadoPdf = resPdf.getResponse().getStatus();
        assertTrue(estadoPdf == 403 || estadoPdf == 404,
                "La descarga de PDF ajeno debe retornar 403 o 404 pero fue: " + estadoPdf);
    }

    @Test
    @DisplayName("DoD 3: Debe mantener riesgo de crisis de manera persistente en BD si el usuario abandona tras responder item 9")
    void debeMantenerRiesgoCrisisSiUsuarioAbandonaTrasItemNueve() throws Exception {
        String correo = correoUnico();
        registrar(correo);
        String token = tokenAcceso(iniciarSesion(correo));

        // Consentimiento y creacion de evaluacion
        mockMvc.perform(post("/api/consentimientos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("confirmaMayoriaEdad", true))))
                .andExpect(status().isCreated());

        MvcResult resCrear = mockMvc.perform(post("/api/triaje/evaluaciones")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isCreated())
                .andReturn();
        String evaluacionId = leer(resCrear).get("id").asText();

        // Registrar el item 9 de PHQ-9 con valor 2 (riesgoCrisis = true)
        mockMvc.perform(put("/api/triaje/evaluaciones/" + evaluacionId + "/respuestas")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "instrumento", "PHQ9",
                                "indicePregunta", 9,
                                "valorPuntaje", 2
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riesgoCrisis").value(true));

        // Simular abandono sin finalizar: se adelanta la fecha de creacion para simular vencimiento
        jdbc.update("UPDATE evaluaciones SET fecha_creacion = DATE_SUB(NOW(), INTERVAL 25 HOUR) WHERE id = ?", evaluacionId);

        if (casoUsoAbandonar != null) {
            casoUsoAbandonar.ejecutar();
        }

        // Verificar en la base de datos que 'evaluaciones.riesgo_crisis' sea true de manera persistente
        Boolean riesgoEnBaseDatos = jdbc.queryForObject(
                "SELECT riesgo_crisis FROM evaluaciones WHERE id = ?",
                Boolean.class,
                evaluacionId
        );
        assertThat(riesgoEnBaseDatos).isNotNull().isTrue();

        String estadoDb = jdbc.queryForObject(
                "SELECT estado FROM evaluaciones WHERE id = ?",
                String.class,
                evaluacionId
        );
        assertThat(estadoDb).isNotEqualTo("COMPLETADA");
    }

    @Test
    @DisplayName("DoD 4: Debe eliminar datos en cascada al borrar cuenta conservando solo registro anonimizado en auditoria")
    void debeEliminarDatosEnCascadaAlBorrarCuenta() throws Exception {
        String correo = correoUnico();
        long usuarioId = registrar(correo);
        String token = tokenAcceso(iniciarSesion(correo));

        // Registrar consentimiento
        mockMvc.perform(post("/api/consentimientos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("confirmaMayoriaEdad", true))))
                .andExpect(status().isCreated());

        // Iniciar evaluacion
        MvcResult resCrear = mockMvc.perform(post("/api/triaje/evaluaciones")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isCreated())
                .andReturn();
        String evaluacionId = leer(resCrear).get("id").asText();

        // Responder al menos un item
        mockMvc.perform(put("/api/triaje/evaluaciones/" + evaluacionId + "/respuestas")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "instrumento", "PHQ9",
                                "indicePregunta", 1,
                                "valorPuntaje", 1
                        ))))
                .andExpect(status().isOk());

        // Comprobar precondicion: registros existen antes del borrado
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM usuarios WHERE id = ?", Integer.class, usuarioId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM consentimientos WHERE usuario_id = ?", Integer.class, usuarioId)).isPositive();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM evaluaciones WHERE usuario_id = ?", Integer.class, usuarioId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM respuestas_evaluacion WHERE evaluacion_id = ?", Integer.class, evaluacionId)).isEqualTo(1);

        // Ejecutar DELETE /api/usuarios/yo
        mockMvc.perform(delete("/api/usuarios/yo")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNoContent());

        // Comprobar que en las tablas no queda ningun registro del usuario
        Integer usuariosRestantes = jdbc.queryForObject("SELECT COUNT(*) FROM usuarios WHERE id = ?", Integer.class, usuarioId);
        assertThat(usuariosRestantes).isZero();

        Integer consentimientosRestantes = jdbc.queryForObject("SELECT COUNT(*) FROM consentimientos WHERE usuario_id = ?", Integer.class, usuarioId);
        assertThat(consentimientosRestantes).isZero();

        Integer evaluacionesRestantes = jdbc.queryForObject("SELECT COUNT(*) FROM evaluaciones WHERE usuario_id = ?", Integer.class, usuarioId);
        assertThat(evaluacionesRestantes).isZero();

        Integer respuestasRestantes = jdbc.queryForObject("SELECT COUNT(*) FROM respuestas_evaluacion WHERE evaluacion_id = ?", Integer.class, evaluacionId);
        assertThat(respuestasRestantes).isZero();

        // Conservando unicamente el registro anonimizado en 'registro_auditoria' con accion 'CUENTA_ELIMINADA'
        List<Map<String, Object>> filasAuditoria = jdbc.queryForList(
                "SELECT accion, usuario_id, entidad_id FROM registro_auditoria WHERE entidad_id = ? AND accion = 'CUENTA_ELIMINADA'",
                String.valueOf(usuarioId)
        );
        assertThat(filasAuditoria).hasSize(1);
        assertThat(filasAuditoria.get(0).get("accion")).isEqualTo("CUENTA_ELIMINADA");
        assertThat(filasAuditoria.get(0).get("usuario_id")).isNull();
    }

    @Test
    @DisplayName("DoD 5: Debe rechazar la finalizacion con 400 Bad Request si faltan respuestas (15 de 16)")
    void debeRechazarFinalizacionSiFaltanRespuestas() throws Exception {
        String correo = correoUnico();
        registrar(correo);
        String token = tokenAcceso(iniciarSesion(correo));

        // Consentimiento y evaluacion
        mockMvc.perform(post("/api/consentimientos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("confirmaMayoriaEdad", true))))
                .andExpect(status().isCreated());

        MvcResult resCrear = mockMvc.perform(post("/api/triaje/evaluaciones")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isCreated())
                .andReturn();
        String evaluacionId = leer(resCrear).get("id").asText();

        // Registrar 9 respuestas de PHQ-9 (todas en 0)
        for (int i = 1; i <= 9; i++) {
            mockMvc.perform(put("/api/triaje/evaluaciones/" + evaluacionId + "/respuestas")
                            .header(HttpHeaders.AUTHORIZATION, bearer(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of(
                                    "instrumento", "PHQ9",
                                    "indicePregunta", i,
                                    "valorPuntaje", 0
                            ))))
                    .andExpect(status().isOk());
        }

        // Registrar 6 respuestas de GAD-7 (falta el item 7 para completar las 16)
        for (int i = 1; i <= 6; i++) {
            mockMvc.perform(put("/api/triaje/evaluaciones/" + evaluacionId + "/respuestas")
                            .header(HttpHeaders.AUTHORIZATION, bearer(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of(
                                    "instrumento", "GAD7",
                                    "indicePregunta", i,
                                    "valorPuntaje", 0
                            ))))
                    .andExpect(status().isOk());
        }

        // Verificar que exactamente hay 15 respuestas registradas en BD
        Integer totalRespuestas = jdbc.queryForObject(
                "SELECT COUNT(*) FROM respuestas_evaluacion WHERE evaluacion_id = ?",
                Integer.class,
                evaluacionId
        );
        assertThat(totalRespuestas).isEqualTo(15);

        // Intentar finalizar evaluacion incompleta: debe retornar 400 Bad Request
        mockMvc.perform(post("/api/triaje/evaluaciones/" + evaluacionId + "/finalizar")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("EVALUACION_INCOMPLETA"))
                .andExpect(jsonPath("$.mensaje").isNotEmpty());

        // Verificar que en base de datos la evaluacion continua en estado EN_CURSO
        String estadoDb = jdbc.queryForObject(
                "SELECT estado FROM evaluaciones WHERE id = ?",
                String.class,
                evaluacionId
        );
        assertThat(estadoDb).isEqualTo("EN_CURSO");
    }
}
