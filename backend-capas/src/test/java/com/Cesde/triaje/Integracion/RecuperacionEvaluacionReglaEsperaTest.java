package com.Cesde.triaje.Integracion;

import com.Cesde.triaje.Modelo.Enumeracion.EstadoEvaluacion;
import com.Cesde.triaje.Modelo.MEvaluacion;
import com.Cesde.triaje.Modelo.MUsuario;
import com.Cesde.triaje.Repositorio.IEvaluacionRepositorio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RecuperacionEvaluacionReglaEsperaTest extends PruebaIntegracionBase {

    @Autowired
    private IEvaluacionRepositorio evaluacionRepositorio;

    private String crearEvaluacionCompletada(String tokenPaciente) throws Exception {
        // Dar consentimiento
        mockMvc.perform(post("/api/consentimientos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenPaciente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("confirmaMayoriaEdad", true))))
                .andExpect(status().isCreated());

        // Iniciar evaluacion
        MvcResult resCrear = mockMvc.perform(post("/api/triaje/evaluaciones")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenPaciente)))
                .andExpect(status().isCreated())
                .andReturn();
        String evaluacionId = leer(resCrear).get("id").asText();

        // Responder PHQ-9 (9 items)
        for (int i = 1; i <= 9; i++) {
            mockMvc.perform(put("/api/triaje/evaluaciones/" + evaluacionId + "/respuestas")
                            .header(HttpHeaders.AUTHORIZATION, bearer(tokenPaciente))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("instrumento", "PHQ9", "indicePregunta", i, "valorPuntaje", 0))))
                    .andExpect(status().isOk());
        }

        // Responder GAD-7 (7 items)
        for (int i = 1; i <= 7; i++) {
            mockMvc.perform(put("/api/triaje/evaluaciones/" + evaluacionId + "/respuestas")
                            .header(HttpHeaders.AUTHORIZATION, bearer(tokenPaciente))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("instrumento", "GAD7", "indicePregunta", i, "valorPuntaje", 0))))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(post("/api/triaje/evaluaciones/" + evaluacionId + "/finalizar")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenPaciente)))
                .andExpect(status().isOk());

        return evaluacionId;
    }

    @Test
    @DisplayName("a) Crear evaluación cuando hay una EN_CURSO devuelve la existente sin duplicar")
    void crearEvaluacionCuandoHayEnCursoDevuelveLaExistente() throws Exception {
        String correo = correoUnico();
        long pacienteId = registrar(correo);
        String token = tokenAcceso(iniciarSesion(correo));

        mockMvc.perform(post("/api/consentimientos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("confirmaMayoriaEdad", true))))
                .andExpect(status().isCreated());

        // Primera llamada: crea la evaluacion
        MvcResult res1 = mockMvc.perform(post("/api/triaje/evaluaciones")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isCreated())
                .andReturn();
        String id1 = leer(res1).get("id").asText();

        // Segunda llamada con evaluación EN_CURSO: devuelve la misma existente
        MvcResult res2 = mockMvc.perform(post("/api/triaje/evaluaciones")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isCreated())
                .andReturn();
        String id2 = leer(res2).get("id").asText();

        assertThat(id2).isEqualTo(id1);
        Integer total = jdbc.queryForObject("SELECT COUNT(*) FROM evaluaciones WHERE usuario_id = ?", Integer.class, pacienteId);
        assertThat(total).isEqualTo(1);
    }

    @Test
    @DisplayName("b) Si hay varias EN_CURSO por datos previos inconsistentes, conserva la más reciente y las demás quedan ABANDONADA")
    void variasEvaluacionesEnCursoConservaLaMasRecienteYAbandonaLasDemas() throws Exception {
        String correo = correoUnico();
        long pacienteId = registrar(correo);
        String token = tokenAcceso(iniciarSesion(correo));
        MUsuario paciente = usuarioRepositorio.findById(pacienteId).orElseThrow();

        mockMvc.perform(post("/api/consentimientos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("confirmaMayoriaEdad", true))))
                .andExpect(status().isCreated());

        Instant base = Instant.now();
        // Insertar 3 evaluaciones EN_CURSO con fechas distintas
        MEvaluacion eAntigua = new MEvaluacion(paciente);
        eAntigua.setFechaCreacion(base.minus(20, ChronoUnit.MINUTES));
        evaluacionRepositorio.save(eAntigua);

        MEvaluacion eIntermedia = new MEvaluacion(paciente);
        eIntermedia.setFechaCreacion(base.minus(10, ChronoUnit.MINUTES));
        evaluacionRepositorio.save(eIntermedia);

        MEvaluacion eReciente = new MEvaluacion(paciente);
        eReciente.setFechaCreacion(base.minus(1, ChronoUnit.MINUTES));
        evaluacionRepositorio.save(eReciente);

        // Llamar a crearEvaluacion
        MvcResult res = mockMvc.perform(post("/api/triaje/evaluaciones")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isCreated())
                .andReturn();
        String idDevuelta = leer(res).get("id").asText();

        assertThat(idDevuelta).isEqualTo(eReciente.getId());

        // Verificar estados en base de datos
        MEvaluacion dbReciente = evaluacionRepositorio.findById(eReciente.getId()).orElseThrow();
        MEvaluacion dbIntermedia = evaluacionRepositorio.findById(eIntermedia.getId()).orElseThrow();
        MEvaluacion dbAntigua = evaluacionRepositorio.findById(eAntigua.getId()).orElseThrow();

        assertThat(dbReciente.getEstado()).isEqualTo(EstadoEvaluacion.EN_CURSO);
        assertThat(dbIntermedia.getEstado()).isEqualTo(EstadoEvaluacion.ABANDONADA);
        assertThat(dbAntigua.getEstado()).isEqualTo(EstadoEvaluacion.ABANDONADA);
    }

    @Test
    @DisplayName("c) Crear evaluación antes de 30 días de la última COMPLETADA falla con 409 y mensaje con fecha legible")
    void crearEvaluacionAntesDe30DiasDeUltimaCompletadaFallaCon409YFechaLegible() throws Exception {
        String correo = correoUnico();
        long pacienteId = registrar(correo);
        String token = tokenAcceso(iniciarSesion(correo));

        // Completar una evaluación
        String evaluacionId = crearEvaluacionCompletada(token);

        // Simular que fue completada hace 10 días
        MEvaluacion eval = evaluacionRepositorio.findById(evaluacionId).orElseThrow();
        Instant fechaFin = Instant.now().minus(10, ChronoUnit.DAYS);
        eval.setFechaFinalizacion(fechaFin);
        evaluacionRepositorio.save(eval);

        Instant fechaEsperada = fechaFin.plus(30, ChronoUnit.DAYS);
        String fechaLegibleEsperada = DateTimeFormatter.ofPattern("dd/MM/yyyy")
                .withZone(ZoneId.systemDefault())
                .format(fechaEsperada);

        // Intentar crear otra evaluación antes de los 30 días
        mockMvc.perform(post("/api/triaje/evaluaciones")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("REGLA_DE_NEGOCIO"))
                .andExpect(jsonPath("$.mensaje", containsString("Debes esperar 30 días entre evaluaciones completadas")))
                .andExpect(jsonPath("$.mensaje", containsString(fechaLegibleEsperada)));
    }

    @Test
    @DisplayName("d) Crear evaluación después de 30 días de la última COMPLETADA procede normalmente")
    void crearEvaluacionDespuesDe30DiasProcedeNormalmente() throws Exception {
        String correo = correoUnico();
        long pacienteId = registrar(correo);
        String token = tokenAcceso(iniciarSesion(correo));

        // Completar una evaluación
        String evaluacionId = crearEvaluacionCompletada(token);

        // Simular que fue completada hace 31 días
        MEvaluacion eval = evaluacionRepositorio.findById(evaluacionId).orElseThrow();
        Instant fechaFin = Instant.now().minus(31, ChronoUnit.DAYS);
        eval.setFechaFinalizacion(fechaFin);
        evaluacionRepositorio.save(eval);

        // Intentar crear nueva evaluación: debe tener éxito (201 Created)
        MvcResult resNueva = mockMvc.perform(post("/api/triaje/evaluaciones")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isCreated())
                .andReturn();

        String nuevoId = leer(resNueva).get("id").asText();
        assertThat(nuevoId).isNotEqualTo(evaluacionId);

        MEvaluacion dbNueva = evaluacionRepositorio.findById(nuevoId).orElseThrow();
        assertThat(dbNueva.getEstado()).isEqualTo(EstadoEvaluacion.EN_CURSO);
    }
}
