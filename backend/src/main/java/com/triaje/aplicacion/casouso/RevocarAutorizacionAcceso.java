package com.triaje.aplicacion.casouso;

import com.triaje.aplicacion.dto.DatosAutorizacion;
import com.triaje.aplicacion.puerto.EjecutorTransaccional;
import com.triaje.aplicacion.puerto.RepositorioAuditoria;
import com.triaje.aplicacion.puerto.RepositorioAutorizaciones;
import com.triaje.aplicacion.puerto.RepositorioEvaluaciones;
import com.triaje.aplicacion.puerto.RepositorioCitas;
import com.triaje.dominio.Autorizacion;
import com.triaje.dominio.Cita;
import com.triaje.dominio.EstadoCita;
import com.triaje.dominio.MaquinaEstadosCita;
import com.triaje.dominio.RolUsuario;
import com.triaje.dominio.excepcion.ExcepcionEvaluacionNoEncontrada;
import com.triaje.dominio.modelo.AccionAuditoria;
import com.triaje.dominio.modelo.Evaluacion;
import com.triaje.dominio.modelo.RegistroAuditoria;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Caso de uso: el paciente revoca el acceso a su evaluacion para un clinico especifico.
 * Cancela automaticamente todas las citas en estado PROPUESTA asociadas a esa autorizacion,
 * manteniendo las citas en estado CONFIRMADA.
 */
public class RevocarAutorizacionAcceso {

    private final RepositorioAutorizaciones repositorioAutorizaciones;
    private final RepositorioEvaluaciones repositorioEvaluaciones;
    private final RepositorioCitas repositorioCitas;
    private final RepositorioAuditoria repositorioAuditoria;
    private final MaquinaEstadosCita maquinaEstadosCita;
    private final EjecutorTransaccional transacciones;
    private final Clock reloj;

    public RevocarAutorizacionAcceso(RepositorioAutorizaciones repositorioAutorizaciones,
                                    RepositorioEvaluaciones repositorioEvaluaciones,
                                    RepositorioCitas repositorioCitas,
                                    RepositorioAuditoria repositorioAuditoria,
                                    MaquinaEstadosCita maquinaEstadosCita,
                                    EjecutorTransaccional transacciones,
                                    Clock reloj) {
        this.repositorioAutorizaciones = Objects.requireNonNull(repositorioAutorizaciones);
        this.repositorioEvaluaciones = Objects.requireNonNull(repositorioEvaluaciones);
        this.repositorioCitas = Objects.requireNonNull(repositorioCitas);
        this.repositorioAuditoria = Objects.requireNonNull(repositorioAuditoria);
        this.maquinaEstadosCita = Objects.requireNonNull(maquinaEstadosCita);
        this.transacciones = Objects.requireNonNull(transacciones);
        this.reloj = Objects.requireNonNull(reloj);
    }

    public DatosAutorizacion ejecutar(String evaluacionId, Long clinicoId, Long pacienteId) {
        return transacciones.ejecutar(() -> {
            Evaluacion evaluacion = repositorioEvaluaciones.buscarPorId(evaluacionId)
                    .orElseThrow(() -> new ExcepcionEvaluacionNoEncontrada("La evaluacion no existe o no tienes acceso a ella."));

            if (!evaluacion.getUsuarioId().equals(pacienteId)) {
                throw new ExcepcionEvaluacionNoEncontrada("La evaluacion no existe o no tienes acceso a ella.");
            }

            Autorizacion autorizacion = repositorioAutorizaciones.buscarPorEvaluacionYClinico(evaluacionId, clinicoId)
                    .orElseThrow(() -> new ExcepcionEvaluacionNoEncontrada("No existe autorizacion para el clinico en esta evaluacion."));

            Instant ahora = Instant.now(reloj);
            Autorizacion revocada = autorizacion.revocar(ahora);
            Autorizacion guardada = repositorioAutorizaciones.guardar(revocada);

            repositorioAuditoria.registrar(new RegistroAuditoria(
                    pacienteId,
                    AccionAuditoria.AUTORIZACION_REVOCADA,
                    evaluacionId,
                    ahora
            ));

            // Cancelar automaticamente todas las citas en estado PROPUESTA asociadas a esa autorizacion
            List<Cita> citasPropuestas = repositorioCitas.listarPorAutorizacionIdYEstado(autorizacion.getId(), EstadoCita.PROPUESTA);
            for (Cita cita : citasPropuestas) {
                Cita cancelada = cita.cancelar(RolUsuario.PACIENTE, ahora, maquinaEstadosCita);
                repositorioCitas.guardar(cancelada);

                repositorioAuditoria.registrar(new RegistroAuditoria(
                        pacienteId,
                        AccionAuditoria.CITA_CANCELADA,
                        cancelada.getId(),
                        ahora
                ));
            }

            return DatosAutorizacion.desde(guardada);
        });
    }
}
