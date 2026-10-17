package com.triaje.infraestructura.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * Formato uniforme de error de toda la API.
 *
 * @param fecha    momento del error
 * @param estado   código HTTP
 * @param error    código estable y legible por máquina (por ejemplo CREDENCIALES_INVALIDAS)
 * @param mensaje  mensaje en español para la persona usuaria
 * @param ruta     ruta solicitada
 * @param detalles errores por campo, si aplica
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record RespuestaError(Instant fecha, int estado, String error, String mensaje, String ruta,
                             List<DetalleError> detalles) {

    public record DetalleError(String campo, String mensaje) {
    }
}
