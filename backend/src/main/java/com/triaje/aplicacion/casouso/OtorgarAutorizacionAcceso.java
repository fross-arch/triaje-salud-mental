package com.triaje.aplicacion.casouso;

import com.triaje.aplicacion.dto.ComandoOtorgarAutorizacion;
import com.triaje.aplicacion.dto.DatosAutorizacion;
import com.triaje.aplicacion.puerto.EjecutorTransaccional;
import com.triaje.aplicacion.puerto.RepositorioAuditoria;
import com.triaje.aplicacion.puerto.RepositorioAutorizaciones;
import com.triaje.aplicacion.puerto.RepositorioConsentimientos;
import com.triaje.aplicacion.puerto.RepositorioEvaluaciones;
import com.triaje.aplicacion.puerto.RepositorioUsuarios;
import com.triaje.dominio.Autorizacion;
import com.triaje.dominio.RolUsuario;
import com.triaje.dominio.excepcion.ExcepcionDatoInvalido;
import com.triaje.dominio.excepcion.ExcepcionDominio;
import com.triaje.dominio.excepcion.ExcepcionEvaluacionIncompleta;
import com.triaje.dominio.excepcion.ExcepcionEvaluacionNoEncontrada;
import com.triaje.dominio.excepcion.ExcepcionUsuarioNoEncontrado;
import com.triaje.dominio.modelo.AccionAuditoria;
import com.triaje.dominio.modelo.EstadoEvaluacion;
import com.triaje.dominio.modelo.Evaluacion;
import com.triaje.dominio.modelo.RegistroAuditoria;
import com.triaje.dominio.modelo.Usuario;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Caso de uso: el paciente autoriza el acceso a una evaluacion completada a un clinico activo.
 */
public class OtorgarAutorizacionAcceso {

    private final RepositorioAutorizaciones repositorioAutorizaciones;
    private final RepositorioEvaluaciones repositorioEvaluaciones;
    private final RepositorioConsentimientos repositorioConsentimientos;
    private final RepositorioUsuarios repositorioUsuarios;
    private final RepositorioAuditoria repositorioAuditoria;
    private final EjecutorTransaccional transacciones;
    private final Clock reloj;

    public OtorgarAutorizacionAcceso(RepositorioAutorizaciones repositorioAutorizaciones,
                                    RepositorioEvaluaciones repositorioEvaluaciones,
                                    RepositorioConsentimientos repositorioConsentimientos,
                                    RepositorioUsuarios repositorioUsuarios,
                                    RepositorioAuditoria repositorioAuditoria,
                                    EjecutorTransaccional transacciones,
                                    Clock reloj) {
        this.repositorioAutorizaciones = Objects.requireNonNull(repositorioAutorizaciones);
        this.repositorioEvaluaciones = Objects.requireNonNull(repositorioEvaluaciones);
        this.repositorioConsentimientos = Objects.requireNonNull(repositorioConsentimientos);
        this.repositorioUsuarios = Objects.requireNonNull(repositorioUsuarios);
        this.repositorioAuditoria = Objects.requireNonNull(repositorioAuditoria);
        this.transacciones = Objects.requireNonNull(transacciones);
        this.reloj = Objects.requireNonNull(reloj);
    }

    public DatosAutorizacion ejecutar(ComandoOtorgarAutorizacion comando) {
        return transacciones.ejecutar(() -> {
            Evaluacion evaluacion = repositorioEvaluaciones.buscarPorId(comando.evaluacionId())
                    .orElseThrow(() -> new ExcepcionEvaluacionNoEncontrada("La evaluacion no existe o no tienes acceso a ella."));

            if (!evaluacion.getUsuarioId().equals(comando.pacienteId())) {
                throw new ExcepcionEvaluacionNoEncontrada("La evaluacion no existe o no tienes acceso a ella.");
            }

            if (evaluacion.getEstado() != EstadoEvaluacion.COMPLETADA) {
                throw new ExcepcionEvaluacionIncompleta("Solo se pueden autorizar evaluaciones en estado COMPLETADA.");
            }

            var consentimientoOpt = repositorioConsentimientos.buscarVigentePorUsuarioId(comando.pacienteId());
            if (consentimientoOpt.isEmpty() || !consentimientoOpt.get().estaVigente()) {
                throw new ExcepcionDominio("El consentimiento informado debe estar vigente para otorgar autorizaciones.");
            }

            Usuario clinico = repositorioUsuarios.buscarPorId(comando.clinicoId())
                    .orElseThrow(() -> new ExcepcionUsuarioNoEncontrado("El clinico no existe."));

            if (clinico.getRol() != RolUsuario.CLINICO || !clinico.isActivo()) {
                throw new ExcepcionDatoInvalido("El profesional de la salud debe tener rol CLINICO y encontrarse activo.");
            }

            Instant ahora = Instant.now(reloj);
            Optional<Autorizacion> previaOpt = repositorioAutorizaciones.buscarPorEvaluacionYClinico(
                    comando.evaluacionId(), comando.clinicoId());

            Autorizacion autorizacionAGuardar;
            if (previaOpt.isPresent()) {
                autorizacionAGuardar = previaOpt.get().otorgar(ahora);
            } else {
                autorizacionAGuardar = Autorizacion.nueva(
                        comando.evaluacionId(),
                        comando.pacienteId(),
                        comando.clinicoId(),
                        Autorizacion.VERSION_TEXTO_PREDETERMINADA,
                        ahora
                );
            }

            Autorizacion guardada = repositorioAutorizaciones.guardar(autorizacionAGuardar);

            repositorioAuditoria.registrar(new RegistroAuditoria(
                    comando.pacienteId(),
                    AccionAuditoria.AUTORIZACION_OTORGADA,
                    comando.evaluacionId(),
                    ahora
            ));

            return DatosAutorizacion.desde(guardada);
        });
    }
}
