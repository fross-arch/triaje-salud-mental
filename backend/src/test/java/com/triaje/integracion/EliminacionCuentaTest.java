package com.triaje.integracion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EliminacionCuentaTest extends PruebaIntegracionBase {

    @Test
    @DisplayName("Debe eliminar al usuario, sus datos en cascada y registrar evento CUENTA_ELIMINADA")
    void debeEliminarCuentaYDatosEnCascada() throws Exception {
        String correo = correoUnico();
        long usuarioId = registrar(correo);

        MvcResult login = iniciarSesion(correo);
        String token = tokenAcceso(login);

        // Simulamos inserciones en tablas con dependencias FK
        String evaluacionId = UUID.randomUUID().toString();
        jdbc.update("INSERT INTO consentimientos (usuario_id, version_consentimiento, version_politica, confirma_mayoria_edad) " +
                "VALUES (?, 'v1', 'v1', true)", usuarioId);
        jdbc.update("INSERT INTO evaluaciones (id, usuario_id, estado) VALUES (?, ?, 'EN_CURSO')", evaluacionId, usuarioId);
        jdbc.update("INSERT INTO respuestas_evaluacion (evaluacion_id, instrumento, indice_pregunta, valor_puntaje) " +
                "VALUES (?, 'PHQ9', 1, 2)", evaluacionId);

        // Verificar datos antes del borrado
        Integer tokensAntes = jdbc.queryForObject("SELECT COUNT(*) FROM tokens_renovacion WHERE usuario_id = ?", Integer.class, usuarioId);
        assertThat(tokensAntes).isPositive();

        // Ejecutar eliminación de cuenta
        mockMvc.perform(delete("/api/usuarios/yo")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNoContent());

        // Verificar borrado en tabla usuarios
        Integer existeUsuario = jdbc.queryForObject("SELECT COUNT(*) FROM usuarios WHERE id = ?", Integer.class, usuarioId);
        assertThat(existeUsuario).isZero();

        // Verificar borrado en cascada
        Integer consentimientos = jdbc.queryForObject("SELECT COUNT(*) FROM consentimientos WHERE usuario_id = ?", Integer.class, usuarioId);
        assertThat(consentimientos).isZero();

        Integer evaluaciones = jdbc.queryForObject("SELECT COUNT(*) FROM evaluaciones WHERE usuario_id = ?", Integer.class, usuarioId);
        assertThat(evaluaciones).isZero();

        Integer respuestas = jdbc.queryForObject("SELECT COUNT(*) FROM respuestas_evaluacion WHERE evaluacion_id = ?", Integer.class, evaluacionId);
        assertThat(respuestas).isZero();

        Integer tokens = jdbc.queryForObject("SELECT COUNT(*) FROM tokens_renovacion WHERE usuario_id = ?", Integer.class, usuarioId);
        assertThat(tokens).isZero();

        // Verificar registro de auditoria CUENTA_ELIMINADA con usuario_id en NULL (ON DELETE SET NULL) y entidad_id con el id
        List<Map<String, Object>> filasAuditoria = jdbc.queryForList(
                "SELECT accion, usuario_id, entidad_id FROM registro_auditoria WHERE entidad_id = ?", String.valueOf(usuarioId));
        assertThat(filasAuditoria).isNotEmpty();
        assertThat(filasAuditoria.stream().anyMatch(f -> "CUENTA_ELIMINADA".equals(f.get("accion")))).isTrue();
        assertThat(filasAuditoria.get(0).get("usuario_id")).isNull();

        // El usuario ya no puede consultar su perfil
        mockMvc.perform(get("/api/usuarios/yo")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNotFound());
    }
}
