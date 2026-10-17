package com.triaje.aplicacion.casouso;

import com.triaje.aplicacion.dto.ComandoProponerCita;
import com.triaje.aplicacion.dto.DatosCitaClinico;
import com.triaje.aplicacion.puerto.EjecutorTransaccional;
import com.triaje.aplicacion.puerto.RepositorioAuditoria;
import com.triaje.aplicacion.puerto.RepositorioAutorizaciones;
import com.triaje.aplicacion.puerto.RepositorioCitas;
import com.triaje.dominio.Autorizacion;
import com.triaje.dominio.Cita;
import com.triaje.dominio.EstadoCita;
import com.triaje.dominio.MaquinaEstadosCita;
import com.triaje.dominio.excepcion.ExcepcionDatoInvalido;
import com.triaje.dominio.excepcion.ExcepcionDominio;
import com.triaje.dominio.excepcion.ExcepcionEvaluacionNoEncontrada;
import com.triaje.dominio.modelo.AccionAuditoria;
import com.triaje.dominio.modelo.RegistroAuditoria;

import com.triaje.aplicacion.puerto.RepositorioUsuarios;
import com.triaje.dominio.modelo.Usuario;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

/**
 * Caso de uso: el clinico propone una cita al paciente.
 * Verifica estrictamente en la misma peticion la existencia de autorizacion vigente.
 * Valida fecha futura y ausencia de citas en conflicto (PROPUESTA o CONFIRMADA) para ese clinico.
 */
public class ProponerCita {

    private final RepositorioCitas repositorioCitas;
    private final RepositorioAutorizaciones repositorioAutorizaciones;
    private final RepositorioUsuarios repositorioUsuarios;
    private final RepositorioAuditoria repositorioAuditoria;
    private final MaquinaEstadosCita maquinaEstadosCita;
    private final EjecutorTransaccional transacciones;
    private final Clock reloj;

    public ProponerCita(RepositorioCitas repositorioCitas,
                        RepositorioAutorizaciones repositorioAutorizaciones,
                        RepositorioUsuarios repositorioUsuarios,
                        RepositorioAuditoria repositorioAuditoria,
                        MaquinaEstadosCita maquinaEstadosCita,
                        EjecutorTransaccional transacciones,
                        Clock reloj) {
        this.repositorioCitas = Objects.requireNonNull(repositorioCitas);
        this.repositorioAutorizaciones = Objects.requireNonNull(repositorioAutorizaciones);
        this.repositorioUsuarios = Objects.requireNonNull(repositorioUsuarios);
        this.repositorioAuditoria = Objects.requireNonNull(repositorioAuditoria);
        this.maquinaEstadosCita = Objects.requireNonNull(maquinaEstadosCita);
        this.transacciones = Objects.requireNonNull(transacciones);
        this.reloj = Objects.requireNonNull(reloj);
    }

    public DatosCitaClinico ejecutar(ComandoProponerCita comando) {
        return transacciones.ejecutar(() -> {
            Instant ahora = Instant.now(reloj);

            if (comando.fechaHora() == null || !comando.fechaHora().isAfter(ahora)) {
                throw new ExcepcionDatoInvalido("La fecha y hora de la cita debe ser futura.");
            }

            Autorizacion autorizacion = repositorioAutorizaciones
                    .buscarVigentePorEvaluacionYClinico(comando.evaluacionId(), comando.clinicoId())
                    .orElseThrow(() -> new ExcepcionEvaluacionNoEncontrada(
                            "La evaluacion no existe o no tienes autorizacion vigente para acceder a ella."));

            Instant fechaNormalizada = comando.fechaHora().truncatedTo(ChronoUnit.SECONDS);

            boolean enConflicto = repositorioCitas.existePorClinicoYFechaHoraYEstados(
                    comando.clinicoId(),
                    fechaNormalizada,
                    List.of(EstadoCita.PROPUESTA, EstadoCita.CONFIRMADA)
            );

            if (enConflicto) {
                throw new ExcepcionDominio("El clinico ya tiene una cita propuesta o confirmada en esa fecha y hora exacta.");
            }

            boolean citaActivaExiste = repositorioCitas.existePorPacienteYClinicoYEstados(
                    autorizacion.getPacienteId(),
                    comando.clinicoId(),
                    List.of(EstadoCita.PROPUESTA, EstadoCita.CONFIRMADA)
            );

            if (citaActivaExiste) {
                throw new ExcepcionDominio("Ya existe una cita activa (propuesta o confirmada) entre este paciente y clinico.");
            }

            Cita nueva = Cita.proponer(
                    autorizacion.getId(),
                    autorizacion.getPacienteId(),
                    comando.clinicoId(),
                    fechaNormalizada,
                    comando.modalidad(),
                    comando.mensaje(),
                    ahora,
                    maquinaEstadosCita
            );

            Cita guardada = repositorioCitas.guardar(nueva);

            repositorioAuditoria.registrar(new RegistroAuditoria(
                    comando.clinicoId(),
                    AccionAuditoria.CITA_PROPUESTA,
                    guardada.getId(),
                    ahora
            ));

            String nombrePaciente = repositorioUsuarios.buscarPorId(autorizacion.getPacienteId())
                    .map(Usuario::getNombreCompleto)
                    .orElse("Paciente");

            return DatosCitaClinico.desde(guardada, nombrePaciente);
        });
    }
}
