package com.triaje.infraestructura.web.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.triaje.infraestructura.web.dto.RespuestaError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.List;

/**
 * Construye y escribe respuestas de error con el formato uniforme.
 */
@Component
public class FabricaRespuestaError {

    private final ObjectMapper mapeador;
    private final Clock reloj;

    public FabricaRespuestaError(ObjectMapper mapeador, Clock reloj) {
        this.mapeador = mapeador;
        this.reloj = reloj;
    }

    public RespuestaError crear(HttpStatus estado, String codigo, String mensaje, String ruta,
                                List<RespuestaError.DetalleError> detalles) {
        return new RespuestaError(reloj.instant(), estado.value(), codigo, mensaje, ruta, detalles);
    }

    public void escribir(HttpServletRequest solicitud, HttpServletResponse respuesta, HttpStatus estado,
                         String codigo, String mensaje) throws IOException {
        respuesta.setStatus(estado.value());
        respuesta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        respuesta.setCharacterEncoding(StandardCharsets.UTF_8.name());
        mapeador.writeValue(respuesta.getOutputStream(),
                crear(estado, codigo, mensaje, solicitud.getRequestURI(), List.of()));
    }
}
