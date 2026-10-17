package com.triaje.aplicacion.casouso;

import com.triaje.aplicacion.puerto.EjecutorTransaccional;
import com.triaje.aplicacion.puerto.RepositorioAuditoria;
import com.triaje.aplicacion.puerto.RepositorioAutorizaciones;
import com.triaje.aplicacion.puerto.RepositorioCitas;
import com.triaje.aplicacion.puerto.RepositorioConsentimientos;
import com.triaje.aplicacion.puerto.RepositorioEvaluaciones;
import com.triaje.dominio.Autorizacion;
import com.triaje.dominio.Cita;
import com.triaje.dominio.EstadoCita;
import com.triaje.dominio.MaquinaEstadosCita;
import com.triaje.dominio.RolUsuario;
import com.triaje.dominio.excepcion.ExcepcionDominio;
import com.triaje.dominio.modelo.AccionAuditoria;
import com.triaje.dominio.modelo.Consentimiento;
import com.triaje.dominio.modelo.EstadoEvaluacion;
import com.triaje.dominio.modelo.RegistroAuditoria;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Revoca el consentimiento informado del usuario autenticado y en cascada
 * revoca de forma automatica todas las autorizaciones vigentes del paciente,
 * cancelando automaticamente todas las citas en estado PROPUESTA asociadas a dichas autorizaciones.
 * Las citas en estado CONFIRMADA se mantienen.
 */
public class RevocarConsentimiento {

    private final RepositorioConsentimientos repositorioConsentimientos;
    private final RepositorioAutorizaciones repositorioAutorizaciones;
    private final RepositorioCitas repositorioCitas;
    private final RepositorioEvaluaciones repositorioEvaluaciones;
    private final RepositorioAuditoria repositorioAuditoria;
    private final MaquinaEstadosCita maquinaEstadosCita;
    private final EjecutorTransaccional transacciones;
    private final Clock reloj;

    public RevocarConsentimiento(RepositorioConsentimientos repositorioConsentimientos,
                                 RepositorioAutorizaciones repositorioAutorizaciones,
                                 RepositorioCitas repositorioCitas,
                                 RepositorioEvaluaciones repositorioEvaluaciones,
                                 RepositorioAuditoria repositorioAuditoria,
                                 MaquinaEstadosCita maquinaEstadosCita,
                                 EjecutorTransaccional transacciones,
                                 Clock reloj) {
        this.repositorioConsentimientos = Objects.requireNonNull(repositorioConsentimientos);
        this.repositorioAutorizaciones = Objects.requireNonNull(repositorioAutorizaciones);
        this.repositorioCitas = Objects.requireNonNull(repositorioCitas);
        this.repositorioEvaluaciones = Objects.requireNonNull(repositorioEvaluaciones);
        this.repositorioAuditoria = Objects.requireNonNull(repositorioAuditoria);
        this.maquinaEstadosCita = Objects.requireNonNull(maquinaEstadosCita);
        this.transacciones = Objects.requireNonNull(transacciones);
        this.reloj = Objects.requireNonNull(reloj);
    }

    public boolean ejecutar(Long usuarioId) {
        return transacciones.ejecutar(() -> {
            if (repositorioEvaluaciones.existePorUsuarioIdYEstado(usuarioId, EstadoEvaluacion.EN_CURSO)) {
                throw new ExcepcionDominio("Hay una evaluación en curso; debes finalizarla o cancelarla antes de revocar el consentimiento");
            }

            var consentimientoOpt = repositorioConsentimientos.buscarVigentePorUsuarioId(usuarioId);
            if (consentimientoOpt.isEmpty()) {
                return false;
            }

            Consentimiento consentimiento = consentimientoOpt.get();
            Instant ahora = Instant.now(reloj);
            consentimiento.revocar(ahora);
            repositorioConsentimientos.guardar(consentimiento);

            repositorioAuditoria.registrar(new RegistroAuditoria(
                    usuarioId,
                    AccionAuditoria.CONSENTIMIENTO_REVOCADO,
                    String.valueOf(consentimiento.getId()),
                    ahora
            ));

            List<Autorizacion> autorizacionesVigentes = repositorioAutorizaciones.listarVigentesPorPacienteId(usuarioId);
            for (Autorizacion autorizacion : autorizacionesVigentes) {
                repositorioAuditoria.registrar(new RegistroAuditoria(
                        usuarioId,
                        AccionAuditoria.AUTORIZACION_REVOCADA,
                        autorizacion.getEvaluacionId(),
                        ahora
                ));

                // Cancelar automaticamente citas PROPUESTA asociadas a cada autorizacion revocada
                List<Cita> citasPropuestas = repositorioCitas.listarPorAutorizacionIdYEstado(autorizacion.getId(), EstadoCita.PROPUESTA);
                for (Cita cita : citasPropuestas) {
                    Cita cancelada = cita.cancelar(RolUsuario.PACIENTE, ahora, maquinaEstadosCita);
                    repositorioCitas.guardar(cancelada);

                    repositorioAuditoria.registrar(new RegistroAuditoria(
                            usuarioId,
                            AccionAuditoria.CITA_CANCELADA,
                            cancelada.getId(),
                            ahora
                    ));
                }
            }
            repositorioAutorizaciones.revocarVigentesPorPacienteId(usuarioId, ahora);

            return true;
        });
    }
}
