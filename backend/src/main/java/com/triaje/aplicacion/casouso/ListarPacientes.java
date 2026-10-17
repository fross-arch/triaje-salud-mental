package com.triaje.aplicacion.casouso;

import com.triaje.aplicacion.dto.DatosPacienteAdministracion;
import com.triaje.aplicacion.puerto.RepositorioUsuarios;
import com.triaje.dominio.RolUsuario;

import java.util.List;
import java.util.Objects;

/**
 * Caso de uso: el administrador lista el historial basico de pacientes registrados.
 * Aplica estricta minimizacion de datos: no expone detalles clinicos, diagnosticos ni respuestas.
 */
public class ListarPacientes {

    private final RepositorioUsuarios repositorioUsuarios;

    public ListarPacientes(RepositorioUsuarios repositorioUsuarios) {
        this.repositorioUsuarios = Objects.requireNonNull(repositorioUsuarios);
    }

    public List<DatosPacienteAdministracion> ejecutar() {
        return repositorioUsuarios.listarPorRol(RolUsuario.PACIENTE).stream()
                .map(DatosPacienteAdministracion::desde)
                .toList();
    }
}
