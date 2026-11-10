package com.Cesde.triaje.Integracion;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FlujoCribadoConsentimientoCrisisTest extends PruebaIntegracionBase {

    @Test
    @DisplayName("Debe consultar recursos de ayuda publicamente sin autenticacion")
    void debeConsultarRecursosAyudaSinAutenticacion() throws Exception {
        mockMvc.perform(get("/api/recursos-ayuda"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].nombre").value("Linea 123"))
                .andExpect(jsonPath("$[1].nombre").value("Linea 192 opcion 4"))
                .andExpect(jsonPath("$[2].nombre").value("Linea Amiga Saludable Medellin"));
    }

    @Test
    @DisplayName("Debe rechazar la creacion de evaluacion si no hay consentimiento otorgado")
    void debeRechazarCrearEvaluacionSinConsentimiento() throws Exception {
        String correo = correoUnico();
        registrar(correo);
        String token = tokenAcceso(iniciarSesion(correo));

        mockMvc.perform(post("/api/triaje/evaluaciones")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACCESO_DENEGADO"));
    }

    @Test
    @DisplayName("Debe rechazar la creacion de evaluacion si el consentimiento fue revocado")
    void debeRechazarCrearEvaluacionConConsentimientoRevocado() throws Exception {
        String correo = correoUnico();
        registrar(correo);
        String token = tokenAcceso(iniciarSesion(correo));

        // Aceptar consentimiento
        mockMvc.perform(post("/api/consentimientos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("confirmaMayoriaEdad", true))))
                .andExpect(status().isCreated());

        // Revocar consentimiento
        mockMvc.perform(post("/api/consentimientos/revocar")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());

        // Verificar estado vigente = false
        mockMvc.perform(get("/api/consentimientos/vigente")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vigente").value(false));

        // Intentar crear evaluacion
        mockMvc.perform(post("/api/triaje/evaluaciones")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACCESO_DENEGADO"));
    }

    @Test
    @DisplayName("Flujo completo exitoso: consentimiento -> crear evaluacion -> responder 16 items -> finalizar -> auditar")
    void debeEjecutarFlujoCompletoExitosoYAuditar() throws Exception {
        String correo = correoUnico();
        registrar(correo);
        String token = tokenAcceso(iniciarSesion(correo));

        // 1. Consentimiento
        mockMvc.perform(post("/api/consentimientos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("confirmaMayoriaEdad", true))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.vigente").value(true));

        // 2. Crear evaluacion
        MvcResult resCrear = mockMvc.perform(post("/api/triaje/evaluaciones")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("EN_CURSO"))
                .andExpect(jsonPath("$.riesgoCrisis").value(false))
                .andReturn();

        String evaluacionId = leer(resCrear).get("id").asText();

        // 3. Responder las 9 de PHQ-9 (todas en 0, sin crisis)
        for (int i = 1; i <= 9; i++) {
            mockMvc.perform(put("/api/triaje/evaluaciones/" + evaluacionId + "/respuestas")
                            .header(HttpHeaders.AUTHORIZATION, bearer(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of(
                                    "instrumento", "PHQ9",
                                    "indicePregunta", i,
                                    "valorPuntaje", 0
                            ))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.riesgoCrisis").value(false));
        }

        // 4. Responder las 7 de GAD-7 (todas en 0)
        for (int i = 1; i <= 7; i++) {
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

        // 5. Finalizar evaluacion
        mockMvc.perform(post("/api/triaje/evaluaciones/" + evaluacionId + "/finalizar")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("COMPLETADA"))
                .andExpect(jsonPath("$.puntajePhq9").value(0))
                .andExpect(jsonPath("$.puntajeGad7").value(0))
                .andExpect(jsonPath("$.severidadDepresion").value("MINIMA"))
                .andExpect(jsonPath("$.severidadAnsiedad").value("MINIMA"))
                .andExpect(jsonPath("$.riesgoCrisis").value(false))
                .andExpect(jsonPath("$.resultadoMinimo").value(true));

        // 6. Consultar detalle
        mockMvc.perform(get("/api/triaje/evaluaciones/" + evaluacionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evaluacion.id").value(evaluacionId))
                .andExpect(jsonPath("$.respuestas", hasSize(16)));

        // 7. Verificar auditoria en BD
        Integer auditConsentimiento = jdbc.queryForObject(
                "SELECT count(*) FROM registro_auditoria WHERE accion = 'CONSENTIMIENTO_ACEPTADO'",
                Integer.class
        );
        Integer auditEvaluacion = jdbc.queryForObject(
                "SELECT count(*) FROM registro_auditoria WHERE accion = 'EVALUACION_COMPLETADA' AND entidad_id = ?",
                Integer.class,
                evaluacionId
        );
        assertEquals(1, auditConsentimiento);
        assertEquals(1, auditEvaluacion);
    }

    @Test
    @DisplayName("Deteccion inmediata de crisis: responder item 9 con valor 1 marca riesgo_crisis=true en BD antes de finalizar")
    void debeDetectarCrisisDeInmediatoConItem9() throws Exception {
        String correo = correoUnico();
        registrar(correo);
        String token = tokenAcceso(iniciarSesion(correo));

        // Consentimiento y creacion
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

        // Responder item 9 de PHQ-9 con valor 1
        mockMvc.perform(put("/api/triaje/evaluaciones/" + evaluacionId + "/respuestas")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "instrumento", "PHQ9",
                                "indicePregunta", 9,
                                "valorPuntaje", 1
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riesgoCrisis").value(true));

        // Verificacion en base de datos ANTES de finalizar
        Boolean riesgoCrisisDb = jdbc.queryForObject(
                "SELECT riesgo_crisis FROM evaluaciones WHERE id = ?",
                Boolean.class,
                evaluacionId
        );
        assertTrue(riesgoCrisisDb != null && riesgoCrisisDb);
    }

    @Test
    @DisplayName("Debe rechazar finalizacion si la evaluacion esta incompleta")
    void debeRechazarFinalizacionIncompleta() throws Exception {
        String correo = correoUnico();
        registrar(correo);
        String token = tokenAcceso(iniciarSesion(correo));

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

        // Solo respondemos una pregunta
        mockMvc.perform(put("/api/triaje/evaluaciones/" + evaluacionId + "/respuestas")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "instrumento", "PHQ9",
                                "indicePregunta", 1,
                                "valorPuntaje", 0
                        ))))
                .andExpect(status().isOk());

        // Intentar finalizar
        mockMvc.perform(post("/api/triaje/evaluaciones/" + evaluacionId + "/finalizar")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("EVALUACION_INCOMPLETA"));
    }

    @Test
    @DisplayName("Debe bloquear acceso entre usuarios: Usuario B no puede responder ni ver evaluacion de Usuario A")
    void debeBloquearAccesoEntreUsuarios() throws Exception {
        String correoA = correoUnico();
        registrar(correoA);
        String tokenA = tokenAcceso(iniciarSesion(correoA));

        String correoB = correoUnico();
        registrar(correoB);
        String tokenB = tokenAcceso(iniciarSesion(correoB));

        // Usuario A crea consentimiento y evaluacion
        mockMvc.perform(post("/api/consentimientos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("confirmaMayoriaEdad", true))))
                .andExpect(status().isCreated());

        MvcResult resCrear = mockMvc.perform(post("/api/triaje/evaluaciones")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenA)))
                .andExpect(status().isCreated())
                .andReturn();
        String evaluacionIdA = leer(resCrear).get("id").asText();

        // Usuario B intenta responder evaluacion de Usuario A -> 404
        mockMvc.perform(put("/api/triaje/evaluaciones/" + evaluacionIdA + "/respuestas")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "instrumento", "PHQ9",
                                "indicePregunta", 1,
                                "valorPuntaje", 0
                        ))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("EVALUACION_NO_ENCONTRADA"));

        // Usuario B intenta consultar detalle de evaluacion de Usuario A -> 404
        mockMvc.perform(get("/api/triaje/evaluaciones/" + evaluacionIdA)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenB)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("EVALUACION_NO_ENCONTRADA"));

        // Usuario B intenta finalizar evaluacion de Usuario A -> 404
        mockMvc.perform(post("/api/triaje/evaluaciones/" + evaluacionIdA + "/finalizar")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenB)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("EVALUACION_NO_ENCONTRADA"));
    }
}
