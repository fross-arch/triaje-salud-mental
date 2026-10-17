package com.triaje.integracion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.MySQLContainer;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base de las pruebas de integración: un único contenedor MySQL 8 compartido por todas las clases
 * (patrón contenedor único) para que Flyway aplique las migraciones reales y Hibernate valide el esquema.
 * Requiere Docker en ejecución.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("prueba")
public abstract class PruebaIntegracionBase {

    protected static final String CONTRASENA_VALIDA = "ClaveSegura123";
    protected static final String NOMBRE_COOKIE = "token_renovacion";

    static final MySQLContainer<?> MYSQL;
    static final boolean TIENE_DOCKER;

    static {
        boolean dockerDisponible = false;
        MySQLContainer<?> contenedor = null;
        try {
            if (org.testcontainers.DockerClientFactory.instance().isDockerAvailable()) {
                contenedor = new MySQLContainer<>("mysql:8.0")
                        .withDatabaseName("triaje_prueba_db")
                        .withUsername("triaje_prueba")
                        .withPassword("clave_prueba")
                        .withCommand("--character-set-server=utf8mb4", "--collation-server=utf8mb4_unicode_ci");
                contenedor.start();
                dockerDisponible = true;
            }
        } catch (Throwable t) {
            dockerDisponible = false;
        }
        TIENE_DOCKER = dockerDisponible;
        MYSQL = contenedor;
    }

    @DynamicPropertySource
    static void configurarBaseDatos(DynamicPropertyRegistry registro) {
        if (TIENE_DOCKER && MYSQL != null) {
            registro.add("spring.datasource.url", MYSQL::getJdbcUrl);
            registro.add("spring.datasource.username", MYSQL::getUsername);
            registro.add("spring.datasource.password", MYSQL::getPassword);
        }
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper mapeador;

    @Autowired
    protected JdbcTemplate jdbc;

    @org.junit.jupiter.api.BeforeEach
    void limpiarBaseDatos() {
        jdbc.execute("SET FOREIGN_KEY_CHECKS = 0");
        jdbc.execute("TRUNCATE TABLE registro_auditoria");
        jdbc.execute("TRUNCATE TABLE tokens_renovacion");
        jdbc.execute("TRUNCATE TABLE respuestas_evaluacion");
        jdbc.execute("TRUNCATE TABLE evaluaciones");
        jdbc.execute("TRUNCATE TABLE consentimientos");
        jdbc.execute("TRUNCATE TABLE citas");
        jdbc.execute("TRUNCATE TABLE autorizaciones_acceso");
        jdbc.execute("TRUNCATE TABLE usuarios");
        jdbc.execute("SET FOREIGN_KEY_CHECKS = 1");
    }

    protected static String correoUnico() {
        return "persona-" + UUID.randomUUID() + "@ejemplo.com";
    }

    protected String json(Object cuerpo) throws Exception {
        return mapeador.writeValueAsString(cuerpo);
    }

    protected JsonNode leer(MvcResult resultado) throws Exception {
        return mapeador.readTree(resultado.getResponse().getContentAsString());
    }

    protected long registrar(String correo) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/autenticacion/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nombreCompleto", "Persona de Prueba",
                                "correo", correo,
                                "contrasena", CONTRASENA_VALIDA))))
                .andExpect(status().isCreated())
                .andReturn();
        return leer(resultado).get("id").asLong();
    }

    protected MvcResult iniciarSesion(String correo) throws Exception {
        return mockMvc.perform(post("/api/autenticacion/inicio-sesion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("correo", correo, "contrasena", CONTRASENA_VALIDA))))
                .andExpect(status().isOk())
                .andReturn();
    }

    protected String tokenAcceso(MvcResult resultado) throws Exception {
        return leer(resultado).get("tokenAcceso").asText();
    }

    protected Cookie cookieRenovacion(MvcResult resultado) {
        return resultado.getResponse().getCookie(NOMBRE_COOKIE);
    }

    protected static String bearer(String token) {
        return "Bearer " + token;
    }

    protected static String encabezadoCookie(MvcResult resultado) {
        return resultado.getResponse().getHeader(HttpHeaders.SET_COOKIE);
    }
}
