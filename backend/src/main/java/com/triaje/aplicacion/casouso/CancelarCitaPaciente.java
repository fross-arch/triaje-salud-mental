package com.triaje.aplicacion.casouso;

import com.triaje.aplicacion.dto.DatosCitaPaciente;
import com.triaje.aplicacion.puerto.EjecutorTransaccional;
import com.triaje.aplicacion.puerto.RepositorioAuditoria;
import com.triaje.aplicacion.puerto.RepositorioCitas;
import com.triaje.aplicacion.puerto.RepositorioUsuarios;
import com.triaje.dominio.Cita;
import com.triaje.dominio.MaquinaEstadosCita;
import com.triaje.dominio.RolUsuario;
import com.triaje.dominio.excepcion.ExcepcionCitaNoEncontrada;
import com.triaje.dominio.modelo.AccionAuditoria;
import com.triaje.dominio.modelo.RegistroAuditoria;
import com.triaje.dominio.modelo.Usuario;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/**
 * Caso de uso: el paciente cancela una cita propia en estado PROPUESTA o CONFIRMADA.
 */
public class CancelarCitaPaciente {

    private final RepositorioCitas repositorioCitas;
    private final RepositorioUsuarios repositorioUsuarios;
    private final RepositorioAuditoria repositorioAuditoria;
    private final MaquinaEstadosCita maquinaEstadosCita;
    private final EjecutorTransaccional transacciones;
    private final Clock reloj;

    public CancelarCitaPaciente(RepositorioCitas repositorioCitas,
                                RepositorioUsuarios repositorioUsuarios,
                                RepositorioAuditoria repositorioAuditoria,
                                MaquinaEstadosCita maquinaEstadosCita,
                                EjecutorTransaccional transacciones,
                                Clock reloj) {
        this.repositorioCitas = Objects.requireNonNull(repositorioCitas);
        this.repositorioUsuarios = Objects.requireNonNull(repositorioUsuarios);
        this.repositorioAuditoria = Objects.requireNonNull(repositorioAuditoria);
        this.maquinaEstadosCita = Objects.requireNonNull(maquinaEstadosCita);
        this.transacciones = Objects.requireNonNull(transacciones);
        this.reloj = Objects.requireNonNull(reloj);
    }

    public DatosCitaPaciente ejecutar(String citaId, Long pacienteId) {
        return transacciones.ejecutar(() -> {
            Cita cita = repositorioCitas.buscarPorId(citaId)
                    .orElseThrow(() -> new ExcepcionCitaNoEncontrada("La cita no existe o no tienes acceso a ella."));

            if (!cita.getPacienteId().equals(pacienteId)) {
                throw new ExcepcionCitaNoEncontrada("La cita no existe o no tienes acceso a ella.");
            }

            Instant ahora = Instant.now(reloj);
            Cita cancelada = cita.cancelar(RolUsuario.PACIENTE, ahora, maquinaEstadosCita);
            Cita guardada = repositorioCitas.guardar(cancelada);

            repositorioAuditoria.registrar(new RegistroAuditoria(
                    pacienteId,
                    AccionAuditoria.CITA_CANCELADA,
                    guardada.getId(),
                    ahora
            ));

            String nombreClinico = repositorioUsuarios.buscarPorId(guardada.getClinicoId())
                    .map(Usuario::getNombreCompleto)
                    .orElse("Clinico");

            return DatosCitaPaciente.desde(guardada, nombreClinico);
        });
    }
}
