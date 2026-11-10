package com.Cesde.triaje.Repositorio;

import java.time.Instant;

/**
 * Proyección de sólo lectura para fecha de creación.
 * Garantiza privacidad clínica estricta: sólo fecha, sin datos personales.
 */
public interface IFechaCreacionProjection {

    Instant getFechaCreacion();
}
