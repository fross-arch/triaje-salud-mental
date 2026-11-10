package com.Cesde.triaje.Repositorio;

import com.Cesde.triaje.Modelo.Enumeracion.SeveridadAnsiedad;
import com.Cesde.triaje.Modelo.Enumeracion.SeveridadDepresion;

import java.time.Instant;

/**
 * Proyección basada en interfaz para evaluaciones anónimas.
 * Excluye estrictamente cualquier identificador de usuario o de evaluación
 * para garantizar las reglas de privacidad y confidencialidad clínica.
 */
public interface IEvaluacionAnonimaProjection {
    Instant getFechaFinalizacion();
    Integer getPuntajePhq9();
    Integer getPuntajeGad7();
    SeveridadDepresion getSeveridadDepresion();
    SeveridadAnsiedad getSeveridadAnsiedad();
    boolean isRiesgoCrisis();
}
