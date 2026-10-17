package com.triaje.infraestructura.seguridad;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.triaje.integracion.PruebaIntegracionBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de auditoria de observabilidad y logging para garantizar el cumplimiento de
 * la Ley 1581 de 2012 y el Decreto 1377 de 2013 de Colombia:
 * Cero presencia de Datos Personales Identificables (PII), respuestas clinicas,
 * contrasenas o encabezados de autorizacion en consolas y registros.
 */
@ExtendWith(OutputCaptureExtension.class)
class AuditoriaLogsTest extends PruebaIntegracionBase {

    private static final Pattern PATRON_CORREO =
            Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");

    private static final Pattern PATRON_DATOS_CLINICOS =
            Pattern.compile("(?i)(valorPuntaje|indicePregunta|respuestas_evaluacion|item[\\s_]*9[\\s_]*valor[\\s_]*2)");

    private Logger rootLogger;
    private ListAppender<ILoggingEvent> appenderLogs;

    @BeforeEach
    void configurarCapturadorLogs() {
        rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        appenderLogs = new ListAppender<>();
        appenderLogs.start();
        rootLogger.addAppender(appenderLogs);
    }

    @AfterEach
    void limpiarCapturadorLogs() {
        if (rootLogger != null && appenderLogs != null) {
            rootLogger.detachAppender(appenderLogs);
            appenderLogs.stop();
        }
    }

    @Test
    @DisplayName("Debe garantizar cero presencia de correos, nombres y respuestas clinicas en logs durante registro y respuestas")
    void noDebeRegistrarCorreosNiNombresEnFlujoDeRegistroYRespuestas(CapturedOutput salidaConsola) throws Exception {
        String nombreSensible = "Alejandro Maria Del Carmen Sanchez";
        String correoSensible = "alejandro.sanchez.privado77@dominio-sensible.com";
        String contrasenaSensible = "ClaveUltraSegura99$#";

        // 1. Registro de usuario
        MvcResult resRegistro = mockMvc.perform(post("/api/autenticacion/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nombreCompleto", nombreSensible,
                                "correo", correoSensible,
                                "contrasena", contrasenaSensible))))
                .andExpect(status().isCreated())
                .andReturn();
        long usuarioId = leer(resRegistro).get("id").asLong();

        // 2. Inicio de sesion
        MvcResult resLogin = mockMvc.perform(post("/api/autenticacion/inicio-sesion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "correo", correoSensible,
                                "contrasena", contrasenaSensible))))
                .andExpect(status().isOk())
                .andReturn();
        String token = tokenAcceso(resLogin);

        // 3. Registrar consentimiento
        mockMvc.perform(post("/api/consentimientos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("confirmaMayoriaEdad", true))))
                .andExpect(status().isCreated());

        // 4. Crear evaluacion
        MvcResult resCrear = mockMvc.perform(post("/api/triaje/evaluaciones")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isCreated())
                .andReturn();
        String evaluacionId = leer(resCrear).get("id").asText();

        // 5. Guardar respuesta clinica sensible (item 9 con valor 2)
        mockMvc.perform(put("/api/triaje/evaluaciones/" + evaluacionId + "/respuestas")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "instrumento", "PHQ9",
                                "indicePregunta", 9,
                                "valorPuntaje", 2))))
                .andExpect(status().isOk());

        // Recolectar todos los logs capturados
        String registrosLog = appenderLogs.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.joining("\n"));

        String salidaCompleta = registrosLog + "\n" + (salidaConsola != null ? salidaConsola.getAll() : "");

        // Asercion 1: Ningun correo electronico debe aparecer en logs
        Matcher matcherCorreo = PATRON_CORREO.matcher(salidaCompleta);
        assertFalse(matcherCorreo.find(),
                () -> "Se detecto un correo electronico en los logs de la aplicacion: " + matcherCorreo.group());
        assertThat(salidaCompleta).doesNotContain(correoSensible);

        // Asercion 2: El nombre completo de la persona nunca debe figurar en logs
        assertThat(salidaCompleta).doesNotContain(nombreSensible);
        Pattern patronNombre = Pattern.compile("(?i)\\bAlejandro\\b|\\bDel Carmen\\b");
        assertFalse(patronNombre.matcher(salidaCompleta).find(),
                "Se detectaron partes del nombre de la persona en los logs");

        // Asercion 3: La contrasena nunca debe imprimirse en los logs
        assertThat(salidaCompleta).doesNotContain(contrasenaSensible);

        // Asercion 4: Ningun valor de respuesta individual ni indicador clinico debe exponerse
        Matcher matcherClinico = PATRON_DATOS_CLINICOS.matcher(salidaCompleta);
        assertFalse(matcherClinico.find(),
                () -> "Se detecto contenido o valor de respuesta clinica en logs: " + matcherClinico.group());
        assertThat(salidaCompleta).doesNotContain("\"valorPuntaje\":2");
        assertThat(salidaCompleta).doesNotContain("\"indicePregunta\":9");

        // Asercion 5: Se permite registrar identificadores opacos y metadatos tecnicos
        Pattern patronMetadatoTecnico = Pattern.compile("(?i)Evaluacion iniciada exitosamente\\. Id: " + evaluacionId);
        assertTrue(patronMetadatoTecnico.matcher(salidaCompleta).find(),
                "Debe registrarse el identificador opaco de la evaluacion como metadato tecnico");
        assertThat(salidaCompleta).contains("Nuevo usuario registrado exitosamente. Id: " + usuarioId);
    }
}
