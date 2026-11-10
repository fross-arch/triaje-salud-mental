package com.Cesde.triaje.Integracion;

import com.Cesde.triaje.Modelo.Enumeracion.RolUsuario;
import com.Cesde.triaje.Tarea.TTareaProgramada;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GestionCitasTest extends PruebaIntegracionBase {

    @Autowired
    private TTareaProgramada tareaProgramada;

    private String crearEvaluacionCompletada(String tokenPaciente) throws Exception {
        // Otorgar consentimiento
        mockMvc.perform(post("/api/consentimientos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenPaciente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("confirmaMayoriaEdad", true))))
                .andExpect(status().isCreated());

        // Crear evaluacion
        MvcResult resCrear = mockMvc.perform(post("/api/triaje/evaluaciones")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenPaciente)))
                .andExpect(status().isCreated())
                .andReturn();
        String evaluacionId = leer(resCrear).get("id").asText();

        // Responder items PHQ-9
        for (int i = 1; i <= 9; i++) {
            mockMvc.perform(put("/api/triaje/evaluaciones/" + evaluacionId + "/respuestas")
                            .header(HttpHeaders.AUTHORIZATION, bearer(tokenPaciente))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of(
                                    "instrumento", "PHQ9",
                                    "indicePregunta", i,
                                    "valorPuntaje", 0
                            ))))
                    .andExpect(status().isOk());
        }

        // Responder items GAD-7
        for (int i = 1; i <= 7; i++) {
            mockMvc.perform(put("/api/triaje/evaluaciones/" + evaluacionId + "/respuestas")
                            .header(HttpHeaders.AUTHORIZATION, bearer(tokenPaciente))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of(
                                    "instrumento", "GAD7",
                                    "indicePregunta", i,
                                    "valorPuntaje", 0
                            ))))
                    .andExpect(status().isOk());
        }

        // Finalizar
        mockMvc.perform(post("/api/triaje/evaluaciones/" + evaluacionId + "/finalizar")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenPaciente)))
                .andExpect(status().isOk());

        return evaluacionId;
    }

    private void autorizarClinico(String tokenPaciente, String evaluacionId, Long clinicoId) throws Exception {
        mockMvc.perform(post("/api/triaje/evaluaciones/" + evaluacionId + "/autorizaciones")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenPaciente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("clinicoId", clinicoId, "confirmaAutorizacion", true))))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Proponer cita sin autorizacion vigente o con autorizacion revocada falla con 404")
    void proponerCitaSinAutorizacionVigenteOFallaCon404() throws Exception {
        String correoP = correoUnico();
        registrar(correoP);
        String tokenP = tokenAcceso(iniciarSesion(correoP));
        String evaluacionId = crearEvaluacionCompletada(tokenP);

        String correoC = correoUnico();
        Long clinicoId = crearUsuarioDirecto(correoC, "Dra. Clinica Test", RolUsuario.CLINICO, true);
        String tokenC = tokenAcceso(iniciarSesion(correoC));

        Instant fechaFutura = Instant.now().plus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);

        // 1. Sin autorizacion previa -> 404 Not Found
        mockMvc.perform(post("/api/clinico/citas")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenC))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "evaluacionId", evaluacionId,
                                "fechaHora", fechaFutura.toString(),
                                "modalidad", "PRESENCIAL",
                                "mensaje", "Primera sesion de orientacion"
                        ))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("EVALUACION_NO_ENCONTRADA"));

        // 2. Se autoriza y luego se revoca
        autorizarClinico(tokenP, evaluacionId, clinicoId);
        mockMvc.perform(post("/api/triaje/evaluaciones/" + evaluacionId + "/autorizaciones/" + clinicoId + "/revocar")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenP)))
                .andExpect(status().isOk());

        // Con autorizacion revocada -> 404 Not Found
        mockMvc.perform(post("/api/clinico/citas")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenC))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "evaluacionId", evaluacionId,
                                "fechaHora", fechaFutura.toString(),
                                "modalidad", "PRESENCIAL"
                        ))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("EVALUACION_NO_ENCONTRADA"));
    }

    @Test
    @DisplayName("Proponer cita en fecha/hora pasada falla con 400 Bad Request")
    void proponerCitaEnFechaPasadaFallaCon400() throws Exception {
        String correoP = correoUnico();
        registrar(correoP);
        String tokenP = tokenAcceso(iniciarSesion(correoP));
        String evaluacionId = crearEvaluacionCompletada(tokenP);

        String correoC = correoUnico();
        Long clinicoId = crearUsuarioDirecto(correoC, "Dr. Tiempo Pasado", RolUsuario.CLINICO, true);
        String tokenC = tokenAcceso(iniciarSesion(correoC));

        autorizarClinico(tokenP, evaluacionId, clinicoId);

        Instant fechaPasada = Instant.now().minus(1, ChronoUnit.HOURS).truncatedTo(ChronoUnit.SECONDS);

        mockMvc.perform(post("/api/clinico/citas")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenC))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "evaluacionId", evaluacionId,
                                "fechaHora", fechaPasada.toString(),
                                "modalidad", "VIRTUAL"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("DATO_INVALIDO"));
    }

    @Test
    @DisplayName("Dos propuestas de cita del mismo clinico en la misma fecha y hora exacta retornan 409 Conflict")
    void dosPropuestasMismoClinicoMismaFechaRetornan409() throws Exception {
        String correoP1 = correoUnico();
        registrar(correoP1);
        String tokenP1 = tokenAcceso(iniciarSesion(correoP1));
        String evaluacionId1 = crearEvaluacionCompletada(tokenP1);

        String correoP2 = correoUnico();
        registrar(correoP2);
        String tokenP2 = tokenAcceso(iniciarSesion(correoP2));
        String evaluacionId2 = crearEvaluacionCompletada(tokenP2);

        String correoC = correoUnico();
        Long clinicoId = crearUsuarioDirecto(correoC, "Dr. Ocupado", RolUsuario.CLINICO, true);
        String tokenC = tokenAcceso(iniciarSesion(correoC));

        autorizarClinico(tokenP1, evaluacionId1, clinicoId);
        autorizarClinico(tokenP2, evaluacionId2, clinicoId);

        Instant fechaExacta = Instant.now().plus(3, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);

        // Primera propuesta -> 201 Created
        mockMvc.perform(post("/api/clinico/citas")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenC))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "evaluacionId", evaluacionId1,
                                "fechaHora", fechaExacta.toString(),
                                "modalidad", "PRESENCIAL",
                                "mensaje", "Cita 1"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PROPUESTA"));

        // Segunda propuesta en la misma fecha y hora exacta con el mismo clinico -> 409 Conflict
        mockMvc.perform(post("/api/clinico/citas")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenC))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "evaluacionId", evaluacionId2,
                                "fechaHora", fechaExacta.toString(),
                                "modalidad", "VIRTUAL",
                                "mensaje", "Cita 2 solapada"
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("REGLA_DE_NEGOCIO"));
    }

    @Test
    @DisplayName("Un paciente no puede confirmar, rechazar ni cancelar citas de otro paciente (404 Not Found)")
    void pacienteNoPuedeManipularCitasDeOtroPaciente() throws Exception {
        // Paciente 1 y evaluacion 1
        String correoP1 = correoUnico();
        registrar(correoP1);
        String tokenP1 = tokenAcceso(iniciarSesion(correoP1));
        String evaluacionId1 = crearEvaluacionCompletada(tokenP1);

        // Paciente 2
        String correoP2 = correoUnico();
        registrar(correoP2);
        String tokenP2 = tokenAcceso(iniciarSesion(correoP2));

        // Clinico
        String correoC = correoUnico();
        Long clinicoId = crearUsuarioDirecto(correoC, "Dra. Compartida", RolUsuario.CLINICO, true);
        String tokenC = tokenAcceso(iniciarSesion(correoC));

        autorizarClinico(tokenP1, evaluacionId1, clinicoId);

        Instant fecha = Instant.now().plus(4, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);

        // Clinico propone cita para paciente 1
        MvcResult resPropuesta = mockMvc.perform(post("/api/clinico/citas")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenC))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "evaluacionId", evaluacionId1,
                                "fechaHora", fecha.toString(),
                                "modalidad", "VIRTUAL"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        String citaId = leer(resPropuesta).get("id").asText();

        // Paciente 2 intenta confirmar cita de Paciente 1 -> 404
        mockMvc.perform(post("/api/citas/" + citaId + "/confirmar")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenP2)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("CITA_NO_ENCONTRADA"));

        // Paciente 2 intenta rechazar cita de Paciente 1 -> 404
        mockMvc.perform(post("/api/citas/" + citaId + "/rechazar")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenP2)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("CITA_NO_ENCONTRADA"));

        // Paciente 2 intenta cancelar cita de Paciente 1 -> 404
        mockMvc.perform(post("/api/citas/" + citaId + "/cancelar")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenP2)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("CITA_NO_ENCONTRADA"));
    }

    @Test
    @DisplayName("Transiciones invalidas retornan 409 Conflict")
    void transicionesInvalidasRetornan409Conflict() throws Exception {
        String correoP = correoUnico();
        registrar(correoP);
        String tokenP = tokenAcceso(iniciarSesion(correoP));
        String evaluacionId = crearEvaluacionCompletada(tokenP);

        String correoC = correoUnico();
        Long clinicoId = crearUsuarioDirecto(correoC, "Dr. Transiciones", RolUsuario.CLINICO, true);
        String tokenC = tokenAcceso(iniciarSesion(correoC));

        autorizarClinico(tokenP, evaluacionId, clinicoId);

        Instant fecha = Instant.now().plus(5, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);

        MvcResult resPropuesta = mockMvc.perform(post("/api/clinico/citas")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenC))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "evaluacionId", evaluacionId,
                                "fechaHora", fecha.toString(),
                                "modalidad", "PRESENCIAL"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        String citaId = leer(resPropuesta).get("id").asText();

        // Paciente rechaza la cita -> pasa a RECHAZADA
        mockMvc.perform(post("/api/citas/" + citaId + "/rechazar")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenP)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RECHAZADA"));

        // Intentar confirmar una cita RECHAZADA -> 409 Conflict
        mockMvc.perform(post("/api/citas/" + citaId + "/confirmar")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenP)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("REGLA_DE_NEGOCIO"));

        // Intentar cancelar una cita ya RECHAZADA -> 409 Conflict
        mockMvc.perform(post("/api/citas/" + citaId + "/cancelar")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenP)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("REGLA_DE_NEGOCIO"));
    }

    @Test
    @DisplayName("La revocacion de la autorizacion cancela las citas PROPUESTA pero mantiene las CONFIRMADA")
    void revocacionAutorizacionCancelaPropuestasYMantieneConfirmadas() throws Exception {
        String correoP = correoUnico();
        registrar(correoP);
        String tokenP = tokenAcceso(iniciarSesion(correoP));
        String evaluacionId = crearEvaluacionCompletada(tokenP);

        String correoC = correoUnico();
        Long clinicoId1 = crearUsuarioDirecto(correoC, "Dr. Verificador Revocacion 1", RolUsuario.CLINICO, true);
        String tokenC1 = tokenAcceso(iniciarSesion(correoC));

        String correoC2 = correoUnico();
        Long clinicoId2 = crearUsuarioDirecto(correoC2, "Dr. Verificador Revocacion 2", RolUsuario.CLINICO, true);
        String tokenC2 = tokenAcceso(iniciarSesion(correoC2));

        autorizarClinico(tokenP, evaluacionId, clinicoId1);
        autorizarClinico(tokenP, evaluacionId, clinicoId2);

        Instant fecha1 = Instant.now().plus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        Instant fecha2 = Instant.now().plus(3, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);

        // Cita 1: Clinico 1 la propone y el paciente la confirma
        MvcResult res1 = mockMvc.perform(post("/api/clinico/citas")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenC1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "evaluacionId", evaluacionId,
                                "fechaHora", fecha1.toString(),
                                "modalidad", "PRESENCIAL"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        String citaConfirmadaId = leer(res1).get("id").asText();

        mockMvc.perform(post("/api/citas/" + citaConfirmadaId + "/confirmar")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenP)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"));

        // Cita 2: Clinico 2 la propone y permanece en PROPUESTA
        MvcResult res2 = mockMvc.perform(post("/api/clinico/citas")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenC2))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "evaluacionId", evaluacionId,
                                "fechaHora", fecha2.toString(),
                                "modalidad", "VIRTUAL"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        String citaPropuestaId = leer(res2).get("id").asText();

        // Paciente revoca ambas autorizaciones
        mockMvc.perform(post("/api/triaje/evaluaciones/" + evaluacionId + "/autorizaciones/" + clinicoId1 + "/revocar")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenP)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/triaje/evaluaciones/" + evaluacionId + "/autorizaciones/" + clinicoId2 + "/revocar")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenP)))
                .andExpect(status().isOk());

        // Verificar estados en base de datos:
        String estadoCita1 = jdbc.queryForObject("SELECT estado FROM citas WHERE id = ?", String.class, citaConfirmadaId);
        String estadoCita2 = jdbc.queryForObject("SELECT estado FROM citas WHERE id = ?", String.class, citaPropuestaId);

        assertThat(estadoCita1).isEqualTo("CONFIRMADA");
        assertThat(estadoCita2).isEqualTo("CANCELADA");
    }

    @Test
    @DisplayName("La eliminacion de cuenta del paciente o del clinico borra sus citas en cascada")
    void eliminacionDeCuentaBorraCitasEnCascada() throws Exception {
        String correoP = correoUnico();
        long pacienteId = registrar(correoP);
        String tokenP = tokenAcceso(iniciarSesion(correoP));
        String evaluacionId = crearEvaluacionCompletada(tokenP);

        String correoC = correoUnico();
        Long clinicoId = crearUsuarioDirecto(correoC, "Dr. Cascada", RolUsuario.CLINICO, true);
        String tokenC = tokenAcceso(iniciarSesion(correoC));

        autorizarClinico(tokenP, evaluacionId, clinicoId);

        Instant fecha = Instant.now().plus(5, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);

        mockMvc.perform(post("/api/clinico/citas")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenC))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "evaluacionId", evaluacionId,
                                "fechaHora", fecha.toString(),
                                "modalidad", "VIRTUAL"
                        ))))
                .andExpect(status().isCreated());

        Integer citasAntes = jdbc.queryForObject("SELECT COUNT(*) FROM citas WHERE paciente_id = ?", Integer.class, pacienteId);
        assertThat(citasAntes).isEqualTo(1);

        // Paciente con cita activa intenta eliminar su cuenta: se bloquea con 409 por cita activa
        mockMvc.perform(delete("/api/usuarios/yo")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenP)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("REGLA_DE_NEGOCIO"));

        // Se cancela o rechaza la cita antes de eliminar
        String citaId = jdbc.queryForObject("SELECT id FROM citas WHERE paciente_id = ?", String.class, pacienteId);
        mockMvc.perform(post("/api/citas/" + citaId + "/cancelar")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenP)))
                .andExpect(status().isOk());

        // Con la cita cancelada, ahora el paciente elimina su cuenta exitosamente
        mockMvc.perform(delete("/api/usuarios/yo")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenP)))
                .andExpect(status().isNoContent());

        Integer citasDespues = jdbc.queryForObject("SELECT COUNT(*) FROM citas WHERE paciente_id = ?", Integer.class, pacienteId);
        assertThat(citasDespues).isZero();
    }

    @Test
    @DisplayName("Listados de citas devuelven nombres y nunca correos, y la auditoria registra todas las acciones")
    void listadosYAuditoriaDeCitas() throws Exception {
        String correoP = correoUnico();
        long pacienteId = registrar(correoP);
        String tokenP = tokenAcceso(iniciarSesion(correoP));
        String evaluacionId = crearEvaluacionCompletada(tokenP);

        String correoC = correoUnico();
        Long clinicoId = crearUsuarioDirecto(correoC, "Dra. Auditora", RolUsuario.CLINICO, true);
        String tokenC = tokenAcceso(iniciarSesion(correoC));

        autorizarClinico(tokenP, evaluacionId, clinicoId);

        Instant fecha = Instant.now().plus(6, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);

        // 1. Proponer cita (auditoria CITA_PROPUESTA)
        MvcResult resPropuesta = mockMvc.perform(post("/api/clinico/citas")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenC))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "evaluacionId", evaluacionId,
                                "fechaHora", fecha.toString(),
                                "modalidad", "PRESENCIAL",
                                "mensaje", "Consulta de seguimiento"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombrePaciente").value("Persona de Prueba"))
                .andExpect(jsonPath("$.correo").doesNotExist())
                .andReturn();
        String citaId = leer(resPropuesta).get("id").asText();

        // 2. Clinico lista citas -> ve nombrePaciente y nunca correo
        mockMvc.perform(get("/api/clinico/citas")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenC)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombrePaciente").value("Persona de Prueba"))
                .andExpect(jsonPath("$[0].correo").doesNotExist());

        // 3. Paciente lista citas -> ve nombreClinico y nunca correo
        mockMvc.perform(get("/api/citas")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenP)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombreClinico").value("Dra. Auditora"))
                .andExpect(jsonPath("$[0].correo").doesNotExist());

        // 4. Paciente confirma (auditoria CITA_CONFIRMADA)
        mockMvc.perform(post("/api/citas/" + citaId + "/confirmar")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenP)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"));

        // 5. Clinico cancela (auditoria CITA_CANCELADA con motivo)
        mockMvc.perform(post("/api/clinico/citas/" + citaId + "/cancelar")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenC))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("observaciones", "Cancelada por motivo clinico justificado"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"))
                .andExpect(jsonPath("$.observaciones").value("Cancelada por motivo clinico justificado"));

        // Verificar eventos de auditoria en base de datos
        Integer autPropuesta = jdbc.queryForObject(
                "SELECT COUNT(*) FROM registro_auditoria WHERE accion = 'CITA_PROPUESTA' AND usuario_id = ? AND entidad_id = ?",
                Integer.class, clinicoId, citaId);
        assertThat(autPropuesta).isEqualTo(1);

        Integer autConfirmada = jdbc.queryForObject(
                "SELECT COUNT(*) FROM registro_auditoria WHERE accion = 'CITA_CONFIRMADA' AND usuario_id = ? AND entidad_id = ?",
                Integer.class, pacienteId, citaId);
        assertThat(autConfirmada).isEqualTo(1);

        Integer autCancelada = jdbc.queryForObject(
                "SELECT COUNT(*) FROM registro_auditoria WHERE accion = 'CITA_CANCELADA' AND usuario_id = ? AND entidad_id = ?",
                Integer.class, clinicoId, citaId);
        assertThat(autCancelada).isEqualTo(1);
    }

    @Test
    @DisplayName("Tarea programada cancela citas PROPUESTA pasadas")
    void tareaProgramadaCancelaCitasPropuestasPasadas() throws Exception {
        String correoP = correoUnico();
        registrar(correoP);
        String tokenP = tokenAcceso(iniciarSesion(correoP));
        String evaluacionId = crearEvaluacionCompletada(tokenP);

        String correoC = correoUnico();
        Long clinicoId = crearUsuarioDirecto(correoC, "Dr. Scheduler", RolUsuario.CLINICO, true);
        String tokenC = tokenAcceso(iniciarSesion(correoC));

        autorizarClinico(tokenP, evaluacionId, clinicoId);

        Instant fechaFutura = Instant.now().plus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);

        MvcResult res = mockMvc.perform(post("/api/clinico/citas")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenC))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "evaluacionId", evaluacionId,
                                "fechaHora", fechaFutura.toString(),
                                "modalidad", "PRESENCIAL"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        String citaId = leer(res).get("id").asText();

        // Modificar en base de datos la fecha_hora para simular que ya expiro
        Instant fechaPasada = Instant.now().minus(2, ChronoUnit.DAYS);
        jdbc.update("UPDATE citas SET fecha_hora = ? WHERE id = ?", fechaPasada, citaId);

        // Ejecutar la tarea programada
        tareaProgramada.cancelarCitasPropuestasVencidas();

        // Verificar que paso a CANCELADA
        String estadoFinal = jdbc.queryForObject("SELECT estado FROM citas WHERE id = ?", String.class, citaId);
        assertThat(estadoFinal).isEqualTo("CANCELADA");
    }

    @Test
    @DisplayName("Cita única activa: Proponer nueva cita teniendo cita activa (PROPUESTA o CONFIRMADA) con el mismo clínico rechaza con 409 Conflict")
    void citaUnicaActivaRechazaNuevaPropuestaCon409() throws Exception {
        String correoP = correoUnico();
        registrar(correoP);
        String tokenP = tokenAcceso(iniciarSesion(correoP));
        String evaluacionId = crearEvaluacionCompletada(tokenP);

        String correoC = correoUnico();
        Long clinicoId = crearUsuarioDirecto(correoC, "Dra. Cita Unica", RolUsuario.CLINICO, true);
        String tokenC = tokenAcceso(iniciarSesion(correoC));

        autorizarClinico(tokenP, evaluacionId, clinicoId);

        Instant fecha1 = Instant.now().plus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        Instant fecha2 = Instant.now().plus(3, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);

        // 1. Clinico propone primera cita (estado PROPUESTA)
        mockMvc.perform(post("/api/clinico/citas")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenC))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "evaluacionId", evaluacionId,
                                "fechaHora", fecha1.toString(),
                                "modalidad", "PRESENCIAL"
                        ))))
                .andExpect(status().isCreated());

        // 2. Clinico intenta proponer segunda cita con la primera aún en PROPUESTA -> 409 Conflict
        mockMvc.perform(post("/api/clinico/citas")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenC))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "evaluacionId", evaluacionId,
                                "fechaHora", fecha2.toString(),
                                "modalidad", "VIRTUAL"
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("REGLA_DE_NEGOCIO"));
    }

    @Test
    @DisplayName("Transición a CUMPLIDA registra observaciones obligatorias y genera auditoría CITA_CUMPLIDA")
    void cumplirCitaClinicoExitosoYValidaciones() throws Exception {
        String correoP = correoUnico();
        registrar(correoP);
        String tokenP = tokenAcceso(iniciarSesion(correoP));
        String evaluacionId = crearEvaluacionCompletada(tokenP);

        String correoC = correoUnico();
        Long clinicoId = crearUsuarioDirecto(correoC, "Dr. Cumplidor", RolUsuario.CLINICO, true);
        String tokenC = tokenAcceso(iniciarSesion(correoC));

        autorizarClinico(tokenP, evaluacionId, clinicoId);

        Instant fecha = Instant.now().plus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);

        MvcResult res = mockMvc.perform(post("/api/clinico/citas")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenC))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "evaluacionId", evaluacionId,
                                "fechaHora", fecha.toString(),
                                "modalidad", "PRESENCIAL"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        String citaId = leer(res).get("id").asText();

        // 1. Intento de cumplir sin observaciones -> 400 Bad Request
        mockMvc.perform(post("/api/clinico/citas/" + citaId + "/cumplir")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenC))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("observaciones", ""))))
                .andExpect(status().isBadRequest());

        // 2. Cumplir con observaciones clínicas válidas
        mockMvc.perform(post("/api/clinico/citas/" + citaId + "/cumplir")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenC))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("observaciones", "Sesión de valoración inicial completada satisfactoriamente."))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CUMPLIDA"))
                .andExpect(jsonPath("$.observaciones").value("Sesión de valoración inicial completada satisfactoriamente."));

        // 3. Verificar auditoría CITA_CUMPLIDA
        Integer auditCumplida = jdbc.queryForObject(
                "SELECT COUNT(*) FROM registro_auditoria WHERE accion = 'CITA_CUMPLIDA' AND usuario_id = ? AND entidad_id = ?",
                Integer.class, clinicoId, citaId);
        assertThat(auditCumplida).isEqualTo(1);
    }

    @Test
    @DisplayName("Posponer cita valida fecha futura, detecta conflicto de horario (409) y registra auditoría CITA_POSPUESTA")
    void posponerCitaClinicoConConflictoYExito() throws Exception {
        String correoP1 = correoUnico();
        registrar(correoP1);
        String tokenP1 = tokenAcceso(iniciarSesion(correoP1));
        String evaluacionId1 = crearEvaluacionCompletada(tokenP1);

        String correoP2 = correoUnico();
        registrar(correoP2);
        String tokenP2 = tokenAcceso(iniciarSesion(correoP2));
        String evaluacionId2 = crearEvaluacionCompletada(tokenP2);

        String correoC = correoUnico();
        Long clinicoId = crearUsuarioDirecto(correoC, "Dr. Posponedor", RolUsuario.CLINICO, true);
        String tokenC = tokenAcceso(iniciarSesion(correoC));

        autorizarClinico(tokenP1, evaluacionId1, clinicoId);
        autorizarClinico(tokenP2, evaluacionId2, clinicoId);

        Instant fecha1 = Instant.now().plus(3, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        Instant fecha2 = Instant.now().plus(4, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);

        MvcResult res1 = mockMvc.perform(post("/api/clinico/citas")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenC))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "evaluacionId", evaluacionId1,
                                "fechaHora", fecha1.toString(),
                                "modalidad", "PRESENCIAL"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        String citaId1 = leer(res1).get("id").asText();

        // Cita con el paciente 2 en fecha2
        mockMvc.perform(post("/api/clinico/citas")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenC))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "evaluacionId", evaluacionId2,
                                "fechaHora", fecha2.toString(),
                                "modalidad", "PRESENCIAL"
                        ))))
                .andExpect(status().isCreated());

        // 1. Posponer cita1 a fecha pasada -> 400 Bad Request
        mockMvc.perform(put("/api/clinico/citas/" + citaId1 + "/posponer")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenC))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nuevaFechaHora", Instant.now().minus(1, ChronoUnit.HOURS).toString(),
                                "observaciones", "Reprogramación"
                        ))))
                .andExpect(status().isBadRequest());

        // 2. Posponer cita1 a fecha2 (conflicto con cita de paciente 2) -> 409 Conflict
        mockMvc.perform(put("/api/clinico/citas/" + citaId1 + "/posponer")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenC))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nuevaFechaHora", fecha2.toString(),
                                "observaciones", "Intento de reprogramación conflictiva"
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("REGLA_DE_NEGOCIO"));

        // 3. Posponer cita1 a una fecha sin conflicto -> 200 OK
        Instant nuevaFechaLibre = Instant.now().plus(5, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        mockMvc.perform(put("/api/clinico/citas/" + citaId1 + "/posponer")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenC))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nuevaFechaHora", nuevaFechaLibre.toString(),
                                "observaciones", "Reprogramada de común acuerdo"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fechaHora").value(nuevaFechaLibre.toString()));

        Integer auditPospuesta = jdbc.queryForObject(
                "SELECT COUNT(*) FROM registro_auditoria WHERE accion = 'CITA_POSPUESTA' AND usuario_id = ? AND entidad_id = ?",
                Integer.class, clinicoId, citaId1);
        assertThat(auditPospuesta).isEqualTo(1);
    }

    @Test
    @DisplayName("Bloqueo de nueva evaluación: Rechaza con 409 si el paciente tiene cita activa o evaluación compartida esperando atención")
    void bloqueoDeNuevaEvaluacionConCitaActivaOEsperandoAtencion() throws Exception {
        String correoP = correoUnico();
        registrar(correoP);
        String tokenP = tokenAcceso(iniciarSesion(correoP));
        String evaluacionId = crearEvaluacionCompletada(tokenP);

        String correoC = correoUnico();
        Long clinicoId = crearUsuarioDirecto(correoC, "Dr. Bloqueo Evaluacion", RolUsuario.CLINICO, true);
        String tokenC = tokenAcceso(iniciarSesion(correoC));

        autorizarClinico(tokenP, evaluacionId, clinicoId);

        // Paciente tiene evaluación compartida esperando atención (autorización vigente sin cita cumplida)
        // Intentar crear nueva evaluación -> 409 Conflict
        mockMvc.perform(post("/api/triaje/evaluaciones")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenP)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("REGLA_DE_NEGOCIO"));
    }

    @Test
    @DisplayName("Bloqueo de revocación con evaluación en curso: Rechaza con 409 si el paciente tiene evaluación en EN_CURSO")
    void bloqueoDeRevocacionConsentimientoConEvaluacionEnCurso() throws Exception {
        String correoP = correoUnico();
        registrar(correoP);
        String tokenP = tokenAcceso(iniciarSesion(correoP));

        // Dar consentimiento
        mockMvc.perform(post("/api/consentimientos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenP))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("confirmaMayoriaEdad", true))))
                .andExpect(status().isCreated());

        // Iniciar evaluación (queda en EN_CURSO)
        mockMvc.perform(post("/api/triaje/evaluaciones")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenP)))
                .andExpect(status().isCreated());

        // Intentar revocar el consentimiento general mientras está en curso -> 409 Conflict
        mockMvc.perform(post("/api/consentimientos/revocar")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenP)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("REGLA_DE_NEGOCIO"))
                .andExpect(jsonPath("$.mensaje").value("Hay una evaluación en curso; debes finalizarla o cancelarla antes de revocar el consentimiento"));
    }
}
