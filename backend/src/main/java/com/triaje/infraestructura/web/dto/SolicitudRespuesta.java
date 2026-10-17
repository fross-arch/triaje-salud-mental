package com.triaje.infraestructura.web.dto;

import com.triaje.dominio.modelo.InstrumentoCribado;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SolicitudRespuesta(
        @NotNull(message = "El instrumento es obligatorio.")
        InstrumentoCribado instrumento,

        @NotNull(message = "El índice de la pregunta es obligatorio.")
        @Min(value = 1, message = "El índice de la pregunta debe ser al menos 1.")
        @Max(value = 9, message = "El índice de la pregunta no puede superar 9.")
        Integer indicePregunta,

        @NotNull(message = "El valor del puntaje es obligatorio.")
        @Min(value = 0, message = "El puntaje debe ser al menos 0.")
        @Max(value = 3, message = "El puntaje no puede ser mayor a 3.")
        Integer valorPuntaje
) {
}
