package com.triaje.aplicacion.casouso;

import com.triaje.aplicacion.dto.ComandoPosponerCita;
import com.triaje.aplicacion.dto.DatosCitaClinico;
import com.triaje.aplicacion.puerto.EjecutorTransaccional;
import com.triaje.aplicacion.puerto.RepositorioAuditoria;
import com.triaje.aplicacion.puerto.RepositorioCitas;
import com.triaje.aplicacion.puerto.RepositorioUsuarios;
import com.triaje.dominio.Cita;
import com.triaje.dominio.EstadoCita;
import com.triaje.dominio.MaquinaEstadosCita;
import com.triaje.dominio.excepcion.ExcepcionCitaNoEncontrada;
import com.triaje.dominio.excepcion.ExcepcionDominio;
import com.triaje.dominio.modelo.AccionAuditoria;
import com.triaje.dominio.modelo.RegistroAuditoria;
import com.triaje.dominio.modelo.Usuario;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

/**
 * Caso de uso: el clinico pospone/reprograma una cita propia en estado PROPUESTA o CONFIRMADA.
 * Valida fecha futura y ausencia de conflicto de horario con otras citas del clinico.
 */
public class PosponerCitaClinico {

    private final RepositorioCitas repositorioCitas;
    private final RepositorioUsuarios repositorioUsuarios;
    private final RepositorioAuditoria repositorioAuditoria;
    private final MaquinaEstadosCita maquinaEstadosCita;
    private final EjecutorTransaccional transacciones;
    private final Clock reloj;

    public PosponerCitaClinico(RepositorioCitas repositorioCitas,
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

    public DatosCitaClinico ejecutar(ComandoPosponerCita comando) {
        return transacciones.ejecutar(() -> {
            Cita cita = repositorioCitas.buscarPorId(comando.citaId())
                    .orElseThrow(() -> new ExcepcionCitaNoEncontrada("La cita no existe o no tienes acceso a ella."));

            if (!cita.getClinicoId().equals(comando.clinicoId())) {
                throw new ExcepcionCitaNoEncontrada("La cita no existe o no tienes acceso a ella.");
            }

            Instant ahora = Instant.now(reloj);
            Instant fechaNormalizada = comando.nuevaFechaHora() != null
                    ? comando.nuevaFechaHora().truncatedTo(ChronoUnit.SECONDS)
                    : null;

            boolean enConflicto = repositorioCitas.existePorClinicoYFechaHoraYEstadosYIdDistinto(
                    comando.clinicoId(),
                    fechaNormalizada,
                    List.of(EstadoCita.PROPUESTA, EstadoCita.CONFIRMADA),
                    comando.citaId()
            );

            if (enConflicto) {
                throw new ExcepcionDominio("El clinico ya tiene una cita propuesta o confirmada en esa fecha y hora exacta.");
            }

            Cita pospuesta = cita.posponer(fechaNormalizada, comando.observaciones(), ahora, maquinaEstadosCita);
            Cita guardada = repositorioCitas.guardar(pospuesta);

            repositorioAuditoria.registrar(new RegistroAuditoria(
                    comando.clinicoId(),
                    AccionAuditoria.CITA_POSPUESTA,
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
