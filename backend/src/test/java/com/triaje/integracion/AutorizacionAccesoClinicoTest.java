package com.triaje.integracion;

import com.fasterxml.jackson.databind.JsonNode;
import com.triaje.aplicacion.puerto.ClienteReportes;
import com.triaje.aplicacion.puerto.CodificadorContrasenas;
import com.triaje.aplicacion.puerto.RepositorioUsuarios;
import com.triaje.dominio.RolUsuario;
import com.triaje.dominio.modelo.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AutorizacionAccesoClinicoTest extends PruebaIntegracionBase {

    @Autowired
    private RepositorioUsuarios repositorioUsuarios;

    @Autowired
    private CodificadorContrasenas codificador;

    @MockBean
    private ClienteReportes clienteReportes;

    private Long crearUsuarioDirecto(String correo, String nombreCompleto, RolUsuario rol, boolean activo) {
        return crearUsuarioDirecto(correo, nombreCompleto, rol, activo, null);
    }

    private Long crearUsuarioDirecto(String correo, String nombreCompleto, RolUsuario rol, boolean activo, String especialidad) {
        String hash = codificador.codificar(CONTRASENA_VALIDA);
        Usuario usuario = new Usuario(
                null,
                nombreCompleto,
                correo,
                hash,
                rol,
                activo,
                Instant.now(),
                especialidad
        );
        return repositorioUsuarios.guardar(usuario).getId();
    }

    private String crearEvaluacionCompletada(String tokenPaciente, boolean conCrisis) throws Exception {
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

        // Responder 9 items PHQ-9 (item 9 con valor 1 si conCrisis para activar riesgoCrisis)
        for (int i = 1; i <= 9; i++) {
            int valor = (i == 9 && conCrisis) ? 1 : 0;
            mockMvc.perform(put("/api/triaje/evaluaciones/" + evaluacionId + "/respuestas")
                            .header(HttpHeaders.AUTHORIZATION, bearer(tokenPaciente))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of(
                                    "instrumento", "PHQ9",
                                    "indicePregunta", i,
                                    "valorPuntaje", valor
                            ))))
                    .andExpect(status().isOk());
        }

        // Responder 7 items GAD-7
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

    @Test
    @DisplayName("GET /api/clinicos lista únicamente los clínicos activos con id y nombreCompleto")
    void debeListarUnicamenteClinicosActivos() throws Exception {
        String correoPaciente = correoUnico();
        registrar(correoPaciente);
        String tokenPaciente = tokenAcceso(iniciarSesion(correoPaciente));

        String correoActivo = correoUnico();
        Long clinicoActivoId = crearUsuarioDirecto(correoActivo, "Dra. Ana Gomez", RolUsuario.CLINICO, true, "Psicologia Clinica");

        String correoInactivo = correoUnico();
        crearUsuarioDirecto(correoInactivo, "Dr. Carlos Inactivo", RolUsuario.CLINICO, false);

        mockMvc.perform(get("/api/clinicos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenPaciente)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + clinicoActivoId + ")].nombreCompleto").value("Dra. Ana Gomez"))
                .andExpect(jsonPath("$[?(@.id == " + clinicoActivoId + ")].especialidad").value("Psicologia Clinica"))
                .andExpect(jsonPath("$[?(@.nombreCompleto == 'Dr. Carlos Inactivo')]").doesNotExist());
    }

    @Test
    @DisplayName("Un clínico sin autorización hacia una evaluación recibe 404 al consultar su detalle o su PDF")
    void clinicoSinAutorizacionRecibe404EnDetalleYPdf() throws Exception {
        String correoPaciente = correoUnico();
        registrar(correoPaciente);
        String tokenPaciente = tokenAcceso(iniciarSesion(correoPaciente));
        String evaluacionId = crearEvaluacionCompletada(tokenPaciente, false);

        String correoClinico = correoUnico();
        crearUsuarioDirecto(correoClinico, "Dr. Sin Permiso", RolUsuario.CLINICO, true);
        String tokenClinico = tokenAcceso(iniciarSesion(correoClinico));

        // Detalle -> 404
        mockMvc.perform(get("/api/clinico/evaluaciones/" + evaluacionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenClinico)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("EVALUACION_NO_ENCONTRADA"));

        // PDF -> 404
        mockMvc.perform(get("/api/clinico/evaluaciones/" + evaluacionId + "/pdf")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenClinico)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("EVALUACION_NO_ENCONTRADA"));
    }

    @Test
    @DisplayName("Un Clínico B no puede ver ni listar una evaluación autorizada exclusivamente al Clínico A")
    void clinicoBNoPuedeVerNiListarEvaluacionAutorizadaAlClinicoA() throws Exception {
        String correoPaciente = correoUnico();
        registrar(correoPaciente);
        String tokenPaciente = tokenAcceso(iniciarSesion(correoPaciente));
        String evaluacionId = crearEvaluacionCompletada(tokenPaciente, false);

        String correoClinicoA = correoUnico();
        Long clinicoAId = crearUsuarioDirecto(correoClinicoA, "Dr. Clinico A", RolUsuario.CLINICO, true);
        String tokenClinicoA = tokenAcceso(iniciarSesion(correoClinicoA));

        String correoClinicoB = correoUnico();
        crearUsuarioDirecto(correoClinicoB, "Dr. Clinico B", RolUsuario.CLINICO, true);
        String tokenClinicoB = tokenAcceso(iniciarSesion(correoClinicoB));

        // Paciente autoriza exclusivamente al Clinico A
        mockMvc.perform(post("/api/triaje/evaluaciones/" + evaluacionId + "/autorizaciones")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenPaciente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("clinicoId", clinicoAId, "confirmaAutorizacion", true))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.vigente").value(true));

        // Clinico A lista y ve la evaluacion
        mockMvc.perform(get("/api/clinico/evaluaciones")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenClinicoA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.evaluacionId == '" + evaluacionId + "')]").exists());

        // Clinico B lista y NO ve la evaluacion
        mockMvc.perform(get("/api/clinico/evaluaciones")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenClinicoB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.evaluacionId == '" + evaluacionId + "')]").doesNotExist());

        // Clinico B consulta detalle directo -> 404
        mockMvc.perform(get("/api/clinico/evaluaciones/" + evaluacionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenClinicoB)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("EVALUACION_NO_ENCONTRADA"));
    }

    @Test
    @DisplayName("Tras ejecutar la revocación, el clínico pierde acceso de forma inmediata (404 en la siguiente llamada)")
    void clinicoPierdeAccesoInmediatamenteTrasRevocacion() throws Exception {
        String correoPaciente = correoUnico();
        registrar(correoPaciente);
        String tokenPaciente = tokenAcceso(iniciarSesion(correoPaciente));
        String evaluacionId = crearEvaluacionCompletada(tokenPaciente, false);

        String correoClinico = correoUnico();
        Long clinicoId = crearUsuarioDirecto(correoClinico, "Dra. Acceso Temporal", RolUsuario.CLINICO, true);
        String tokenClinico = tokenAcceso(iniciarSesion(correoClinico));

        // Otorgar autorizacion
        mockMvc.perform(post("/api/triaje/evaluaciones/" + evaluacionId + "/autorizaciones")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenPaciente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("clinicoId", clinicoId, "confirmaAutorizacion", true))))
                .andExpect(status().isCreated());

        // Clinico puede acceder al detalle
        mockMvc.perform(get("/api/clinico/evaluaciones/" + evaluacionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenClinico)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evaluacionId").value(evaluacionId))
                .andExpect(jsonPath("$.correo").doesNotExist()) // Nunca debe exponer correo
                .andExpect(jsonPath("$.respuestas", hasSize(16)))
                .andExpect(jsonPath("$.respuestas[0].instrumento").value("PHQ9"))
                .andExpect(jsonPath("$.respuestas[0].indicePregunta").value(1))
                .andExpect(jsonPath("$.respuestas[0].enunciadoPregunta").value("Poco interés o placer en hacer cosas"))
                .andExpect(jsonPath("$.respuestas[0].valorPuntaje").value(0))
                .andExpect(jsonPath("$.respuestas[0].textoFrecuencia").value("Ningún día"))
                .andExpect(jsonPath("$.respuestas[9].instrumento").value("GAD7"))
                .andExpect(jsonPath("$.respuestas[9].indicePregunta").value(1))
                .andExpect(jsonPath("$.respuestas[9].enunciadoPregunta").value("Sentirse nervioso/a, intranquilo/a o con los nervios de punta"))
                .andExpect(jsonPath("$.respuestas[9].valorPuntaje").value(0))
                .andExpect(jsonPath("$.respuestas[9].textoFrecuencia").value("Ningún día"));

        // Paciente revoca autorizacion
        mockMvc.perform(post("/api/triaje/evaluaciones/" + evaluacionId + "/autorizaciones/" + clinicoId + "/revocar")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenPaciente)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vigente").value(false))
                .andExpect(jsonPath("$.estado").value("REVOCADA"));

        // Clinico consulta inmediatamente -> 404
        mockMvc.perform(get("/api/clinico/evaluaciones/" + evaluacionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenClinico)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("EVALUACION_NO_ENCONTRADA"));

        // Tampoco aparece en su listado
        mockMvc.perform(get("/api/clinico/evaluaciones")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenClinico)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.evaluacionId == '" + evaluacionId + "')]").doesNotExist());
    }

    @Test
    @DisplayName("Revocar el consentimiento general del paciente revoca en cascada sus autorizaciones hacia los clínicos")
    void revocarConsentimientoGeneralRevocaEnCascadaAutorizaciones() throws Exception {
        String correoPaciente = correoUnico();
        registrar(correoPaciente);
        String tokenPaciente = tokenAcceso(iniciarSesion(correoPaciente));
        String evaluacionId = crearEvaluacionCompletada(tokenPaciente, false);

        String correoClinico = correoUnico();
        Long clinicoId = crearUsuarioDirecto(correoClinico, "Dr. Clinico Cascada", RolUsuario.CLINICO, true);
        String tokenClinico = tokenAcceso(iniciarSesion(correoClinico));

        // Autorizar clinico
        mockMvc.perform(post("/api/triaje/evaluaciones/" + evaluacionId + "/autorizaciones")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenPaciente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("clinicoId", clinicoId, "confirmaAutorizacion", true))))
                .andExpect(status().isCreated());

        // Confirmar acceso previo
        mockMvc.perform(get("/api/clinico/evaluaciones/" + evaluacionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenClinico)))
                .andExpect(status().isOk());

        // Revocar consentimiento general del paciente
        mockMvc.perform(post("/api/consentimientos/revocar")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenPaciente)))
                .andExpect(status().isOk());

        // El clinico ya no puede acceder (404)
        mockMvc.perform(get("/api/clinico/evaluaciones/" + evaluacionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenClinico)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("EVALUACION_NO_ENCONTRADA"));

        // Comprobar que en la base de datos la autorizacion quedo con fecha_revocacion NOT NULL
        Integer autorizacionesVigentes = jdbc.queryForObject(
                "SELECT COUNT(*) FROM autorizaciones_acceso WHERE evaluacion_id = ? AND fecha_revocacion IS NULL",
                Integer.class, evaluacionId);
        assertThat(autorizacionesVigentes).isZero();
    }

    @Test
    @DisplayName("Intentar autorizar una evaluación ajena o en estado EN_CURSO falla con 400 o 404")
    void debeFallarAlAutorizarEvaluacionAjenaOEnCurso() throws Exception {
        String correoA = correoUnico();
        registrar(correoA);
        String tokenA = tokenAcceso(iniciarSesion(correoA));
        String evaluacionA = crearEvaluacionCompletada(tokenA, false);

        String correoB = correoUnico();
        registrar(correoB);
        String tokenB = tokenAcceso(iniciarSesion(correoB));

        // Paciente B tiene consentimiento
        mockMvc.perform(post("/api/consentimientos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("confirmaMayoriaEdad", true))))
                .andExpect(status().isCreated());

        // Paciente B crea una evaluacion en curso (no completada)
        MvcResult resEnCurso = mockMvc.perform(post("/api/triaje/evaluaciones")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenB)))
                .andExpect(status().isCreated())
                .andReturn();
        String evaluacionEnCursoB = leer(resEnCurso).get("id").asText();

        String correoClinico = correoUnico();
        Long clinicoId = crearUsuarioDirecto(correoClinico, "Dr. Destinatario", RolUsuario.CLINICO, true);

        // 1. Paciente B intenta autorizar la evaluacion de A -> 404
        mockMvc.perform(post("/api/triaje/evaluaciones/" + evaluacionA + "/autorizaciones")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("clinicoId", clinicoId, "confirmaAutorizacion", true))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("EVALUACION_NO_ENCONTRADA"));

        // 2. Paciente B intenta autorizar su propia evaluacion pero que esta EN_CURSO -> 400
        mockMvc.perform(post("/api/triaje/evaluaciones/" + evaluacionEnCursoB + "/autorizaciones")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("clinicoId", clinicoId, "confirmaAutorizacion", true))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("EVALUACION_INCOMPLETA"));
    }

    @Test
    @DisplayName("El paciente conserva la capacidad de consultar y descargar el PDF de su propia evaluación sin necesidad de autorizar a ningún clínico")
    void pacienteConservaCapacidadConsultarYDescargarPdfSinAutorizarClinico() throws Exception {
        byte[] pdfPrueba = "%PDF-1.4 simulated patient report".getBytes();
        when(clienteReportes.generarReportePdf(any())).thenReturn(pdfPrueba);

        String correo = correoUnico();
        registrar(correo);
        String token = tokenAcceso(iniciarSesion(correo));
        String evaluacionId = crearEvaluacionCompletada(token, false);

        // Consultar detalle propio
        mockMvc.perform(get("/api/triaje/evaluaciones/" + evaluacionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evaluacion.id").value(evaluacionId));

        // Descargar PDF propio sin ninguna autorizacion creada
        mockMvc.perform(get("/api/triaje/evaluaciones/" + evaluacionId + "/pdf")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"reporte-cribado.pdf\""))
                .andExpect(result -> assertThat(result.getResponse().getContentAsByteArray()).isEqualTo(pdfPrueba));
    }

    @Test
    @DisplayName("El borrado de cuenta del paciente ('DELETE /api/usuarios/yo') elimina sus autorizaciones en cascada")
    void borradoDeCuentaEliminaAutorizacionesEnCascada() throws Exception {
        String correo = correoUnico();
        long pacienteId = registrar(correo);
        String token = tokenAcceso(iniciarSesion(correo));
        String evaluacionId = crearEvaluacionCompletada(token, false);

        String correoClinico = correoUnico();
        Long clinicoId = crearUsuarioDirecto(correoClinico, "Dra. Clinica FK", RolUsuario.CLINICO, true);

        // Crear autorizacion
        mockMvc.perform(post("/api/triaje/evaluaciones/" + evaluacionId + "/autorizaciones")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("clinicoId", clinicoId, "confirmaAutorizacion", true))))
                .andExpect(status().isCreated());

        // Verificar precondicion en base de datos
        Integer autAntes = jdbc.queryForObject(
                "SELECT COUNT(*) FROM autorizaciones_acceso WHERE paciente_id = ?", Integer.class, pacienteId);
        assertThat(autAntes).isEqualTo(1);

        // Borrar cuenta
        mockMvc.perform(delete("/api/usuarios/yo")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNoContent());

        // Verificar que autorizaciones_acceso no contiene filas del paciente
        Integer autDespues = jdbc.queryForObject(
                "SELECT COUNT(*) FROM autorizaciones_acceso WHERE paciente_id = ?", Integer.class, pacienteId);
        assertThat(autDespues).isZero();
    }

    @Test
    @DisplayName("Volver a autorizar a un clínico reactiva la fila existente actualizando fecha_otorgamiento y fecha_revocacion = NULL")
    void reactivacionDeAutorizacionReutilizaFila() throws Exception {
        String correoPaciente = correoUnico();
        registrar(correoPaciente);
        String tokenPaciente = tokenAcceso(iniciarSesion(correoPaciente));
        String evaluacionId = crearEvaluacionCompletada(tokenPaciente, false);

        String correoClinico = correoUnico();
        Long clinicoId = crearUsuarioDirecto(correoClinico, "Dr. Reautorizado", RolUsuario.CLINICO, true);

        // 1. Otorgar
        MvcResult res1 = mockMvc.perform(post("/api/triaje/evaluaciones/" + evaluacionId + "/autorizaciones")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenPaciente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("clinicoId", clinicoId, "confirmaAutorizacion", true))))
                .andExpect(status().isCreated())
                .andReturn();
        String autId1 = leer(res1).get("id").asText();

        // 2. Revocar
        mockMvc.perform(post("/api/triaje/evaluaciones/" + evaluacionId + "/autorizaciones/" + clinicoId + "/revocar")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenPaciente)))
                .andExpect(status().isOk());

        // 3. Volver a otorgar
        MvcResult res2 = mockMvc.perform(post("/api/triaje/evaluaciones/" + evaluacionId + "/autorizaciones")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenPaciente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("clinicoId", clinicoId, "confirmaAutorizacion", true))))
                .andExpect(status().isCreated())
                .andReturn();
        String autId2 = leer(res2).get("id").asText();

        // Debe ser exactamente el mismo UUID reactivado
        assertThat(autId2).isEqualTo(autId1);

        Integer conteoFilas = jdbc.queryForObject(
                "SELECT COUNT(*) FROM autorizaciones_acceso WHERE evaluacion_id = ? AND clinico_id = ?",
                Integer.class, evaluacionId, clinicoId);
        assertThat(conteoFilas).isEqualTo(1);
    }

    @Test
    @DisplayName("El listado del clínico ordena primero las evaluaciones con riesgoCrisis=true y luego por fecha descendente")
    void listadoClinicoOrdenaPorRiesgoCrisisYFecha() throws Exception {
        String correoClinico = correoUnico();
        Long clinicoId = crearUsuarioDirecto(correoClinico, "Dr. Clasificador", RolUsuario.CLINICO, true);
        String tokenClinico = tokenAcceso(iniciarSesion(correoClinico));

        // Paciente 1: Evaluacion sin crisis
        String correoP1 = correoUnico();
        registrar(correoP1);
        String tokenP1 = tokenAcceso(iniciarSesion(correoP1));
        String evSinCrisis = crearEvaluacionCompletada(tokenP1, false);

        // Paciente 2: Evaluacion con crisis
        String correoP2 = correoUnico();
        registrar(correoP2);
        String tokenP2 = tokenAcceso(iniciarSesion(correoP2));
        String evConCrisis = crearEvaluacionCompletada(tokenP2, true);

        // Autorizar ambas al clinico
        mockMvc.perform(post("/api/triaje/evaluaciones/" + evSinCrisis + "/autorizaciones")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenP1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("clinicoId", clinicoId, "confirmaAutorizacion", true))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/triaje/evaluaciones/" + evConCrisis + "/autorizaciones")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenP2))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("clinicoId", clinicoId, "confirmaAutorizacion", true))))
                .andExpect(status().isCreated());

        // Consultar listado clinico: evConCrisis debe aparecer de primera
        MvcResult resLista = mockMvc.perform(get("/api/clinico/evaluaciones")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenClinico)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode items = leer(resLista);
        assertThat(items.size()).isEqualTo(2);
        assertThat(items.get(0).get("evaluacionId").asText()).isEqualTo(evConCrisis);
        assertThat(items.get(0).get("riesgoCrisis").asBoolean()).isTrue();
        assertThat(items.get(1).get("evaluacionId").asText()).isEqualTo(evSinCrisis);
        assertThat(items.get(1).get("riesgoCrisis").asBoolean()).isFalse();
    }

    @Test
    @DisplayName("El clínico con autorización puede descargar el PDF oficial recibiendo el attachment correcto y registrando auditoría")
    void clinicoConAutorizacionDescargaPdfYAudita() throws Exception {
        byte[] pdfPrueba = "%PDF-1.4 simulated clinical report".getBytes();
        when(clienteReportes.generarReportePdf(any())).thenReturn(pdfPrueba);

        String correoPaciente = correoUnico();
        registrar(correoPaciente);
        String tokenPaciente = tokenAcceso(iniciarSesion(correoPaciente));
        String evaluacionId = crearEvaluacionCompletada(tokenPaciente, false);

        String correoClinico = correoUnico();
        Long clinicoId = crearUsuarioDirecto(correoClinico, "Dr. Descargador", RolUsuario.CLINICO, true);
        String tokenClinico = tokenAcceso(iniciarSesion(correoClinico));

        mockMvc.perform(post("/api/triaje/evaluaciones/" + evaluacionId + "/autorizaciones")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenPaciente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("clinicoId", clinicoId, "confirmaAutorizacion", true))))
                .andExpect(status().isCreated());

        // Descarga clinica
        mockMvc.perform(get("/api/clinico/evaluaciones/" + evaluacionId + "/pdf")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenClinico)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reporte-cribado.pdf"))
                .andExpect(result -> assertThat(result.getResponse().getContentAsByteArray()).isEqualTo(pdfPrueba));

        // Verificar auditoria PDF_DESCARGADO_CLINICO
        Integer auditoriaPdf = jdbc.queryForObject(
                "SELECT COUNT(*) FROM registro_auditoria WHERE accion = 'PDF_DESCARGADO_CLINICO' AND usuario_id = ? AND entidad_id = ?",
                Integer.class, clinicoId, evaluacionId);
        assertThat(auditoriaPdf).isEqualTo(1);
    }

    @Test
    @DisplayName("Restricción de compartir tras revocación: Si un paciente revocó su consentimiento, no puede compartir evaluaciones (409)")
    void compartirEvaluacionTrasRevocarConsentimientoFallaConConflicto() throws Exception {
        String correoPaciente = correoUnico();
        registrar(correoPaciente);
        String tokenPaciente = tokenAcceso(iniciarSesion(correoPaciente));
        String evaluacionId = crearEvaluacionCompletada(tokenPaciente, false);

        String correoClinico = correoUnico();
        Long clinicoId = crearUsuarioDirecto(correoClinico, "Dr. Destino Post-Revocacion", RolUsuario.CLINICO, true);

        // Paciente revoca su consentimiento
        mockMvc.perform(post("/api/consentimientos/revocar")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenPaciente)))
                .andExpect(status().isOk());

        // Intenta compartir la evaluacion -> debe ser rechazada con 409 Conflict (REGLA_DE_NEGOCIO)
        mockMvc.perform(post("/api/triaje/evaluaciones/" + evaluacionId + "/autorizaciones")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenPaciente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("clinicoId", clinicoId, "confirmaAutorizacion", true))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("REGLA_DE_NEGOCIO"));
    }
}
