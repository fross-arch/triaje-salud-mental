package com.triaje.aplicacion.casouso;

import com.triaje.aplicacion.dto.DatosCitaClinico;
import com.triaje.aplicacion.puerto.EjecutorTransaccional;
import com.triaje.aplicacion.puerto.RepositorioAuditoria;
import com.triaje.aplicacion.puerto.RepositorioCitas;
import com.triaje.aplicacion.puerto.RepositorioUsuarios;
import com.triaje.dominio.Cita;
import com.triaje.dominio.MaquinaEstadosCita;
import com.triaje.dominio.excepcion.ExcepcionCitaNoEncontrada;
import com.triaje.dominio.modelo.AccionAuditoria;
import com.triaje.dominio.modelo.RegistroAuditoria;
import com.triaje.dominio.modelo.Usuario;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/**
 * Caso de uso: el clinico cumple una cita en estado PROPUESTA o CONFIRMADA registrando observaciones clinicas.
 */
public class CumplirCitaClinico {

    private final RepositorioCitas repositorioCitas;
    private final RepositorioUsuarios repositorioUsuarios;
    private final RepositorioAuditoria repositorioAuditoria;
    private final MaquinaEstadosCita maquinaEstadosCita;
    private final EjecutorTransaccional transacciones;
    private final Clock reloj;

    public CumplirCitaClinico(RepositorioCitas repositorioCitas,
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

    public DatosCitaClinico ejecutar(String citaId, Long clinicoId, String observaciones) {
        return transacciones.ejecutar(() -> {
            Cita cita = repositorioCitas.buscarPorId(citaId)
                    .orElseThrow(() -> new ExcepcionCitaNoEncontrada("La cita no existe o no tienes acceso a ella."));

            if (!cita.getClinicoId().equals(clinicoId)) {
                throw new ExcepcionCitaNoEncontrada("La cita no existe o no tienes acceso a ella.");
            }

            Instant ahora = Instant.now(reloj);
            Cita cumplida = cita.cumplir(observaciones, ahora, maquinaEstadosCita);
            Cita guardada = repositorioCitas.guardar(cumplida);

            repositorioAuditoria.registrar(new RegistroAuditoria(
                    clinicoId,
                    AccionAuditoria.CITA_CUMPLIDA,
                    guardada.getId(),
                    ahora
            ));

            String nombrePaciente = repositorioUsuarios.buscarPorId(guardada.getPacienteId())
                    .map(Usuario::getNombreCompleto)
                    .orElse("Paciente");

            return DatosCitaClinico.desde(guardada, nombrePaciente);
        });
    }
}
