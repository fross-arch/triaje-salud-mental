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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ActualizacionPerfilTest extends PruebaIntegracionBase {

    @Autowired
    private RepositorioUsuarios repositorioUsuarios;

    @Autowired
    private CodificadorContrasenas codificador;

    private Long crearUsuarioDirecto(String correo, RolUsuario rol, boolean activo, String especialidad) {
        String hash = codificador.codificar(CONTRASENA_VALIDA);
        Usuario usuario = new Usuario(
                null,
                "Usuario " + rol.name(),
                correo,
                hash,
                rol,
                activo,
                Instant.now(),
                especialidad
        );
        return repositorioUsuarios.guardar(usuario).getId();
    }

    @Test
    @DisplayName("Actualiza datos básicos (nombre y correo) del usuario autenticado exitosamente")
    void debeActualizarDatosBasicosPerfilCorrectamente() throws Exception {
        String correoOriginal = correoUnico();
        long usuarioId = registrar(correoOriginal);
        String token = tokenAcceso(iniciarSesion(correoOriginal));

        String nuevoCorreo = correoUnico();
        String nuevoNombre = "Nombre Actualizado Exitoso";

        mockMvc.perform(put("/api/usuarios/yo")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nombreCompleto", nuevoNombre,
                                "correo", nuevoCorreo
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(usuarioId))
                .andExpect(jsonPath("$.nombreCompleto").value(nuevoNombre))
                .andExpect(jsonPath("$.correo").value(nuevoCorreo.toLowerCase()));

        // Verificar que GET /api/usuarios/yo refleje los cambios
        mockMvc.perform(get("/api/usuarios/yo")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreCompleto").value(nuevoNombre))
                .andExpect(jsonPath("$.correo").value(nuevoCorreo.toLowerCase()));
    }

    @Test
    @DisplayName("Cambio de contraseña exitoso validando contraseña actual y longitud mínima de 10 caracteres")
    void debePermitirCambioDeContrasenaExitoso() throws Exception {
        String correo = correoUnico();
        registrar(correo);
        String token = tokenAcceso(iniciarSesion(correo));

        String nuevaClave = "NuevaClaveValida2026!";

        mockMvc.perform(put("/api/usuarios/yo")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nombreCompleto", "Usuario Clave Cambiada",
                                "correo", correo,
                                "contrasenaActual", CONTRASENA_VALIDA,
                                "nuevaContrasena", nuevaClave
                        ))))
                .andExpect(status().isOk());

        // Verificar que puede iniciar sesión con la nueva contraseña
        mockMvc.perform(post("/api/autenticacion/inicio-sesion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("correo", correo, "contrasena", nuevaClave))))
                .andExpect(status().isOk());

        // La contraseña anterior ya no es válida
        mockMvc.perform(post("/api/autenticacion/inicio-sesion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("correo", correo, "contrasena", CONTRASENA_VALIDA))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Rechaza la actualización si la contraseña actual es errónea (400 Bad Request)")
    void debeRechazarCambioContrasenaSiClaveActualEsErronea() throws Exception {
        String correo = correoUnico();
        registrar(correo);
        String token = tokenAcceso(iniciarSesion(correo));

        mockMvc.perform(put("/api/usuarios/yo")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nombreCompleto", "Usuario Error Clave",
                                "correo", correo,
                                "contrasenaActual", "ClaveIncorrecta999",
                                "nuevaContrasena", "NuevaClaveSuperSegura123"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("La contraseña actual no coincide"));
    }

    @Test
    @DisplayName("Rechaza la actualización si la nueva contraseña tiene menos de 10 caracteres (400 Bad Request)")
    void debeRechazarCambioContrasenaSiNuevaClaveTieneMenosDe10Caracteres() throws Exception {
        String correo = correoUnico();
        registrar(correo);
        String token = tokenAcceso(iniciarSesion(correo));

        mockMvc.perform(put("/api/usuarios/yo")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nombreCompleto", "Usuario Clave Corta",
                                "correo", correo,
                                "contrasenaActual", CONTRASENA_VALIDA,
                                "nuevaContrasena", "Corta123" // 8 caracteres
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("La contraseña debe tener al menos 10 caracteres."));
    }

    @Test
    @DisplayName("Rechaza la actualización si el nuevo correo ya pertenece a otro usuario (409 Conflict)")
    void debeRechazarActualizacionSiCorreoYaPerteneceAOtroUsuario() throws Exception {
        String correoUsuario1 = correoUnico();
        registrar(correoUsuario1);

        String correoUsuario2 = correoUnico();
        registrar(correoUsuario2);
        String token2 = tokenAcceso(iniciarSesion(correoUsuario2));

        mockMvc.perform(put("/api/usuarios/yo")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token2))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nombreCompleto", "Usuario 2 Intentando Robar Correo",
                                "correo", correoUsuario1
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CORREO_YA_REGISTRADO"));
    }

    @Test
    @DisplayName("Permite actualizar especialidad a un usuario con rol CLINICO")
    void debePermitirActualizarEspecialidadSiEsClinico() throws Exception {
        String correoClinico = correoUnico();
        long clinicoId = crearUsuarioDirecto(correoClinico, RolUsuario.CLINICO, true, "Psicología General");
        String token = tokenAcceso(iniciarSesion(correoClinico));

        mockMvc.perform(put("/api/usuarios/yo")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nombreCompleto", "Dr. Clinico Especialista",
                                "correo", correoClinico,
                                "especialidad", "Neuropsicología"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(clinicoId))
                .andExpect(jsonPath("$.especialidad").value("Neuropsicología"));

        // Comprobar con GET /api/usuarios/yo
        mockMvc.perform(get("/api/usuarios/yo")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.especialidad").value("Neuropsicología"));
    }

    @Test
    @DisplayName("Ignora el campo especialidad si el usuario no tiene rol CLINICO")
    void debeIgnorarEspecialidadSiUsuarioNoEsClinico() throws Exception {
        String correoPaciente = correoUnico();
        long pacienteId = registrar(correoPaciente);
        String token = tokenAcceso(iniciarSesion(correoPaciente));

        mockMvc.perform(put("/api/usuarios/yo")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nombreCompleto", "Paciente Sin Especialidad",
                                "correo", correoPaciente,
                                "especialidad", "Neuropsicología"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(pacienteId))
                .andExpect(jsonPath("$.especialidad").doesNotExist());
    }

    @Test
    @DisplayName("Registra la acción PERFIL_ACTUALIZADO en la auditoría con el usuario_id del actor y cero PII")
    void debeRegistrarAuditoriaSinPII() throws Exception {
        String correo = correoUnico();
        long usuarioId = registrar(correo);
        String token = tokenAcceso(iniciarSesion(correo));

        mockMvc.perform(put("/api/usuarios/yo")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nombreCompleto", "Nombre Auditoria Verificada",
                                "correo", correo
                        ))))
                .andExpect(status().isOk());

        List<Map<String, Object>> registros = jdbc.queryForList(
                "SELECT * FROM registro_auditoria WHERE usuario_id = ? AND accion = 'PERFIL_ACTUALIZADO'",
                usuarioId
        );
        assertThat(registros).hasSize(1);
        Map<String, Object> reg = registros.get(0);
        assertThat(reg.get("usuario_id")).isEqualTo(usuarioId);
        assertThat(reg.get("entidad_id")).isEqualTo(String.valueOf(usuarioId));

        // Verificar que en toda la tabla registro_auditoria no existan campos con nombre o correo
        List<Map<String, Object>> todos = jdbc.queryForList("SELECT * FROM registro_auditoria");
        for (Map<String, Object> r : todos) {
            String texto = r.toString();
            assertThat(texto).doesNotContain(correo);
            assertThat(texto).doesNotContain("Nombre Auditoria Verificada");
            assertThat(texto).doesNotContain(CONTRASENA_VALIDA);
        }
    }
}
