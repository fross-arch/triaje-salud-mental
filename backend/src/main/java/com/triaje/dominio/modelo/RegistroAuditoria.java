package com.triaje.dominio.modelo;

import java.time.Instant;
import java.util.Objects;

/**
 * Evento de auditoría. Solo contiene identificadores, nunca datos personales ni respuestas.
 *
 * @param usuarioId identificador del usuario (puede ser nulo, por ejemplo tras eliminar la cuenta)
 * @param accion    acción registrada
 * @param entidadId identificador opcional de la entidad afectada
 * @param fecha     momento del evento
 */
public record RegistroAuditoria(Long usuarioId, AccionAuditoria accion, String entidadId, Instant fecha) {

    public RegistroAuditoria {
        Objects.requireNonNull(accion, "La acción de auditoría es obligatoria");
        Objects.requireNonNull(fecha, "La fecha de auditoría es obligatoria");
        if (entidadId != null && entidadId.length() > 36) {
            throw new IllegalArgumentException("El identificador de entidad no puede superar 36 caracteres");
        }
    }
}
