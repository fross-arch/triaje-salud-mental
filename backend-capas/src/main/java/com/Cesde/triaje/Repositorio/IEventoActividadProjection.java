package com.Cesde.triaje.Repositorio;

import com.Cesde.triaje.Modelo.Enumeracion.AccionAuditoria;

import java.time.Instant;

/**
 * Proyección de sólo lectura para eventos agregados de actividad del sistema.
 * Garantiza privacidad clínica estricta: NO expone IDs de usuario, entidades ni detalles individuales.
 */
public interface IEventoActividadProjection {

    AccionAuditoria getAccion();

    Instant getFechaCreacion();
}
