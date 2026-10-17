package com.triaje.aplicacion.casouso;

import com.triaje.aplicacion.dto.DatosEvaluacion;
import com.triaje.aplicacion.puerto.EjecutorTransaccional;
import com.triaje.aplicacion.puerto.RepositorioAutorizaciones;
import com.triaje.aplicacion.puerto.RepositorioCitas;
import com.triaje.aplicacion.puerto.RepositorioConsentimientos;
import com.triaje.aplicacion.puerto.RepositorioEvaluaciones;
import com.triaje.dominio.Autorizacion;
import com.triaje.dominio.Cita;
import com.triaje.dominio.EstadoCita;
import com.triaje.dominio.excepcion.ExcepcionAccesoDenegado;
import com.triaje.dominio.excepcion.ExcepcionDominio;
import com.triaje.dominio.modelo.Evaluacion;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Inicia una nueva evaluacion de cribado en estado EN_CURSO.
 * Exige que el usuario posea un consentimiento informado vigente.
 */
public class CrearEvaluacion {

    private final RepositorioEvaluaciones repositorioEvaluaciones;
    private final RepositorioConsentimientos repositorioConsentimientos;
    private final RepositorioCitas repositorioCitas;
    private final RepositorioAutorizaciones repositorioAutorizaciones;
    private final EjecutorTransaccional transacciones;
    private final Clock reloj;

    public CrearEvaluacion(RepositorioEvaluaciones repositorioEvaluaciones,
                           RepositorioConsentimientos repositorioConsentimientos,
                           RepositorioCitas repositorioCitas,
                           RepositorioAutorizaciones repositorioAutorizaciones,
                           EjecutorTransaccional transacciones,
                           Clock reloj) {
        this.repositorioEvaluaciones = Objects.requireNonNull(repositorioEvaluaciones);
        this.repositorioConsentimientos = Objects.requireNonNull(repositorioConsentimientos);
        this.repositorioCitas = Objects.requireNonNull(repositorioCitas);
        this.repositorioAutorizaciones = Objects.requireNonNull(repositorioAutorizaciones);
        this.transacciones = Objects.requireNonNull(transacciones);
        this.reloj = Objects.requireNonNull(reloj);
    }

    public DatosEvaluacion ejecutar(Long usuarioId) {
        return transacciones.ejecutar(() -> {
            boolean tieneConsentimientoVigente = repositorioConsentimientos.buscarVigentePorUsuarioId(usuarioId).isPresent();
            if (!tieneConsentimientoVigente) {
                throw new ExcepcionAccesoDenegado("Debes aceptar el consentimiento informado antes de iniciar una evaluación.");
            }

            if (repositorioCitas.existePorPacienteYEstados(usuarioId, List.of(EstadoCita.PROPUESTA, EstadoCita.CONFIRMADA))) {
                throw new ExcepcionDominio("No puedes iniciar una nueva evaluación mientras tengas una cita activa pendiente de atención.");
            }

            List<Autorizacion> autorizacionesVigentes = repositorioAutorizaciones.listarVigentesPorPacienteId(usuarioId);
            for (Autorizacion autorizacion : autorizacionesVigentes) {
                List<Cita> citasCumplidas = repositorioCitas.listarPorAutorizacionIdYEstado(autorizacion.getId(), EstadoCita.CUMPLIDA);
                if (citasCumplidas.isEmpty()) {
                    throw new ExcepcionDominio("No puedes iniciar una nueva evaluación mientras tengas una evaluación compartida en espera de atención clínica.");
                }
            }

            String id = UUID.randomUUID().toString();
            Instant ahora = Instant.now(reloj);
            Evaluacion evaluacion = Evaluacion.nueva(id, usuarioId, ahora);
            Evaluacion guardada = repositorioEvaluaciones.guardar(evaluacion);

            return DatosEvaluacion.desde(guardada, null);
        });
    }
}
