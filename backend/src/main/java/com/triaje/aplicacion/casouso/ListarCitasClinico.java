package com.triaje.aplicacion.casouso;

import com.triaje.aplicacion.dto.DatosCitaClinico;
import com.triaje.aplicacion.puerto.RepositorioCitas;
import com.triaje.aplicacion.puerto.RepositorioUsuarios;
import com.triaje.dominio.Cita;
import com.triaje.dominio.modelo.Usuario;

import java.util.List;
import java.util.Objects;

/**
 * Caso de uso: el clinico lista sus citas asignadas.
 * Devuelve id, fechaHora, modalidad, mensaje, estado y nombrePaciente. Nunca expone el correo.
 */
public class ListarCitasClinico {

    private final RepositorioCitas repositorioCitas;
    private final RepositorioUsuarios repositorioUsuarios;

    public ListarCitasClinico(RepositorioCitas repositorioCitas, RepositorioUsuarios repositorioUsuarios) {
        this.repositorioCitas = Objects.requireNonNull(repositorioCitas);
        this.repositorioUsuarios = Objects.requireNonNull(repositorioUsuarios);
    }

    public List<DatosCitaClinico> ejecutar(Long clinicoId) {
        List<Cita> citas = repositorioCitas.listarPorClinicoId(clinicoId);
        return citas.stream()
                .map(cita -> {
                    String nombrePaciente = repositorioUsuarios.buscarPorId(cita.getPacienteId())
                            .map(Usuario::getNombreCompleto)
                            .orElse("Paciente");
                    return DatosCitaClinico.desde(cita, nombrePaciente);
                })
                .toList();
    }
}
