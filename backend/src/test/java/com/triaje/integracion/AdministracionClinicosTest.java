package com.triaje.integracion;

import com.triaje.aplicacion.puerto.CodificadorContrasenas;
import com.triaje.aplicacion.puerto.RepositorioUsuarios;
import com.triaje.dominio.RolUsuario;
import com.triaje.dominio.modelo.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdministracionClinicosTest extends PruebaIntegracionBase {

    @Autowired
    private RepositorioUsuarios repositorioUsuarios;

    @Autowired
    private CodificadorContrasenas codificador;

    private Long crearUsuarioDirecto(String correo, RolUsuario rol, boolean activo) {
        String hash = codificador.codificar(CONTRASENA_VALIDA);
        Usuario usuario = new Usuario(
                null,
                "Usuario " + rol.name(),
                correo,
                hash,
                rol,
                activo,
                Instant.now()
        );
        return repositorioUsuarios.guardar(usuario).getId();
    }

    @Test
    @DisplayName("Un registro publico que envie rol CLINICO debe guardarse siempre como PACIENTE")
    void debeIgnorarRolEnRegistroPublicoYGuardarComoPaciente() throws Exception {
        String correo = correoUnico();

        mockMvc.perform(post("/api/autenticacion/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nombreCompleto", "Solicitante Astuto",
                                "correo", correo,
                                "contrasena", CONTRASENA_VALIDA,
                                "rol", "CLINICO"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rol").value("PACIENTE"));

        Usuario guardado = repositorioUsuarios.buscarPorCorreo(correo).orElseThrow();
        assertThat(guardado.getRol()).isEqualTo(RolUsuario.PACIENTE);
        assertThat(guardado.isActivo()).isTrue();
    }

    @Test
    @DisplayName("Un usuario con rol PACIENTE que invoque /api/administracion/** o /api/clinico/** debe recibir 403 Forbidden")
    void debeRechazarCon403AccesoDePacienteAAdministracionOClinico() throws Exception {
        String correo = correoUnico();
        registrar(correo);
        MvcResult login = iniciarSesion(correo);
        String tokenPaciente = tokenAcceso(login);

        // Intento en administracion
        mockMvc.perform(get("/api/administracion/clinicos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenPaciente)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.estado").value(403))
                .andExpect(jsonPath("$.error").value("ACCESO_DENEGADO"));

        mockMvc.perform(post("/api/administracion/clinicos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenPaciente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nombreCompleto", "Nuevo Clinico",
                                "correo", correoUnico(),
                                "contrasenaInicial", CONTRASENA_VALIDA))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.estado").value(403))
                .andExpect(jsonPath("$.error").value("ACCESO_DENEGADO"));

        // Intento en clinico
        mockMvc.perform(get("/api/clinico/evaluaciones")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenPaciente)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.estado").value(403))
                .andExpect(jsonPath("$.error").value("ACCESO_DENEGADO"));
    }

    @Test
    @DisplayName("Un usuario CLINICO desactivado no puede iniciar sesion, ni renovar token, ni hacer peticiones autenticadas")
    void clinicoDesactivadoNoPuedeIniciarSesionNiRenovarToken() throws Exception {
        String correoAdmin = correoUnico();
        Long adminId = crearUsuarioDirecto(correoAdmin, RolUsuario.ADMINISTRADOR, true);
        MvcResult loginAdmin = iniciarSesion(correoAdmin);
        String tokenAdmin = tokenAcceso(loginAdmin);

        String correoClinico = correoUnico();
        MvcResult resCrear = mockMvc.perform(post("/api/administracion/clinicos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nombreCompleto", "Dra. Lucia Ramirez",
                                "correo", correoClinico,
                                "contrasenaInicial", CONTRASENA_VALIDA))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rol").value("CLINICO"))
                .andExpect(jsonPath("$.activo").value(true))
                .andReturn();

        long clinicoId = leer(resCrear).get("id").asLong();

        // 1. El clínico inicia sesión exitosamente mientras está activo
        MvcResult loginClinico = iniciarSesion(correoClinico);
        String tokenClinico = tokenAcceso(loginClinico);
        var cookieClinico = cookieRenovacion(loginClinico);

        // 2. El administrador desactiva al clínico
        mockMvc.perform(post("/api/administracion/clinicos/" + clinicoId + "/desactivar")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));

        // 3. El clínico desactivado no puede iniciar sesión
        mockMvc.perform(post("/api/autenticacion/inicio-sesion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("correo", correoClinico, "contrasena", CONTRASENA_VALIDA))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("CREDENCIALES_INVALIDAS"));

        // 4. El clínico desactivado no puede renovar token con su cookie anterior
        mockMvc.perform(post("/api/autenticacion/renovar")
                        .cookie(cookieClinico))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("SESION_INVALIDA"));

        // 5. El clínico desactivado no puede usar su token anterior en peticiones autenticadas
        mockMvc.perform(get("/api/usuarios/yo")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenClinico)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("NO_AUTENTICADO"));

        // 6. El administrador reactiva al clínico
        mockMvc.perform(post("/api/administracion/clinicos/" + clinicoId + "/activar")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(true));

        // 7. El clínico activado puede volver a iniciar sesión
        MvcResult nuevoLogin = iniciarSesion(correoClinico);
        assertThat(tokenAcceso(nuevoLogin)).isNotBlank();
    }

    @Test
    @DisplayName("Un ADMINISTRADOR no debe tener acceso a endpoints de consulta de evaluaciones ni descarga de PDFs")
    void administradorNoPuedeConsultarEvaluacionesNiDescargarPdfs() throws Exception {
        String correoAdmin = correoUnico();
        crearUsuarioDirecto(correoAdmin, RolUsuario.ADMINISTRADOR, true);
        MvcResult loginAdmin = iniciarSesion(correoAdmin);
        String tokenAdmin = tokenAcceso(loginAdmin);

        // Consulta de evaluaciones
        mockMvc.perform(get("/api/triaje/evaluaciones")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.estado").value(403))
                .andExpect(jsonPath("$.error").value("ACCESO_DENEGADO"));

        mockMvc.perform(get("/api/triaje/evaluaciones/evaluacion-ficticia-123")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.estado").value(403))
                .andExpect(jsonPath("$.error").value("ACCESO_DENEGADO"));

        // Descarga de PDF
        mockMvc.perform(get("/api/triaje/evaluaciones/evaluacion-ficticia-123/pdf")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.estado").value(403))
                .andExpect(jsonPath("$.error").value("ACCESO_DENEGADO"));
    }

    @Test
    @DisplayName("GET /api/usuarios/yo debe devolver nombreCompleto y rol para cualquier usuario autenticado")
    void getUsuariosYoDevuelveNombreCompletoYRol() throws Exception {
        String correo = correoUnico();
        Long adminId = crearUsuarioDirecto(correo, RolUsuario.ADMINISTRADOR, true);
        MvcResult login = iniciarSesion(correo);
        String token = tokenAcceso(login);

        mockMvc.perform(get("/api/usuarios/yo")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(adminId))
                .andExpect(jsonPath("$.nombreCompleto").value("Usuario ADMINISTRADOR"))
                .andExpect(jsonPath("$.rol").value("ADMINISTRADOR"));
    }

    @Test
    @DisplayName("Debe registrar en auditoria las acciones CLINICO_CREADO, CLINICO_DESACTIVADO y CLINICO_ACTIVADO sin datos personales")
    void debeRegistrarAuditoriaParaOperacionesClinicas() throws Exception {
        String correoAdmin = correoUnico();
        Long adminId = crearUsuarioDirecto(correoAdmin, RolUsuario.ADMINISTRADOR, true);
        MvcResult loginAdmin = iniciarSesion(correoAdmin);
        String tokenAdmin = tokenAcceso(loginAdmin);

        String correoClinico = correoUnico();
        MvcResult resCrear = mockMvc.perform(post("/api/administracion/clinicos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nombreCompleto", "Dr. Roberto Mendoza",
                                "correo", correoClinico,
                                "contrasenaInicial", CONTRASENA_VALIDA))))
                .andExpect(status().isCreated())
                .andReturn();

        long clinicoId = leer(resCrear).get("id").asLong();

        mockMvc.perform(post("/api/administracion/clinicos/" + clinicoId + "/desactivar")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/administracion/clinicos/" + clinicoId + "/activar")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin)))
                .andExpect(status().isOk());

        // Verificar la tabla registro_auditoria
        Integer countCreado = jdbc.queryForObject(
                "SELECT COUNT(*) FROM registro_auditoria WHERE usuario_id = ? AND accion = 'CLINICO_CREADO' AND entidad_id = ?",
                Integer.class, adminId, String.valueOf(clinicoId));
        assertThat(countCreado).isEqualTo(1);

        Integer countDesactivado = jdbc.queryForObject(
                "SELECT COUNT(*) FROM registro_auditoria WHERE usuario_id = ? AND accion = 'CLINICO_DESACTIVADO' AND entidad_id = ?",
                Integer.class, adminId, String.valueOf(clinicoId));
        assertThat(countDesactivado).isEqualTo(1);

        Integer countActivado = jdbc.queryForObject(
                "SELECT COUNT(*) FROM registro_auditoria WHERE usuario_id = ? AND accion = 'CLINICO_ACTIVADO' AND entidad_id = ?",
                Integer.class, adminId, String.valueOf(clinicoId));
        assertThat(countActivado).isEqualTo(1);

        // Verificar que no se hayan guardado nombres ni correos en entidad_id ni en ninguna columna
        Integer conCorreoEnAuditoria = jdbc.queryForObject(
                "SELECT COUNT(*) FROM registro_auditoria WHERE entidad_id LIKE '%@%'",
                Integer.class);
        assertThat(conCorreoEnAuditoria).isZero();
    }

    @Test
    @DisplayName("GET /api/administracion/clinicos lista los clinicos con su estado activo")
    void debeListarClinicosConEstadoActivo() throws Exception {
        String correoAdmin = correoUnico();
        crearUsuarioDirecto(correoAdmin, RolUsuario.ADMINISTRADOR, true);
        MvcResult loginAdmin = iniciarSesion(correoAdmin);
        String tokenAdmin = tokenAcceso(loginAdmin);

        String clinico1 = correoUnico();
        String clinico2 = correoUnico();
        crearUsuarioDirecto(clinico1, RolUsuario.CLINICO, true);
        crearUsuarioDirecto(clinico2, RolUsuario.CLINICO, false);

        mockMvc.perform(get("/api/administracion/clinicos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].rol").value("CLINICO"))
                .andExpect(jsonPath("$[1].rol").value("CLINICO"));
    }

    @Test
    @DisplayName("POST /api/administracion/clinicos permite recibir especialidad opcional y se expone")
    void debeCrearClinicoConEspecialidadOpcional() throws Exception {
        String correoAdmin = correoUnico();
        crearUsuarioDirecto(correoAdmin, RolUsuario.ADMINISTRADOR, true);
        String tokenAdmin = tokenAcceso(iniciarSesion(correoAdmin));

        String correoClinico = correoUnico();
        MvcResult res = mockMvc.perform(post("/api/administracion/clinicos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nombreCompleto", "Dra. Especialista",
                                "correo", correoClinico,
                                "contrasenaInicial", CONTRASENA_VALIDA,
                                "especialidad", "Psiquiatria Clinica"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.especialidad").value("Psiquiatria Clinica"))
                .andReturn();

        long clinicoId = leer(res).get("id").asLong();
        Usuario usuario = repositorioUsuarios.buscarPorId(clinicoId).orElseThrow();
        assertThat(usuario.getEspecialidad()).isEqualTo("Psiquiatria Clinica");
    }

    @Test
    @DisplayName("PUT /api/administracion/clinicos/{id} actualiza nombre, correo y especialidad con auditoria")
    void debeActualizarClinicoExitosamente() throws Exception {
        String correoAdmin = correoUnico();
        Long adminId = crearUsuarioDirecto(correoAdmin, RolUsuario.ADMINISTRADOR, true);
        String tokenAdmin = tokenAcceso(iniciarSesion(correoAdmin));

        String correoClinico = correoUnico();
        MvcResult resCrear = mockMvc.perform(post("/api/administracion/clinicos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nombreCompleto", "Dr. Inicial",
                                "correo", correoClinico,
                                "contrasenaInicial", CONTRASENA_VALIDA))))
                .andExpect(status().isCreated())
                .andReturn();
        long clinicoId = leer(resCrear).get("id").asLong();

        String nuevoCorreo = correoUnico();
        mockMvc.perform(put("/api/administracion/clinicos/" + clinicoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nombreCompleto", "Dr. Modificado",
                                "correo", nuevoCorreo,
                                "especialidad", "Neurologia"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreCompleto").value("Dr. Modificado"))
                .andExpect(jsonPath("$.correo").value(nuevoCorreo))
                .andExpect(jsonPath("$.especialidad").value("Neurologia"));

        Integer auditActualizado = jdbc.queryForObject(
                "SELECT COUNT(*) FROM registro_auditoria WHERE usuario_id = ? AND accion = 'CLINICO_ACTUALIZADO' AND entidad_id = ?",
                Integer.class, adminId, String.valueOf(clinicoId));
        assertThat(auditActualizado).isEqualTo(1);
    }

    @Test
    @DisplayName("PUT /api/administracion/clinicos/{id} retorna 404 Not Found si el ID no existe")
    void debeRetornar404SiClinicoNoExisteAlActualizar() throws Exception {
        String correoAdmin = correoUnico();
        crearUsuarioDirecto(correoAdmin, RolUsuario.ADMINISTRADOR, true);
        String tokenAdmin = tokenAcceso(iniciarSesion(correoAdmin));

        mockMvc.perform(put("/api/administracion/clinicos/999999")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nombreCompleto", "Dr. Fantasma",
                                "correo", correoUnico(),
                                "especialidad", "Psiquiatria"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.estado").value(404))
                .andExpect(jsonPath("$.error").value("USUARIO_NO_ENCONTRADO"))
                .andExpect(jsonPath("$.mensaje").value("El clinico no existe."));
    }

    @Test
    @DisplayName("PUT /api/administracion/clinicos/{id} retorna 404 Not Found si el usuario no tiene rol CLINICO")
    void debeRetornar404SiUsuarioNoEsClinicoAlActualizar() throws Exception {
        String correoAdmin = correoUnico();
        crearUsuarioDirecto(correoAdmin, RolUsuario.ADMINISTRADOR, true);
        String tokenAdmin = tokenAcceso(iniciarSesion(correoAdmin));

        String correoPaciente = correoUnico();
        long pacienteId = registrar(correoPaciente);

        mockMvc.perform(put("/api/administracion/clinicos/" + pacienteId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nombreCompleto", "Paciente Hack",
                                "correo", correoUnico(),
                                "especialidad", "Falsa"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.estado").value(404))
                .andExpect(jsonPath("$.error").value("USUARIO_NO_ENCONTRADO"))
                .andExpect(jsonPath("$.mensaje").value("El usuario especificado no es un clinico."));
    }

    @Test
    @DisplayName("PUT /api/administracion/clinicos/{id} exige rol ADMINISTRADOR (403 para PACIENTE, 401 para anonimo)")
    void debeRechazarActualizarClinicoSinRolAdministrador() throws Exception {
        String correoPaciente = correoUnico();
        registrar(correoPaciente);
        String tokenPaciente = tokenAcceso(iniciarSesion(correoPaciente));

        // Intento por un PACIENTE -> 403 Forbidden
        mockMvc.perform(put("/api/administracion/clinicos/1")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenPaciente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nombreCompleto", "Dr. No Autorizado",
                                "correo", correoUnico(),
                                "especialidad", "General"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.estado").value(403))
                .andExpect(jsonPath("$.error").value("ACCESO_DENEGADO"));

        // Intento anónimo -> 401 Unauthorized
        mockMvc.perform(put("/api/administracion/clinicos/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nombreCompleto", "Dr. Anonimo",
                                "correo", correoUnico(),
                                "especialidad", "General"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.estado").value(401))
                .andExpect(jsonPath("$.error").value("NO_AUTENTICADO"));
    }

    @Test
    @DisplayName("DELETE /api/administracion/clinicos/{id} elimina el clinico con auditoria")
    void debeEliminarClinicoExitosamente() throws Exception {
        String correoAdmin = correoUnico();
        Long adminId = crearUsuarioDirecto(correoAdmin, RolUsuario.ADMINISTRADOR, true);
        String tokenAdmin = tokenAcceso(iniciarSesion(correoAdmin));

        String correoClinico = correoUnico();
        MvcResult resCrear = mockMvc.perform(post("/api/administracion/clinicos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nombreCompleto", "Dr. Por Borrar",
                                "correo", correoClinico,
                                "contrasenaInicial", CONTRASENA_VALIDA))))
                .andExpect(status().isCreated())
                .andReturn();
        long clinicoId = leer(resCrear).get("id").asLong();

        mockMvc.perform(delete("/api/administracion/clinicos/" + clinicoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin)))
                .andExpect(status().isNoContent());

        assertThat(repositorioUsuarios.buscarPorId(clinicoId)).isEmpty();

        Integer auditEliminado = jdbc.queryForObject(
                "SELECT COUNT(*) FROM registro_auditoria WHERE usuario_id = ? AND accion = 'CLINICO_ELIMINADO' AND entidad_id = ?",
                Integer.class, adminId, String.valueOf(clinicoId));
        assertThat(auditEliminado).isEqualTo(1);
    }

    @Test
    @DisplayName("GET /api/administracion/pacientes lista id, nombre, correo, fechaRegistro, activo con minimizacion clinica")
    void debeListarPacientesConMinimizacionClinica() throws Exception {
        String correoAdmin = correoUnico();
        crearUsuarioDirecto(correoAdmin, RolUsuario.ADMINISTRADOR, true);
        String tokenAdmin = tokenAcceso(iniciarSesion(correoAdmin));

        String correoP = correoUnico();
        long pacienteId = registrar(correoP);

        mockMvc.perform(get("/api/administracion/pacientes")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.id == " + pacienteId + ")].nombre").value("Persona de Prueba"))
                .andExpect(jsonPath("$[?(@.id == " + pacienteId + ")].correo").value(correoP))
                .andExpect(jsonPath("$[?(@.id == " + pacienteId + ")].activo").value(true))
                .andExpect(jsonPath("$[?(@.id == " + pacienteId + ")].fechaRegistro").exists())
                // Minimizacion clinica estricta: NO debe exponer diagnosticos, puntajes, severidad
                .andExpect(jsonPath("$[?(@.id == " + pacienteId + ")].puntajePhq9").doesNotExist())
                .andExpect(jsonPath("$[?(@.id == " + pacienteId + ")].diagnostico").doesNotExist())
                .andExpect(jsonPath("$[?(@.id == " + pacienteId + ")].respuestas").doesNotExist());
    }

    @Test
    @DisplayName("PUT /api/administracion/clinicos/{id} permite restablecer contraseña con BCrypt y el clínico puede iniciar sesión con la nueva clave")
    void debeRestablecerContrasenaDeClinicoConBCryptYPermitirLogin() throws Exception {
        String correoAdmin = correoUnico();
        crearUsuarioDirecto(correoAdmin, RolUsuario.ADMINISTRADOR, true);
        String tokenAdmin = tokenAcceso(iniciarSesion(correoAdmin));

        String correoClinico = correoUnico();
        MvcResult resCrear = mockMvc.perform(post("/api/administracion/clinicos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nombreCompleto", "Dr. Clave Vieja",
                                "correo", correoClinico,
                                "contrasenaInicial", CONTRASENA_VALIDA))))
                .andExpect(status().isCreated())
                .andReturn();
        long clinicoId = leer(resCrear).get("id").asLong();

        String nuevaClave = "NuevaClaveSuperSegura2026!";
        mockMvc.perform(put("/api/administracion/clinicos/" + clinicoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nombreCompleto", "Dr. Clave Renovada",
                                "correo", correoClinico,
                                "especialidad", "Psiquiatría",
                                "nuevaContrasena", nuevaClave))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreCompleto").value("Dr. Clave Renovada"));

        // Comprobar que la clave antigua ya no funciona
        mockMvc.perform(post("/api/autenticacion/inicio-sesion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("correo", correoClinico, "contrasena", CONTRASENA_VALIDA))))
                .andExpect(status().isUnauthorized());

        // Comprobar que la nueva clave sí funciona
        MvcResult nuevoLogin = mockMvc.perform(post("/api/autenticacion/inicio-sesion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("correo", correoClinico, "contrasena", nuevaClave))))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(tokenAcceso(nuevoLogin)).isNotBlank();
    }

    @Test
    @DisplayName("PUT /api/administracion/clinicos/{id} rechaza con 400 Bad Request si la nueva contraseña tiene menos de 10 caracteres")
    void debeRechazarActualizarClinicoSiNuevaContrasenaTieneMenosDe10Caracteres() throws Exception {
        String correoAdmin = correoUnico();
        crearUsuarioDirecto(correoAdmin, RolUsuario.ADMINISTRADOR, true);
        String tokenAdmin = tokenAcceso(iniciarSesion(correoAdmin));

        String correoClinico = correoUnico();
        MvcResult resCrear = mockMvc.perform(post("/api/administracion/clinicos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nombreCompleto", "Dr. Clave Corta",
                                "correo", correoClinico,
                                "contrasenaInicial", CONTRASENA_VALIDA))))
                .andExpect(status().isCreated())
                .andReturn();
        long clinicoId = leer(resCrear).get("id").asLong();

        mockMvc.perform(put("/api/administracion/clinicos/" + clinicoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nombreCompleto", "Dr. Clave Corta",
                                "correo", correoClinico,
                                "nuevaContrasena", "Corta123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estado").value(400))
                .andExpect(jsonPath("$.error").value("DATO_INVALIDO"))
                .andExpect(jsonPath("$.mensaje").value("La contraseña debe tener al menos 10 caracteres."));
    }

    @Test
    @DisplayName("PUT /api/administracion/clinicos/{id} rechaza con 409 Conflict si el nuevo correo ya está registrado")
    void debeRechazarActualizarClinicoSiCorreoYaExiste() throws Exception {
        String correoAdmin = correoUnico();
        crearUsuarioDirecto(correoAdmin, RolUsuario.ADMINISTRADOR, true);
        String tokenAdmin = tokenAcceso(iniciarSesion(correoAdmin));

        String correoClinico1 = correoUnico();
        String correoClinico2 = correoUnico();
        Long clinico1Id = crearUsuarioDirecto(correoClinico1, RolUsuario.CLINICO, true);
        crearUsuarioDirecto(correoClinico2, RolUsuario.CLINICO, true);

        mockMvc.perform(put("/api/administracion/clinicos/" + clinico1Id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nombreCompleto", "Dr. Clinico Duplicado",
                                "correo", correoClinico2))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.estado").value(409))
                .andExpect(jsonPath("$.error").value("CORREO_YA_REGISTRADO"));
    }

    @Test
    @DisplayName("PUT /api/administracion/pacientes/{id} actualiza nombre, correo y contraseña con BCrypt y auditoría PACIENTE_ACTUALIZADO")
    void debeActualizarPacienteExitosamenteYRegistrarAuditoria() throws Exception {
        String correoAdmin = correoUnico();
        Long adminId = crearUsuarioDirecto(correoAdmin, RolUsuario.ADMINISTRADOR, true);
        String tokenAdmin = tokenAcceso(iniciarSesion(correoAdmin));

        String correoPaciente = correoUnico();
        long pacienteId = registrar(correoPaciente);

        String nuevoCorreo = correoUnico();
        String nuevaClave = "ClavePacienteNueva2026!";

        mockMvc.perform(put("/api/administracion/pacientes/" + pacienteId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nombreCompleto", "Paciente Nombre Modificado",
                                "correo", nuevoCorreo,
                                "nuevaContrasena", nuevaClave))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(pacienteId))
                .andExpect(jsonPath("$.nombre").value("Paciente Nombre Modificado"))
                .andExpect(jsonPath("$.correo").value(nuevoCorreo))
                .andExpect(jsonPath("$.activo").value(true));

        // Verificar que el paciente pueda iniciar sesión con la nueva clave y el nuevo correo
        MvcResult loginPaciente = mockMvc.perform(post("/api/autenticacion/inicio-sesion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("correo", nuevoCorreo, "contrasena", nuevaClave))))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(tokenAcceso(loginPaciente)).isNotBlank();

        // Verificar auditoría
        Integer auditActualizado = jdbc.queryForObject(
                "SELECT COUNT(*) FROM registro_auditoria WHERE usuario_id = ? AND accion = 'PACIENTE_ACTUALIZADO' AND entidad_id = ?",
                Integer.class, adminId, String.valueOf(pacienteId));
        assertThat(auditActualizado).isEqualTo(1);

        // Verificar que no se expongan datos personales en la auditoría
        Integer datosPersonalesEnAuditoria = jdbc.queryForObject(
                "SELECT COUNT(*) FROM registro_auditoria WHERE entidad_id LIKE '%@%' OR entidad_id LIKE '%Modificado%'",
                Integer.class);
        assertThat(datosPersonalesEnAuditoria).isZero();
    }

    @Test
    @DisplayName("PUT /api/administracion/pacientes/{id} rechaza con 400 Bad Request si la nueva contraseña tiene menos de 10 caracteres")
    void debeRechazarActualizarPacienteSiNuevaContrasenaTieneMenosDe10Caracteres() throws Exception {
        String correoAdmin = correoUnico();
        crearUsuarioDirecto(correoAdmin, RolUsuario.ADMINISTRADOR, true);
        String tokenAdmin = tokenAcceso(iniciarSesion(correoAdmin));

        String correoPaciente = correoUnico();
        long pacienteId = registrar(correoPaciente);

        mockMvc.perform(put("/api/administracion/pacientes/" + pacienteId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nombreCompleto", "Paciente Corto",
                                "correo", correoPaciente,
                                "nuevaContrasena", "Abc123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estado").value(400))
                .andExpect(jsonPath("$.error").value("DATO_INVALIDO"))
                .andExpect(jsonPath("$.mensaje").value("La contraseña debe tener al menos 10 caracteres."));
    }

    @Test
    @DisplayName("PUT /api/administracion/pacientes/{id} rechaza con 409 Conflict si el nuevo correo ya existe")
    void debeRechazarActualizarPacienteSiCorreoYaExiste() throws Exception {
        String correoAdmin = correoUnico();
        crearUsuarioDirecto(correoAdmin, RolUsuario.ADMINISTRADOR, true);
        String tokenAdmin = tokenAcceso(iniciarSesion(correoAdmin));

        String correoPaciente1 = correoUnico();
        String correoPaciente2 = correoUnico();
        long paciente1Id = registrar(correoPaciente1);
        registrar(correoPaciente2);

        mockMvc.perform(put("/api/administracion/pacientes/" + paciente1Id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nombreCompleto", "Paciente Conflicto",
                                "correo", correoPaciente2))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.estado").value(409))
                .andExpect(jsonPath("$.error").value("CORREO_YA_REGISTRADO"));
    }

    @Test
    @DisplayName("PUT /api/administracion/pacientes/{id} exige rol ADMINISTRADOR (403 para PACIENTE, 401 para anónimo)")
    void debeRechazarActualizarPacienteSinRolAdministrador() throws Exception {
        String correoPaciente = correoUnico();
        long pacienteId = registrar(correoPaciente);
        String tokenPaciente = tokenAcceso(iniciarSesion(correoPaciente));

        // Intento por PACIENTE -> 403 Forbidden
        mockMvc.perform(put("/api/administracion/pacientes/" + pacienteId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenPaciente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nombreCompleto", "Hack",
                                "correo", correoUnico()))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.estado").value(403))
                .andExpect(jsonPath("$.error").value("ACCESO_DENEGADO"));

        // Intento anónimo -> 401 Unauthorized
        mockMvc.perform(put("/api/administracion/pacientes/" + pacienteId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nombreCompleto", "Hack Anonimo",
                                "correo", correoUnico()))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.estado").value(401))
                .andExpect(jsonPath("$.error").value("NO_AUTENTICADO"));
    }

    @Test
    @DisplayName("PUT /api/administracion/pacientes/{id} retorna 404 si el usuario no es un paciente")
    void debeRetornar404SiUsuarioNoEsPacienteAlActualizar() throws Exception {
        String correoAdmin = correoUnico();
        crearUsuarioDirecto(correoAdmin, RolUsuario.ADMINISTRADOR, true);
        String tokenAdmin = tokenAcceso(iniciarSesion(correoAdmin));

        String correoClinico = correoUnico();
        Long clinicoId = crearUsuarioDirecto(correoClinico, RolUsuario.CLINICO, true);

        mockMvc.perform(put("/api/administracion/pacientes/" + clinicoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nombreCompleto", "Dr. Confundido",
                                "correo", correoUnico()))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.estado").value(404))
                .andExpect(jsonPath("$.error").value("USUARIO_NO_ENCONTRADO"))
                .andExpect(jsonPath("$.mensaje").value("El usuario especificado no es un paciente."));
    }
}
