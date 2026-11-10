package com.Cesde.triaje.Repositorio;

/**
 * Proyección basada en interfaz para listar clínicos activos (estilo concesionario).
 */
public interface IClinicoActivoProjection {
    Long getId();
    String getNombreCompleto();
    String getEspecialidad();
}
