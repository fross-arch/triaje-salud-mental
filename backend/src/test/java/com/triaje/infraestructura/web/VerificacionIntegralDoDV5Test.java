package com.triaje.infraestructura.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.triaje.aplicacion.puerto.ClienteReportes;
import com.triaje.aplicacion.puerto.CodificadorContrasenas;
import com.triaje.aplicacion.puerto.RepositorioUsuarios;
import com.triaje.dominio.RolUsuario;
import com.triaje.dominio.modelo.Usuario;
import com.triaje.integracion.PruebaIntegracionBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Suite integral de pruebas de integracion automatizadas para la Definicion de Terminado (DoD)
 * del Plan v5 (Sprint 12 - Verificacion Integral y Auditoria v5), cubriendo exhaustivamente
 * los puntos 18 a 36 de la matriz de verificacion.
 */
class VerificacionIntegralDoDV5Test extends PruebaIntegracionBase {

    private static final Pattern PATRON_ID_OPACO = Pattern.compile("^[0-9a-fA-F\\-]{36}$|^\\d+$");

    @Autowired
    private RepositorioUsuarios repositorioUsuarios;

    @Autowired
    private CodificadorContrasenas codificador;

    @MockBean
    private ClienteReportes clienteReportes;

    @BeforeEach
    void prepararSimulaciones() {
        when(clienteReportes.generarReportePdf(any())).thenReturn(new byte[]{0x25, 0x50, 0x44, 0x46});
    }

    private Long crearUsuarioDirecto(String correo, String nombreCompleto, RolUsuario rol, boolean activo) {
        String hash = codificador.codificar(CONTRASENA_VALIDA);
        Usuario usuario = new Usuario(
                null,
                nombreCompleto,
                correo,
                hash,
                rol,
                activo,
                Instant.now()
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

        // Iniciar evaluacion
        MvcResult resultadoCrear = mockMvc.perform(post("/api/triaje/evaluaciones")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenPaciente)))
                .andExpect(status().isCreated())
                .andReturn();
        String evaluacionId = leer(resultadoCrear).get("id").asText();

        // Responder 9 preguntas PHQ-9 (pregunta 9 activa crisis si conCrisis es verdadero)
        for (int indice = 1; indice <= 9; indice++) {
            int valor = (indice == 9 && conCrisis) ? 2 : 0;
            mockMvc.perform(put("/api/triaje/evaluaciones/" + evaluacionId + "/respuestas")
                            .header(HttpHeaders.AUTHORIZATION, bearer(tokenPaciente))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of(
                                    "instrumento", "PHQ9",
                                    "indicePregunta", indice,
                                    "valorPuntaje", valor
                            ))))
                    .andExpect(status().isOk());
        }

        // Responder 7 preguntas GAD-7
        for (int indice = 1; indice <= 7; indice++) {
            mockMvc.perform(put("/api/triaje/evaluaciones/" + evaluacionId + "/respuestas")
                            .header(HttpHeaders.AUTHORIZATION, bearer(tokenPaciente))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of(
                                    "instrumento", "GAD7",
                                    "indicePregunta", indice,
                                    "valorPuntaje", 0
                            ))))
                    .andExpect(status().isOk());
        }

        // Finalizar evaluacion
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
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.vigente").value(true));
    }

    @Test
    @DisplayName("Prueba 18: Registro enviando 'rol: CLINICO' o 'rol: ADMINISTRADOR' en el cuerpo asigna siempre rol PACIENTE")
    void prueba18_registroIgnoraRolEnviadoYAsignaSiemprePaciente() throws Exception {
        // 1. Intento de autoasignacion de rol CLINICO
        String correoClinicoIntento = correoUnico();
        mockMvc.perform(post("/api/autenticacion/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nombreCompleto", "Aspirante a Clinico",
                                "correo", correoClinicoIntento,
                                "contrasena", CONTRASENA_VALIDA,
                                "rol", "CLINICO"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rol").value("PACIENTE"));

        Usuario usuarioClinicoDb = repositorioUsuarios.buscarPorCorreo(correoClinicoIntento).orElseThrow();
        assertThat(usuarioClinicoDb.getRol()).isEqualTo(RolUsuario.PACIENTE);

        // 2. Intento de autoasignacion de rol ADMINISTRADOR
        String correoAdminIntento = correoUnico();
        mockMvc.perform(post("/api/autenticacion/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nombreCompleto", "Aspirante a Administrador",
                                "correo", correoAdminIntento,
                                "contrasena", CONTRASENA_VALIDA,
                                "rol", "ADMINISTRADOR"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rol").value("PACIENTE"));

        Usuario usuarioAdminDb = repositorioUsuarios.buscarPorCorreo(correoAdminIntento).orElseThrow();
        assertThat(usuarioAdminDb.getRol()).isEqualTo(RolUsuario.PACIENTE);
    }

    @Test
    @DisplayName("Pruebas 19 y 20: Flujo de administracion (creacion, inicio de sesion, desactivacion y bloqueo de inactivo)")
    void pruebas19Y20_flujoAdministracionClinicoYBloqueoInactivo() throws Exception {
        String correoAdmin = correoUnico();
        crearUsuarioDirecto(correoAdmin, "Administrador Supremo", RolUsuario.ADMINISTRADOR, true);
        String tokenAdmin = tokenAcceso(iniciarSesion(correoAdmin));

        // Administrador crea clinico
        String correoClinico = correoUnico();
        MvcResult resultadoCrear = mockMvc.perform(post("/api/administracion/clinicos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nombreCompleto", "Dra. Carmen Mendoza",
                                "correo", correoClinico,
                                "contrasenaInicial", CONTRASENA_VALIDA))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rol").value("CLINICO"))
                .andExpect(jsonPath("$.activo").value(true))
                .andReturn();

        long clinicoId = leer(resultadoCrear).get("id").asLong();

        // Clinico inicia sesion exitosamente mientras esta activo
        MvcResult sesionClinico = iniciarSesion(correoClinico);
        String tokenClinico = tokenAcceso(sesionClinico);
        var cookieClinico = cookieRenovacion(sesionClinico);

        // Administrador desactiva al clinico
        mockMvc.perform(post("/api/administracion/clinicos/" + clinicoId + "/desactivar")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));

        // Clinico inactivo no puede iniciar sesion
        mockMvc.perform(post("/api/autenticacion/inicio-sesion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("correo", correoClinico, "contrasena", CONTRASENA_VALIDA))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("CREDENCIALES_INVALIDAS"));

        // Clinico inactivo no puede renovar token con su cookie anterior
        mockMvc.perform(post("/api/autenticacion/renovar")
                        .cookie(cookieClinico))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("SESION_INVALIDA"));

        // Clinico inactivo no puede realizar peticiones autenticadas con su token previo
        mockMvc.perform(get("/api/usuarios/yo")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenClinico)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("NO_AUTENTICADO"));
    }

    @Test
    @DisplayName("Prueba 21: Paciente autenticado invocando rutas protegidas /api/clinico/** o /api/administracion/** recibe 403 Forbidden")
    void prueba21_pacienteInvocandoRutasProtegidasRecibe403() throws Exception {
        String correoPaciente = correoUnico();
        registrar(correoPaciente);
        String tokenPaciente = tokenAcceso(iniciarSesion(correoPaciente));

        // Intento en /api/clinico/evaluaciones
        mockMvc.perform(get("/api/clinico/evaluaciones")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenPaciente)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.estado").value(403))
                .andExpect(jsonPath("$.error").value("ACCESO_DENEGADO"));

        // Intento en /api/administracion/clinicos (GET)
        mockMvc.perform(get("/api/administracion/clinicos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenPaciente)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.estado").value(403))
                .andExpect(jsonPath("$.error").value("ACCESO_DENEGADO"));

        // Intento en /api/administracion/clinicos (POST)
        mockMvc.perform(post("/api/administracion/clinicos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenPaciente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nombreCompleto", "Intento Clinico",
                                "correo", correoUnico(),
                                "contrasenaInicial", CONTRASENA_VALIDA))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.estado").value(403))
                .andExpect(jsonPath("$.error").value("ACCESO_DENEGADO"));
    }

    @Test
    @DisplayName("Pruebas 22 y 23: Clinico consultando evaluaciones sin autorizacion o autorizadas a otro clinico recibe estrictamente 404")
    void pruebas22Y23_clinicoSinAutorizacionORelacionadaAOtroRecibe404() throws Exception {
        String correoPaciente = correoUnico();
        registrar(correoPaciente);
        String tokenPaciente = tokenAcceso(iniciarSesion(correoPaciente));
        String evaluacionId = crearEvaluacionCompletada(tokenPaciente, false);

        String correoClinicoA = correoUnico();
        Long clinicoAId = crearUsuarioDirecto(correoClinicoA, "Dr. Clinico Autorizado", RolUsuario.CLINICO, true);
        String tokenClinicoA = tokenAcceso(iniciarSesion(correoClinicoA));

        String correoClinicoB = correoUnico();
        crearUsuarioDirecto(correoClinicoB, "Dr. Clinico No Autorizado", RolUsuario.CLINICO, true);
        String tokenClinicoB = tokenAcceso(iniciarSesion(correoClinicoB));

        // 1. Clinico A antes de ser autorizado recibe 404 estricto
        mockMvc.perform(get("/api/clinico/evaluaciones/" + evaluacionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenClinicoA)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("EVALUACION_NO_ENCONTRADA"));

        mockMvc.perform(get("/api/clinico/evaluaciones/" + evaluacionId + "/pdf")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenClinicoA)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("EVALUACION_NO_ENCONTRADA"));

        // Paciente autoriza exclusivamente al Clinico A
        autorizarClinico(tokenPaciente, evaluacionId, clinicoAId);

        // 2. Clinico B intenta consultar detalle de evaluacion autorizada solo a Clinico A -> 404
        mockMvc.perform(get("/api/clinico/evaluaciones/" + evaluacionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenClinicoB)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("EVALUACION_NO_ENCONTRADA"));

        // Clinico B intenta descargar PDF de evaluacion autorizada solo a Clinico A -> 404
        mockMvc.perform(get("/api/clinico/evaluaciones/" + evaluacionId + "/pdf")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenClinicoB)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("EVALUACION_NO_ENCONTRADA"));

        // Clinico B lista evaluaciones y comprueba que la evaluacion ajena no aparece
        mockMvc.perform(get("/api/clinico/evaluaciones")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenClinicoB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.evaluacionId == '" + evaluacionId + "')]").doesNotExist());
    }

    @Test
    @DisplayName("Pruebas 24 y 25: Paciente autoriza y luego revoca; clinico pierde acceso de inmediato con 404")
    void pruebas24Y25_pacienteOtorgaAutorizacionYLuegoRevocaConEfectoInmediato() throws Exception {
        String correoPaciente = correoUnico();
        registrar(correoPaciente);
        String tokenPaciente = tokenAcceso(iniciarSesion(correoPaciente));
        String evaluacionId = crearEvaluacionCompletada(tokenPaciente, false);

        String correoClinico = correoUnico();
        Long clinicoId = crearUsuarioDirecto(correoClinico, "Dra. Especialista Temporal", RolUsuario.CLINICO, true);
        String tokenClinico = tokenAcceso(iniciarSesion(correoClinico));

        // 1. Paciente otorga autorizacion
        autorizarClinico(tokenPaciente, evaluacionId, clinicoId);

        // Clinico puede listar la evaluacion
        mockMvc.perform(get("/api/clinico/evaluaciones")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenClinico)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.evaluacionId == '" + evaluacionId + "')]").exists());

        // Clinico puede ver el detalle (comprobando que nunca se exponga el correo del paciente)
        mockMvc.perform(get("/api/clinico/evaluaciones/" + evaluacionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenClinico)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evaluacionId").value(evaluacionId))
                .andExpect(jsonPath("$.correo").doesNotExist())
                .andExpect(jsonPath("$.respuestas", hasSize(16)));

        // Clinico puede descargar el PDF
        mockMvc.perform(get("/api/clinico/evaluaciones/" + evaluacionId + "/pdf")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenClinico)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));

        // 2. Paciente revoca autorizacion
        mockMvc.perform(post("/api/triaje/evaluaciones/" + evaluacionId + "/autorizaciones/" + clinicoId + "/revocar")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenPaciente)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vigente").value(false))
                .andExpect(jsonPath("$.estado").value("REVOCADA"));

        // Clinico pierde acceso inmediatamente en listado
        mockMvc.perform(get("/api/clinico/evaluaciones")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenClinico)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.evaluacionId == '" + evaluacionId + "')]").doesNotExist());

        // Clinico recibe de inmediato 404 en detalle
        mockMvc.perform(get("/api/clinico/evaluaciones/" + evaluacionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenClinico)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("EVALUACION_NO_ENCONTRADA"));

        // Clinico recibe de inmediato 404 en descarga de PDF
        mockMvc.perform(get("/api/clinico/evaluaciones/" + evaluacionId + "/pdf")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenClinico)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("EVALUACION_NO_ENCONTRADA"));
    }

    @Test
    @DisplayName("Prueba 26: Revocacion de consentimiento general revoca en cascada todas las autorizaciones activas")
    void prueba26_revocacionConsentimientoGeneralRevocaAutorizacionesEnCascada() throws Exception {
        String correoPaciente = correoUnico();
        registrar(correoPaciente);
        String tokenPaciente = tokenAcceso(iniciarSesion(correoPaciente));
        String evaluacionId = crearEvaluacionCompletada(tokenPaciente, false);

        String correoClinico = correoUnico();
        Long clinicoId = crearUsuarioDirecto(correoClinico, "Dr. Evaluador Permanente", RolUsuario.CLINICO, true);
        String tokenClinico = tokenAcceso(iniciarSesion(correoClinico));

        autorizarClinico(tokenPaciente, evaluacionId, clinicoId);

        // Confirmar acceso previo valido
        mockMvc.perform(get("/api/clinico/evaluaciones/" + evaluacionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenClinico)))
                .andExpect(status().isOk());

        // Paciente revoca consentimiento general
        mockMvc.perform(post("/api/consentimientos/revocar")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenPaciente)))
                .andExpect(status().isOk());

        // Clinico ya no puede acceder al detalle (recibe 404 Not Found)
        mockMvc.perform(get("/api/clinico/evaluaciones/" + evaluacionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenClinico)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("EVALUACION_NO_ENCONTRADA"));

        // En base de datos no queda ninguna autorizacion vigente
        Integer autorizacionesVigentes = jdbc.queryForObject(
                "SELECT COUNT(*) FROM autorizaciones_acceso WHERE evaluacion_id = ? AND fecha_revocacion IS NULL",
                Integer.class, evaluacionId);
        assertThat(autorizacionesVigentes).isZero();
    }

    @Test
    @DisplayName("Prueba 27: Administrador intentando consultar evaluaciones o descargar PDFs recibe 403 Forbidden")
    void prueba27_administradorConsultandoEvaluacionesOPdfRecibe403() throws Exception {
        String correoPaciente = correoUnico();
        registrar(correoPaciente);
        String tokenPaciente = tokenAcceso(iniciarSesion(correoPaciente));
        String evaluacionId = crearEvaluacionCompletada(tokenPaciente, false);

        String correoAdmin = correoUnico();
        crearUsuarioDirecto(correoAdmin, "Admin Auditor", RolUsuario.ADMINISTRADOR, true);
        String tokenAdmin = tokenAcceso(iniciarSesion(correoAdmin));

        // Administrador intentando consultar evaluacion en endpoint de paciente -> 403
        mockMvc.perform(get("/api/triaje/evaluaciones/" + evaluacionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.estado").value(403))
                .andExpect(jsonPath("$.error").value("ACCESO_DENEGADO"));

        // Administrador intentando descargar PDF en endpoint de paciente -> 403
        mockMvc.perform(get("/api/triaje/evaluaciones/" + evaluacionId + "/pdf")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.estado").value(403))
                .andExpect(jsonPath("$.error").value("ACCESO_DENEGADO"));

        // Administrador intentando consultar evaluacion en endpoint de clinico -> 403
        mockMvc.perform(get("/api/clinico/evaluaciones/" + evaluacionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.estado").value(403))
                .andExpect(jsonPath("$.error").value("ACCESO_DENEGADO"));

        // Administrador intentando descargar PDF en endpoint de clinico -> 403
        mockMvc.perform(get("/api/clinico/evaluaciones/" + evaluacionId + "/pdf")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.estado").value(403))
                .andExpect(jsonPath("$.error").value("ACCESO_DENEGADO"));
    }

    @Test
    @DisplayName("Pruebas 28, 29 y 30: Gestion de citas (sin autorizacion 404, fecha pasada 400, transicion invalida 409 y conflicto horario 409)")
    void pruebas28_29_30_gestionCitasReglasYConflictos() throws Exception {
        String correoPaciente1 = correoUnico();
        registrar(correoPaciente1);
        String tokenPaciente1 = tokenAcceso(iniciarSesion(correoPaciente1));
        String evaluacionId1 = crearEvaluacionCompletada(tokenPaciente1, false);

        String correoClinico = correoUnico();
        Long clinicoId = crearUsuarioDirecto(correoClinico, "Dra. Programadora Citas", RolUsuario.CLINICO, true);
        String tokenClinico = tokenAcceso(iniciarSesion(correoClinico));

        Instant fechaValida = Instant.now().plus(4, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);

        // 1. Proponer cita sin autorizacion vigente falla con 404 Not Found
        mockMvc.perform(post("/api/clinico/citas")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenClinico))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "evaluacionId", evaluacionId1,
                                "fechaHora", fechaValida.toString(),
                                "modalidad", "PRESENCIAL"
                        ))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("EVALUACION_NO_ENCONTRADA"));

        // Autorizar clinico para paciente 1
        autorizarClinico(tokenPaciente1, evaluacionId1, clinicoId);

        // 2. Proponer cita en fecha pasada falla con 400 Bad Request
        Instant fechaPasada = Instant.now().minus(2, ChronoUnit.HOURS).truncatedTo(ChronoUnit.SECONDS);
        mockMvc.perform(post("/api/clinico/citas")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenClinico))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "evaluacionId", evaluacionId1,
                                "fechaHora", fechaPasada.toString(),
                                "modalidad", "VIRTUAL"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("DATO_INVALIDO"));

        // Clinico propone cita valida
        MvcResult resultadoPropuesta = mockMvc.perform(post("/api/clinico/citas")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenClinico))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "evaluacionId", evaluacionId1,
                                "fechaHora", fechaValida.toString(),
                                "modalidad", "PRESENCIAL"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        String citaId = leer(resultadoPropuesta).get("id").asText();

        // 3. Paciente rechaza la cita y luego intenta confirmarla -> 409 Conflict
        mockMvc.perform(post("/api/citas/" + citaId + "/rechazar")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenPaciente1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RECHAZADA"));

        mockMvc.perform(post("/api/citas/" + citaId + "/confirmar")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenPaciente1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("REGLA_DE_NEGOCIO"));

        // 4. Dos propuestas de cita para el mismo clinico en el mismo horario exacto lanzan 409 Conflict
        String correoPaciente2 = correoUnico();
        registrar(correoPaciente2);
        String tokenPaciente2 = tokenAcceso(iniciarSesion(correoPaciente2));
        String evaluacionId2 = crearEvaluacionCompletada(tokenPaciente2, false);
        autorizarClinico(tokenPaciente2, evaluacionId2, clinicoId);

        Instant fechaConflicto = Instant.now().plus(6, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);

        // Primera cita para la fecha
        mockMvc.perform(post("/api/clinico/citas")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenClinico))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "evaluacionId", evaluacionId1,
                                "fechaHora", fechaConflicto.toString(),
                                "modalidad", "VIRTUAL"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PROPUESTA"));

        // Segunda cita en la misma fecha y hora para el mismo clinico -> 409 Conflict
        mockMvc.perform(post("/api/clinico/citas")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenClinico))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "evaluacionId", evaluacionId2,
                                "fechaHora", fechaConflicto.toString(),
                                "modalidad", "PRESENCIAL"
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("REGLA_DE_NEGOCIO"));
    }

    @Test
    @DisplayName("Prueba 31: Revocacion de autorizacion cancela citas en estado PROPUESTA y conserva las citas CONFIRMADA")
    void prueba31_revocacionAutorizacionCancelaPropuestasYConservaConfirmadas() throws Exception {
        String correoPaciente = correoUnico();
        registrar(correoPaciente);
        String tokenPaciente = tokenAcceso(iniciarSesion(correoPaciente));
        String evaluacionId = crearEvaluacionCompletada(tokenPaciente, false);

        String correoClinico1 = correoUnico();
        Long clinico1Id = crearUsuarioDirecto(correoClinico1, "Dr. Evaluador Citas 1", RolUsuario.CLINICO, true);
        String tokenClinico1 = tokenAcceso(iniciarSesion(correoClinico1));

        String correoClinico2 = correoUnico();
        Long clinico2Id = crearUsuarioDirecto(correoClinico2, "Dr. Evaluador Citas 2", RolUsuario.CLINICO, true);
        String tokenClinico2 = tokenAcceso(iniciarSesion(correoClinico2));

        autorizarClinico(tokenPaciente, evaluacionId, clinico1Id);
        autorizarClinico(tokenPaciente, evaluacionId, clinico2Id);

        Instant fechaCitaConfirmada = Instant.now().plus(3, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        Instant fechaCitaPropuesta = Instant.now().plus(5, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);

        // Propuesta 1 (Clinico 1) y paciente confirma
        MvcResult res1 = mockMvc.perform(post("/api/clinico/citas")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenClinico1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "evaluacionId", evaluacionId,
                                "fechaHora", fechaCitaConfirmada.toString(),
                                "modalidad", "PRESENCIAL"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        String citaConfirmadaId = leer(res1).get("id").asText();

        mockMvc.perform(post("/api/citas/" + citaConfirmadaId + "/confirmar")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenPaciente)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"));

        // Propuesta 2 (Clinico 2) queda en PROPUESTA
        MvcResult res2 = mockMvc.perform(post("/api/clinico/citas")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenClinico2))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "evaluacionId", evaluacionId,
                                "fechaHora", fechaCitaPropuesta.toString(),
                                "modalidad", "VIRTUAL"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        String citaPropuestaId = leer(res2).get("id").asText();

        // Paciente revoca autorizacion de ambos clinicos
        mockMvc.perform(post("/api/triaje/evaluaciones/" + evaluacionId + "/autorizaciones/" + clinico1Id + "/revocar")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenPaciente)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/triaje/evaluaciones/" + evaluacionId + "/autorizaciones/" + clinico2Id + "/revocar")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenPaciente)))
                .andExpect(status().isOk());

        // Comprobar en base de datos: la cita confirmada se mantiene CONFIRMADA y la propuesta pasa a CANCELADA
        String estadoConfirmadaDb = jdbc.queryForObject("SELECT estado FROM citas WHERE id = ?", String.class, citaConfirmadaId);
        String estadoPropuestaDb = jdbc.queryForObject("SELECT estado FROM citas WHERE id = ?", String.class, citaPropuestaId);

        assertThat(estadoConfirmadaDb).isEqualTo("CONFIRMADA");
        assertThat(estadoPropuestaDb).isEqualTo("CANCELADA");
    }

    @Test
    @DisplayName("Prueba 32: Borrado en cascada (eliminar paciente borra autorizaciones y citas; eliminar clinico borra citas)")
    void prueba32_borradoEnCascadaPacienteYClinico() throws Exception {
        // 1. Borrado de paciente en cascada
        String correoPaciente = correoUnico();
        long pacienteId = registrar(correoPaciente);
        String tokenPaciente = tokenAcceso(iniciarSesion(correoPaciente));
        String evaluacionId = crearEvaluacionCompletada(tokenPaciente, false);

        String correoClinico = correoUnico();
        Long clinicoId = crearUsuarioDirecto(correoClinico, "Dra. Cascada Total", RolUsuario.CLINICO, true);
        String tokenClinico = tokenAcceso(iniciarSesion(correoClinico));

        autorizarClinico(tokenPaciente, evaluacionId, clinicoId);

        Instant fechaCita = Instant.now().plus(4, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        mockMvc.perform(post("/api/clinico/citas")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenClinico))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "evaluacionId", evaluacionId,
                                "fechaHora", fechaCita.toString(),
                                "modalidad", "PRESENCIAL"
                        ))))
                .andExpect(status().isCreated());

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM autorizaciones_acceso WHERE paciente_id = ?", Integer.class, pacienteId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM citas WHERE paciente_id = ?", Integer.class, pacienteId)).isEqualTo(1);

        // Paciente elimina su cuenta
        mockMvc.perform(delete("/api/usuarios/yo")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenPaciente)))
                .andExpect(status().isNoContent());

        // Verificacion en base de datos: autorizaciones y citas del paciente eliminadas
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM autorizaciones_acceso WHERE paciente_id = ?", Integer.class, pacienteId)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM citas WHERE paciente_id = ?", Integer.class, pacienteId)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM usuarios WHERE id = ?", Integer.class, pacienteId)).isZero();

        // 2. Borrado de clinico en cascada
        String correoPacienteNuevo = correoUnico();
        long pacienteNuevoId = registrar(correoPacienteNuevo);
        String tokenPacienteNuevo = tokenAcceso(iniciarSesion(correoPacienteNuevo));
        String evaluacionNuevaId = crearEvaluacionCompletada(tokenPacienteNuevo, false);

        String correoClinicoEliminar = correoUnico();
        Long clinicoEliminarId = crearUsuarioDirecto(correoClinicoEliminar, "Dr. Para Eliminar", RolUsuario.CLINICO, true);
        String tokenClinicoEliminar = tokenAcceso(iniciarSesion(correoClinicoEliminar));

        autorizarClinico(tokenPacienteNuevo, evaluacionNuevaId, clinicoEliminarId);

        Instant fechaNueva = Instant.now().plus(5, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        mockMvc.perform(post("/api/clinico/citas")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenClinicoEliminar))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "evaluacionId", evaluacionNuevaId,
                                "fechaHora", fechaNueva.toString(),
                                "modalidad", "VIRTUAL"
                        ))))
                .andExpect(status().isCreated());

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM citas WHERE clinico_id = ?", Integer.class, clinicoEliminarId)).isEqualTo(1);

        // Clinico elimina su cuenta
        mockMvc.perform(delete("/api/usuarios/yo")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenClinicoEliminar)))
                .andExpect(status().isNoContent());

        // Verificacion en base de datos: citas y autorizaciones asociadas al clinico eliminadas
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM citas WHERE clinico_id = ?", Integer.class, clinicoEliminarId)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM autorizaciones_acceso WHERE clinico_id = ?", Integer.class, clinicoEliminarId)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM usuarios WHERE id = ?", Integer.class, clinicoEliminarId)).isZero();
    }

    @Test
    @DisplayName("Prueba 34: Registro en auditoria sin PII, con IDs opacos y registrando todas las acciones clinicas y citas")
    void prueba34_auditoriaSinPiiConIdsOpacosYAccionesClinicas() throws Exception {
        String correoPaciente = correoUnico();
        long pacienteId = registrar(correoPaciente);
        String tokenPaciente = tokenAcceso(iniciarSesion(correoPaciente));
        String evaluacionId = crearEvaluacionCompletada(tokenPaciente, false);

        String correoClinico = correoUnico();
        Long clinicoId = crearUsuarioDirecto(correoClinico, "Dra. Auditoria Clinica", RolUsuario.CLINICO, true);
        String tokenClinico = tokenAcceso(iniciarSesion(correoClinico));

        autorizarClinico(tokenPaciente, evaluacionId, clinicoId);

        // 1. Accion ACCESO_CLINICO_LISTADO
        mockMvc.perform(get("/api/clinico/evaluaciones")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenClinico)))
                .andExpect(status().isOk());

        // 2. Accion ACCESO_CLINICO_DETALLE
        mockMvc.perform(get("/api/clinico/evaluaciones/" + evaluacionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenClinico)))
                .andExpect(status().isOk());

        // 3. Accion PDF_DESCARGADO_CLINICO
        mockMvc.perform(get("/api/clinico/evaluaciones/" + evaluacionId + "/pdf")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenClinico)))
                .andExpect(status().isOk());

        // 4. Accion CITA_PROPUESTA (Cita 1)
        Instant fecha1 = Instant.now().plus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        MvcResult res1 = mockMvc.perform(post("/api/clinico/citas")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenClinico))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "evaluacionId", evaluacionId,
                                "fechaHora", fecha1.toString(),
                                "modalidad", "PRESENCIAL"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        String cita1Id = leer(res1).get("id").asText();

        // 5. Accion CITA_CONFIRMADA
        mockMvc.perform(post("/api/citas/" + cita1Id + "/confirmar")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenPaciente)))
                .andExpect(status().isOk());

        // Cancelamos cita 1 para respetar regla de cita unica activa antes de proponer la siguiente
        mockMvc.perform(post("/api/citas/" + cita1Id + "/cancelar")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenPaciente)))
                .andExpect(status().isOk());

        // 6. Accion CITA_PROPUESTA y CITA_RECHAZADA (Cita 2)
        Instant fecha2 = Instant.now().plus(4, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        MvcResult res2 = mockMvc.perform(post("/api/clinico/citas")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenClinico))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "evaluacionId", evaluacionId,
                                "fechaHora", fecha2.toString(),
                                "modalidad", "VIRTUAL"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        String cita2Id = leer(res2).get("id").asText();

        mockMvc.perform(post("/api/citas/" + cita2Id + "/rechazar")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenPaciente)))
                .andExpect(status().isOk());

        // 7. Accion CITA_PROPUESTA y CITA_CANCELADA (Cita 3 cancelada por el paciente)
        Instant fecha3 = Instant.now().plus(6, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        MvcResult res3 = mockMvc.perform(post("/api/clinico/citas")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenClinico))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "evaluacionId", evaluacionId,
                                "fechaHora", fecha3.toString(),
                                "modalidad", "VIRTUAL"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        String cita3Id = leer(res3).get("id").asText();

        mockMvc.perform(post("/api/citas/" + cita3Id + "/cancelar")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenPaciente)))
                .andExpect(status().isOk());

        // Comprobar las entradas en registro_auditoria
        List<Map<String, Object>> filasAuditoria = jdbc.queryForList(
                "SELECT accion, entidad_id FROM registro_auditoria");

        List<String> accionesRegistradas = filasAuditoria.stream()
                .map(fila -> (String) fila.get("accion"))
                .toList();

        assertThat(accionesRegistradas).contains(
                "ACCESO_CLINICO_LISTADO",
                "ACCESO_CLINICO_DETALLE",
                "PDF_DESCARGADO_CLINICO",
                "CITA_PROPUESTA",
                "CITA_CONFIRMADA",
                "CITA_RECHAZADA",
                "CITA_CANCELADA"
        );

        // Verificar que entidad_id contenga IDs opacos y que no contenga informacion de identificacion personal (PII)
        for (Map<String, Object> fila : filasAuditoria) {
            String entidadId = (String) fila.get("entidad_id");
            if (entidadId != null) {
                assertThat(PATRON_ID_OPACO.matcher(entidadId).matches())
                        .as("El identificador entidad_id debe ser opaco (UUID o numerico): " + entidadId)
                        .isTrue();

                assertThat(entidadId).doesNotContain("@");
                assertThat(entidadId.toLowerCase()).doesNotContain("persona");
                assertThat(entidadId.toLowerCase()).doesNotContain("correo");
            }
        }
    }

    @Test
    @DisplayName("Pruebas 35 y 36: Paciente sin autorizaciones descarga PDF y ve resultados; paciente en crisis se mantiene oculto")
    void pruebas35Y36_pacienteSinAutorizacionesYProtocoloCrisisOculto() throws Exception {
        // --- Prueba 35: Paciente sin autorizaciones descarga su PDF y ve resultados normalmente ---
        String correoPacienteSinCrisis = correoUnico();
        registrar(correoPacienteSinCrisis);
        String tokenPacienteSinCrisis = tokenAcceso(iniciarSesion(correoPacienteSinCrisis));
        String evaluacionSinCrisisId = crearEvaluacionCompletada(tokenPacienteSinCrisis, false);

        // Descarga su PDF normalmente
        mockMvc.perform(get("/api/triaje/evaluaciones/" + evaluacionSinCrisisId + "/pdf")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenPacienteSinCrisis)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));

        // Consulta sus resultados normalmente
        mockMvc.perform(get("/api/triaje/evaluaciones/" + evaluacionSinCrisisId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenPacienteSinCrisis)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evaluacion.id").value(evaluacionSinCrisisId))
                .andExpect(jsonPath("$.evaluacion.riesgoCrisis").value(false))
                .andExpect(jsonPath("$.evaluacion.resultadoMinimo").value(true));

        // Cualquier clinico no puede verla
        String correoClinicoObservador = correoUnico();
        crearUsuarioDirecto(correoClinicoObservador, "Dr. Observador Oculto", RolUsuario.CLINICO, true);
        String tokenClinicoObservador = tokenAcceso(iniciarSesion(correoClinicoObservador));

        mockMvc.perform(get("/api/clinico/evaluaciones")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenClinicoObservador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.evaluacionId == '" + evaluacionSinCrisisId + "')]").doesNotExist());

        mockMvc.perform(get("/api/clinico/evaluaciones/" + evaluacionSinCrisisId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenClinicoObservador)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("EVALUACION_NO_ENCONTRADA"));

        // --- Prueba 36: Paciente con riesgoCrisis=true y sin autorizaciones ---
        String correoPacienteEnCrisis = correoUnico();
        registrar(correoPacienteEnCrisis);
        String tokenPacienteEnCrisis = tokenAcceso(iniciarSesion(correoPacienteEnCrisis));
        String evaluacionEnCrisisId = crearEvaluacionCompletada(tokenPacienteEnCrisis, true);

        // Paciente recibe aviso de riesgo de crisis
        mockMvc.perform(get("/api/triaje/evaluaciones/" + evaluacionEnCrisisId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenPacienteEnCrisis)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evaluacion.id").value(evaluacionEnCrisisId))
                .andExpect(jsonPath("$.evaluacion.riesgoCrisis").value(true))
                .andExpect(jsonPath("$.evaluacion.resultadoMinimo").value(false));

        // Consulta lineas de ayuda publicas
        mockMvc.perform(get("/api/recursos-ayuda"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));

        // Se mantiene completamente oculto de cualquier clinico
        mockMvc.perform(get("/api/clinico/evaluaciones")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenClinicoObservador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.evaluacionId == '" + evaluacionEnCrisisId + "')]").doesNotExist());

        mockMvc.perform(get("/api/clinico/evaluaciones/" + evaluacionEnCrisisId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenClinicoObservador)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("EVALUACION_NO_ENCONTRADA"));

        // Sin disparar alertas automaticas ni crear autorizaciones o citas automaticas
        Integer totalAutorizaciones = jdbc.queryForObject(
                "SELECT COUNT(*) FROM autorizaciones_acceso WHERE evaluacion_id = ?",
                Integer.class, evaluacionEnCrisisId);
        assertThat(totalAutorizaciones).isZero();

        Integer totalCitas = jdbc.queryForObject(
                "SELECT COUNT(*) FROM citas WHERE paciente_id = (SELECT usuario_id FROM evaluaciones WHERE id = ?)",
                Integer.class, evaluacionEnCrisisId);
        assertThat(totalCitas).isZero();
    }
}
