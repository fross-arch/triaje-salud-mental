package com.Cesde.triaje.Integracion;

import com.Cesde.triaje.Modelo.Enumeracion.RolUsuario;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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

class EliminacionCompletaCuentaTest extends PruebaIntegracionBase {

    @Test
    @DisplayName("Eliminación completa de cuenta borra todas las entidades en orden explícito, anonimiza auditoría e invalida credenciales")
    void eliminacionCompletaDeCuentaBorraDatosYRevocaAcceso() throws Exception {
        // 1. Registro e inicio de sesión del paciente
        String correo = correoUnico();
        long pacienteId = registrar(correo);

        MvcResult loginRes = iniciarSesion(correo);
        String tokenAccesoViejo = tokenAcceso(loginRes);
        Cookie cookieRenovacionVieja = cookieRenovacion(loginRes);
        assertThat(cookieRenovacionVieja).isNotNull();

        // 2. Consentimiento
        mockMvc.perform(post("/api/consentimientos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAccesoViejo))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("confirmaMayoriaEdad", true))))
                .andExpect(status().isCreated());

        // 3. Evaluación y respuestas
        MvcResult resCrear = mockMvc.perform(post("/api/triaje/evaluaciones")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAccesoViejo)))
                .andExpect(status().isCreated())
                .andReturn();
        String evaluacionId = leer(resCrear).get("id").asText();

        for (int i = 1; i <= 9; i++) {
            mockMvc.perform(put("/api/triaje/evaluaciones/" + evaluacionId + "/respuestas")
                            .header(HttpHeaders.AUTHORIZATION, bearer(tokenAccesoViejo))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("instrumento", "PHQ9", "indicePregunta", i, "valorPuntaje", 1))))
                    .andExpect(status().isOk());
        }
        for (int i = 1; i <= 7; i++) {
            mockMvc.perform(put("/api/triaje/evaluaciones/" + evaluacionId + "/respuestas")
                            .header(HttpHeaders.AUTHORIZATION, bearer(tokenAccesoViejo))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("instrumento", "GAD7", "indicePregunta", i, "valorPuntaje", 1))))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(post("/api/triaje/evaluaciones/" + evaluacionId + "/finalizar")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAccesoViejo)))
                .andExpect(status().isOk());

        // 4. Clínico y Autorización
        String correoClinico = correoUnico();
        Long clinicoId = crearUsuarioDirecto(correoClinico, "Dra. Evaluadora", RolUsuario.CLINICO, true);

        MvcResult resAut = mockMvc.perform(post("/api/triaje/evaluaciones/" + evaluacionId + "/autorizaciones")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAccesoViejo))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("clinicoId", clinicoId, "confirmaAutorizacion", true))))
                .andExpect(status().isCreated())
                .andReturn();
        String autorizacionId = leer(resAut).get("id").asText();

        // 5. Cita propuesta por el clínico y luego cancelada por el paciente
        MvcResult loginClinico = iniciarSesion(correoClinico);
        String tokenClinico = tokenAcceso(loginClinico);

        Instant fechaCita = Instant.now().plus(3, ChronoUnit.DAYS);
        MvcResult resCita = mockMvc.perform(post("/api/clinico/citas")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenClinico))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "evaluacionId", evaluacionId,
                                "fechaHora", fechaCita.toString(),
                                "modalidad", "VIRTUAL",
                                "mensaje", "Consulta inicial"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        String citaId = leer(resCita).get("id").asText();

        mockMvc.perform(post("/api/citas/" + citaId + "/cancelar")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAccesoViejo)))
                .andExpect(status().isOk());

        // Comprobar precondición: existen registros vinculados al paciente en todas las tablas
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM usuarios WHERE id = ?", Integer.class, pacienteId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM consentimientos WHERE usuario_id = ?", Integer.class, pacienteId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tokens_renovacion WHERE usuario_id = ?", Integer.class, pacienteId)).isGreaterThanOrEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM evaluaciones WHERE usuario_id = ?", Integer.class, pacienteId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM respuestas_evaluacion WHERE evaluacion_id = ?", Integer.class, evaluacionId)).isEqualTo(16);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM autorizaciones_acceso WHERE paciente_id = ?", Integer.class, pacienteId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM citas WHERE paciente_id = ?", Integer.class, pacienteId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM registro_auditoria WHERE usuario_id = ?", Integer.class, pacienteId)).isGreaterThan(0);

        // 6. Eliminar cuenta
        MvcResult resEliminar = mockMvc.perform(delete("/api/usuarios/yo")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAccesoViejo)))
                .andExpect(status().isNoContent())
                .andReturn();

        // El endpoint borra la cookie de renovación
        String setCookieHeader = encabezadoCookie(resEliminar);
        assertThat(setCookieHeader).isNotNull();
        assertThat(setCookieHeader).contains("token_renovacion");
        assertThat(setCookieHeader.toLowerCase()).containsAnyOf("max-age=0", "expires=");

        // 7. Verificación en base de datos: 0 filas del usuario en todas las tablas
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM usuarios WHERE id = ?", Integer.class, pacienteId)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM consentimientos WHERE usuario_id = ?", Integer.class, pacienteId)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tokens_renovacion WHERE usuario_id = ?", Integer.class, pacienteId)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM evaluaciones WHERE usuario_id = ?", Integer.class, pacienteId)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM respuestas_evaluacion WHERE evaluacion_id = ?", Integer.class, evaluacionId)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM autorizaciones_acceso WHERE paciente_id = ?", Integer.class, pacienteId)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM citas WHERE paciente_id = ?", Integer.class, pacienteId)).isZero();

        // Verificación de auditoría: los registros anteriores quedaron anonimizados (usuario_id = null)
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM registro_auditoria WHERE usuario_id = ?", Integer.class, pacienteId)).isZero();

        // Se registra evento CUENTA_ELIMINADA con usuarioId=null y entidadId=pacienteId
        Integer auditoriaEliminada = jdbc.queryForObject(
                "SELECT COUNT(*) FROM registro_auditoria WHERE accion = 'CUENTA_ELIMINADA' AND entidad_id = ? AND usuario_id IS NULL",
                Integer.class,
                String.valueOf(pacienteId)
        );
        assertThat(auditoriaEliminada).isEqualTo(1);

        // 8. Intentos posteriores fallan
        // Inicio de sesión con las mismas credenciales falla (401 CREDENCIALES_INVALIDAS)
        mockMvc.perform(post("/api/autenticacion/inicio-sesion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("correo", correo, "contrasena", CONTRASENA_VALIDA))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("CREDENCIALES_INVALIDAS"));

        // Renovar con el token de renovación revocado/eliminado falla (401 SESION_INVALIDA)
        mockMvc.perform(post("/api/autenticacion/renovar")
                        .cookie(cookieRenovacionVieja))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("SESION_INVALIDA"));

        // Token de acceso anterior deja de funcionar
        mockMvc.perform(get("/api/usuarios/yo")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAccesoViejo)))
                .andExpect(status().isUnauthorized());
    }
}
