package com.triaje.aplicacion.casouso;

import com.triaje.aplicacion.dto.DatosCitaPaciente;
import com.triaje.aplicacion.puerto.RepositorioCitas;
import com.triaje.aplicacion.puerto.RepositorioUsuarios;
import com.triaje.dominio.Cita;
import com.triaje.dominio.modelo.Usuario;

import java.util.List;
import java.util.Objects;

/**
 * Caso de uso: el paciente lista sus citas agendadas o propuestas.
 * Devuelve id, fechaHora, modalidad, mensaje, estado y nombreClinico. Nunca expone el correo.
 */
public class ListarCitasPaciente {

    private final RepositorioCitas repositorioCitas;
    private final RepositorioUsuarios repositorioUsuarios;

    public ListarCitasPaciente(RepositorioCitas repositorioCitas, RepositorioUsuarios repositorioUsuarios) {
        this.repositorioCitas = Objects.requireNonNull(repositorioCitas);
        this.repositorioUsuarios = Objects.requireNonNull(repositorioUsuarios);
    }

    public List<DatosCitaPaciente> ejecutar(Long pacienteId) {
        List<Cita> citas = repositorioCitas.listarPorPacienteId(pacienteId);
        return citas.stream()
                .map(cita -> {
                    String nombreClinico = repositorioUsuarios.buscarPorId(cita.getClinicoId())
                            .map(Usuario::getNombreCompleto)
                            .orElse("Clinico");
                    return DatosCitaPaciente.desde(cita, nombreClinico);
                })
                .toList();
    }
}
