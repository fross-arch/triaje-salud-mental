package com.Cesde.triaje.Integracion;

import com.Cesde.triaje.Modelo.Enumeracion.RolUsuario;
import com.Cesde.triaje.Modelo.MUsuario;
import com.Cesde.triaje.Repositorio.IUsuarioRepositorio;
import com.Cesde.triaje.Servicio.SReporte;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("prueba")
public abstract class PruebaIntegracionBase {

    protected static final String CONTRASENA_VALIDA = "ClaveSegura123!";
    protected static final String NOMBRE_COOKIE = "token_renovacion";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper mapeador;

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected IUsuarioRepositorio usuarioRepositorio;

    @Autowired
    protected PasswordEncoder codificador;

    @Autowired
    protected com.Cesde.triaje.Seguridad.LimitadorIntentos limitadorIntentos;

    @MockBean
    protected SReporte reporteServicio;

    @BeforeEach
    void limpiarBaseDatos() {
        if (limitadorIntentos != null) {
            limitadorIntentos.limpiar();
        }
        when(reporteServicio.generarReportePdf(any(), any(), any()))
                .thenReturn("%PDF-1.4 Mock de prueba de reporte clinico en memoria".getBytes());

        jdbc.execute("SET REFERENTIAL_INTEGRITY FALSE");
        jdbc.execute("TRUNCATE TABLE registro_auditoria");
        jdbc.execute("TRUNCATE TABLE tokens_renovacion");
        jdbc.execute("TRUNCATE TABLE respuestas_evaluacion");
        jdbc.execute("TRUNCATE TABLE evaluaciones");
        jdbc.execute("TRUNCATE TABLE consentimientos");
        jdbc.execute("TRUNCATE TABLE citas");
        jdbc.execute("TRUNCATE TABLE autorizaciones_acceso");
        jdbc.execute("TRUNCATE TABLE usuarios");
        jdbc.execute("SET REFERENTIAL_INTEGRITY TRUE");
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

    protected Long crearUsuarioDirecto(String correo, String nombreCompleto, RolUsuario rol, boolean activo) {
        return crearUsuarioDirecto(correo, nombreCompleto, rol, activo, null);
    }

    protected Long crearUsuarioDirecto(String correo, String nombreCompleto, RolUsuario rol, boolean activo, String especialidad) {
        String hash = codificador.encode(CONTRASENA_VALIDA);
        MUsuario usuario = new MUsuario(
                nombreCompleto,
                correo,
                hash,
                rol
        );
        usuario.setActivo(activo);
        usuario.setEspecialidad(especialidad);
        usuario.setFechaCreacion(Instant.now());
        return usuarioRepositorio.save(usuario).getId();
    }
}
